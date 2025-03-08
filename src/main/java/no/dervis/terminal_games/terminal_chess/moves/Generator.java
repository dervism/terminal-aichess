package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.attacks.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Generator implements Chess {

    private final Bitboard board;

    public Generator(Bitboard board) {
        this.board = board;
    }

    public List<Integer> generateMoves(int color) {
        List<Integer> moves = new ArrayList<>();
        long enPassantTarget = getEnPassantTarget(Objects.requireNonNullElse(board.history().peekLast(), 0));

        generateKingMoves(moves, color);
        generatePawnMoves(moves, color, enPassantTarget);
        generateKnightMoves(moves, color);
        generateSlidingPieceMoves(moves, color);

        return filterLegalMoves(moves, color);
    }

    public List<Integer> filterLegalMoves(List<Integer> moves, int color) {
        List<Integer> legalMoves = new ArrayList<>();
        int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(color));

        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            if (!isKingInCheck(color, kingSquare)) {
                legalMoves.add(move);
            }

        }
        return legalMoves;
    }

    public long generateMovesAttacks(int color) {
        List<Integer> moves = new ArrayList<>();
        long enPassantTarget = getEnPassantTarget(Objects.requireNonNullElse(board.history().peekLast(), 0));

        generateKingMoves(moves, color);
        generatePawnMoves(moves, color, enPassantTarget);
        generateKnightMoves(moves, color);

        long movesAttacks = 0;
        for (Integer move : moves) {
            movesAttacks |= Move.createMove(move).left();
        }

        return movesAttacks;
    }

    boolean isKingInCheck(int color, int kingSquare) {
        // Get attack masks for all piece types
        long rookAttacksMask = RookAttacks.getAllRookAttacks(kingSquare);
        long bishopAttacksMask = BishopAttacks.getAllBishopAttacks(kingSquare);
        long knightAttacksMask = KnightAttacks.getAllKnightAttacks(kingSquare);
        long pawnAttacksMask = PawnAttacks.getAllPawnAttacks(kingSquare, color);
        long kingAttacksMask = KingAttacks.getAllKingAttacks(kingSquare);

        // Get opponent pieces
        long opponentRooks = board.getRooks(1 - color);
        long opponentBishops = board.getBishops(1 - color);
        long opponentQueens = board.getQueens(1 - color);
        long opponentKnights = board.getKnights(1 - color);
        long opponentPawns = board.getPawns(1 - color);
        long opponentKing = board.kingPiece(1 - color);

        // Get all pieces on the board
        long allPieces = board.allPieces();

        // Check for attacks from non-sliding pieces (knight, pawn, king)
        if ((knightAttacksMask & opponentKnights) != 0) return true;
        if ((pawnAttacksMask & opponentPawns) != 0) return true;
        if ((kingAttacksMask & opponentKing) != 0) return true;

        // Check for rook attacks (including queen's rook-like moves)
        long rookAttackers = rookAttacksMask & (opponentRooks | opponentQueens);
        while (rookAttackers != 0) {
            int attackerSquare = Long.numberOfTrailingZeros(rookAttackers);
            long between = getBetweenMask(kingSquare, attackerSquare);
            if ((between & allPieces) == 0) return true;
            rookAttackers &= (rookAttackers - 1); // Clear the least significant bit
        }

        // Check for bishop attacks (including queen's bishop-like moves)
        long bishopAttackers = bishopAttacksMask & (opponentBishops | opponentQueens);
        while (bishopAttackers != 0) {
            int attackerSquare = Long.numberOfTrailingZeros(bishopAttackers);
            long between = getBetweenMask(kingSquare, attackerSquare);
            if ((between & allPieces) == 0) return true;
            bishopAttackers &= (bishopAttackers - 1); // Clear the least significant bit
        }

        return false;
    }

    private long getBetweenMask(int square1, int square2) {
        int rank1 = square1 / 8, file1 = square1 % 8;
        int rank2 = square2 / 8, file2 = square2 % 8;
        long mask = 0L;

        if (rank1 == rank2) {
            // Horizontal
            int minFile = Math.min(file1, file2);
            int maxFile = Math.max(file1, file2);
            for (int f = minFile + 1; f < maxFile; f++) {
                mask |= 1L << (rank1 * 8 + f);
            }
        } else if (file1 == file2) {
            // Vertical
            int minRank = Math.min(rank1, rank2);
            int maxRank = Math.max(rank1, rank2);
            for (int r = minRank + 1; r < maxRank; r++) {
                mask |= 1L << (r * 8 + file1);
            }
        } else if (Math.abs(rank1 - rank2) == Math.abs(file1 - file2)) {
            // Diagonal
            int rankStep = (rank2 > rank1) ? 1 : -1;
            int fileStep = (file2 > file1) ? 1 : -1;
            int r = rank1 + rankStep;
            int f = file1 + fileStep;
            while (r != rank2 && f != file2) {
                mask |= 1L << (r * 8 + f);
                r += rankStep;
                f += fileStep;
            }
        }

        return mask;
    }


    public static long getMask(int square, boolean isRook) {
        long mask = 0L;
        int rank = square / 8; // Determine the rank of the square (0 to 7)
        int file = square % 8; // Determine the file of the square (0 to 7)

        if (isRook) {
            // Mask for horizontal (rank) and vertical (file) sliding directions
            for (int f = 0; f < 8; f++) {
                if (f != file) mask |= (1L << (rank * 8 + f)); // Same rank (horizontal movement)
            }
            for (int r = 0; r < 8; r++) {
                if (r != rank) mask |= (1L << (r * 8 + file)); // Same file (vertical movement)
            }
        } else {
            // Mask for diagonal sliding directions
            for (int offset = 1; offset < 8; offset++) {
                // Diagonals extending in all directions
                if (file + offset < 8 && rank + offset < 8) mask |= (1L << ((rank + offset) * 8 + (file + offset))); // Up-right
                if (file - offset >= 0 && rank + offset < 8) mask |= (1L << ((rank + offset) * 8 + (file - offset))); // Up-left
                if (file + offset < 8 && rank - offset >= 0) mask |= (1L << ((rank - offset) * 8 + (file + offset))); // Down-right
                if (file - offset >= 0 && rank - offset >= 0) mask |= (1L << ((rank - offset) * 8 + (file - offset))); // Down-left
            }
        }

        return mask;
    }

    private long getEnPassantTarget(int lastMove) {
        int fromSquare = lastMove >>> 14;
        int toSquare = (lastMove >>> 7) & 0x3F;
        int piece = board.getPiece(toSquare);

        if (piece == (/* White pawn */ 0) && (toSquare - fromSquare) == 16) {
            return 1L << (toSquare - 8);
        } else if (piece == (/* Black pawn */ 6) && (fromSquare - toSquare) == 16) {
            return 1L << (toSquare + 8);
        } else {
            return 0L;
        }
    }

    private void generateSlidingPieceMoves(List<Integer> moves, int color) {
        moves.addAll(new RookMoveGenerator(board).generateRookMoves(color));
        moves.addAll(new BishopMoveGenerator(board).generateBishopMoves(color));
        moves.addAll(new QueenMoveGenerator(board).generateQueenMoves(color));
    }

    private void generatePawnMoves(List<Integer> moves, int color, long enPassantTarget) {
        moves.addAll(new PawnMoveGenerator(board).generatePawnMoves(color, enPassantTarget));
    }

    private void generateKnightMoves(List<Integer> moves, int color) {
        moves.addAll(new KnightMoveGenerator(board).generateKnightMoves(color));
    }

    private void generateKingMoves(List<Integer> moves, int color) {
        moves.addAll(new KingMoveGenerator(board).generateKingMoves(color));
    }
}

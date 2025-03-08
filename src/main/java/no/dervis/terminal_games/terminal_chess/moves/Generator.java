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

        // Check for attacks from each piece type
        if ((rookAttacksMask & (opponentRooks | opponentQueens)) != 0) return true;
        if ((bishopAttacksMask & (opponentBishops | opponentQueens)) != 0) return true;
        if ((knightAttacksMask & opponentKnights) != 0) return true;
        if ((pawnAttacksMask & opponentPawns) != 0) return true;
        if ((kingAttacksMask & opponentKing) != 0) return true;



        return false;
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

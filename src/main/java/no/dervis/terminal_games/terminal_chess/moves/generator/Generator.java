package no.dervis.terminal_games.terminal_chess.moves.generator;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KingAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KnightAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.PawnAttacks;

import java.util.ArrayList;
import java.util.List;

public class Generator implements Chess {

    private final Bitboard board;

    public Generator(Bitboard board) {
        this.board = board;
    }

    public enum GameState {
        ONGOING, CHECKMATE, STALEMATE, INSUFFICIENT_MATERIAL,
        FIFTY_MOVE_RULE, THREEFOLD_REPETITION
    }

    public GameState getGameState(int color) {
        if (hasInsufficientMaterial()) return GameState.INSUFFICIENT_MATERIAL;
        if (board.halfMoveClock() >= 100) return GameState.FIFTY_MOVE_RULE;

        List<Integer> legalMoves = generateMoves(color);
        if (!legalMoves.isEmpty()) return GameState.ONGOING;

        int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(color));
        if (isKingInCheck(board, color, kingSquare)) return GameState.CHECKMATE;

        return GameState.STALEMATE;
    }

    /**
     * Checks whether the most recent position in the given history has
     * occurred at least three times (threefold repetition draw).
     * <p>Position tracking is kept outside of {@link Bitboard} so that the
     * engine's internal search (which calls {@code makeMove} millions of
     * times) is not burdened with string allocation and list growth.
     * Game loops should maintain their own {@code List<String>} by calling
     * {@link Bitboard#positionKey()} after each real game move.</p>
     */
    public static boolean isThreefoldRepetition(List<String> positions) {
        if (positions.size() < 5) return false;
        String current = positions.getLast();
        int count = 0;
        for (String pos : positions) {
            if (pos.equals(current) && ++count >= 3) return true;
        }
        return false;
    }

    public boolean hasInsufficientMaterial() {
        long whitePawns = board.getPawns(white);
        long blackPawns = board.getPawns(black);
        long whiteRooks = board.getRooks(white);
        long blackRooks = board.getRooks(black);
        long whiteQueens = board.getQueens(white);
        long blackQueens = board.getQueens(black);

        if ((whitePawns | blackPawns | whiteRooks | blackRooks | whiteQueens | blackQueens) != 0) {
            return false;
        }

        long whiteKnights = board.getKnights(white);
        long blackKnights = board.getKnights(black);
        long whiteBishops = board.getBishops(white);
        long blackBishops = board.getBishops(black);

        int whiteMinorCount = Long.bitCount(whiteKnights) + Long.bitCount(whiteBishops);
        int blackMinorCount = Long.bitCount(blackKnights) + Long.bitCount(blackBishops);

        // King vs King
        if (whiteMinorCount == 0 && blackMinorCount == 0) return true;
        // King+minor vs King
        if (whiteMinorCount <= 1 && blackMinorCount == 0) return true;
        if (whiteMinorCount == 0 && blackMinorCount <= 1) return true;

        return false;
    }

    public List<Integer> generateMoves(int color) {
        List<Integer> moves = new ArrayList<>();
        long enPassantTarget = getEnPassantTarget(board.lastMove());

        generateKingMoves(moves, color);
        generatePawnMoves(moves, color, enPassantTarget);
        generateKnightMoves(moves, color);
        generateSlidingPieceMoves(moves, color);

        return filterLegalMoves(moves, color);
    }

    public List<Integer> filterLegalMoves(List<Integer> moves, int color) {
        List<Integer> legalMoves = new ArrayList<>();

        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            int kingSquare = Long.numberOfTrailingZeros(copy.kingPiece(color));
            if (!isKingInCheck(copy, color, kingSquare)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    /**
     * Checks if the king of the given color is in check on the given board.
     *
     * <p>For sliding pieces (rook, bishop, queen), uses magic bitboard lookups
     * which naturally account for blocking pieces — no manual between-mask
     * calculation needed. The magic lookup from the king's square with the
     * current occupancy gives exactly the squares the king "sees" along each
     * ray. If any opponent sliding piece sits on one of those squares, it
     * attacks the king.</p>
     */
    public static boolean isKingInCheck(Bitboard board, int color, int kingSquare) {
        int opponent = 1 - color;
        long allPieces = board.allPieces();

        // Non-sliding pieces: precomputed lookup, no blocker consideration needed
        if ((KnightAttacks.getAllKnightAttacks(kingSquare) & board.getKnights(opponent)) != 0) return true;
        if ((PawnAttacks.getAllPawnAttacks(kingSquare, color) & board.getPawns(opponent)) != 0) return true;
        if ((KingAttacks.getAllKingAttacks(kingSquare) & board.kingPiece(opponent)) != 0) return true;

        // Sliding pieces: magic bitboard lookup handles blockers automatically.
        // "What squares does a rook/bishop on the king's square see?" — if an
        // opponent rook/bishop/queen is on one of those squares, it's giving check.
        long opponentRooksQueens = board.getRooks(opponent) | board.getQueens(opponent);
        if ((MagicBitboard.rookAttacks(kingSquare, allPieces) & opponentRooksQueens) != 0) return true;

        long opponentBishopsQueens = board.getBishops(opponent) | board.getQueens(opponent);
        if ((MagicBitboard.bishopAttacks(kingSquare, allPieces) & opponentBishopsQueens) != 0) return true;

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

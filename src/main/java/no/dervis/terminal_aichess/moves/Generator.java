package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.board.Bitboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Generator {

    private final Bitboard board;

    public Generator(Bitboard board) {
        this.board = board;
    }

    public List<Integer> generateMoves(int color) {
        List<Integer> moves = new ArrayList<>();
        long enPassantTarget = getEnPassantTarget(Objects.requireNonNullElse(board.history().peekLast(), 0));

        generatePawnMoves(moves, color, enPassantTarget);
        generateKnightMoves(moves, color);
        generateBishopMoves(moves, color);
        generateRookMoves(moves, color);
        generateQueenMoves(moves, color);
        generateKingMoves(moves, color);

        return moves;
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

    private void generatePawnMoves(List<Integer> moves, int color, long enPassantTarget) {
        moves.addAll(new PawnMoveGenerator(board).generatePawnMoves(color, enPassantTarget));
    }

    private void generateKnightMoves(List<Integer> moves, int color) {
        moves.addAll(new KnightMoveGenerator(board).generateKnightMoves(color));
    }

    private void generateBishopMoves(List<Integer> moves, int color) {
        moves.addAll(new BishopMoveGenerator(board).generateBishopMoves(color));
    }

    private void generateRookMoves(List<Integer> moves, int color) {
        moves.addAll(new RookMoveGenerator(board).generateRookMoves(color));
    }

    private void generateQueenMoves(List<Integer> moves, int color) {
        moves.addAll(new QueenMoveGenerator(board).generateQueenMoves(color));
    }

    private void generateKingMoves(List<Integer> moves, int color) {
        moves.addAll(new KingMoveGenerator(board).generateKingMoves(color));
    }
}

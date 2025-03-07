package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

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

    private boolean isKingInCheck(int color, int kingSquare) {


        return false;
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

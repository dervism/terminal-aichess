package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board.Tuple3;
import no.dervis.terminal_aichess.Chess;

public record Move(
        int piece,
        int color,
        int fromSquare,
        int toSquare,
        int moveType,
        int promotionPiece) {

    public static Move createMove(
            int piece,
            int color,
            int fromSquare,
            int toSquare
    ) {
        return new Move(
                piece,
                color,
                fromSquare,
                toSquare,
                Chess.MoveType.NORMAL.ordinal(),
                0);
    }

    public static Move createWhiteMove(
            int piece,
            int fromSquare,
            int toSquare
    ) {
        return new Move(
                piece,
                Chess.white,
                fromSquare,
                toSquare,
                Chess.MoveType.NORMAL.ordinal(),
                0);
    }

    public static Move createMove(
            int piece,
            int color,
            int fromSquare,
            int toSquare,
            int moveType,
            int promotionPiece
    ) {
        return new Move(
                piece,
                color,
                fromSquare,
                toSquare,
                moveType,
                promotionPiece);
    }

    public static Move createMove(int move, Bitboard board){
        int fromSquare = move >>> 14;
        int toSquare = (move >>> 7) & 0x3F;
        int moveType = (move >>> 4) & 0x7;
        int promotionPiece = move & 0xF;

        int piece = board.getPiece(fromSquare);
        int color = piece / 6;
        int pieceType = piece % 6;

        return new Move(
                pieceType,
                color,
                fromSquare,
                toSquare,
                moveType,
                promotionPiece
        );
    }

    public static int createMove(Tuple3 from, Tuple3 to) {
        return (from.index() << 14) | (to.index() << 7);
    }

    @Override
    public String toString() {
        return "Move{" +
                "piece=" + piece +
                ", color=" + color +
                ", fromSquare=" + fromSquare +
                ", toSquare=" + toSquare +
                ", moveType=" + moveType +
                ", promotionPiece=" + promotionPiece +
                '}';
    }
}

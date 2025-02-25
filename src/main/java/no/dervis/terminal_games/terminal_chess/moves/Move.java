package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.Chess;

public record Move(
        int piece,
        int color,
        int fromSquare,
        int toSquare,
        int moveType,
        int promotionPiece) {

    public static int createMove(
            int fromSquare,
            int toSquare,
            int moveType
    ) {
        return createMove(fromSquare, toSquare, moveType, 0);
    }

    public static int createMove(
            int fromSquare,
            int toSquare,
            int moveType,
            int promotionPiece
    ) {
        return (fromSquare << 14) |
                (toSquare << 7) |
                (moveType << 4) |
                promotionPiece;
    }

    public static int createMove(Move move) {
        int fromSquare = move.fromSquare;
        int toSquare = move.toSquare;
        int moveType = move.moveType;
        int promotionPiece = move.promotionPiece;

        return (fromSquare << 14) |
                (toSquare << 7) |
                (moveType << 4) |
                promotionPiece;
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

    public String toStringShort() {
        return "(" +
                Chess.pieceToStr.apply(color == 0 ? piece : piece + 6) + "," +
                Tuple3.of(fromSquare).square().toLowerCase() + "->" +
                Tuple3.of(toSquare).square().toLowerCase() + ")";
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

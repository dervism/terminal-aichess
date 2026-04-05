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
        String from = Tuple3.of(fromSquare).square().toLowerCase();
        String to = Tuple3.of(toSquare).square().toLowerCase();
        String promo = promotionPiece > 0 ? "=" + promotionLetter() : "";
        return Chess.pieceToStr.apply(color == 0 ? piece : piece + 6).trim()
                + " " + from + " → " + to + promo;
    }

    /**
     * Returns the move in standard algebraic notation, e.g. Nc3, e4, Bxe5, O-O.
     * Capture detection uses the {@code moveType} field — generators tag captures
     * with {@link Chess.MoveType#ATTACK} and en passant with
     * {@link Chess.MoveType#EN_PASSANT}.
     */
    public String toAlgebraic() {
        if (moveType == Chess.MoveType.CASTLE_KING_SIDE.ordinal()) return "O-O";
        if (moveType == Chess.MoveType.CASTLE_QUEEN_SIDE.ordinal()) return "O-O-O";

        boolean isCapture = moveType == Chess.MoveType.ATTACK.ordinal()
                         || moveType == Chess.MoveType.EN_PASSANT.ordinal();

        String to = Tuple3.of(toSquare).square().toLowerCase();
        String pieceLetter = switch (piece) {
            case Chess.knight -> "N";
            case Chess.bishop -> "B";
            case Chess.rook   -> "R";
            case Chess.queen  -> "Q";
            case Chess.king   -> "K";
            default -> "";  // pawn
        };

        String capture = isCapture ? "x" : "";

        if (piece == Chess.pawn) {
            String fromFile = isCapture
                    ? String.valueOf((char) ('a' + Tuple3.of(fromSquare).file()))
                    : "";
            String promo = promotionPiece > 0 ? "=" + promotionLetter() : "";
            return fromFile + capture + to + promo;
        }

        return pieceLetter + capture + to;
    }

    private String promotionLetter() {
        return switch (promotionPiece % 6) {
            case Chess.knight -> "N";
            case Chess.bishop -> "B";
            case Chess.rook   -> "R";
            case Chess.queen  -> "Q";
            default -> "";
        };
    }

    @Override
    public String toString() {
        return toStringShort();
    }
}

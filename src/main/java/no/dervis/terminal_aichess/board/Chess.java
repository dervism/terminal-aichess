package no.dervis.terminal_aichess.board;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface Chess {

    int pawn = 0, bpawn = 6;
    int knight = 1, bknight = 7;
    int bishop = 2, bbishop = 8;
    int rook = 3, brook = 9;
    int queen = 4, bqueen = 10;
    int king = 5, bking = 11;
    int empty = -1;

    int white = 0;
    int black = 1;

    int[] piece_value = {198, 817, 836, 1270, 2521, 0};

    int INFINITE = Integer.MAX_VALUE;
    int CHECKMATE = 6;
    int DRAW = 7;

    enum MoveType {
        NORMAL,
        EN_PASSANT,
        CASTLE_KING_SIDE,
        CASTLE_QUEEN_SIDE,
    }

    Function<Integer, String> pieceToStr = pieceType -> switch (pieceType) {
        case pawn    -> " ♙ ";
        case knight  -> " ♘ ";
        case bishop  -> " ♗ ";
        case rook    -> " ♖ ";
        case queen   -> " ♕ ";
        case king    -> " ♔ ";
        case bpawn   -> " ♟ ";
        case bknight -> " ♞ ";
        case bbishop -> " ♝ ";
        case brook   -> " ♜ ";
        case bqueen  -> " ♛ ";
        case bking   -> " ♚ ";
        case empty   -> "   ";
        default -> throw new IllegalStateException(STR."Unexpected value: \{pieceType}");
    };

    BiFunction<Bitboard, Boolean, StringBuilder> boardToStr = (board, reverse) -> {
        StringBuilder builder = new StringBuilder();
        IntStream.range(0, 8)
                .map(i -> reverse ? (8 - 1 - i) : i)
                .forEach(row -> {
                    String line = IntStream.range(0, 8)
                            .mapToObj(col -> {
                                String square = pieceToStr.apply(board.getPiece(BoardPrinter.indexFn.apply(row, col)));
                                if ((row + col) % 2 == 0) {
                                    return "\u001B[47m"+square+"\u001B[0m";
                                } else {
                                    return square;
                                }
                            })
                            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                            .toString();
                    builder.append(line).append(System.lineSeparator());
                });
        return builder;
    };

    Function<Bitboard, StringBuilder> printBoard = board -> boardToStr.apply(board, true);
}

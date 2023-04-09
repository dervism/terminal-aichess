package no.dervis;

import java.util.function.Function;
import java.util.stream.IntStream;

public interface Chess {

    int pawn = 0;
    int knight = 1;
    int bishop = 2;
    int rook = 3;
    int queen = 4;
    int king = 5;
    int empty = -1;

    int white = 0;
    int black = 1;

    int[] piece_value = {198, 817, 836, 1270, 2521, 0};

    int INFINITE = Integer.MAX_VALUE;
    int CHECKMATE = 6;
    int DRAW = 7;

    Function<Integer, String> pieceToStr = pieceType -> switch (pieceType) {
        case pawn -> " ♙ ";
        case knight -> " ♘ ";
        case bishop -> " ♗ ";
        case rook -> " ♖ ";
        case queen -> " ♕ ";
        case king -> " ♔ ";
        case pawn + 6 -> " ♟︎ ";
        case knight + 6 -> " ♞ ";
        case bishop + 6 -> " ♝ ";
        case rook + 6 -> " ♜ ";
        case queen + 6 -> " ♛ ";
        case king + 6 -> " ♚ ";
        case empty -> "   ";
        default -> throw new IllegalStateException("Unexpected value: " + pieceType);
    };

    Function<Bitboard, String> boardToStr = board -> {
        StringBuilder builder = new StringBuilder();
        IntStream.range(0, 8)
                .forEach(row -> {
                    String line = IntStream.range(0, 8)
                            .mapToObj(col -> {
                                String square = pieceToStr.apply(board.getPiece( board.indexFn.apply(row, col)));
                                if ((row + col) % 2 == 0) {
                                    return "\u001B[47m" + square + "\u001B[0m";
                                } else {
                                    return square;
                                }
                            })
                            .flatMap(square -> IntStream.range(0, 1).mapToObj(i -> square))
                            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                            .toString();
                    IntStream.range(0, 1).forEach(i -> builder.append(line).append(System.lineSeparator()));
                });
        return builder.toString();
    };
}

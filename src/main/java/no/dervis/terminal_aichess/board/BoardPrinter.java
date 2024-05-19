package no.dervis.terminal_aichess.board;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface BoardPrinter {

    BiFunction<Integer, Integer, Integer> indexFn = (rank, file) -> rank * 8 + file;
    Function<Integer, Integer> rowFn = index -> index / 8;
    Function<Integer, Integer> columnFn = index -> index % 8;
    Function<Integer, String> columnToStr = index -> switch (columnFn.apply(index)) {
        case 0 -> "A";
        case 1 -> "B";
        case 2 -> "C";
        case 3 -> "D";
        case 4 -> "E";
        case 5 -> "F";
        case 6 -> "G";
        case 7 -> "H";
        default -> throw new IllegalStateException(STR."Unexpected value: \{columnFn.apply(index)}");
    };

    Function<String, Integer> columnToIndex = index -> switch (index) {
        case "A" -> 0;
        case "B" -> 1;
        case "C" -> 2;
        case "D" -> 3;
        case "E" -> 4;
        case "F" -> 5;
        case "G" -> 6;
        case "H" -> 7;
        default -> throw new IllegalStateException(STR."Unexpected value: \{index}");
    };

}

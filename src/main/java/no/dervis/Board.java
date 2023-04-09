package no.dervis;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.dervis.Board.T3.of;

public interface Board {

    long FILE_A = 0x0101010101010101L;
    long FILE_B = 0x0202020202020202L;
    long FILE_C = 0x0404040404040404L;
    long FILE_D = 0x0808080808080808L;
    long FILE_E = 0x1010101010101010L;
    long FILE_F = 0x2020202020202020L;
    long FILE_G = 0x4040404040404040L;
    long FILE_H = 0x8080808080808080L;

    /**
     * Function that converts a Long to binary string
     */
    Function<Long, String> bitsToString = n -> String
            .format("%64s", "")
            .replace(' ', '0');

    /**
     * A Tuple holding three values
     */
    record T3(int rank, int file, int index) {
        static T3 of(int index) {
            return new T3(rowFn.apply(index), columnFn.apply(index), index);
        }

        public String square() {
            return columnToStr.apply(file) + (rank + 1);
        }

        @Override
        public String toString() {
            return "T3( r=" + rank +
                    ", f=" + file +
                    ", i=" + index +
                    ')';
        }
    }

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
        default -> throw new IllegalStateException("Unexpected value: " + columnFn.apply(index));
    };

    T3 a1 = of(0), b1 = of(1), c1 = of(2), d1 = of(3), e1 = of(4), f1 = of(5), g1 = of(6), h1 = of(7);
    T3 a2 = of(8), b2 = of(9), c2 = of(10), d2 = of(11), e2 = of(12), f2 = of(13), g2 = of(14), h2 = of(15);
    T3 a3 = of(16), b3 = of(17), c3 = of(18), d3 = of(19), e3 = of(20), f3 = of(21), g3 = of(22), h3 = of(23);
    T3 a4 = of(24), b4 = of(25), c4 = of(26), d4 = of(27), e4 = of(28), f4 = of(29), g4 = of(30), h4 = of(31);
    T3 a5 = of(32), b5 = of(33), c5 = of(34), d5 = of(35), e5 = of(36), f5 = of(37), g5 = of(38), h5 = of(39);
    T3 a6 = of(40), b6 = of(41), c6 = of(42), d6 = of(43), e6 = of(44), f6 = of(45), g6 = of(46), h6 = of(47);
    T3 a7 = of(48), b7 = of(49), c7 = of(50), d7 = of(51), e7 = of(52), f7 = of(53), g7 = of(54), h7 = of(55);
    T3 a8 = of(56), b8 = of(57), c8 = of(58), d8 = of(59), e8 = of(60), f8 = of(61), g8 = of(62), h8 = of(63);

    T3[] fileA = {a1, a2, a3, a4, a5, a6, a7, a8};
    T3[] fileB = {b1, b2, b3, b4, b5, b6, b7, b8};
    T3[] fileC = {c1, c2, c3, c4, c5, c6, c7, c8};
    T3[] fileD = {d1, d2, d3, d4, d5, d6, d7, d8};
    T3[] fileE = {e1, e2, e3, e4, e5, e6, e7, e8};
    T3[] fileF = {f1, f2, f3, f4, f5, f6, f7, f8};
    T3[] fileG = {g1, g2, g3, g4, g5, g6, g7, g8};
    T3[] fileH = {h1, h2, h3, h4, h5, h6, h7, h8};

    T3[] rank1 = {a1, b1, c1, d1, e1, f1, g1, h1};
    T3[] rank2 = {a2, b2, c2, d2, e2, f2, g2, h2};
    T3[] rank3 = {a3, b3, c3, d3, e3, f3, g3, h3};
    T3[] rank4 = {a4, b4, c4, d4, e4, f4, g4, h4};
    T3[] rank5 = {a5, b5, c5, d5, e5, f5, g5, h5};
    T3[] rank6 = {a6, b6, c6, d6, e6, f6, g6, h6};
    T3[] rank7 = {a7, b7, c7, d7, e7, f7, g7, h7};
    T3[] rank8 = {a8, b8, c8, d8, e8, f8, g8, h8};

    T3[] allSquares = {
            a8, b8, c8, d8, e8, f8, g8, h8,
            a7, b7, c7, d7, e7, f7, g7, h7,
            a6, b6, c6, d6, e6, f6, g6, h6,
            a5, b5, c5, d5, e5, f5, g5, h5,
            a4, b4, c4, d4, e4, f4, g4, h4,
            a3, b3, c3, d3, e3, f3, g3, h3,
            a2, b2, c2, d2, e2, f2, g2, h2,
            a1, b1, c1, d1, e1, f1, g1, h1
    };

    T3[] leftRightDiagonal1 = {a1};
    T3[] leftRightDiagonal2 = {a2, b1};
    T3[] leftRightDiagonal3 = {a3, b2, c1};
    T3[] leftRightDiagonal4 = {a4, b3, c2, d1};
    T3[] leftRightDiagonal5 = {a5, b4, c3, d2, e1};
    T3[] leftRightDiagonal6 = {a6, b5, c4, d3, e2, f1};
    T3[] leftRightDiagonal7 = {a7, b6, c5, d4, e3, f2, g1};
    T3[] leftRightDiagonal8 = {a8, b7, c6, d5, e4, f3, g2, h1};
    T3[] leftRightDiagonal9 = {b8, c7, d6, e5, f4, g3, h2};
    T3[] leftRightDiagonal10 = {c8, d7, e6, f5, g4, h3};
    T3[] leftRightDiagonal11 = {d8, e7, f6, g5, h4};
    T3[] leftRightDiagonal12 = {e8, f7, g6, h5};
    T3[] leftRightDiagonal13 = {f8, g7, h6};
    T3[] leftRightDiagonal14 = {g8, h7};
    T3[] leftRightDiagonal15 = {h8};

    T3[] rightLeftDiagonal1 = {h1};
    T3[] rightLeftDiagonal2 = {g1, h2};
    T3[] rightLeftDiagonal3 = {f1, g2, h3};
    T3[] rightLeftDiagonal4 = {e1, f2, g3, h4};
    T3[] rightLeftDiagonal5 = {d1, e2, f3, g4, h5};
    T3[] rightLeftDiagonal6 = {c1, d2, e3, f4, g5, h6};
    T3[] rightLeftDiagonal7 = {b1, c2, d3, e4, f5, g6, h7};
    T3[] rightLeftDiagonal8 = {a1, b2, c3, d4, e5, f6, g7, h8};
    T3[] rightLeftDiagonal9 = {a2, b3, c4, d5, e6, f7, g8};
    T3[] rightLeftDiagonal10 = {a3, b4, c5, d6, e7, f8};
    T3[] rightLeftDiagonal11 = {a4, b5, c6, d7, e8};
    T3[] rightLeftDiagonal12 = {a5, b6, c7, d8};
    T3[] rightLeftDiagonal13 = {a6, b7, c8};
    T3[] rightLeftDiagonal14 = {a7, b8};
    T3[] rightLeftDiagonal15 = {a8};

    List<T3[]> allDiagonals = List.of(
            leftRightDiagonal1,
            leftRightDiagonal2,
            leftRightDiagonal3,
            leftRightDiagonal4,
            leftRightDiagonal5,
            leftRightDiagonal6,
            leftRightDiagonal7,
            leftRightDiagonal8,
            leftRightDiagonal9,
            leftRightDiagonal10,
            leftRightDiagonal11,
            leftRightDiagonal12,
            leftRightDiagonal13,
            leftRightDiagonal14,
            leftRightDiagonal15,
            rightLeftDiagonal1,
            rightLeftDiagonal2,
            rightLeftDiagonal3,
            rightLeftDiagonal4,
            rightLeftDiagonal5,
            rightLeftDiagonal6,
            rightLeftDiagonal7,
            rightLeftDiagonal8,
            rightLeftDiagonal9,
            rightLeftDiagonal10,
            rightLeftDiagonal11,
            rightLeftDiagonal12,
            rightLeftDiagonal13,
            rightLeftDiagonal14,
            rightLeftDiagonal15
    );

    List<T3[]> filesAndRanks = List.of(
            fileA,
            fileB,
            fileC,
            fileD,
            fileE,
            fileF,
            fileG,
            fileH,
            rank1,
            rank2,
            rank3,
            rank4,
            rank5,
            rank6,
            rank7,
            rank8
    );

    /**
     * Returns all the squares grouped into diagonals, rows and columns.
     */
    List<T3[]> allDiagonalsFilesAndRanks = Stream.of(allDiagonals, filesAndRanks)
            .flatMap(Collection::stream).collect(Collectors.toList());

    /**
     * Returns the diagonals, rows and columns that contains the given square.
     */
    Function<Integer, List<T3[]>> filterSquares = index -> allDiagonalsFilesAndRanks.stream().filter(t3s -> {
        for (T3 t3 : t3s) if (t3.index == index) return true;
        return false;
    }).toList();

    /**
     * Convert a list of T3's to a list of strings.
     */
    Function<List<T3[]>, List<List<String>>> squaresToStr = t3s ->
            t3s.stream().map(t -> Arrays.stream(t).map(T3::square).toList()).toList();

    /**
     * Returns the diagonals, rows and columns that contains the given square as list of strings.
     */
    Function<Integer, List<List<String>>> squaresStrFn = filterSquares.andThen(squaresToStr);
}

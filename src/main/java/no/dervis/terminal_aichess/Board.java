package no.dervis.terminal_aichess;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static no.dervis.terminal_aichess.Board.Tuple3.of;

public interface Board {

    int RIGHT_EDGE = 0;
    int LEFT_EDGE = 7;

    long FILE_A = 0x0101010101010101L;
    long FILE_B = 0x0202020202020202L;
    long FILE_C = 0x0404040404040404L;
    long FILE_D = 0x0808080808080808L;
    long FILE_E = 0x1010101010101010L;
    long FILE_F = 0x2020202020202020L;
    long FILE_G = 0x4040404040404040L;
    long FILE_H = 0x8080808080808080L;

    Function<Long, String> longToString = n -> String
            .format("%64s", Long.toBinaryString(n))
            .replace(' ', '0');

    Function<Integer, String> intToString = n -> String
            .format("%32s", Integer.toBinaryString(n))
            .replace(' ', '0');

    Function<Byte, String> byteToString = n -> String
            .format("%4s", Integer.toBinaryString(n))
            .replace(' ', '0');


    record Tuple3(int rank, int file, int index) {
        static Tuple3 of(int index) {
            return new Tuple3(rowFn.apply(index), columnFn.apply(index), index);
        }

        public String square() {
            return columnToStr.apply(file) + (rank + 1);
        }

        @Override
        public String toString() {
            return STR."T3( r=\{rank}, f=\{file}, i=\{index}\{')'}";
        }
    }

    record Tuple2<T, B>(T left, B right) {
        static <T, B> Tuple2<T, B> of(T left, B right) { return new Tuple2<>(left, right); }
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

    Function<Tuple2<String, Integer>, Tuple3> t2ToT3 = value ->
            new Tuple3(
                    /*row*/    value.right(),
                    /*column*/ columnToIndex.apply(value.left()),
                    /*index*/  indexFn.apply(value.right(), columnToIndex.apply(value.left())));

    Tuple3 a1 = of(0), b1 = of(1), c1 = of(2), d1 = of(3), e1 = of(4), f1 = of(5), g1 = of(6), h1 = of(7);
    Tuple3 a2 = of(8), b2 = of(9), c2 = of(10), d2 = of(11), e2 = of(12), f2 = of(13), g2 = of(14), h2 = of(15);
    Tuple3 a3 = of(16), b3 = of(17), c3 = of(18), d3 = of(19), e3 = of(20), f3 = of(21), g3 = of(22), h3 = of(23);
    Tuple3 a4 = of(24), b4 = of(25), c4 = of(26), d4 = of(27), e4 = of(28), f4 = of(29), g4 = of(30), h4 = of(31);
    Tuple3 a5 = of(32), b5 = of(33), c5 = of(34), d5 = of(35), e5 = of(36), f5 = of(37), g5 = of(38), h5 = of(39);
    Tuple3 a6 = of(40), b6 = of(41), c6 = of(42), d6 = of(43), e6 = of(44), f6 = of(45), g6 = of(46), h6 = of(47);
    Tuple3 a7 = of(48), b7 = of(49), c7 = of(50), d7 = of(51), e7 = of(52), f7 = of(53), g7 = of(54), h7 = of(55);
    Tuple3 a8 = of(56), b8 = of(57), c8 = of(58), d8 = of(59), e8 = of(60), f8 = of(61), g8 = of(62), h8 = of(63);

    Tuple3[] fileA = {a1, a2, a3, a4, a5, a6, a7, a8};
    Tuple3[] fileB = {b1, b2, b3, b4, b5, b6, b7, b8};
    Tuple3[] fileC = {c1, c2, c3, c4, c5, c6, c7, c8};
    Tuple3[] fileD = {d1, d2, d3, d4, d5, d6, d7, d8};
    Tuple3[] fileE = {e1, e2, e3, e4, e5, e6, e7, e8};
    Tuple3[] fileF = {f1, f2, f3, f4, f5, f6, f7, f8};
    Tuple3[] fileG = {g1, g2, g3, g4, g5, g6, g7, g8};
    Tuple3[] fileH = {h1, h2, h3, h4, h5, h6, h7, h8};

    Tuple3[] rank1 = {a1, b1, c1, d1, e1, f1, g1, h1};
    Tuple3[] rank2 = {a2, b2, c2, d2, e2, f2, g2, h2};
    Tuple3[] rank3 = {a3, b3, c3, d3, e3, f3, g3, h3};
    Tuple3[] rank4 = {a4, b4, c4, d4, e4, f4, g4, h4};
    Tuple3[] rank5 = {a5, b5, c5, d5, e5, f5, g5, h5};
    Tuple3[] rank6 = {a6, b6, c6, d6, e6, f6, g6, h6};
    Tuple3[] rank7 = {a7, b7, c7, d7, e7, f7, g7, h7};
    Tuple3[] rank8 = {a8, b8, c8, d8, e8, f8, g8, h8};

    Tuple3[] allSquares = {
            a8, b8, c8, d8, e8, f8, g8, h8,
            a7, b7, c7, d7, e7, f7, g7, h7,
            a6, b6, c6, d6, e6, f6, g6, h6,
            a5, b5, c5, d5, e5, f5, g5, h5,
            a4, b4, c4, d4, e4, f4, g4, h4,
            a3, b3, c3, d3, e3, f3, g3, h3,
            a2, b2, c2, d2, e2, f2, g2, h2,
            a1, b1, c1, d1, e1, f1, g1, h1
    };

    // Left-to-right diagonals
    long[] leftRightDiagonals = {
            0x0000000000000201L, // a2, b1
            0x0000000000040201L, // a3, b2, c1
            0x0000000008040201L, // a4, b3, c2, d1
            0x0000001008040201L, // a5, b4, c3, d2, e1
            0x0000201008040201L, // a6, b5, c4, d3, e2, f1
            0x0040201008040201L, // a7, b6, c5, d4, e3, f2, g1
            0x8040201008040201L, // a8, b7, c6, d5, e4, f3, g2, h1
            0x0080402010080402L, // b8, c7, d6, e5, f4, g3, h2
            0x0000804020100804L, // c8, d7, e6, f5, g4, h3
            0x0000008040201008L, // d8, e7, f6, g5, h4
            0x0000000080402010L, // e8, f7, g6, h5
            0x0000000000804020L, // f8, g7, h6
            0x0000000000008040L  // g8, h7
    };

    // Right-to-left diagonals
    long[] rightLeftDiagonals = {
            0x0000000000004002L, // g1, h2
            0x0000000000402004L, // f1, g2, h3
            0x0000000040201008L, // e1, f2, g3, h4
            0x00000040201008010L, // d1, e2, f3, g4, h5
            0x00004020100804020L, // c1, d2, e3, f4, g5, h6
            0x00402010080402040L, // b1, c2, d3, e4, f5, g6, h7
            0x4020100804020100L, // a1, b2, c3, d4, e5, f6, g7, h8
            0x4020100804020000L, // a2, b3, c4, d5, e6, f7, g8
            0x4020100804000000L, // a3, b4, c5, d6, e7, f8
            0x4020100800000000L, // a4, b5, c6, d7, e8
            0x4020100000000000L, // a5, b6, c7, d8
            0x4020000000000000L, // a6, b7, c8
            0x4000000000000000L  // a7, b8
    };

    Map<Integer, Long> diagonalLookupTable = Map.<Integer, Long>ofEntries(
            new SimpleImmutableEntry<>(a1.index, 0x4020100804020100L),
            new SimpleImmutableEntry<>(a2.index, 0x0000000000000201L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(a3.index, 0x0000000000040201L | 0x4020100804000000L),
            new SimpleImmutableEntry<>(a4.index, 0x0000000008040201L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(a5.index, 0x0000001008040201L | 0x4020100000000000L),
            new SimpleImmutableEntry<>(a6.index, 0x0000201008040201L | 0x4020000000000000L),
            new SimpleImmutableEntry<>(a7.index, 0x0040201008040201L | 0x4000000000000000L),
            new SimpleImmutableEntry<>(a8.index, 0x8040201008040201L),
            new SimpleImmutableEntry<>(b1.index, 0x0000000000000201L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(b2.index, 0x0000000000040201L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(b3.index, 0x0000000008040201L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(b4.index, 0x0000001008040201L | 0x4020100804000000L),
            new SimpleImmutableEntry<>(b5.index, 0x0000201008040201L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(b6.index, 0x0040201008040201L | 0x4020100000000000L),
            new SimpleImmutableEntry<>(b7.index, 0x8040201008040201L | 0x4020000000000000L),
            new SimpleImmutableEntry<>(b8.index, 0x0080402010080402L | 0x4000000000000000L),
            new SimpleImmutableEntry<>(c1.index, 0x0000000000040201L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(c2.index, 0x0000000008040201L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(c3.index, 0x0000001008040201L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(c4.index, 0x0000201008040201L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(c5.index, 0x0040201008040201L | 0x4020100804000000L),
            new SimpleImmutableEntry<>(c6.index, 0x8040201008040201L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(c7.index, 0x0080402010080402L | 0x4020100000000000L),
            new SimpleImmutableEntry<>(c8.index, 0x0000804020100804L | 0x4020000000000000L),
            new SimpleImmutableEntry<>(d1.index, 0x0000000008040201L | 0x00000040201008010L),
            new SimpleImmutableEntry<>(d2.index, 0x0000001008040201L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(d3.index, 0x0000201008040201L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(d4.index, 0x0040201008040201L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(d5.index, 0x8040201008040201L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(d6.index, 0x0080402010080402L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(d7.index, 0x0000804020100804L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(d8.index, 0x0000008040201008L | 0x4020100000000000L),
            new SimpleImmutableEntry<>(e1.index, 0x0000001008040201L | 0x0000000040201008L),
            new SimpleImmutableEntry<>(e2.index, 0x0000201008040201L | 0x00000040201008010L),
            new SimpleImmutableEntry<>(e3.index, 0x0040201008040201L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(e4.index, 0x8040201008040201L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(e5.index, 0x0080402010080402L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(e6.index, 0x0000804020100804L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(e7.index, 0x0000008040201008L | 0x4020100804000000L),
            new SimpleImmutableEntry<>(e8.index, 0x0000000080402010L | 0x4020100800000000L),
            new SimpleImmutableEntry<>(f1.index, 0x0000201008040201L | 0x0000000000402004L),
            new SimpleImmutableEntry<>(f2.index, 0x0040201008040201L | 0x0000000040201008L),
            new SimpleImmutableEntry<>(f3.index, 0x8040201008040201L | 0x00000040201008010L),
            new SimpleImmutableEntry<>(f4.index, 0x0080402010080402L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(f5.index, 0x0000804020100804L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(f6.index, 0x0000008040201008L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(f7.index, 0x0000000080402010L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(f8.index, 0x0000000000804020L | 0x4020100804000000L),
            new SimpleImmutableEntry<>(g1.index, 0x0040201008040201L | 0x0000000000004002L),
            new SimpleImmutableEntry<>(g2.index, 0x8040201008040201L | 0x0000000000402004L),
            new SimpleImmutableEntry<>(g3.index, 0x0080402010080402L | 0x0000000040201008L),
            new SimpleImmutableEntry<>(g4.index, 0x0000804020100804L | 0x00000040201008010L),
            new SimpleImmutableEntry<>(g5.index, 0x0000008040201008L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(g6.index, 0x0000000080402010L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(g7.index, 0x0000000000804020L | 0x4020100804020100L),
            new SimpleImmutableEntry<>(g8.index, 0x0000000000008040L | 0x4020100804020000L),
            new SimpleImmutableEntry<>(h1.index, 0x8040201008040201L),
            new SimpleImmutableEntry<>(h2.index, 0x0000804020100804L | 0x0000000000004002L),
            new SimpleImmutableEntry<>(h3.index, 0x0000804020100804L | 0x0000000000402004L),
            new SimpleImmutableEntry<>(h4.index, 0x0000008040201008L | 0x0000000040201008L),
            new SimpleImmutableEntry<>(h5.index, 0x0000000080402010L | 0x00000040201008010L),
            new SimpleImmutableEntry<>(h6.index, 0x0000000000804020L | 0x00004020100804020L),
            new SimpleImmutableEntry<>(h7.index, 0x0000000000008040L | 0x00402010080402040L),
            new SimpleImmutableEntry<>(h8.index, 0x4020100804020100L)
    );

    Tuple3[] leftRightDiagonal2 = {a2, b1};
    Tuple3[] leftRightDiagonal3 = {a3, b2, c1};
    Tuple3[] leftRightDiagonal4 = {a4, b3, c2, d1};
    Tuple3[] leftRightDiagonal5 = {a5, b4, c3, d2, e1};
    Tuple3[] leftRightDiagonal6 = {a6, b5, c4, d3, e2, f1};
    Tuple3[] leftRightDiagonal7 = {a7, b6, c5, d4, e3, f2, g1};
    Tuple3[] leftRightDiagonal8 = {a8, b7, c6, d5, e4, f3, g2, h1};
    Tuple3[] leftRightDiagonal9 = {b8, c7, d6, e5, f4, g3, h2};
    Tuple3[] leftRightDiagonal10 = {c8, d7, e6, f5, g4, h3};
    Tuple3[] leftRightDiagonal11 = {d8, e7, f6, g5, h4};
    Tuple3[] leftRightDiagonal12 = {e8, f7, g6, h5};
    Tuple3[] leftRightDiagonal13 = {f8, g7, h6};
    Tuple3[] leftRightDiagonal14 = {g8, h7};

    Tuple3[] rightLeftDiagonal2 = {g1, h2};
    Tuple3[] rightLeftDiagonal3 = {f1, g2, h3};
    Tuple3[] rightLeftDiagonal4 = {e1, f2, g3, h4};
    Tuple3[] rightLeftDiagonal5 = {d1, e2, f3, g4, h5};
    Tuple3[] rightLeftDiagonal6 = {c1, d2, e3, f4, g5, h6};
    Tuple3[] rightLeftDiagonal7 = {b1, c2, d3, e4, f5, g6, h7};
    Tuple3[] rightLeftDiagonal8 = {a1, b2, c3, d4, e5, f6, g7, h8};
    Tuple3[] rightLeftDiagonal9 = {a2, b3, c4, d5, e6, f7, g8};
    Tuple3[] rightLeftDiagonal10 = {a3, b4, c5, d6, e7, f8};
    Tuple3[] rightLeftDiagonal11 = {a4, b5, c6, d7, e8};
    Tuple3[] rightLeftDiagonal12 = {a5, b6, c7, d8};
    Tuple3[] rightLeftDiagonal13 = {a6, b7, c8};
    Tuple3[] rightLeftDiagonal14 = {a7, b8};

    List<Tuple3[]> allDiagonals = List.of(
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
            rightLeftDiagonal14
    );

    List<Tuple3[]> filesAndRanks = List.of(
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

    Function<Long, String> binaryToStr = Long::toHexString;

    Function<Bitboard, String> boardToHex = board -> binaryToStr.apply(board.allPieces());

    static boolean isWithinBoardLimit(int square){
        return square >= 0 && square < 64;
    }
}

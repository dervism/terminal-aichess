package no.dervis.terminal_aichess.board;

import java.util.function.Function;

import static no.dervis.terminal_aichess.board.Board.Tuple3.of;

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

    record Tuple3(int rank, int file, int index) {
        public static Tuple3 of(int index) {
            return new Tuple3(BoardPrinter.rowFn.apply(index), BoardPrinter.columnFn.apply(index), index);
        }

        public String square() {
            return BoardPrinter.columnToStr.apply(file) + (rank + 1);
        }

        @Override
        public String toString() {
            return STR."T3( r=\{rank}, f=\{file}, i=\{index}\{')'}";
        }
    }

    record Tuple2<T, B>(T left, B right) {
        public static <T, B> Tuple2<T, B> of(T left, B right) { return new Tuple2<>(left, right); }
    }

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

    Function<Long, String> binaryToStr = Long::toHexString;

    Function<Bitboard, String> boardToHex = board -> binaryToStr.apply(board.allPieces());
}

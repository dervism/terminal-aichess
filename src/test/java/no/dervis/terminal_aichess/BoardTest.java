package no.dervis.terminal_aichess;

import org.junit.jupiter.api.Test;

import static no.dervis.terminal_aichess.Board.*;
import static no.dervis.terminal_aichess.Chess.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTest {

    @Test
    void testConvertIndexToCoordinates() {
        assertEquals(0, a1.file());
        assertEquals(0, a1.rank());
        assertEquals(0, indexFn.apply(a1.rank(), a1.file()));

        assertEquals(4, e5.file());
        assertEquals(4, e5.rank());
        assertEquals(36, indexFn.apply(e5.rank(), e5.file()));

        assertEquals(7, h8.file());
        assertEquals(7, h8.rank());
        assertEquals(63, indexFn.apply(h8.rank(), h8.file()));
    }

    @Test
    void fileEHexMatchesBoardHex() {
        Bitboard board = new Bitboard();
        long fileE = FILE_E;

        setPiecesFromBits(fileE, board);

        String boardHexString = boardToHex.apply(board);
        String fileHexStr = binaryToStr.apply(fileE);
        assertEquals(fileHexStr, boardHexString);

        System.out.println(boardToStr.apply(board, true));
        System.out.println(boardHexString);
        System.out.println(fileHexStr);
    }

    @Test
    void fileAHexMatchesBoardHex() {
        Bitboard board = new Bitboard();
        long fileA = FILE_A;

        setPiecesFromBits(fileA, board);

        String boardHexString = boardToHex.apply(board);
        String fileHexStr = binaryToStr.apply(fileA);
        assertEquals(fileHexStr, boardHexString);

        System.out.println(boardToStr.apply(board, true));
        System.out.println(boardHexString);
        System.out.println(fileHexStr);
    }

    @Test
    void fileHHexMatchesBoardHex() {
        Bitboard board = new Bitboard();
        long fileH = FILE_H;

        setPiecesFromBits(fileH, board);

        String boardHexString = boardToHex.apply(board);
        String fileHexStr = binaryToStr.apply(fileH);
        assertEquals(fileHexStr, boardHexString);

        System.out.println(boardToStr.apply(board, true));
        System.out.println(boardHexString);
        System.out.println(fileHexStr);
    }

    private static void setPiecesFromBits(long bits, Bitboard board) {
        for (int i = 0; i < 64; i++) {
            if ((bits & (1L << i)) != 0) {
                board.setPiece(pawn, white, i);
            }
        }
    }
}
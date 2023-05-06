package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckHelperTest  implements Board, Chess {

    private boolean reverse = false;

    @Test
    void isSquareAttackedOnRank1() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        boolean reverse = false;

        board.setPiece(knight, white, d1.index());
        board.setPiece(pawn, black, e2.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(d1.index(), black));
        assertTrue(checkHelper.isSquareAttackedByPawn(f1.index(), black));
        assertFalse(checkHelper.isSquareAttackedByPawn(e1.index(), black));
    }

    @Test
    void isSquareAttackedOnFile1() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        board.setPiece(pawn, white, a4.index());
        board.setPiece(pawn, black, b5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(a4.index(), black));

        board.setPiece(pawn, white, a2.index());
        board.setPiece(pawn, black, b3.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(a2.index(), black));
        assertFalse(checkHelper.isSquareAttackedByPawn(a2.index(), white));
    }

    @Test
    void isSquareAttackedOnFile8() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        board.setPiece(pawn, white, h4.index());
        board.setPiece(pawn, black, g5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(h4.index(), black));
    }

    @Test
    void isSquareAttackedByCenterPawn() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        board.setPiece(pawn, white, e4.index());
        board.setPiece(pawn, black, d5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(e4.index(), black));
        assertFalse(checkHelper.isSquareAttackedByPawn(e4.index(), white));

        board.setPiece(pawn, black, d5.index());
        board.setPiece(pawn, white, c4.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(d5.index(), white));
        assertTrue(checkHelper.isSquareAttackedByPawn(e4.index(), black));
        assertFalse(checkHelper.isSquareAttackedByPawn(d5.index(), black));
        assertTrue(checkHelper.isSquareAttackedByPawn(d5.index(), white));
    }
}
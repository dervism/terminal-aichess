package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckHelperTest  implements Board, Chess {

    @Test
    void isSquareAttackedByPawn() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        boolean reverse = false;

        board.setPiece(pawn, white, e4.index());
        board.setPiece(pawn, black, d5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(e4.index(), black));
        assertFalse(checkHelper.isSquareAttackedByPawn(e4.index(), white));

        board.setPiece(pawn, white, a4.index());
        board.setPiece(pawn, black, b5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(a4.index(), black));

        board.setPiece(pawn, white, h4.index());
        board.setPiece(pawn, black, g5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByPawn(h4.index(), black));
    }
}
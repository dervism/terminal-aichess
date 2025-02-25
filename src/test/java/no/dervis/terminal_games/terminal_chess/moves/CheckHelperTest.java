package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckHelperTest  implements Board, Chess {

    private final boolean reverse = false;

    @Test
    void isSquareAttackedCastling() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        System.out.println(boardToStr.apply(board, true));

        List<Integer> moves = Arrays.asList(
                moveMaker.apply(e2, e4),
                moveMaker.apply(e7, e5),
                moveMaker.apply(f1, c4),
                moveMaker.apply(d7, d5),
                moveMaker.apply(g1, f3),
                moveMaker.apply(f8, b4),
                moveTypeMaker.apply(e1, g1).apply(MoveType.CASTLE_KING_SIDE)
        );

        moves.forEach(board::makeMove);

        System.out.println(boardToStr.apply(board, true));

        CheckHelper helper = new CheckHelper(board);
        assertFalse(helper.isSquareAttackedBySlidingPiece(e1.index(), black));
    }

    @Test
    void isSquareAttackedByOneBishops() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        System.out.println(boardToStr.apply(board, true));

        List<Integer> moves = Arrays.asList(
                moveMaker.apply(f8, b4),
                moveMaker.apply(d2, d4)
        );

        moves.forEach(board::makeMove);

        System.out.println(boardToStr.apply(board, true));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e1.index(), black));

        board.makeMove(moveMaker.apply(c2, c3));
        assertFalse(helper.isSquareAttackedBySlidingPiece(e1.index(), black));
    }

    @Test
    void isSquareAttackedByTwoBishops() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        System.out.println(boardToStr.apply(board, true));

        List<Integer> moves = Arrays.asList(
                moveMaker.apply(f8, b4),
                moveMaker.apply(c8, h4),
                moveMaker.apply(d2, d4),
                moveMaker.apply(c2, c3),
                moveMaker.apply(f2, f3)
        );

        moves.forEach(board::makeMove);

        System.out.println(boardToStr.apply(board, true));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e1.index(), black));
    }

    @Test
    void isSquareAttackedBySlidingPiece() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        System.out.println(boardToStr.apply(board, true));

        List<Integer> moves = Arrays.asList(
                moveMaker.apply(f2, f3),
                moveMaker.apply(e7, e5),
                moveMaker.apply(a2, a3),
                moveMaker.apply(d8, h4)
        );

        moves.forEach(board::makeMove);

        System.out.println(boardToStr.apply(board, true));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e1.index(), black));

        board.makeMove(moveMaker.apply(g2, g3));
        System.out.println(boardToStr.apply(board, true));

        assertFalse(helper.isSquareAttackedBySlidingPiece(e1.index(), black));
    }

    @Test
    void isSquareAttackedKnight() {
        Bitboard board = new Bitboard();
        CheckHelper checkHelper = new CheckHelper(board);

        // south (west)
        board.setPiece(rook, white, f8.index());
        board.setPiece(knight, black, e6.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(f8.index(), black));

        // west-south
        board.setPiece(pawn, white, e4.index());
        board.setPiece(knight, black, c3.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(e4.index(), black));

        // west
        board.setPiece(pawn, white, a4.index());
        board.setPiece(knight, black, c3.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(a4.index(), black));

        // north (west)
        board.setPiece(pawn, black, h4.index());
        board.setPiece(knight, white, g6.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(h4.index(), white));

        // north (east)
        board.setPiece(knight, white, b4.index());
        board.setPiece(knight, black, c6.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(b4.index(), black));

        // east-south
        board.setPiece(bishop, white, g5.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(g5.index(), black));

        // east-north
        board.setPiece(pawn, white, g7.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(g7.index(), black));

        // south (east)
        board.setPiece(pawn, white, d4.index());
        System.out.println(boardToStr.apply(board, reverse));
        assertTrue(checkHelper.isSquareAttackedByKnight(d4.index(), black));
    }

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
    void isSquareAttackedByRook() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, a4.index());
        board.setPiece(rook, white, h4.index());
        board.setPiece(king, black, e4.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedByRook(e4.index(), white));
    }

    @Test
    void isNotAttackedByRook() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, a3.index());
        board.setPiece(rook, white, h5.index());
        board.setPiece(king, black, e4.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertFalse(helper.isSquareAttackedByRook(e4.index(), white));
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

    @Test
    void isSquareAttackedBySlidingPieceTest1() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, e4.index());
        board.setPiece(king, black, e2.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e2.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest2() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, e4.index());
        board.setPiece(king, black, e7.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e7.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest3() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, a1.index());
        board.setPiece(bishop, black, e5.index());
        board.setPiece(king, black, h8.index());
        System.out.println(boardToStr.apply(board, true));

        CheckHelper helper = new CheckHelper(board);
        assertFalse(helper.isSquareAttackedBySlidingPiece(h8.index(), white));
    }
    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase1() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, e4.index());
        board.setPiece(king, black, a7.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertFalse(helper.isSquareAttackedBySlidingPiece(a7.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase2() {
        Bitboard board = new Bitboard();
        board.setPiece(bishop, white, e4.index());
        board.setPiece(king, black, c6.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(c6.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase3() {
        Bitboard board = new Bitboard();
        board.setPiece(bishop, white, g2.index());
        board.setPiece(queen, black, e4.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(e4.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase4() {
        Bitboard board = new Bitboard();
        board.setPiece(bishop, white, d4.index());
        board.setPiece(king, black, a7.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(a7.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase5() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, white, h1.index());
        board.setPiece(king, black, h7.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(h7.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase6() {
        Bitboard board = new Bitboard();
        board.setPiece(bishop, white, f6.index());
        board.setPiece(king, black, h8.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(h8.index(), white));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_AdditionalCase7() {
        Bitboard board = new Bitboard();
        board.setPiece(queen, black, d4.index());
        board.setPiece(king, white, a1.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(a1.index(), black));
    }

    @Test
    void isSquareAttackedBySlidingPieceTest_EdgeCase() {
        Bitboard board = new Bitboard();
        board.setPiece(queen, white, a8.index());
        board.setPiece(king, black, h1.index());
        System.out.println(boardToStr.apply(board, reverse));

        CheckHelper helper = new CheckHelper(board);
        assertTrue(helper.isSquareAttackedBySlidingPiece(h1.index(), white));

        board.setPiece(bishop, black, e4.index());
        System.out.println(boardToStr.apply(board, reverse));

        helper = new CheckHelper(board);
        assertFalse(helper.isSquareAttackedBySlidingPiece(h1.index(), white));
    }

}

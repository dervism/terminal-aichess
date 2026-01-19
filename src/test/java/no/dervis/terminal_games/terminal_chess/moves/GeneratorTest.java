package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.dervis.terminal_games.terminal_chess.board.Board.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest implements Chess {
    private Generator generator;
    private Bitboard board;

    @BeforeEach
    void setUp() {
        board = new Bitboard();
        generator = new Generator(board);
    }

    @Test
    void testKingInCheckByRook() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black rook at e8 (square 60)
        board.setPiece(rook, black, 60);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingInCheckByBishop() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black bishop at h4 (square 31)
        board.setPiece(bishop, black, 31);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingInCheckByQueen() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black queen at e8 (square 60)
        board.setPiece(queen, black, 60);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingInCheckByKnight() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black knight at f3 (square 21)
        board.setPiece(knight, black, 21);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingInCheckByPawn() {
        // White king at e4 (square 28)
        board.setPiece(king, white, 28);
        // Black pawn at f5 (square 37)
        board.setPiece(pawn, black, 37);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 28));
    }

    @Test
    void testKingInCheckByEnemyKing() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black king at e2 (square 12)
        board.setPiece(king, black, 12);

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheck() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black rook at a8 (square 56) - not attacking
        board.setPiece(rook, black, 56);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenRookBlockedByFriendlyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black rook at e8 (square 60)
        board.setPiece(rook, black, 60);
        // Black pawn at e4 (square 28) - blocking the rook
        board.setPiece(pawn, black, 28);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenRookBlockedByEnemyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black rook at e8 (square 60)
        board.setPiece(rook, black, 60);
        // White knight at e4 (square 28) - blocking the rook
        board.setPiece(knight, white, 28);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenBishopBlockedByFriendlyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black bishop at h4 (square 31)
        board.setPiece(bishop, black, 31);
        // Black pawn at f2 (square 13) - blocking the bishop
        board.setPiece(pawn, black, 13);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenBishopBlockedByEnemyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black bishop at h4 (square 31)
        board.setPiece(bishop, black, 31);
        // White pawn at f2 (square 13) - blocking the bishop
        board.setPiece(pawn, white, 13);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenQueenBlockedByFriendlyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black queen at e8 (square 60)
        board.setPiece(queen, black, 60);
        // Black knight at e4 (square 28) - blocking the queen
        board.setPiece(knight, black, 28);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKingNotInCheckWhenQueenBlockedByEnemyPiece() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black queen at h4 (square 31)
        board.setPiece(queen, black, 31);
        // White pawn at f2 (square 13) - blocking the queen diagonally
        board.setPiece(pawn, white, 13);

        System.out.println(boardToStr.apply(board, true));

        assertFalse(generator.isKingInCheck(white, 4));
    }

    @Test
    void testKnightNotBlockedByPieces() {
        // White king at e1 (square 4)
        board.setPiece(king, white, 4);
        // Black knight at f3 (square 21)
        board.setPiece(knight, black, 21);
        // Pieces that don't block knight's movement
        board.setPiece(pawn, black, 12); // e2
        board.setPiece(pawn, white, 13); // f2
        board.setPiece(pawn, black, 20); // e3

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 4));
    }

    @Test
    void testPawnNotBlockedByPieces() {
        // White king at e4 (square 28)
        board.setPiece(king, white, 28);
        // Black pawn at f5 (square 37)
        board.setPiece(pawn, black, 37);
        // Pieces that don't block pawn's attack
        board.setPiece(pawn, black, 36); // e5
        board.setPiece(pawn, white, 29); // f4

        System.out.println(boardToStr.apply(board, true));

        assertTrue(generator.isKingInCheck(white, 28));
    }

    @Test
    void testKingInCheckScenario() {
        Bitboard board = new Bitboard();
        board.setPiece(rook, black, a8.index());
        board.setPiece(queen, black, d8.index());
        board.setPiece(king, black, e8.index());
        board.setPiece(bishop, black, f8.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(pawn, black, a7.index());
        board.setPiece(pawn, black, b7.index());
        board.setPiece(pawn, black, c7.index());
        board.setPiece(pawn, black, e7.index());
        board.setPiece(pawn, black, f7.index());
        board.setPiece(pawn, black, h7.index());
        board.setPiece(knight, black, h6.index());
        board.setPiece(pawn, white, g6.index());
        board.setPiece(pawn, black, d6.index());
        board.setPiece(king, white, e1.index());
        board.setPiece(pawn, white, a2.index());
        board.setPiece(pawn, white, b2.index());
        board.setPiece(pawn, white, c2.index());
        board.setPiece(pawn, white, f2.index());
        board.setPiece(pawn, white, g2.index());
        board.setPiece(pawn, white, h2.index());
        board.setPiece(pawn, white, d4.index());
        board.setPiece(bishop, white, c4.index());
        board.setPiece(bishop, white, c1.index());
        board.setPiece(knight, black, b4.index());
        board.setPiece(knight, white, b1.index());
        board.setPiece(rook, white, a1.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(queen, white, f3.index());

        board.makeMove(Move.createMove(f3.index(), f7.index(), MoveType.NORMAL.ordinal()));

        System.out.println(boardToStr.apply(board, false));

        assertEquals(queen, board.getPiece(f7.index()));

        Generator generator = new Generator(board);

        boolean blackKingInCheck = generator.isKingInCheck(black, e8.index());
        assertTrue(blackKingInCheck, "Black king should be in check due to the white queen on F7.");

        List<Integer> moves = generator.generateMoves(board.turn());

        assertEquals(2, moves.size(), "There should be two moves to get the king out of check.");
    }
}

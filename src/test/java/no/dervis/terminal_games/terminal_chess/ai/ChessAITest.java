package no.dervis.terminal_games.terminal_chess.ai;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessAITest implements Chess, Board {

    @Test
    void testFindsLegalMoveFromStartingPosition() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        ChessAI ai = new ChessAI();

        int move = ai.findBestMove(board, 1000);
        assertNotEquals(0, move, "AI should find a legal move from starting position.");

        int from = move >>> 14;
        int to = (move >>> 7) & 0x3F;
        assertTrue(from >= 0 && from < 64, "From square should be valid.");
        assertTrue(to >= 0 && to < 64, "To square should be valid.");
    }

    @Test
    void testFindsMateInOne() {
        // Classic back-rank mate: White Rook a1 delivers Ra8#.
        // Black king trapped behind own pawns on f7, g7, h7.
        Bitboard board = new Bitboard();
        board.setPiece(king, white, g1.index());
        board.setPiece(rook, white, a1.index());
        board.setPiece(king, black, g8.index());
        board.setPiece(pawn, black, f7.index());
        board.setPiece(pawn, black, g7.index());
        board.setPiece(pawn, black, h7.index());

        System.out.println(boardToStr.apply(board, true));

        ChessAI ai = new ChessAI();
        int move = ai.findBestMove(board, 2000);

        int to = (move >>> 7) & 0x3F;
        assertEquals(a8.index(), to,
                "AI should find Ra8# (back-rank mate), but moved to square " + to);
    }

    @Test
    void testCapturesFreeQueen() {
        // White to move, black queen undefended on d4, white knight can capture
        Bitboard board = new Bitboard();
        board.setPiece(king, white, e1.index());
        board.setPiece(knight, white, e2.index());
        board.setPiece(king, black, e8.index());
        board.setPiece(queen, black, d4.index());

        System.out.println(boardToStr.apply(board, true));

        ChessAI ai = new ChessAI();
        int move = ai.findBestMove(board, 1000);

        int to = (move >>> 7) & 0x3F;
        assertEquals(d4.index(), to,
                "AI should capture the free queen on d4.");
    }

    @Test
    void testEvaluationSymmetry() {
        // Starting position should evaluate close to 0 (with tempo bonus)
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        int eval = Evaluation.evaluate(board);
        assertTrue(Math.abs(eval) < 50,
                "Starting position evaluation should be near 0, got " + eval);
    }
}

package no.dervis.terminal_games.terminal_chess.ai;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ParallelChessAI;
import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParallelChessAITest implements Chess, Board {

    @Test
    void testFindsLegalMoveFromStartingPosition() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        ParallelChessAI ai = new ParallelChessAI(4);

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
        Bitboard board = new Bitboard();
        board.setPiece(king, white, g1.index());
        board.setPiece(rook, white, a1.index());
        board.setPiece(king, black, g8.index());
        board.setPiece(pawn, black, f7.index());
        board.setPiece(pawn, black, g7.index());
        board.setPiece(pawn, black, h7.index());

        ParallelChessAI ai = new ParallelChessAI(4);
        int move = ai.findBestMove(board, 2000);

        int to = (move >>> 7) & 0x3F;
        assertEquals(a8.index(), to,
                "AI should find Ra8# (back-rank mate), but moved to square " + to);
    }

    @Test
    void testCapturesFreeQueen() {
        Bitboard board = new Bitboard();
        board.setPiece(king, white, e1.index());
        board.setPiece(knight, white, e2.index());
        board.setPiece(king, black, e8.index());
        board.setPiece(queen, black, d4.index());

        ParallelChessAI ai = new ParallelChessAI(4);
        int move = ai.findBestMove(board, 1000);

        int to = (move >>> 7) & 0x3F;
        assertEquals(d4.index(), to,
                "AI should capture the free queen on d4.");
    }

    @Test
    void testSingleThreadFallback() {
        // Verify it works with just 1 thread (no parallelism)
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        ParallelChessAI ai = new ParallelChessAI(1);

        int move = ai.findBestMove(board, 500);
        assertNotEquals(0, move, "Single-thread mode should find a legal move.");
    }
}

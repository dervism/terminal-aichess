package no.dervis.terminal_games.game_ai;

import no.dervis.terminal_games.bitcheckers.BitCheckersBoard;
import no.dervis.terminal_games.bitcheckers.BitCheckersEvaluation;
import no.dervis.terminal_games.bitcheckers.CheckersMove;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.dervis.terminal_games.bitcheckers.BitCheckersBoard.BLACK;
import static no.dervis.terminal_games.bitcheckers.BitCheckersBoard.WHITE;
import static org.junit.jupiter.api.Assertions.*;

class AlphaBetaSearchTest {

    private final AlphaBetaSearch<CheckersMove> search =
            new AlphaBetaSearch<>(new BitCheckersEvaluation());

    @Test
    void findsOnlyLegalMove() {
        BitCheckersBoard board = new BitCheckersBoard();
        // Single black man with exactly one legal move
        board.setPiece(BLACK, false, 0); // a1, can only go to b2 (sq 9)
        board.setPiece(WHITE, false, 63); // keep white alive
        board.setTurn(BLACK);

        var result = search.findBestMove(board, 4);
        assertNotNull(result.move());
        assertEquals(0, result.move().from());
        assertEquals(9, result.move().to());
    }

    @Test
    void choosesCaptureMoveOverSimpleMove() {
        BitCheckersBoard board = new BitCheckersBoard();
        // Black man on a1 (sq 0) — can simple-move to b2 (sq 9)
        // Black man on c3 (sq 18), white man on d4 (sq 27) — jump available to e5 (sq 36)
        board.setPiece(BLACK, false, 0);
        board.setPiece(BLACK, false, 18);
        board.setPiece(WHITE, false, 27);
        board.setPiece(WHITE, false, 63); // keep white alive
        board.setTurn(BLACK);

        var result = search.findBestMove(board, 4);
        // Forced capture rule means only jumps are legal
        assertTrue(result.move().isJump(), "AI should play the capture move");
    }

    @Test
    void findsForcedWinByCapturingLastPiece() {
        BitCheckersBoard board = new BitCheckersBoard();
        // Black men on c3 (sq 18) and e3 (sq 20)
        // White man on d4 (sq 27) — jumpable by both c3 and e3
        // Both jumps capture white's only piece → win
        board.setPiece(BLACK, false, 18);
        board.setPiece(BLACK, false, 20);
        board.setPiece(WHITE, false, 27);
        board.setTurn(BLACK);

        var result = search.findBestMove(board, 6);
        assertNotNull(result.move());
        assertTrue(result.move().isJump(), "AI should play the capture");
        assertTrue(result.score() > AlphaBetaSearch.WIN_THRESHOLD,
                "Score should indicate a win: " + result.score());
    }

    @Test
    void searchReturnsWithinTimeLimit() {
        BitCheckersBoard board = new BitCheckersBoard();
        board.initialise();

        long start = System.currentTimeMillis();
        var result = search.findBestMove(board, 500L);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result.move());
        assertTrue(elapsed < 2000, "Search should complete within a reasonable time. Elapsed: " + elapsed);
    }

    @Test
    void fixedDepthSearchReturnsMove() {
        BitCheckersBoard board = new BitCheckersBoard();
        board.initialise();

        var result = search.findBestMove(board, 3);
        assertNotNull(result.move());
        assertTrue(result.depth() > 0);
        assertTrue(result.nodes() > 0);
    }

    @Test
    void searchResultContainsMetadata() {
        BitCheckersBoard board = new BitCheckersBoard();
        board.initialise();

        var result = search.findBestMove(board, 4);
        assertNotNull(result.move());
        assertTrue(result.depth() >= 1, "Should search at least depth 1");
        assertTrue(result.nodes() >= 1, "Should search at least 1 node");
    }

    @Test
    void aiDoesNotCrashOnTerminalPosition() {
        BitCheckersBoard board = new BitCheckersBoard();
        // Only black pieces — white has none → terminal
        board.setPiece(BLACK, false, 0);
        board.setTurn(BLACK);

        List<CheckersMove> moves = board.generateMoves();
        // Terminal, so generateMoves may return empty or moves.
        // Either way, search should handle it gracefully.
        if (!moves.isEmpty()) {
            var result = search.findBestMove(board, 4);
            assertNotNull(result.move());
        }
    }
}

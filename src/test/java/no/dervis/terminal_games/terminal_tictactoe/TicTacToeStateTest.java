package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.terminal_games.game_ai.AlphaBetaSearch;
import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.PlayerSymbol;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicTacToeStateTest {

    private final AlphaBetaSearch<Integer> search =
            new AlphaBetaSearch<>(new TicTacToeEvaluation());

    // ---- GameState wrapper tests ----

    @Test
    void generateMovesReturnsAllFreeSquares() {
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);
        assertEquals(9, state.generateMoves().size());
    }

    @Test
    void makeMoveReducesFreeSquares() {
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);
        state.makeMove(4); // centre
        assertEquals(8, state.generateMoves().size());
        assertEquals(PlayerSymbol.X, game.getCellValue(4));
    }

    @Test
    void makeMoveAlternatesTurn() {
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);
        assertEquals(PlayerSymbol.X, state.currentSymbol());
        state.makeMove(0);
        assertEquals(PlayerSymbol.O, state.currentSymbol());
        state.makeMove(1);
        assertEquals(PlayerSymbol.X, state.currentSymbol());
    }

    @Test
    void unmakeMoveRestoresState() {
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);
        state.makeMove(4);
        state.unmakeMove(4);
        assertEquals(PlayerSymbol.E, game.getCellValue(4));
        assertEquals(PlayerSymbol.X, state.currentSymbol());
        assertEquals(9, state.generateMoves().size());
    }

    @Test
    void isTerminalDetectsWin() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X,
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.O);
        assertTrue(state.isTerminal());
    }

    @Test
    void isTerminalDetectsDraw() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.X,
                PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.O,
                PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.O
        ));
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);
        assertTrue(state.isTerminal());
        assertEquals(0, state.terminalScore());
    }

    @Test
    void terminalScoreIsNegativeForLoser() {
        TicTacToe game = new TicTacToe();
        // X has won — it's O's turn, so O is the loser
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X,
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.O);
        assertEquals(-AlphaBetaSearch.WIN_SCORE, state.terminalScore());
    }

    // ---- AI behaviour tests ----

    @Test
    void aiBlocksImmediateWin() {
        // X has two in a row, about to win. O must block.
        //   X | X | .
        //   O | . | .
        //   . | . | .
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.E,
                PlayerSymbol.O, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.O);
        var result = search.findBestMove(state, 10);
        assertEquals(2, result.move(), "O must block at position 2 (top-right)");
    }

    @Test
    void aiTakesImmediateWin() {
        // O has two in a row, can win immediately.
        //   O | O | .
        //   X | X | .
        //   . | . | .
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.O);
        var result = search.findBestMove(state, 10);
        assertEquals(2, result.move(), "O should complete the top row");
        assertTrue(result.score() > AlphaBetaSearch.WIN_THRESHOLD);
    }

    @Test
    void perfectPlayDrawsOn3x3() {
        // Two perfect players should always draw on 3×3.
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);

        while (!state.isTerminal()) {
            var result = search.findBestMove(state, 10);
            state.makeMove(result.move());
        }

        assertEquals(0, state.terminalScore(), "Perfect play on 3×3 must be a draw");
    }

    @Test
    void makeUnmakeRoundTripMultipleMoves() {
        TicTacToe game = new TicTacToe();
        TicTacToeState state = new TicTacToeState(game, PlayerSymbol.X);

        state.makeMove(0);
        state.makeMove(4);
        state.makeMove(8);

        state.unmakeMove(8);
        state.unmakeMove(4);
        state.unmakeMove(0);

        assertEquals(9, state.generateMoves().size());
        assertEquals(PlayerSymbol.X, state.currentSymbol());
        for (int i = 0; i < 9; i++) {
            assertEquals(PlayerSymbol.E, game.getCellValue(i));
        }
    }
}

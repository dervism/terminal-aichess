package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.terminal_games.game_ai.Evaluator;
import no.dervis.terminal_games.game_ai.GameState;
import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.PlayerSymbol;

/**
 * Evaluates non-terminal TicTacToe positions from the current player's
 * perspective. Scores potential winning lines: a line with only friendly
 * symbols and empty cells is worth 4^(friendly count).
 *
 * <p>For standard 3×3, the search reaches terminal nodes at full depth
 * so this evaluator is rarely needed. On larger boards (e.g. 6×6 with
 * 4-in-a-row) it guides the search when depth is limited.</p>
 */
public class TicTacToeEvaluation implements Evaluator<Integer> {

    @Override
    public int evaluate(GameState<Integer> state) {
        TicTacToeState s = (TicTacToeState) state;
        TicTacToe game = s.game();
        PlayerSymbol me  = s.currentSymbol();
        PlayerSymbol opp = (me == PlayerSymbol.X) ? PlayerSymbol.O : PlayerSymbol.X;
        return lineThreats(game, me, opp) - lineThreats(game, opp, me);
    }

    private int lineThreats(TicTacToe game, PlayerSymbol symbol, PlayerSymbol opponent) {
        int size = (int) Math.sqrt(game.getBoardSize());
        int win  = game.xInARow;
        int score = 0;

        // Rows
        for (int r = 0; r < size; r++)
            for (int c = 0; c <= size - win; c++)
                score += scoreLine(game, r * size + c, 1, win, symbol, opponent);

        // Columns
        for (int c = 0; c < size; c++)
            for (int r = 0; r <= size - win; r++)
                score += scoreLine(game, r * size + c, size, win, symbol, opponent);

        // Diagonals (\)
        for (int r = 0; r <= size - win; r++)
            for (int c = 0; c <= size - win; c++)
                score += scoreLine(game, r * size + c, size + 1, win, symbol, opponent);

        // Anti-diagonals (/)
        for (int r = 0; r <= size - win; r++)
            for (int c = win - 1; c < size; c++)
                score += scoreLine(game, r * size + c, size - 1, win, symbol, opponent);

        return score;
    }

    private int scoreLine(TicTacToe game, int start, int step, int len,
                           PlayerSymbol me, PlayerSymbol opp) {
        int myCount = 0;
        for (int i = 0; i < len; i++) {
            PlayerSymbol cell = game.getCellValue(start + i * step);
            if (cell == opp) return 0;  // blocked by opponent
            if (cell == me) myCount++;
        }
        if (myCount == 0) return 0;
        // Exponential scoring: 2-in-a-row worth much more than 1
        return (int) Math.pow(4, myCount);
    }
}

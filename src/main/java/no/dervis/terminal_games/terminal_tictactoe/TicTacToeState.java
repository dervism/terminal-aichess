package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.terminal_games.game_ai.AlphaBetaSearch;
import no.dervis.terminal_games.game_ai.GameState;
import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.PlayerSymbol;
import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.State;

import java.util.List;

/**
 * Adapts {@link TicTacToe} to the generic {@link GameState} interface so that
 * {@link AlphaBetaSearch} can play it. Moves are represented as board-cell
 * indices (0-based).
 */
public class TicTacToeState implements GameState<Integer> {

    private final TicTacToe game;
    private PlayerSymbol currentSymbol;

    public TicTacToeState(TicTacToe game, PlayerSymbol firstToMove) {
        this.game = game;
        this.currentSymbol = firstToMove;
    }

    public TicTacToe game() { return game; }
    public PlayerSymbol currentSymbol() { return currentSymbol; }

    @Override
    public List<Integer> generateMoves() {
        return game.getFreeSquares(game.board()).stream()
                .map(TicTacToe.Cell::i)
                .toList();
    }

    @Override
    public void makeMove(Integer move) {
        game.setMove(move, currentSymbol);
        currentSymbol = (currentSymbol == PlayerSymbol.X) ? PlayerSymbol.O : PlayerSymbol.X;
    }

    @Override
    public void unmakeMove(Integer move) {
        game.setMove(move, PlayerSymbol.E);
        currentSymbol = (currentSymbol == PlayerSymbol.X) ? PlayerSymbol.O : PlayerSymbol.X;
    }

    @Override
    public boolean isTerminal() {
        return game.checkGameState() != State.InProgress;
    }

    @Override
    public int terminalScore() {
        State state = game.checkGameState();
        if (state == State.Draw) return 0;
        // If someone won, the winner is the player who just moved (the opponent
        // of the current player), so the current player lost.
        return -AlphaBetaSearch.WIN_SCORE;
    }
}

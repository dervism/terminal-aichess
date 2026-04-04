package no.dervis.terminal_games.game_ai;

import java.util.List;

/**
 * Represents a mutable game state for use with alpha-beta search.
 * All scores are from the perspective of the side to move:
 * positive means good for the current player.
 *
 * @param <M> the move type
 */
public interface GameState<M> {

    /** Generate all legal moves for the side to move. */
    List<M> generateMoves();

    /** Apply a move, switching the side to move. */
    void makeMove(M move);

    /** Undo a move, restoring the previous state. */
    void unmakeMove(M move);

    /** True when the game is over (no legal moves, no pieces, etc.). */
    boolean isTerminal();

    /**
     * Score of a terminal position from the current player's perspective.
     * Only called when {@link #isTerminal()} returns true.
     * Typical values: positive = current player wins, negative = loss, 0 = draw.
     */
    int terminalScore();
}

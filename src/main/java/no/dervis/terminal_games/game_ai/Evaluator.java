package no.dervis.terminal_games.game_ai;

/**
 * Evaluates a non-terminal game position.
 * Returns a score from the side to move's perspective:
 * positive means the position favours the current player.
 *
 * @param <M> the move type
 */
@FunctionalInterface
public interface Evaluator<M> {
    int evaluate(GameState<M> state);
}

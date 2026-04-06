package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;

/**
 * Common interface for chess AI engines.
 */
public interface Engine {

    /**
     * Finds the best move for the current side to move.
     *
     * @param board       the current board position
     * @param timeLimitMs maximum time in milliseconds for the search
     * @return the best move (encoded int), or 0 if no legal moves
     */
    int findBestMove(Bitboard board, long timeLimitMs);

    /**
     * Resets per-game state so the engine can be reused across multiple games
     * (e.g. in a tournament). The default implementation is a no-op.
     */
    default void reset() {}
}

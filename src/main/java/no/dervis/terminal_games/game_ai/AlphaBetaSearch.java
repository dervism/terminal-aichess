package no.dervis.terminal_games.game_ai;

import java.util.List;

/**
 * Generic negamax alpha-beta search with iterative deepening and time management.
 * Works with any game that implements {@link GameState} and any {@link Evaluator}.
 *
 * @param <M> the move type
 */
public class AlphaBetaSearch<M> {

    public static final int INFINITY = 100_000;
    public static final int WIN_SCORE = 99_000;
    public static final int WIN_THRESHOLD = 98_000;

    private final Evaluator<M> evaluator;
    private long deadline;
    private boolean timeUp;
    private int nodesSearched;
    private int depthReached;

    public AlphaBetaSearch(Evaluator<M> evaluator) {
        this.evaluator = evaluator;
    }

    /** Time-limited iterative deepening search. */
    public SearchResult<M> findBestMove(GameState<M> state, long timeLimitMs) {
        deadline = System.currentTimeMillis() + timeLimitMs;
        timeUp = false;
        return iterativeDeepening(state, 64, true);
    }

    /** Fixed-depth search (no time limit). */
    public SearchResult<M> findBestMove(GameState<M> state, int maxDepth) {
        deadline = Long.MAX_VALUE;
        timeUp = false;
        return iterativeDeepening(state, maxDepth, false);
    }

    /**
     * Performs an iterative deepening search to find the best move in a game state.
     * This method incrementally increases the search depth, using alpha-beta pruning
     * at each depth level to evaluate moves. It can optionally enforce a time limit
     * to terminate the search early if required.
     *
     * @param state         the current game state from which moves are analyzed
     * @param maxDepth      the maximum depth to search in the game tree
     * @param useTimeLimit  whether to enforce a time limit during the search
     * @return a SearchResult object containing the best move found, its score,
     *         the maximum depth reached, and the number of nodes searched
     */
    private SearchResult<M> iterativeDeepening(GameState<M> state, int maxDepth, boolean useTimeLimit) {
        M bestMove = null;
        int bestScore = 0;

        List<M> moves = state.generateMoves();
        if (moves.isEmpty()) return new SearchResult<>(null, 0, 0, 0);
        if (moves.size() == 1) return new SearchResult<>(moves.getFirst(), 0, 1, 0);

        for (int depth = 1; depth <= maxDepth; depth++) {
            nodesSearched = 0;
            M currentBest = null;
            int alpha = -INFINITY;

            for (M move : moves) {
                state.makeMove(move);
                int score = -alphaBeta(state, depth - 1, -INFINITY, -alpha);
                state.unmakeMove(move);

                if (timeUp) break;

                if (score > alpha) {
                    alpha = score;
                    currentBest = move;
                }
            }

            if (timeUp && currentBest == null) break;

            if (currentBest != null) {
                bestMove = currentBest;
                bestScore = alpha;
                depthReached = depth;
            }

            if (Math.abs(bestScore) > WIN_THRESHOLD) break;
            if (useTimeLimit && System.currentTimeMillis() > deadline - (deadline - System.currentTimeMillis() + 1) / 3)
                break;
        }

        return new SearchResult<>(
                bestMove != null ? bestMove : moves.getFirst(),
                bestScore, depthReached, nodesSearched);
    }

    /**
     * Performs an alpha-beta pruning search to evaluate the optimal score for the current player
     * within the given game state.
     * Alpha-beta pruning reduces the number of nodes evaluated in the search tree by eliminating
     * branches that cannot produce a better result than previously examined possibilities.
     *
     * The % 4096 is a performance optimization to avoid calling System.currentTimeMillis() on every single node. System clock calls involve a kernel syscall which is relatively expensive compared to the integer arithmetic of the search itself. Checking every 4096th node amortizes that cost — the search stays responsive to the time limit (at worst overshooting by ~4096 nodes) while keeping the per-node overhead near zero.
     *
     * @param state the current game state, representing the position in the game
     *              to be evaluated from the current player's perspective
     * @param depth the maximum depth to search in the game tree
     * @param alpha the best score that the maximizing player can guarantee
     * @param beta  the best score that the minimizing player can guarantee
     * @return the evaluated score of the game state for the current player, adjusted
     *         based on search results and alpha-beta pruning
     */
    private int alphaBeta(GameState<M> state, int depth, int alpha, int beta) {
        if (timeUp) return 0;
        nodesSearched++;
        if (nodesSearched % 4096 == 0 && System.currentTimeMillis() > deadline) {
            timeUp = true;
            return 0;
        }

        if (state.isTerminal()) return state.terminalScore();
        if (depth <= 0) return evaluator.evaluate(state);

        List<M> moves = state.generateMoves();
        if (moves.isEmpty()) return state.terminalScore();

        for (M move : moves) {
            state.makeMove(move);
            int score = -alphaBeta(state, depth - 1, -beta, -alpha);
            state.unmakeMove(move);

            if (timeUp) return 0;
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }

        return alpha;
    }

}

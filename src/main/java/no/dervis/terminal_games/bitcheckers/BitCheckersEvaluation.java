package no.dervis.terminal_games.bitcheckers;

import no.dervis.terminal_games.game_ai.Evaluator;
import no.dervis.terminal_games.game_ai.GameState;

/**
 * Evaluator for bitboard checkers. Returns a score from the current player's
 * perspective: positive is good for the side to move.
 *
 * <p>Features:
 * <ul>
 *   <li>Material: men=100, kings=160</li>
 *   <li>Advancement bonus: men closer to promotion are worth more</li>
 *   <li>Centre control: pieces on the four centre columns score a small bonus</li>
 * </ul>
 */
public class BitCheckersEvaluation implements Evaluator<CheckersMove> {

    private static final int MAN_VALUE  = 100;
    private static final int KING_VALUE = 160;
    private static final int ADVANCE_BONUS = 5;  // per row advanced
    private static final int CENTRE_BONUS  = 3;

    // Columns c-f (indices 2-5)
    private static final long CENTRE_FILES =
            0x3C3C3C3C3C3C3C3CL; // bits 2-5 set in every rank

    @Override
    public int evaluate(GameState<CheckersMove> state) {
        BitCheckersBoard board = (BitCheckersBoard) state;

        long bMen   = board.black() & ~board.kings();
        long bKings = board.black() & board.kings();
        long wMen   = board.white() & ~board.kings();
        long wKings = board.white() & board.kings();

        int blackScore = Long.bitCount(bMen) * MAN_VALUE
                       + Long.bitCount(bKings) * KING_VALUE
                       + advancementScore(bMen, BitCheckersBoard.BLACK)
                       + Long.bitCount((bMen | bKings) & CENTRE_FILES) * CENTRE_BONUS;

        int whiteScore = Long.bitCount(wMen) * MAN_VALUE
                       + Long.bitCount(wKings) * KING_VALUE
                       + advancementScore(wMen, BitCheckersBoard.WHITE)
                       + Long.bitCount((wMen | wKings) & CENTRE_FILES) * CENTRE_BONUS;

        int raw = (board.turn() == BitCheckersBoard.BLACK)
                ? blackScore - whiteScore
                : whiteScore - blackScore;

        return raw;
    }

    /** Bonus for men advancing towards promotion rank. */
    private int advancementScore(long men, int color) {
        int score = 0;
        long remaining = men;
        while (remaining != 0) {
            int sq = Long.numberOfTrailingZeros(remaining);
            int row = sq / 8;
            // Black promotes on row 7 (advancement = row);
            // White promotes on row 0 (advancement = 7 - row).
            int advancement = (color == BitCheckersBoard.BLACK) ? row : 7 - row;
            score += advancement * ADVANCE_BONUS;
            remaining &= remaining - 1;
        }
        return score;
    }
}

package no.dervis.terminal_games.bitcheckers;

/**
 * A checkers move. For multi-jumps, {@code captured} holds all captured pieces
 * in the chain. {@code prevKings} is a snapshot of the kings bitboard before the
 * move, used by {@link BitCheckersBoard#unmakeMove} to restore king state.
 *
 * @param from      square index the piece starts at (0-63)
 * @param to        square index the piece ends at (0-63)
 * @param captured  bitboard of all captured opponent pieces (0 for simple moves)
 * @param prevKings kings bitboard before the move
 */
public record CheckersMove(int from, int to, long captured, long prevKings) {

    public boolean isJump() {
        return captured != 0;
    }

    public int captureCount() {
        return Long.bitCount(captured);
    }

    @Override
    public String toString() {
        String fromStr = squareName(from);
        String toStr = squareName(to);
        return isJump()
                ? fromStr + "x" + toStr + " (" + captureCount() + " captured)"
                : fromStr + "-" + toStr;
    }

    private static String squareName(int square) {
        char col = (char) ('a' + square % 8);
        int row = square / 8 + 1;
        return "" + col + row;
    }
}

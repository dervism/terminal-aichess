package no.dervis.terminal_games.terminal_checkers;

public record Move(Player player, Cell from, Cell to) {

    public String toAlgebraic() {
        return squareName(from) + "-" + squareName(to);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }

    private static String squareName(Cell cell) {
        char col = (char) ('a' + cell.position().col());
        int row = cell.position().row() + 1;
        return "" + col + row;
    }
}

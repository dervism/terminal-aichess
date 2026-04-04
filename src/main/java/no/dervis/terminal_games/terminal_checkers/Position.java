package no.dervis.terminal_games.terminal_checkers;

public record Position(int row, int col) {
    public Cell toCell() {
        return Cell.fromCellNumber((row() * 8) + col() + 1);
    }
}

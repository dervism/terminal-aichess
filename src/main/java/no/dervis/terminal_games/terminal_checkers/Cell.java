package no.dervis.terminal_games.terminal_checkers;

public record Cell(Integer cellNr, Position position) {
    static Cell fromCellNumber(int cellNr) {
        int row = (cellNr - 1) / 8;
        int col = (cellNr - 1) % 8;
        return new Cell(cellNr, new Position(row, col));
    }
}

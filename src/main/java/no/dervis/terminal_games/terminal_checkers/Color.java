package no.dervis.terminal_games.terminal_checkers;

public enum Color {
    WHITE, WHITE_KING, BLACK, BLACK_KING, EMPTY;

    public boolean isKing() {
        return this == WHITE_KING || this == BLACK_KING;
    }

    public Color opposite() {
        return switch (this) {
            case WHITE, WHITE_KING -> BLACK;
            case BLACK, BLACK_KING -> WHITE;
            default -> EMPTY;
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case WHITE -> "⛀";
            case WHITE_KING -> "⛁";
            case BLACK -> "⛂";
            case BLACK_KING -> "⛃";
            case EMPTY -> " ";
        };
    }
}

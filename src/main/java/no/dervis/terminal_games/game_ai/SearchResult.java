package no.dervis.terminal_games.game_ai;

public record SearchResult<M>(M move, int score, int depth, int nodes) {
}

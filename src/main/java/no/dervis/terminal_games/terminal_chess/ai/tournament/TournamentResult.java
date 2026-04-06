package no.dervis.terminal_games.terminal_chess.ai.tournament;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated results for a completed tournament.
 */
public record TournamentResult(
        LocalDateTime startTime,
        LocalDateTime endTime,
        EngineConfig engine1,
        EngineConfig engine2,
        int totalGames,
        List<GameResult> games
) {

    public int engine1Wins() {
        return (int) games.stream()
                .filter(g -> (g.whiteName().equals(engine1.name()) && g.isWhiteWin())
                          || (g.blackName().equals(engine1.name()) && g.isBlackWin()))
                .count();
    }

    public int engine2Wins() {
        return (int) games.stream()
                .filter(g -> (g.whiteName().equals(engine2.name()) && g.isWhiteWin())
                          || (g.blackName().equals(engine2.name()) && g.isBlackWin()))
                .count();
    }

    public int draws() {
        return (int) games.stream().filter(GameResult::isDraw).count();
    }

    public String summary() {
        return """
                Tournament: %s vs %s
                Games: %d | %s wins: %d | %s wins: %d | Draws: %d"""
                .formatted(
                        engine1.name(), engine2.name(),
                        totalGames,
                        engine1.name(), engine1Wins(),
                        engine2.name(), engine2Wins(),
                        draws()
                );
    }
}

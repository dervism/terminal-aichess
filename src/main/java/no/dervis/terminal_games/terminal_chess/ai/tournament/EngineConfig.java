package no.dervis.terminal_games.terminal_chess.ai.tournament;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.Engine;

/**
 * Wraps an {@link Engine} with a human-readable name and a think-time
 * that acts as the engine's "strength" setting.
 */
public record EngineConfig(String name, Engine engine, long thinkTimeMs) {

    public String strengthLabel() {
        return switch ((int) thinkTimeMs) {
            case 1000  -> "Easy";
            case 3000  -> "Medium";
            case 5000  -> "Hard";
            case 10000 -> "Expert";
            default    -> thinkTimeMs + "ms";
        };
    }
}

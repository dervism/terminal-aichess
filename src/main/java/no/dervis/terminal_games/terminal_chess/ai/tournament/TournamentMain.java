package no.dervis.terminal_games.terminal_chess.ai.tournament;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.BookEngine;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ChessAI;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.Engine;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ParallelChessAI;

import java.nio.file.Path;

/**
 * Sample program that runs a tournament between chess engines.
 *
 * <p>Usage: {@code java TournamentMain [rounds] [thinkTimeMs] [matchup] [book]}
 * <ul>
 *   <li>{@code matchup=1} — Single vs Parallel (default)</li>
 *   <li>{@code matchup=2} — Single vs Parallel</li>
 *   <li>{@code book=true|false} — use opening book (default: true)</li>
 * </ul>
 */
public class TournamentMain {

    public static void main(String[] args) {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        long thinkTimeMs = args.length > 1 ? Long.parseLong(args[1]) : 3000;
        int matchup = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        boolean useBook = true;

        EngineConfig e1;
        EngineConfig e2;

        final boolean verbose = false;

        switch (matchup) {
            case 2 -> {
                e1 = new EngineConfig("Single", maybeWithBook(new ChessAI(verbose), useBook), thinkTimeMs);
                e2 = new EngineConfig("Parallel", maybeWithBook(new ParallelChessAI(verbose), useBook), thinkTimeMs);
            }
            default -> {
                e1 = new EngineConfig("Single", maybeWithBook(new ChessAI(verbose), useBook), thinkTimeMs);
                e2 = new EngineConfig("Parallel", maybeWithBook(new ParallelChessAI(verbose), useBook), thinkTimeMs);
            }
        }

        System.out.printf("Starting tournament: %s vs %s%n", e1.name(), e2.name());
        System.out.printf("Rounds: %d | Think time: %d ms | Book: %s%n%n", rounds, thinkTimeMs, useBook);

        TournamentResult result = Tournament.builder()
                .engine1(e1)
                .engine2(e2)
                .rounds(rounds)
                .outputDir(Path.of("tournaments"))
                .verbose(true)
                .build()
                .run();

        System.out.println();
        System.out.println(result.summary());
    }

    private static Engine maybeWithBook(Engine engine, boolean useBook) {
        return useBook ? new BookEngine(engine) : engine;
    }
}

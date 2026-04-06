package no.dervis.terminal_games.terminal_chess.ai.tournament;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ChessAI;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ImprovedParallelChessAI;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ParallelChessAI;

import java.nio.file.Path;

/**
 * Sample program that runs a tournament between chess engines.
 *
 * <p>Usage: {@code java TournamentMain [rounds] [thinkTimeMs] [matchup]}
 * <ul>
 *   <li>{@code matchup=1} — Single vs Parallel (default)</li>
 *   <li>{@code matchup=2} — Parallel vs ImprovedParallel</li>
 *   <li>{@code matchup=3} — Single vs ImprovedParallel</li>
 * </ul>
 */
public class TournamentMain {

    public static void main(String[] args) {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        long thinkTimeMs = args.length > 1 ? Long.parseLong(args[1]) : 500;
        int matchup = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        EngineConfig e1;
        EngineConfig e2;

        final boolean verbose = false;

        switch (matchup) {
            case 2 -> {
                e1 = new EngineConfig("Parallel", new ParallelChessAI(verbose), thinkTimeMs);
                e2 = new EngineConfig("ImprovedParallel", new ImprovedParallelChessAI(verbose), thinkTimeMs);
            }
            case 3 -> {
                e1 = new EngineConfig("Single", new ChessAI(verbose), thinkTimeMs);
                e2 = new EngineConfig("ImprovedParallel", new ImprovedParallelChessAI(verbose), thinkTimeMs);
            }
            default -> {
                e1 = new EngineConfig("Single", new ChessAI(verbose), thinkTimeMs);
                e2 = new EngineConfig("Parallel", new ParallelChessAI(verbose), thinkTimeMs);
            }
        }

        System.out.printf("Starting tournament: %s vs %s%n", e1.name(), e2.name());
        System.out.printf("Rounds: %d | Think time: %d ms%n%n", rounds, thinkTimeMs);

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
}

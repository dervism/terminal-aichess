package no.dervis.terminal_games.terminal_chess.ai.tournament;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ChessAI;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ParallelChessAI;

import java.nio.file.Path;

/**
 * Sample program that runs a tournament between the single-threaded
 * and parallel chess engines.
 *
 * <p>Usage: {@code java TournamentMain [rounds] [thinkTimeMs]}
 */
public class TournamentMain {

    public static void main(String[] args) {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        long thinkTimeMs = args.length > 1 ? Long.parseLong(args[1]) : 1000;

        System.out.println("Starting tournament: Single-threaded vs Parallel (Lazy SMP)");
        System.out.printf("Rounds: %d | Think time: %d ms%n%n", rounds, thinkTimeMs);

        ChessAI engine1 = new ChessAI(false);
        ParallelChessAI engine2 = new ParallelChessAI(false);

        TournamentResult result = Tournament.builder()
                .engine1(new EngineConfig("Single", engine1, thinkTimeMs))
                .engine2(new EngineConfig("Parallel", engine2, thinkTimeMs + 500))
                .rounds(rounds)
                .outputDir(Path.of("tournaments"))
                .verbose(true)
                .build()
                .run();

        System.out.println();
        System.out.println(result.summary());
    }
}

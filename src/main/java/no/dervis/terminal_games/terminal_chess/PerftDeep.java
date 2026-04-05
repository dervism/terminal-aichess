package no.dervis.terminal_games.terminal_chess;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.util.List;

/**
 * Deep perft (performance test) runner. Validates move generation against
 * reference values from the Chess Programming Wiki at depths beyond what
 * is practical in unit tests.
 *
 * <p>Run with: {@code java -cp target/classes no.dervis.terminal_games.terminal_chess.PerftDeep}
 *
 * <p>Reference: <a href="https://www.chessprogramming.org/Perft_Results">chessprogramming.org/Perft_Results</a></p>
 */
public class PerftDeep {

    record PerftCase(String name, String fen, long[] expected) {}

    private static final PerftCase[] CASES = {
            new PerftCase("Position 1 (initial)",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    new long[]{1, 20, 400, 8_902, 197_281, 4_865_609, 119_060_324}),

            new PerftCase("Position 2 (Kiwipete)",
                    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -",
                    new long[]{1, 48, 2_039, 97_862, 4_085_603, 193_690_690, 8_031_647_685L}),

            new PerftCase("Position 3 (endgame)",
                    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -",
                    new long[]{1, 14, 191, 2_812, 43_238, 674_624, 11_030_083, 178_633_661, 3_009_794_393L}),

            new PerftCase("Position 4 (promotions)",
                    "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -",
                    new long[]{1, 6, 264, 9_467, 422_333, 15_833_292, 706_045_033}),

            new PerftCase("Position 5 (discovered checks)",
                    "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -",
                    new long[]{1, 44, 1_486, 62_379, 2_103_487, 89_941_194}),

            new PerftCase("Position 6 (mirrored)",
                    "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -",
                    new long[]{1, 46, 2_079, 89_890, 3_894_594, 164_075_551, 6_923_051_137L}),
    };

    public static void main(String[] args) {
        int maxDepth = 7;
        boolean divideOnFail = false;
        boolean parallel = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--depth", "-d" -> maxDepth = Integer.parseInt(args[++i]);
                case "--divide" -> divideOnFail = true;
                case "--parallel", "-p" -> parallel = true;
                case "--help", "-h" -> {
                    System.out.println("Usage: PerftDeep [--depth N] [--divide] [--parallel]");
                    System.out.println("  --depth N    Maximum depth to test (default: 7)");
                    System.out.println("  --divide     Print per-move breakdown on failure");
                    System.out.println("  --parallel   Parallelize root moves across threads");
                    return;
                }
            }
        }

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.printf("Mode: %s (%d cores available)%n%n",
                parallel ? "parallel" : "single-threaded", cores);

        int totalTests = 0, passed = 0, failed = 0;

        for (PerftCase testCase : CASES) {
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println(testCase.name);
            System.out.println("FEN: " + testCase.fen);
            System.out.println("───────────────────────────────────────────────────────");

            Bitboard board = Bitboard.fromFEN(testCase.fen);
            int depthLimit = Math.min(maxDepth, testCase.expected.length - 1);

            for (int depth = 1; depth <= depthLimit; depth++) {
                totalTests++;
                long expected = testCase.expected[depth];

                long startTime = System.currentTimeMillis();
                long result = parallel ? perftParallel(board, depth) : perft(board, depth);
                long elapsed = System.currentTimeMillis() - startTime;

                boolean ok = result == expected;
                String status = ok ? "OK" : "FAIL";
                long nps = elapsed > 0 ? (result * 1000) / elapsed : 0;

                System.out.printf("  depth %d: %,15d nodes  %6dms  %,12d nps  [%s]",
                        depth, result, elapsed, nps, status);

                if (!ok) {
                    System.out.printf("  (expected %,d, diff %+,d)", expected, result - expected);
                    failed++;
                } else {
                    passed++;
                }
                System.out.println();

                if (!ok && divideOnFail) {
                    System.out.println("  ┌─ divide:");
                    divide(board, depth);
                    System.out.println("  └─");
                }
            }
            System.out.println();
        }

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed, %d total%n", passed, failed, totalTests);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Single-threaded perft.
     */
    static long perft(Bitboard board, int depth) {
        if (depth == 0) return 1;

        Generator generator = new Generator(board);
        List<Integer> moves = generator.generateMoves(board.turn());

        if (depth == 1) return moves.size();

        long nodes = 0;
        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            nodes += perft(copy, depth - 1);
        }
        return nodes;
    }

    /**
     * Parallel perft — distributes root moves across threads,
     * each subtree runs single-threaded. Copy-make ensures
     * each thread has its own board state with zero contention.
     */
    static long perftParallel(Bitboard board, int depth) {
        if (depth <= 2) return perft(board, depth);

        Generator generator = new Generator(board);
        List<Integer> moves = generator.generateMoves(board.turn());

        return moves.parallelStream()
                .mapToLong(move -> {
                    Bitboard copy = board.copy();
                    copy.makeMove(move);
                    return perft(copy, depth - 1);
                })
                .sum();
    }

    private static void divide(Bitboard board, int depth) {
        Generator generator = new Generator(board);
        List<Integer> moves = generator.generateMoves(board.turn());
        long total = 0;

        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            long nodes = perft(copy, depth - 1);
            total += nodes;

            Move m = Move.createMove(move, board);
            String from = Tuple3.of(m.fromSquare()).square().toLowerCase();
            String to = Tuple3.of(m.toSquare()).square().toLowerCase();
            String promo = m.promotionPiece() > 0 ? "=" + promChar(m.promotionPiece()) : "";
            System.out.printf("  │ %s%s%s: %,d%n", from, to, promo, nodes);
        }
        System.out.printf("  │ Total: %,d%n", total);
    }

    private static char promChar(int piece) {
        return switch (piece % 6) {
            case Chess.knight -> 'N';
            case Chess.bishop -> 'B';
            case Chess.rook -> 'R';
            case Chess.queen -> 'Q';
            default -> '?';
        };
    }
}

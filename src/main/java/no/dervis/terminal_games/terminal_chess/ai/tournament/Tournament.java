package no.dervis.terminal_games.terminal_chess.ai.tournament;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a tournament between two engines over a configurable number of games.
 * Colors alternate each game so both engines play as white and black equally.
 *
 * <pre>{@code
 * TournamentResult result = Tournament.builder()
 *         .engine1(new EngineConfig("Single", new ChessAI(), 3000))
 *         .engine2(new EngineConfig("Parallel", new ParallelChessAI(), 3000))
 *         .rounds(10)
 *         .outputDir(Path.of("tournaments"))
 *         .build()
 *         .run();
 * }</pre>
 */
public class Tournament implements Chess {

    private static final int MAX_MOVES_PER_GAME = 300;
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EngineConfig engine1;
    private final EngineConfig engine2;
    private final int rounds;
    private final Path outputDir;
    private final boolean verbose;

    private Tournament(Builder builder) {
        this.engine1 = builder.engine1;
        this.engine2 = builder.engine2;
        this.rounds = builder.rounds;
        this.outputDir = builder.outputDir;
        this.verbose = builder.verbose;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the tournament and returns the aggregated result.
     * Colors alternate each round: in even-numbered games engine1 is white,
     * in odd-numbered games engine2 is white.
     */
    public TournamentResult run() {
        LocalDateTime startTime = LocalDateTime.now();
        List<GameResult> results = new ArrayList<>();

        for (int game = 1; game <= rounds; game++) {
            boolean engine1IsWhite = (game % 2 == 1);
            EngineConfig whiteEngine = engine1IsWhite ? engine1 : engine2;
            EngineConfig blackEngine = engine1IsWhite ? engine2 : engine1;

            if (verbose) {
                System.out.printf("Game %d/%d: %s (W) vs %s (B)%n",
                        game, rounds, whiteEngine.name(), blackEngine.name());
            }

            GameResult result = playGame(game, whiteEngine, blackEngine);
            results.add(result);

            if (verbose) {
                System.out.printf("  Result: %s (%d moves)%n", result.result(), result.totalMoves());
                System.out.println(Chess.boardToStr.apply(Bitboard.fromFEN(result.finalFEN()), true));
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        TournamentResult tournamentResult =
                new TournamentResult(startTime, endTime, engine1, engine2, rounds, results);

        saveResults(tournamentResult);

        if (verbose) {
            System.out.println();
            System.out.println(tournamentResult.summary());
        }

        return tournamentResult;
    }

    private GameResult playGame(int gameNumber,
                                EngineConfig whiteEngine,
                                EngineConfig blackEngine) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);
        List<String> moveHistory = new ArrayList<>();
        List<String> positionHistory = new ArrayList<>();
        positionHistory.add(board.positionKey());
        String result = "";
        String drawReason = "";
        int moveCount = 0;

        while (moveCount < MAX_MOVES_PER_GAME) {
            EngineConfig current = (board.turn() == white) ? whiteEngine : blackEngine;
            int move = current.engine().findBestMove(board, current.thinkTimeMs());

            if (move == 0) break;

            moveHistory.add(Move.createMove(move, board).toAlgebraic());
            board.makeMove(move);
            positionHistory.add(board.positionKey());
            moveCount++;

            Generator.GameState state = generator.getGameState(board.turn());
            switch (state) {
                case CHECKMATE -> {
                    int loser = board.turn();
                    result = (loser == white) ? "0-1" : "1-0";
                }
                case STALEMATE -> {
                    result = "1/2-1/2";
                    drawReason = "stalemate";
                }
                case INSUFFICIENT_MATERIAL -> {
                    result = "1/2-1/2";
                    drawReason = "insufficient material";
                }
                case FIFTY_MOVE_RULE -> {
                    result = "1/2-1/2";
                    drawReason = "50-move rule";
                }
                case ONGOING -> {
                    if (Generator.isThreefoldRepetition(positionHistory)) {
                        result = "1/2-1/2";
                        drawReason = "threefold repetition";
                    }
                }
            }

            if (!result.isEmpty()) break;
        }

        if (result.isEmpty()) {
            result = "1/2-1/2";
            drawReason = "max moves reached";
        }

        return new GameResult(
                gameNumber,
                whiteEngine.name(),
                blackEngine.name(),
                result,
                drawReason,
                board.toFEN(),
                moveCount,
                moveHistory
        );
    }

    private void saveResults(TournamentResult result) {
        try {
            Files.createDirectories(outputDir);
            String filename = "tournament_" + result.startTime().format(FILE_FMT) + ".txt";
            Path file = outputDir.resolve(filename);

            StringBuilder sb = new StringBuilder();
            sb.append("Tournament Results\n");
            sb.append("==================\n\n");
            sb.append("Date:      ").append(result.startTime().format(DISPLAY_FMT)).append('\n');
            sb.append("Engine 1:  ").append(engine1.name())
              .append(" (").append(engine1.strengthLabel()).append(")\n");
            sb.append("Engine 2:  ").append(engine2.name())
              .append(" (").append(engine2.strengthLabel()).append(")\n");
            sb.append("Games:     ").append(result.totalGames()).append('\n');
            sb.append('\n');

            sb.append("Score\n");
            sb.append("-----\n");
            sb.append(String.format("%-20s Wins: %d%n", engine1.name(), result.engine1Wins()));
            sb.append(String.format("%-20s Wins: %d%n", engine2.name(), result.engine2Wins()));
            sb.append(String.format("%-20s %d%n", "Draws:", result.draws()));
            sb.append('\n');

            sb.append("Game Details\n");
            sb.append("------------\n");
            for (GameResult game : result.games()) {
                String resultLabel = game.result();
                if (!game.drawReason().isEmpty()) {
                    resultLabel += " (" + game.drawReason() + ")";
                }
                sb.append(String.format("Game %d: %s (W) vs %s (B) — %s (%d moves)%n",
                        game.gameNumber(),
                        game.whiteName(), game.blackName(),
                        resultLabel, game.totalMoves()));
                sb.append("FEN: ").append(game.finalFEN()).append('\n');
                sb.append("Moves: ");
                List<String> moves = game.moveHistory();
                for (int i = 0; i < moves.size(); i++) {
                    if (i % 2 == 0) sb.append((i / 2) + 1).append(". ");
                    sb.append(moves.get(i));
                    sb.append(i % 2 == 0 ? " " : " ");
                }
                sb.append('\n').append('\n');
            }

            Files.writeString(file, sb.toString());

            if (verbose) {
                System.out.println("Results saved to " + file);
            }
        } catch (IOException e) {
            System.err.println("Failed to save tournament results: " + e.getMessage());
        }
    }

    public static class Builder {
        private EngineConfig engine1;
        private EngineConfig engine2;
        private int rounds = 10;
        private Path outputDir = Path.of("tournaments");
        private boolean verbose = true;

        private Builder() {}

        public Builder engine1(EngineConfig engine1) {
            this.engine1 = engine1;
            return this;
        }

        public Builder engine2(EngineConfig engine2) {
            this.engine2 = engine2;
            return this;
        }

        public Builder rounds(int rounds) {
            if (rounds < 1) throw new IllegalArgumentException("rounds must be >= 1");
            this.rounds = rounds;
            return this;
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Tournament build() {
            if (engine1 == null) throw new IllegalStateException("engine1 is required");
            if (engine2 == null) throw new IllegalStateException("engine2 is required");
            return new Tournament(this);
        }
    }
}

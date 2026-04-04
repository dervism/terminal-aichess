package no.dervis.terminal_games.bitcheckers;

import no.dervis.terminal_games.game_ai.AlphaBetaSearch;

import java.util.List;
import java.util.Scanner;

/**
 * Terminal-based checkers game using the bitboard engine.
 * Human plays white (top, moves down); AI plays black (bottom, moves up).
 */
public class BitCheckers {

    private final BitCheckersBoard board;
    private final AlphaBetaSearch<CheckersMove> search;
    private final long thinkTimeMs;

    public BitCheckers(long thinkTimeMs) {
        this.board = new BitCheckersBoard();
        this.board.initialise();
        this.search = new AlphaBetaSearch<>(new BitCheckersEvaluation());
        this.thinkTimeMs = thinkTimeMs;
    }

    public void play() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Bitboard Checkers ===");
        System.out.println("You are White (\u26C0/\u26C1). Enter moves as e.g. 'a3-b4' or 'a3xc5'.");
        System.out.println("Type 'quit' to exit.\n");

        while (!board.isTerminal()) {
            System.out.println(board.prettyPrint());

            if (board.turn() == BitCheckersBoard.WHITE) {
                // Human move
                List<CheckersMove> legal = board.generateMoves();
                if (legal.isEmpty()) break;

                System.out.println("Legal moves: " + legal);
                System.out.print("Your move: ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("quit")) break;

                CheckersMove move = parseMove(input, legal);
                if (move == null) {
                    System.out.println("Invalid move. Try again.");
                    continue;
                }
                board.makeMove(move);
            } else {
                // AI move
                System.out.println("Thinking...");
                var result = search.findBestMove(board, thinkTimeMs);
                System.out.printf("AI plays: %s  (depth %d, %d nodes, score %d)%n",
                        result.move(), result.depth(), result.nodes(), result.score());
                board.makeMove(result.move());
            }
        }

        System.out.println(board.prettyPrint());
        printResult();
    }

    private void printResult() {
        if (board.blackPieceCount() == 0) {
            System.out.println("White wins!");
        } else if (board.whitePieceCount() == 0) {
            System.out.println("Black wins!");
        } else {
            System.out.println("Game over — no legal moves for " +
                    (board.turn() == BitCheckersBoard.BLACK ? "Black" : "White") + ".");
        }
    }

    private CheckersMove parseMove(String input, List<CheckersMove> legal) {
        // Accept formats: "a3-b4" or "a3xb4" or "a3xc5"
        String cleaned = input.toLowerCase().replaceAll("[x\\-]", " ").trim();
        String[] parts = cleaned.split("\\s+");
        if (parts.length != 2) return null;

        int from = parseSquare(parts[0]);
        int to = parseSquare(parts[1]);
        if (from < 0 || to < 0) return null;

        for (CheckersMove m : legal) {
            if (m.from() == from && m.to() == to) return m;
        }
        return null;
    }

    private int parseSquare(String s) {
        if (s.length() != 2) return -1;
        int col = s.charAt(0) - 'a';
        int row = s.charAt(1) - '1';
        if (col < 0 || col > 7 || row < 0 || row > 7) return -1;
        return row * 8 + col;
    }

    static void main(String[] args) {
        long thinkTime = 3000;
        if (args.length > 0) {
            try { thinkTime = Long.parseLong(args[0]); }
            catch (NumberFormatException ignored) {}
        }
        new BitCheckers(thinkTime).play();
    }
}

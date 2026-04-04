package no.dervis.terminal_games.terminal_chess;

import no.dervis.terminal_games.terminal_chess.ai.ChessAI;
import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple2;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.BoardPrinter;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Generator;
import no.dervis.terminal_games.terminal_chess.moves.Move;

import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

import static no.dervis.terminal_games.terminal_chess.board.Chess.black;
import static no.dervis.terminal_games.terminal_chess.board.Chess.white;

public class TerminalChess implements BoardPrinter {

    final static Scanner scanner = new Scanner(System.in);

    final static Function<Tuple2<String, Integer>, Tuple3> t2ToT3 = value ->
            new Tuple3(
                    /*row*/    value.right(),
                    /*column*/ columnToIndex.apply(value.left()),
                    /*index*/  indexFn.apply(value.right(), columnToIndex.apply(value.left())));

    public static void main(String[] args) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);
        ChessAI ai = new ChessAI();

        System.out.println(Chess.boardToStr.apply(board, true));
        boolean status = true;

        System.out.print("Play white or black? (w/b) ");
        boolean userColor = scanner.nextLine().equalsIgnoreCase("w");
        int userTurn = userColor ? white : black;

        long thinkTimeMs = chooseDifficulty();

        String userInput = "";

        while (status) {

            if (userTurn == board.turn()) {
                userInput = getMoveFromUser();

                if (userInput.equals("q")) {
                    scanner.close();
                    System.out.println("Quitting.");
                    break;
                }

                var userMoves = generator.generateMoves(board.turn());
                var parsedInput = parseMove(userInput);
                parsedInput.flatMap(
                        parsedMove -> userMoves
                                .stream()
                                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                                .filter(t2 -> t2.right().fromSquare() == parsedMove.left().index())
                                .filter(t2 -> t2.right().toSquare() == parsedMove.right().index())
                                .findFirst())
                        .ifPresentOrElse(legalUserMove -> board.makeMove(legalUserMove.left()),
                                () -> System.out.println("Please enter a legal move."));
            } else {
                getMoveFromComputer(ai, board, thinkTimeMs);
            }

            clearTerminal();
            System.out.println(Chess.boardToStr.apply(board, userColor));

            Generator.GameState gameState = generator.getGameState(board.turn());
            switch (gameState) {
                case CHECKMATE -> {
                    int loser = board.turn();
                    System.out.println("Checkmate! " + (loser == white ? "Black" : "White") + " wins!");
                    status = false;
                }
                case STALEMATE -> {
                    System.out.println("Draw by stalemate!");
                    status = false;
                }
                case INSUFFICIENT_MATERIAL -> {
                    System.out.println("Draw by insufficient material!");
                    status = false;
                }
                case ONGOING -> {
                    int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(board.turn()));
                    if (Generator.isKingInCheck(board, board.turn(), kingSquare)) {
                        System.out.println("Check!");
                    }
                }
            }
        }
    }

    private static void getMoveFromComputer(ChessAI ai, Bitboard board, long thinkTimeMs) {
        System.out.println("Thinking...");
        int move = ai.findBestMove(board, thinkTimeMs);
        if (move == 0) {
            System.out.println("No legal moves available.");
            return;
        }
        Move computerMove = Move.createMove(move, board);
        System.out.println("Computer move: " + computerMove.toStringShort());
        board.makeMove(move);
    }

    private static Optional<Tuple2<Tuple3, Tuple3>> parseMove(String move) {
        try {
            String[] split = move.split("-");
            String[] from = split[0].split("");
            String[] to = split[1].split("");

            Tuple3 moveFrom = t2ToT3.apply(new Tuple2<>(from[0].toUpperCase(), Integer.parseInt(from[1])-1));
            Tuple3 moveTo = t2ToT3.apply(new Tuple2<>(to[0].toUpperCase(), Integer.parseInt(to[1])-1));

            return Optional.of(Tuple2.of(moveFrom, moveTo));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String getMoveFromUser() {
        System.out.print("Enter your move: ");
        return scanner.nextLine();
    }

    private static long chooseDifficulty() {
        System.out.println("Select difficulty:");
        System.out.println("  1. Easy       (1 second)");
        System.out.println("  2. Medium     (3 seconds)");
        System.out.println("  3. Hard       (5 seconds)");
        System.out.println("  4. Expert     (10 seconds)");
        System.out.print("Choice (1-4): ");
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> 1000;
            case "2" -> 3000;
            case "3" -> 5000;
            case "4" -> 10000;
            default -> {
                System.out.println("Invalid choice, defaulting to Medium.");
                yield 3000;
            }
        };
    }

    private static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        //System.out.print("\033\143");
        System.out.flush();
    }
}




package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.Board.Tuple2;
import no.dervis.terminal_aichess.Board.Tuple3;
import no.dervis.terminal_aichess.moves.Generator;
import no.dervis.terminal_aichess.moves.Move;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

import static no.dervis.terminal_aichess.Chess.black;
import static no.dervis.terminal_aichess.Chess.white;

public class TerminalChess {

    static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);

        System.out.println(Chess.boardToStr.apply(board, true));
        boolean status = true;

        System.out.print("Play white or black? (w/b)");
        boolean userColor = scanner.nextLine().equalsIgnoreCase("w");
        int userTurn = userColor ? white : black;

        String userInput = "";

        while (status) {

            if (userTurn == board.turn()) {
                userInput = getMoveFromUser();
            }

            clearTerminal();

            if (userInput.equals("q")) {
                scanner.close();
                System.out.println("Quitting.");
                status = false;
            } else {
                List<Integer> moves = generator.generateMoves(board.turn());

                if (userTurn == board.turn()) {
                    var parsedInput = parseMove(userInput);
                    parsedInput.flatMap(parsedMove -> moves
                            .stream()
                            .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                            .filter(t2 -> t2.right().fromSquare() == parsedMove.left().index())
                            .filter(t2 -> t2.right().toSquare() == parsedMove.right().index())
                            .findFirst())
                            .ifPresentOrElse(legalUserMove -> board.makeMove(legalUserMove.left()),
                                    () -> System.out.println("Please enter a legal move."));
                } else {
                    int move = moves.get(new Random().nextInt(0, moves.size()));
                    board.makeMove(move);
                }

                System.out.println(Chess.boardToStr.apply(board, userColor));
            }
        }
    }

    private static Optional<Tuple2<Tuple3, Tuple3>> parseMove(String move) {
        try {
            String[] split = move.split("-");
            String[] from = split[0].split("");
            String[] to = split[1].split("");

            Tuple3 moveFrom = Board.t2ToT3.apply(new Tuple2<>(from[0].toUpperCase(), Integer.parseInt(from[1])-1));
            Tuple3 moveTo = Board.t2ToT3.apply(new Tuple2<>(to[0].toUpperCase(), Integer.parseInt(to[1])-1));

            return Optional.of(Tuple2.of(moveFrom, moveTo));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String getMoveFromUser() {
        System.out.print("Enter your move: ");
        return scanner.nextLine();
    }

    private static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}




package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.Board.T2;
import no.dervis.terminal_aichess.Board.T3;
import no.dervis.terminal_aichess.moves.Generator;
import no.dervis.terminal_aichess.moves.Move;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static no.dervis.terminal_aichess.Chess.black;
import static no.dervis.terminal_aichess.Chess.white;

public class TerminalChess {

    static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator movegen = new Generator(board);

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
                List<Integer> moves = movegen.generateMoves(board.turn());

                if (userTurn == board.turn()) {
                    var t3T3T2 = parseMove(userInput);
                    T2<Integer, Move> userMove = moves
                            .stream()
                            .map(move -> new T2<>(move, Move.createMove(move, board)))
                            .filter(move -> move.right().fromSquare() == t3T3T2.left().index())
                            .filter(move -> move.right().toSquare() == t3T3T2.right().index())
                            .findFirst().orElseThrow();
                    board.makeMove(userMove.left());
                } else {
                    int move = moves.get(new Random().nextInt(0, moves.size()));
                    board.makeMove(move);
                }

                System.out.println(Chess.boardToStr.apply(board, userColor));
            }
        }
    }

    private static T2<T3, T3> parseMove(String move) {

        String[] split = move.split("-");
        String[] from = split[0].split("");
        String[] to = split[1].split("");

        T3 moveFrom = Board.t2ToT3.apply(new T2<>(from[0].toUpperCase(), Integer.parseInt(from[1])-1));
        T3 moveTo = Board.t2ToT3.apply(new T2<>(to[0].toUpperCase(), Integer.parseInt(to[1])-1));

        return T2.of(moveFrom, moveTo);
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




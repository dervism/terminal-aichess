package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.moves.Generator;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class TerminalChess {

    static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator movegen = new Generator(board);

        System.out.println(Chess.boardToStr.apply(board, true));
        boolean status = true;

        while (status) {
            String userMove = getMoveFromUser();
            clearTerminal();

            if (userMove.equals("q")) {
                scanner.close();
                System.out.println("Quitting.");
                status = false;
            } else {
                List<Integer> moves = movegen.generateMoves(0);
                System.out.printf("Found %s moves.", moves.size()).println();
                int move = moves.get(new Random().nextInt(0, moves.size()));
                board.makeMove(move);

                System.out.println(Chess.boardToStr.apply(board, true));
            }
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




package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.board.Bitboard;
import no.dervis.terminal_aichess.board.Board.Tuple2;
import no.dervis.terminal_aichess.board.Board.Tuple3;
import no.dervis.terminal_aichess.board.BoardPrinter;
import no.dervis.terminal_aichess.board.Chess;
import no.dervis.terminal_aichess.moves.Generator;
import no.dervis.terminal_aichess.moves.Move;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;

import static no.dervis.terminal_aichess.board.Chess.black;
import static no.dervis.terminal_aichess.board.Chess.white;

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

        System.out.println(Chess.boardToStr.apply(board, true));
        boolean status = true;

        System.out.print("Play white or black? (w/b)");
        boolean userColor = scanner.nextLine().equalsIgnoreCase("w");
        int userTurn = userColor ? white : black;

        String userInput = "";

        while (status) {

            if (userTurn == board.turn()) {
                userInput = getMoveFromUser();

                List<Integer> userMoves = generator.generateMoves(board.turn());
                var parsedInput = parseMove(userInput);
                parsedInput.flatMap(parsedMove -> userMoves
                                .stream()
                                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                                .filter(t2 -> t2.right().fromSquare() == parsedMove.left().index())
                                .filter(t2 -> t2.right().toSquare() == parsedMove.right().index())
                                .findFirst())
                        .ifPresentOrElse(legalUserMove -> board.makeMove(legalUserMove.left()),
                                () -> System.out.println("Please enter a legal move."));

                List<Integer> computerMoves = generator.generateMoves(board.turn());
                getMoveFromComputer(computerMoves, board);
            } else {
                List<Integer> computerMoves = generator.generateMoves(board.turn());
                getMoveFromComputer(computerMoves, board);
            }

            clearTerminal();
            System.out.println(Chess.boardToStr.apply(board, userColor));


            /*if (board.isCheckmate()) {
                System.out.println("Checkmate!");
                status = false;
            } else if (board.isDraw()) {
                System.out.println("Draw!");
                status = false;
            }*/

            if (userInput.equals("q")) {
                scanner.close();
                System.out.println("Quitting.");
                status = false;
            }
        }
    }

    private static void getMoveFromComputer(List<Integer> moves, Bitboard board) {
        int move = moves.get(new Random().nextInt(0, moves.size()));
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

    private static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        //System.out.print("\033\143");
        System.out.flush();
    }
}




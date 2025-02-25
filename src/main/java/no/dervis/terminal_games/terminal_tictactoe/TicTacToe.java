package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public class TicTacToe {
    public record Cell(int i) {}
    public record Player(int id) {}
    public enum PlayerSymbol { X, O, E }

    public enum State { Draw, PlayerWon, ComputerWon, InProgress }
    public record Board(int size, List<PlayerSymbol> cells) {
        public void setCells(List<PlayerSymbol> updated) {
            cells.clear();
            cells.addAll(updated);
        }
    }

    public int xInARow;
    public int boardSize;

    private final Player user = new Player(1);
    private final Player computer = new Player(2);

    private final Board board;
    public Board board() { return board; }

    private final MinMaxAlgorithm minMax;

    public TicTacToe() {
        this(3, 3 * 3);
    }

    public TicTacToe(int xInARow, int boardSize) {
        this.xInARow = xInARow;
        this.boardSize = boardSize;
        this.board = new Board(boardSize,
                new ArrayList<>(
                        range(0,boardSize).mapToObj(_ -> PlayerSymbol.E).toList()
                )
        );
        this.minMax = new MinMaxAlgorithm(6);
    }

    // pretty print the complete board with row and column numbers, and cell values (E should printed as " ")
    // separate the cells with a separator
    public String prettyPrintBoard() {
        int size = (int) Math.sqrt(board.cells.size());
        Function<PlayerSymbol, String> symStr = sym -> switch (sym) {
            case X -> "⛂";
            case O -> "⛀";
            case E -> " ";
        };

        // Generate column header using streams
        String columnHeader = range(0, size)
                .mapToObj(i -> format("%d | ", i + 1))
                .collect(joining("", "  | ", "\n"));

        // Generate rows using streams
        String rows = range(0, size)
                .mapToObj(i -> format("%d | ", i + 1) + range(0, size)
                        .mapToObj(j -> symStr.apply(board.cells.get(i * size + j)))
                        .collect(joining(" | ", "", " | "))
                )
                .collect(joining("\n"));

        // Combine column and row strings
        return columnHeader + rows + "\n";
    }

    // read user move from terminal
    public int readUserMove() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your move: ");
        String input = scanner.nextLine();
        String[] arr = input.split("-");

        return (int) ((Integer.parseInt(arr[0]) - 1) * Math.sqrt(boardSize) + (Integer.parseInt(arr[1]) - 1));
    }

    public boolean askToPlayFirstOrSecond() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Play first or second? (f/s): ");
        String input = scanner.nextLine();
        return input.equalsIgnoreCase("s");
    }

    // play the game, each player makes a move in turn
    // then print out the board
    public State playGame() {
        System.out.println(prettyPrintBoard());
        State state = State.InProgress;

        if (askToPlayFirstOrSecond()) {
            Pair<State, Integer> nextState = makeComputerMove();
            System.out.println(prettyPrintBoard());
            state = nextState.left();
        }

        while (state == State.InProgress) {
            int userMove = readUserMove();
            board.cells.set(userMove, PlayerSymbol.X);
            System.out.println(prettyPrintBoard());
            state = checkGameState();

            if (state != State.PlayerWon && state != State.Draw) {
                Pair<State, Integer> nextState = makeComputerMove();
                state = nextState.left();
            }
            System.out.println(prettyPrintBoard());
        }

        switch (state) {
            case PlayerWon -> {
                System.out.println("Congratulations! You won!");
                return State.PlayerWon;
            }
            case ComputerWon -> {
                System.out.println("Sorry, you lost. Better luck next time!");
                return State.ComputerWon;
            }
            case Draw -> {
                System.out.println("It's a draw!");
                return State.Draw;
            }
        }

        return State.InProgress;
    }

    public State checkGameState() {
        return checkForWinner(board);
    }

    private State checkForWinner(Board board) {
        if (isWinning(PlayerSymbol.X, board)) return State.PlayerWon;
        if (isWinning(PlayerSymbol.O, board)) return State.ComputerWon;
        if (!getFreeSquares(board).isEmpty()) return State.InProgress;

        return State.Draw;
    }

    private Pair<State, Integer> makeComputerMove() {
        int computerMove = minMax.findBestMove(this);
        setMove(computerMove, PlayerSymbol.O);
        return new Pair<>(checkGameState(), computerMove);
    }

    private boolean hasFreeSquares(Board board) {
        return !getFreeSquares(board).isEmpty();
    }

    public List<Cell> getFreeSquares(Board board) {
        return range(0, board.size())
                .filter(i -> getCellValue(i) == PlayerSymbol.E)
                .mapToObj(Cell::new)
                .collect(Collectors.toCollection(() -> new ArrayList<>(board.size())));
    }

    public void setMove(int position, PlayerSymbol symbol) {
        board.cells.set(position, symbol);
    }

    public PlayerSymbol getCellValue(int position) {
        return board.cells.get(position);
    }

    public int getBoardSize() {
        return board.size();
    }


    private boolean isWinning(PlayerSymbol symbol, Board board) {
        return rowsWin(symbol, board) || colsWin(symbol, board) || diagonalsWin(symbol, board);
    }

    public boolean rowsWin(PlayerSymbol symbol, Board board) {
        int stride = (int) Math.sqrt(board.cells.size());

        return range(0, stride)
                .anyMatch(i ->
                        IntStream.rangeClosed(0, stride - xInARow)
                                .anyMatch(k -> isAllSymbol(board.cells.subList(i * stride + k, i * stride + k + xInARow), symbol))
                );
    }

    public boolean colsWin(PlayerSymbol symbol, Board board) {
        int stride = (int) Math.sqrt(board.cells.size());

        return range(0, stride)
                .anyMatch(i ->
                        IntStream.rangeClosed(0, stride - xInARow)
                                .anyMatch(k ->
                                        isAllSymbol(
                                                range(0, xInARow)
                                                        .mapToObj(j -> board.cells.get(i + (k + j) * stride))
                                                        .collect(Collectors.toList()),
                                                symbol)
                                )
                );
    }

    private boolean diagonalsWin(PlayerSymbol symbol, Board board) {
        int windowSize = xInARow;
        int stride = (int)Math.sqrt(board.cells.size());

        // Check for left-to-right diagonals (\)
        boolean hasLeftToRightDiagonal =
                range(0, stride - windowSize + 1)
                .anyMatch(i ->
                        range(0, stride - windowSize + 1)
                        .anyMatch(j ->
                                range(0, windowSize)
                                .allMatch(k -> board.cells.get((i+k) * stride + j + k) == symbol)));

        // Check for right-to-left diagonals (/)
        boolean hasRightToLeftDiagonal = range(windowSize - 1, stride)
                .anyMatch(i -> range(0, stride - windowSize + 1)
                        .anyMatch(j -> range(0, windowSize)
                                .allMatch(k -> board.cells.get((i-k) * stride + j + k) == symbol)));

        return hasLeftToRightDiagonal || hasRightToLeftDiagonal;
    }

    private boolean isAllSymbol(List<PlayerSymbol> symbols, PlayerSymbol symbol) {
        return symbols.stream().allMatch(cell -> cell == symbol);
    }


    public static void main(String[] args) {
        new TicTacToe(4, 6*6).playGame();
        //new TicTacToe().playGame();
    }

}

package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.Pair;
import no.dervis.terminal_games.game_ai.AlphaBetaSearch;

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

    private final AlphaBetaSearch<Integer> search;
    private final long thinkTimeMs;
    public int xInARow;
    public int boardSize;

    private final Player user = new Player(1);
    private final Player computer = new Player(2);

    private final Board board;
    public Board board() { return board; }


    public TicTacToe() {
        this(3, 3 * 3);
    }

    public TicTacToe(int xInARow, int boardSize) {
        this(xInARow, boardSize, 2000);
    }

    public TicTacToe(int xInARow, int boardSize, long thinkTimeMs) {
        this.xInARow = xInARow;
        this.boardSize = boardSize;
        this.thinkTimeMs = thinkTimeMs;
        this.board = new Board(boardSize,
                new ArrayList<>(
                        range(0,boardSize).mapToObj(_ -> PlayerSymbol.E).toList()
                )
        );
        this.search = new AlphaBetaSearch<>(new TicTacToeEvaluation());
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
    public int readUserMove(Scanner scanner) {
        int size = (int) Math.sqrt(boardSize);
        while (true) {
            System.out.print("Enter your move (row-col, e.g. 1-2): ");
            String input = scanner.nextLine().trim();
            String[] arr = input.split("-");
            if (arr.length != 2) {
                System.out.println("Invalid format. Use row-col, e.g. 1-2.");
                continue;
            }
            try {
                int row = Integer.parseInt(arr[0]) - 1;
                int col = Integer.parseInt(arr[1]) - 1;
                if (row < 0 || row >= size || col < 0 || col >= size) {
                    System.out.println("Out of bounds. Row and column must be 1-" + size + ".");
                    continue;
                }
                int pos = row * size + col;
                if (getCellValue(pos) != PlayerSymbol.E) {
                    System.out.println("That square is already taken.");
                    continue;
                }
                return pos;
            } catch (NumberFormatException e) {
                System.out.println("Invalid numbers. Use row-col, e.g. 1-2.");
            }
        }
    }

    public boolean askToPlayFirstOrSecond(Scanner scanner) {
        System.out.print("Play first or second? (f/s): ");
        String input = scanner.nextLine();
        return input.equalsIgnoreCase("s");
    }

    // play the game, each player makes a move in turn
    // then print out the board
    public State playGame() {
        Scanner scanner = new Scanner(System.in);
        System.out.println(prettyPrintBoard());
        State state = State.InProgress;

        if (askToPlayFirstOrSecond(scanner)) {
            Pair<State, Integer> nextState = makeComputerMove();
            System.out.println(prettyPrintBoard());
            state = nextState.left();
        }

        while (state == State.InProgress) {
            int userMove = readUserMove(scanner);
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
        System.out.print("Thinking...");
        TicTacToeState state = new TicTacToeState(this, PlayerSymbol.O);
        int size = (int) Math.sqrt(boardSize);
        var result = (size <= 3)
                ? search.findBestMove(state, 10)              // full search for 3×3
                : search.findBestMove(state, thinkTimeMs);    // time-limited for larger boards
        int move = result.move();
        int row = move / size + 1;
        int col = move % size + 1;
        System.out.printf(" Computer plays %d-%d (depth %d, %d nodes)%n",
                row, col, result.depth(), result.nodes());
        setMove(move, PlayerSymbol.O);
        return new Pair<>(checkGameState(), move);
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


    static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Board size
        System.out.print("Board size (e.g. 3x3, 6x6, 8x8): ");
        String sizeInput = scanner.nextLine().trim().toLowerCase();
        int side = 3;
        if (sizeInput.contains("x")) {
            String[] parts = sizeInput.split("x");
            side = Integer.parseInt(parts[0]);
        }
        int xInARow = Math.min(side, 5); // cap win-length at 5 for large boards

        // Difficulty
        System.out.println("AI think time in seconds (e.g. 1, 3, 10): ");
        String timeInput = scanner.nextLine().trim();
        long thinkTimeMs = 2000;
        try { thinkTimeMs = Long.parseLong(timeInput) * 1000; }
        catch (NumberFormatException ignored) {}

        System.out.printf("Playing %dx%d with %d-in-a-row, AI thinks %ds%n",
                side, side, xInARow, thinkTimeMs / 1000);

        new TicTacToe(xInARow, side * side, thinkTimeMs).playGame();
    }

}

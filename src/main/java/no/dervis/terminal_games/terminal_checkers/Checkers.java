package no.dervis.terminal_games.terminal_checkers;

import java.util.*;

/**
 * This Checkers game is based on record types I wrote myself
 * as a starting point, while the logic is generated.
 *
 * The code is part of a talk about the pros and cons of generative ai
 * as part of software engineering practice.
 *
 */

public class Checkers {
    public enum Color {
        WHITE, WHITE_KING, BLACK, BLACK_KING, EMPTY;

        public boolean isKing() {
            return this == WHITE_KING || this == BLACK_KING;
        }

        public Color opposite() {
            return switch (this) {
                case WHITE, WHITE_KING -> BLACK;
                case BLACK, BLACK_KING -> WHITE;
                default -> EMPTY;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case WHITE -> "⛀";
                case WHITE_KING -> "⛁";
                case BLACK -> "⛂";
                case BLACK_KING -> "⛃";
                case EMPTY -> " ";
            };
        }
    }

    public record Position(int row, int col) {}
    public record Cell(Integer cellNr, Position position) {
            static Cell fromCellNumber(int cellNr) {
            int row = (cellNr - 1) / 8;
            int col = (cellNr - 1) % 8;
            return new Cell(cellNr, new Position(row, col));
        }
    }
    public record Player(String name, Color color) {}
    public record Board(Map<Cell, Color> cells) {}
    public record Move(Player player, Cell from, Cell to) {}

    private Board board;

    // constructor
    public Checkers() {
        this.board = initializeBoard();
    }

    // initialise a new 8x8 board
    public Board initializeBoard() {
        Map<Cell, Color> cells = new HashMap<>();
        for (int i = 1; i <= 64; i++) {
            if ((i + ((i - 1) / 8)) % 2 == 0) { // Checks for playable (dark) squares
                if (i <= 24) {
                    cells.put(Cell.fromCellNumber(i), Color.BLACK);
                } else if (i > 40) {
                    cells.put(Cell.fromCellNumber(i), Color.WHITE);
                } else {
                    cells.put(Cell.fromCellNumber(i), Color.EMPTY);
                }
            } else {
                cells.put(Cell.fromCellNumber(i), Color.EMPTY);
            }
        }
        return new Board(cells);
    }

    // game logic for playing checkers
    public boolean isGameOver(Board board) {
        return false;
    }

    // function playCheckersWithTwoPlayers
    public void playCheckersWithTwoPlayers(Player player1, Player player2) {
        while (!isGameOver(board)) {
            // get user move from input
            Move move = getUserMove(player1);
            if (!checkIfMoveIsValid(move, board)) {
                System.out.println("Invalid move: " + move);
                continue;
            } else {
                System.out.println("Player move: " + move);
                board = makeMove(move, board);
            }

            // generate a random move for the computer move
            Move computerMove = generateRandomValidMove(player2, board);
            if (computerMove != null){
                System.out.println("Computer move: " + computerMove);
                System.out.println("Computer move: " + printMove(computerMove));
                board = makeMove(computerMove, board);
            }
            printBoard();
        }
    }


    public List<Move> generateAllValidMoves(Player player, Board board) {
        List<Move> validMoves = new ArrayList<>();
        Map<Cell, Color> cells = board.cells();

        cells.forEach((cell, color) -> {
            if (color == player.color() || (color.isKing() && color.opposite() == player.color().opposite())) {
                // Get possible moves for both simple moves and captures
                validMoves.addAll(generateSimpleMoves(player, cell, color, board));
                validMoves.addAll(generateJumpMoves(player, cell, color, board));
            }
        });

        return validMoves;
    }

    public List<Move> generateSimpleMoves(Player player, Cell cell, Color color, Board board) {
        List<Move> moves = new ArrayList<>();
        int direction = color == Color.BLACK || color == Color.BLACK_KING ? 1 : -1; // Black moves down, White moves up
        int[] possibleSteps = {1, -1}; // Move to the left or right

        for (int step : possibleSteps) {
            int newRow = cell.position().row() + direction;
            int newCol = cell.position().col() + step;
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) { // Check bounds
                Cell targetCell = Cell.fromCellNumber(newRow * 8 + newCol + 1);
                if (board.cells().get(targetCell) == Color.EMPTY) {
                    moves.add(new Move(player, cell, targetCell));
                }
            }
        }

        // Add backward moves if the piece is a king
        if (color.isKing()) {
            for (int step : possibleSteps) {
                int newRow = cell.position().row() - direction; // Reverse direction for kings
                int newCol = cell.position().col() + step;
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    Cell targetCell = Cell.fromCellNumber(newRow * 8 + newCol + 1);
                    if (board.cells().get(targetCell) == Color.EMPTY) {
                        moves.add(new Move(player, cell, targetCell));
                    }
                }
            }
        }

        return moves;
    }

    public List<Move> generateJumpMoves(Player player, Cell cell, Color color, Board board) {
        List<Move> moves = new ArrayList<>();
        int direction = color == Color.BLACK || color == Color.BLACK_KING ? 1 : -1;
        int[] possibleSteps = {2, -2}; // Jump over one cell to land two cells away

        for (int step : possibleSteps) {
            int newRow = cell.position().row() + direction * 2;
            int newCol = cell.position().col() + (step / 2);
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                Cell jumpOverCell = Cell.fromCellNumber(cell.position().row() + direction * 8 + (cell.position().col() + (step / 2)) + 1);
                Cell targetCell = Cell.fromCellNumber(newRow * 8 + newCol + 1);
                if (board.cells().get(targetCell) == Color.EMPTY && board.cells().get(jumpOverCell) == color.opposite()) {
                    moves.add(new Move(player, cell, targetCell));
                }
            }
        }

        if (color.isKing()) {
            // Add jumps in the opposite direction as well
            for (int step : possibleSteps) {
                int newRow = cell.position().row() - direction * 2;
                int newCol = cell.position().col() + (step / 2);
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    Cell jumpOverCell = Cell.fromCellNumber(cell.position().row() - direction * 8 + (cell.position().col() + (step / 2)) + 1);
                    Cell targetCell = Cell.fromCellNumber(newRow * 8 + newCol + 1);
                    if (board.cells().get(targetCell) == Color.EMPTY && board.cells().get(jumpOverCell) == color.opposite()) {
                        moves.add(new Move(player, cell, targetCell));
                    }
                }
            }
        }

        return moves;
    }

    public boolean checkIfMoveIsValid(Move move, Board board) {
        Cell fromCell = move.from();
        Cell toCell = move.to();
        Color fromColor = board.cells().get(fromCell);
        Color toColor = board.cells().get(toCell);

        // Ensure the destination cell is empty
        if (toColor != Color.EMPTY) {
            return false;
        }

        // Calculate row and column differences to determine move type
        int rowDiff = toCell.position().row() - fromCell.position().row();
        int colDiff = Math.abs(toCell.position().col() - fromCell.position().col());
        boolean isKing = fromColor.isKing();

        // Check for simple move
        if (Math.abs(rowDiff) == 1 && colDiff == 1) {
            return true;
        }

        // Check for jump move
        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            // Calculate the intermediate cell to check if there is an opponent piece to capture
            int midRow = (fromCell.position().row() + toCell.position().row()) / 2;
            int midCol = (fromCell.position().col() + toCell.position().col()) / 2;
            Cell middleCell = Cell.fromCellNumber(midRow * 8 + midCol + 1);
            Color middleColor = board.cells().get(middleCell);

            // Valid jump if there's an opponent's piece in the middle cell
            return middleColor == fromColor.opposite();
        }

        return false;
    }


    public Move generateRandomValidMove(Player player, Board board) {
        List<Move> validMoves = generateAllValidMoves(player, board);
        if (validMoves.isEmpty()) {
            return null;
        }
        Random rand = new Random();
        return validMoves.get(rand.nextInt(validMoves.size()));
    }


    public Board makeMove(Move move, Board board) {
        Map<Cell, Color> newCells = new HashMap<>(board.cells());
        Cell fromCell = move.from();
        Cell toCell = move.to();
        Color moveColor = newCells.get(fromCell);

        // Move the piece
        newCells.put(toCell, moveColor);
        newCells.put(fromCell, Color.EMPTY);

        // Check if this is a jump and handle capture
        int rowDiff = toCell.position().row() - fromCell.position().row();
        if (Math.abs(rowDiff) == 2) {
            int midRow = (fromCell.position().row() + toCell.position().row()) / 2;
            int midCol = (fromCell.position().col() + toCell.position().col()) / 2;
            Cell middleCell = Cell.fromCellNumber(midRow * 8 + midCol + 1);
            newCells.put(middleCell, Color.EMPTY); // Remove the captured piece
        }

        return new Board(newCells);
    }


    public String printMove(Move move) {
        return cellToChessNotation(move.from()) + "-" + cellToChessNotation(move.to());
    }

    private String cellToChessNotation(Cell cell) {
        int row = cell.position().row();  // Internal 0-indexed row, from top
        int col = cell.position().col();  // Internal 0-indexed column
        char columnLetter = (char) ('a' + col);
        int rowNumber = row + 1;  // +1 to shift from 0-indexed to 1-indexed bottom-up notation
        return "" + columnLetter + rowNumber;
    }

    private Move getUserMove(Player player) {
        // get user move from system input based on cell number from and to
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        String[] move = input.split("-");

        String fromCellString = move[0];
        String toCellString = move[1];

        int fromCell = convertCellToInt(fromCellString);
        int toCell = convertCellToInt(toCellString);
        return new Move(player, Cell.fromCellNumber(fromCell), Cell.fromCellNumber(toCell));
    }

    private int convertCellToInt(String cellString) {
        // Convert the cell string to a numeric cell index
        // First convert column letter to a number from 1 to 8
        int column = cellString.charAt(0) - 'a' + 1;
        // Then convert row digit to number from 1 to 8
        int row = Character.getNumericValue(cellString.charAt(1));
        return (row - 1) * 8 + column;
    }

    // pretty print the board
    public void printBoard() {
        // pretty print the complete board with row and column numbers, and cell values
        // separate the cells with a separator
        String separator = "  +---+---+---+---+---+---+---+---+\n";
        System.out.print("    a   b   c   d   e   f   g   h\n");
        for (int row = 1; row <= 8; row++) {
            System.out.print(separator);
            System.out.print(row + " ");
            for (int column = 1; column <= 8; column++) {
                int cellNr = (row - 1) * 8 + column;
                Cell cell = Cell.fromCellNumber(cellNr);
                Color color = board.cells().get(cell);
                System.out.print("| " + color + " ");
            }
            System.out.println("|");
        }
        System.out.print(separator);
        System.out.print("    a   b   c   d   e   f   g   h\n");
    }

    public static void main(String[] args) {
        // initialise a new game of Checkers with two players
        Checkers checkers = new Checkers();
        checkers.printBoard();
        Player player1 = new Player("Player 1", Color.WHITE);
        Player player2 = new Player("Player 2", Color.BLACK);
        checkers.playCheckersWithTwoPlayers(player1, player2);
    }
}

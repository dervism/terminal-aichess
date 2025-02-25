package no.dervis.terminal_games.terminal_tictactoe;

import java.util.List;

public class MinMaxAlgorithm {
    private final int defaultMaxDepth;
    private static final int WINNING_SCORE = 1000;

    public MinMaxAlgorithm(int maxDepth) {
        this.defaultMaxDepth = maxDepth;
    }

    /**
     * Calculates an appropriate search depth for the minimax algorithm based on the board size.
     * 
     * The method uses the square root of boardSize (not game.xInARow) because:
     * 1. The square root represents the actual dimensions of the board (e.g., 3x3, 6x6)
     * 2. The computational complexity of minimax is O(b^d), where:
     *    - b is the branching factor (number of possible moves)
     *    - d is the search depth
     * 3. For a board of size NxN:
     *    - The branching factor is proportional to N^2 (total cells)
     *    - We need to reduce depth as N increases to maintain reasonable performance
     * 
     * Using game.xInARow would be incorrect because:
     * - xInARow represents the win condition (e.g., 4-in-a-row), not the board dimensions
     * - A 6x6 board with 4-in-a-row has the same computational complexity regardless of xInARow
     * - The search space is determined by board size, not by the win condition
     * 
     * The formula defaultMaxDepth - (size - 3) ensures that:
     * - 3x3 board uses full defaultMaxDepth (no reduction)
     * - Larger boards get progressively reduced depth
     * - Minimum depth is 3 to maintain reasonable play quality
     * 
     * Examples:
     * - 3x3 board (boardSize=9):  depth = defaultMaxDepth
     * - 6x6 board (boardSize=36): depth = defaultMaxDepth - 3
     * - 9x9 board (boardSize=81): depth = defaultMaxDepth - 6
     * 
     * @param boardSize The total number of cells in the board (width * height)
     * @return The calculated search depth, minimum 3
     */
    private int getAdaptiveDepth(int boardSize) {
        // Reduce depth for larger boards
        int size = (int) Math.sqrt(boardSize);
        return Math.max(3, defaultMaxDepth - (size - 3));
    }

    public int findBestMove(TicTacToe game) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        int adaptiveDepth = getAdaptiveDepth(game.getBoardSize());

        System.out.println("Using adaptive depth: " + adaptiveDepth + " for board size: " + game.getBoardSize() + "x" + game.getBoardSize());

        List<TicTacToe.Cell> availableMoves = game.getFreeSquares(game.board());
        availableMoves = sortMoves(availableMoves, game);

        for (TicTacToe.Cell move : availableMoves) {
            game.setMove(move.i(), TicTacToe.PlayerSymbol.O);
            int score = minimax(game, 0, false, Integer.MIN_VALUE, Integer.MAX_VALUE, adaptiveDepth);
            game.setMove(move.i(), TicTacToe.PlayerSymbol.E);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move.i();
            }

            // Early exit if we found a winning move
            if (score >= WINNING_SCORE) {
                return move.i();
            }
        }

        return bestMove;
    }

    private int minimax(TicTacToe game, int depth, boolean isMaximizing, int alpha, int beta, int maxDepth) {
        TicTacToe.State result = game.checkGameState();

        // Terminal states with scaled scores
        if (result == TicTacToe.State.ComputerWon) return WINNING_SCORE - depth;
        if (result == TicTacToe.State.PlayerWon) return depth - WINNING_SCORE;
        if (result == TicTacToe.State.Draw) return 0;
        if (depth == maxDepth) return evaluateBoard(game);

        List<TicTacToe.Cell> availableMoves = game.getFreeSquares(game.board());
        availableMoves = sortMoves(availableMoves, game);

        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (TicTacToe.Cell move : availableMoves) {
                game.setMove(move.i(), TicTacToe.PlayerSymbol.O);
                int score = minimax(game, depth + 1, false, alpha, beta, maxDepth);
                game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) break;
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (TicTacToe.Cell move : availableMoves) {
                game.setMove(move.i(), TicTacToe.PlayerSymbol.X);
                int score = minimax(game, depth + 1, true, alpha, beta, maxDepth);
                game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            return minScore;
        }
    }

    private List<TicTacToe.Cell> sortMoves(List<TicTacToe.Cell> moves, TicTacToe game) {
        int size = (int) Math.sqrt(game.getBoardSize());

        // First, check for immediate winning moves or blocking moves
        for (TicTacToe.Cell move : moves) {
            // Check if this move wins
            game.setMove(move.i(), TicTacToe.PlayerSymbol.O);
            if (game.checkGameState() == TicTacToe.State.ComputerWon) {
                game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
                moves.remove(move);
                moves.add(0, move);
                return moves;
            }
            game.setMove(move.i(), TicTacToe.PlayerSymbol.E);

            // Check if we need to block opponent's win
            game.setMove(move.i(), TicTacToe.PlayerSymbol.X);
            if (game.checkGameState() == TicTacToe.State.PlayerWon) {
                game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
                moves.remove(move);
                moves.add(0, move);
                return moves;
            }
            game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
        }

        // Sort remaining moves by position quality
        moves.sort((a, b) -> {
            int aRow = a.i() / size, aCol = a.i() % size;
            int bRow = b.i() / size, bCol = b.i() % size;

            // Calculate position scores based on distance from center and edges
            int aScore = getPositionScore(aRow, aCol, size);
            int bScore = getPositionScore(bRow, bCol, size);

            return Integer.compare(bScore, aScore);
        });

        return moves;
    }

    private int getPositionScore(int row, int col, int size) {
        int center = size / 2;
        int distFromCenter = Math.abs(row - center) + Math.abs(col - center);
        int distFromEdge = Math.min(Math.min(row, col), Math.min(size - 1 - row, size - 1 - col));
        return 10 * (size - distFromCenter) + 5 * distFromEdge;
    }

    private int evaluateBoard(TicTacToe game) {
        TicTacToe.State state = game.checkGameState();
        if (state == TicTacToe.State.ComputerWon) return WINNING_SCORE;
        if (state == TicTacToe.State.PlayerWon) return -WINNING_SCORE;
        if (state == TicTacToe.State.Draw) return 0;

        return evaluatePosition(game, TicTacToe.PlayerSymbol.O) - evaluatePosition(game, TicTacToe.PlayerSymbol.X);
    }

    private int evaluatePosition(TicTacToe game, TicTacToe.PlayerSymbol symbol) {
        int score = 0;
        int size = (int) Math.sqrt(game.getBoardSize());
        int xInARow = game.xInARow;

        // Evaluate rows and columns
        for (int i = 0; i < size; i++) {
            score += evaluateLine(game, i * size, 1, xInARow, size, symbol) * 2;  // Rows
            score += evaluateLine(game, i, size, xInARow, size, symbol) * 2;      // Columns
        }

        // Evaluate diagonals with higher weight
        for (int i = 0; i <= size - xInARow; i++) {
            for (int j = 0; j <= size - xInARow; j++) {
                score += evaluateLine(game, i * size + j, size + 1, xInARow, size, symbol) * 3;
                if (j + xInARow <= size) {
                    score += evaluateLine(game, i * size + (j + xInARow - 1), size - 1, xInARow, size, symbol) * 3;
                }
            }
        }

        return score;
    }

    private int evaluateLine(TicTacToe game, int start, int increment, int xInARow, int size, TicTacToe.PlayerSymbol symbol) {
        int count = 0;
        int empty = 0;
        TicTacToe.PlayerSymbol opponent = (symbol == TicTacToe.PlayerSymbol.O) ? 
                                          TicTacToe.PlayerSymbol.X : TicTacToe.PlayerSymbol.O;

        // Check xInARow consecutive positions
        for (int i = 0; i < xInARow; i++) {
            int pos = start + i * increment;
            if (pos >= game.getBoardSize()) return 0;

            TicTacToe.PlayerSymbol cell = game.getCellValue(pos);
            if (cell == symbol) count++;
            else if (cell == TicTacToe.PlayerSymbol.E) empty++;
            else return 0; // Opponent's symbol found, no potential here
        }

        // Score based on number of symbols and empty spaces
        if (count == xInARow) return WINNING_SCORE;
        if (count == 0) return 0;
        return (int) Math.pow(4, count) * (empty + 1);
    }
}

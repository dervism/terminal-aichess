package no.dervis.terminal_games.terminal_tictactoe;

import java.util.List;

public class MinMaxAlgorithm {
    private final int maxDepth;

    public MinMaxAlgorithm(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int findBestMove(TicTacToe game) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        List<TicTacToe.Cell> availableMoves = game.getFreeSquares(game.board());

        for (TicTacToe.Cell move : availableMoves) {
            // Try the move
            game.setMove(move.i(), TicTacToe.PlayerSymbol.O);

            // Calculate score for this move
            int score = minimax(game, 0, false, Integer.MIN_VALUE, Integer.MAX_VALUE);

            // Undo the move
            game.setMove(move.i(), TicTacToe.PlayerSymbol.E);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move.i();
            }
        }

        return bestMove;
    }

    private int minimax(TicTacToe game, int depth, boolean isMaximizing, int alpha, int beta) {
        TicTacToe.State result = game.checkGameState();

        // Terminal states
        if (result == TicTacToe.State.ComputerWon) return 10 - depth;
        if (result == TicTacToe.State.PlayerWon) return depth - 10;
        if (result == TicTacToe.State.Draw) return 0;
        if (depth == maxDepth) return evaluateBoard(game);

        List<TicTacToe.Cell> availableMoves = game.getFreeSquares(game.board());

        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (TicTacToe.Cell move : availableMoves) {
                game.setMove(move.i(), TicTacToe.PlayerSymbol.O);
                int score = minimax(game, depth + 1, false, alpha, beta);
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
                int score = minimax(game, depth + 1, true, alpha, beta);
                game.setMove(move.i(), TicTacToe.PlayerSymbol.E);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            return minScore;
        }
    }

    private int evaluateBoard(TicTacToe game) {
        // Simple evaluation: count the difference between computer's and player's potential winning positions
        int computerScore = countPotentialWins(game, TicTacToe.PlayerSymbol.O);
        int playerScore = countPotentialWins(game, TicTacToe.PlayerSymbol.X);
        return computerScore - playerScore;
    }

    private int countPotentialWins(TicTacToe game, TicTacToe.PlayerSymbol symbol) {
        int count = 0;
        int size = game.xInARow;

        // Check rows and columns
        for (int i = 0; i < size; i++) {
            int rowEmpty = 0, rowSymbol = 0;
            int colEmpty = 0, colSymbol = 0;

            for (int j = 0; j < size; j++) {
                // Check row
                TicTacToe.PlayerSymbol rowCell = game.getCellValue(i * size + j);
                if (rowCell == symbol) rowSymbol++;
                else if (rowCell == TicTacToe.PlayerSymbol.E) rowEmpty++;

                // Check column
                TicTacToe.PlayerSymbol colCell = game.getCellValue(j * size + i);
                if (colCell == symbol) colSymbol++;
                else if (colCell == TicTacToe.PlayerSymbol.E) colEmpty++;
            }

            if (rowEmpty + rowSymbol == size) count++;
            if (colEmpty + colSymbol == size) count++;
        }

        // Check diagonals
        int diagEmpty = 0, diagSymbol = 0;
        int antiDiagEmpty = 0, antiDiagSymbol = 0;

        for (int i = 0; i < size; i++) {
            // Main diagonal
            TicTacToe.PlayerSymbol diagCell = game.getCellValue(i * size + i);
            if (diagCell == symbol) diagSymbol++;
            else if (diagCell == TicTacToe.PlayerSymbol.E) diagEmpty++;

            // Anti-diagonal
            TicTacToe.PlayerSymbol antiDiagCell = game.getCellValue(i * size + (size - 1 - i));
            if (antiDiagCell == symbol) antiDiagSymbol++;
            else if (antiDiagCell == TicTacToe.PlayerSymbol.E) antiDiagEmpty++;
        }

        if (diagEmpty + diagSymbol == size) count++;
        if (antiDiagEmpty + antiDiagSymbol == size) count++;

        return count;
    }
}

package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating rook attack squares.
 */

public class RookAttacks {
    private static final long[] rookAttacks = new long[64];

    static {
        initializeRookAttacks();
    }

    private static void initializeRookAttacks() {
        for (int square = 0; square < 64; square++) {
            rookAttacks[square] = calculateRookAttacks(square);
        }
    }

    private static long calculateRookAttacks(int square) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;
        attacks |= calculateDirectionalAttacks(rank, file, 0, 1);   // Right
        attacks |= calculateDirectionalAttacks(rank, file, 0, -1);  // Left
        attacks |= calculateDirectionalAttacks(rank, file, 1, 0);   // Up
        attacks |= calculateDirectionalAttacks(rank, file, -1, 0);  // Down
        return attacks;
    }

    private static long calculateDirectionalAttacks(int rank, int file, int rankIncrement, int fileIncrement) {
        long attacks = 0L;
        for (int r = rank + rankIncrement, f = file + fileIncrement;
             r >= 0 && r <= 7 && f >= 0 && f <= 7;
             r += rankIncrement, f += fileIncrement) {
            attacks |= (1L << (r * 8 + f));
        }
        return attacks;
    }

    public static long getRookAttacks(int square) {
        return rookAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.h1.index();
        Bitboard board = new Bitboard();
        long attacks = getRookAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.rook, Chess.white, i);
            }
        }
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}

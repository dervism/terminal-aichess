package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating rook attack squares.
 */

public class RookAttacks {
    private static final long[] rookAttacks = new long[64];

    static {
        initializeAllRookAttacks();
    }

    private static void initializeAllRookAttacks() {
        for (int square = 0; square < 64; square++) {
            rookAttacks[square] = calculateAllRookAttacks(square);
        }
    }

    private static long calculateAllRookAttacks(int square) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;
        attacks |= calculateAllDirectionalAttacks(rank, file, 0, 1);   // Right
        attacks |= calculateAllDirectionalAttacks(rank, file, 0, -1);  // Left
        attacks |= calculateAllDirectionalAttacks(rank, file, 1, 0);   // Up
        attacks |= calculateAllDirectionalAttacks(rank, file, -1, 0);  // Down
        return attacks;
    }

    private static long calculateAllDirectionalAttacks(int rank, int file, int rankIncrement, int fileIncrement) {
        long attacks = 0L;
        for (int r = rank + rankIncrement, f = file + fileIncrement;
             r >= 0 && r <= 7 && f >= 0 && f <= 7;
             r += rankIncrement, f += fileIncrement) {
            attacks |= (1L << (r * 8 + f));
        }
        return attacks;
    }

    public static long getAllRookAttacks(int square) {
        return rookAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.h1.index();
        Bitboard board = new Bitboard();
        long attacks = getAllRookAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.rook, Chess.white, i);
            }
        }
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}

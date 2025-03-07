package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating bishop attack squares.
 */

public class BishopAttacks {
    private static final long[] bishopAttacks = new long[64];

    static {
        initializeAllBishopAttacks();
    }

    private static void initializeAllBishopAttacks() {
        for (int square = 0; square < 64; square++) {
            bishopAttacks[square] = calculateAllBishopAttacks(square);
        }
    }

    public static long getAllBishopAttacks(int square) {
        return bishopAttacks[square];
    }

    private static long calculateAllBishopAttacks(int square) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;
        attacks |= calculateAllDirectionalAttacks(rank, file, 1, 1);   // Up-Right
        attacks |= calculateAllDirectionalAttacks(rank, file, 1, -1);  // Up-Left
        attacks |= calculateAllDirectionalAttacks(rank, file, -1, 1);  // Down-Right
        attacks |= calculateAllDirectionalAttacks(rank, file, -1, -1); // Down-Left
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

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        Bitboard board = new Bitboard();

        long attacks = getAllBishopAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.bishop, Chess.white, i);
            }
        }

        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}


package no.dervis.terminal_aichess.moves.attacks;

import no.dervis.terminal_aichess.board.Bitboard;
import no.dervis.terminal_aichess.board.Chess;

/**
 * Class for precalculating bishop attack squares.
 */

public class BishopAttacks {
    private static final long[] bishopAttacks = new long[64];

    static {
        initializeBishopAttacks();
    }

    private static void initializeBishopAttacks() {
        for (int square = 0; square < 64; square++) {
            bishopAttacks[square] = calculateBishopAttacks(square);
        }
    }

    private static long calculateBishopAttacks(int square) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;
        attacks |= calculateDirectionalAttacks(rank, file, 1, 1);   // Up-Right
        attacks |= calculateDirectionalAttacks(rank, file, 1, -1);  // Up-Left
        attacks |= calculateDirectionalAttacks(rank, file, -1, 1);  // Down-Right
        attacks |= calculateDirectionalAttacks(rank, file, -1, -1); // Down-Left
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

    public static long getBishopAttacks(int square) {
        return bishopAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        Bitboard board = new Bitboard();

        long attacks = getBishopAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.bishop, Chess.white, i);
            }
        }

        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}


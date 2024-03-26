package no.dervis.terminal_aichess.moves.attacks;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Chess;

/**
 * Class for precalculating queen attack squares.
 */
public class QueenAttacks {
    private static final long[] queenAttacks = new long[64];

    static {
        initializeQueenAttacks();
    }

    private static void initializeQueenAttacks() {
        for (int square = 0; square < 64; square++) {
            queenAttacks[square] = calculateQueenAttacks(square);
        }
    }

    private static long calculateQueenAttacks(int square) {
        long attacks = 0L;
        attacks |= RookAttacks.getRookAttacks(square);  // rook's movements
        attacks |= BishopAttacks.getBishopAttacks(square);  // bishop's movements
        return attacks;
    }

    public static long getQueenAttacks(int square) {
        return queenAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        Bitboard board = new Bitboard();
        long attacks = getQueenAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.queen, Chess.white, i);
            }
        }
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}

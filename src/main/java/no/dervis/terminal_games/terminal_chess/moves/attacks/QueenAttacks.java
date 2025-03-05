package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating queen attack squares.
 */
public class QueenAttacks {
    private static final long[] queenAttacks = new long[64];

    static {
        initializeAllQueenAttacks();
    }

    private static void initializeAllQueenAttacks() {
        for (int square = 0; square < 64; square++) {
            queenAttacks[square] = calculateAllQueenAttacks(square);
        }
    }

    private static long calculateAllQueenAttacks(int square) {
        long attacks = 0L;
        attacks |= RookAttacks.getAllRookAttacks(square);  // rook's movements
        attacks |= BishopAttacks.getAllBishopAttacks(square);  // bishop's movements
        return attacks;
    }

    public static long getAllQueenAttacks(int square) {
        return queenAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e1.index();
        Bitboard board = new Bitboard();
        long attacks = getAllQueenAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.queen, Chess.white, i);
            }
        }
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}

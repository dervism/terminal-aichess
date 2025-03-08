package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating pawn attack squares.
 */
public class PawnAttacks {
    private static final long[][] pawnAttacks = new long[2][64]; // [color][square]

    static {
        initializeAllPawnAttacks();
    }

    private static void initializeAllPawnAttacks() {
        for (int square = 0; square < 64; square++) {
            pawnAttacks[Chess.white][square] = calculatePawnAttacks(square, Chess.white);
            pawnAttacks[Chess.black][square] = calculatePawnAttacks(square, Chess.black);
        }
    }

    private static long calculatePawnAttacks(int square, int color) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;

        // Return no attacks for pawns on first or last rank
        if ((color == Chess.white && (rank == 0 || rank == 7)) || 
            (color == Chess.black && (rank == 0 || rank == 7))) {
            return 0L;
        }

        // White pawns attack upward (rank + 1), Black pawns attack downward (rank - 1)
        int attackRank = (color == Chess.white) ? rank + 1 : rank - 1;

        // Check if the attack rank is valid
        if (attackRank >= 0 && attackRank < 8) {
            // Left diagonal attack
            if (file > 0) {
                attacks |= 1L << (attackRank * 8 + (file - 1));
            }
            // Right diagonal attack
            if (file < 7) {
                attacks |= 1L << (attackRank * 8 + (file + 1));
            }
        }

        return attacks;
    }

    public static long getAllPawnAttacks(int square, int color) {
        return pawnAttacks[color][square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        int color = Chess.white;
        Bitboard board = new Bitboard();

        long attacks = getAllPawnAttacks(square, color);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.pawn, color, i);
            }
        }

        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}

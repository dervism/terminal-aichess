package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating knight attack squares.
 */
public class KnightAttacks {
    private static final long[] knightAttacks = new long[64];

    static {
        initializeAllKnightAttacks();
    }

    private static void initializeAllKnightAttacks() {
        for (int square = 0; square < 64; square++) {
            knightAttacks[square] = calculateKnightAttacks(square);
        }
    }

    private static long calculateKnightAttacks(int square) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;

        // All eight possible knight moves
        int[][] moves = {
            {-2, -1}, {-2, 1},  // Two up, one left/right
            {2, -1},  {2, 1},   // Two down, one left/right
            {-1, -2}, {1, -2},  // One up/down, two left
            {-1, 2},  {1, 2}    // One up/down, two right
        };

        for (int[] move : moves) {
            int newRank = rank + move[0];
            int newFile = file + move[1];
            
            if (newRank >= 0 && newRank < 8 && newFile >= 0 && newFile < 8) {
                attacks |= 1L << (newRank * 8 + newFile);
            }
        }

        return attacks;
    }

    public static long getAllKnightAttacks(int square) {
        return knightAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        Bitboard board = new Bitboard();
        
        long attacks = getAllKnightAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.knight, Chess.white, i);
            }
        }
        
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}
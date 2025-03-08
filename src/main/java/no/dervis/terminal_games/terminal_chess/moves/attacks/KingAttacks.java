package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;

/**
 * Class for precalculating king attack squares.
 */
public class KingAttacks {
    private static final long[] kingAttacks = new long[64];

    static {
        initializeAllKingAttacks();
    }

    private static void initializeAllKingAttacks() {
        for (int square = 0; square < 64; square++) {
            kingAttacks[square] = calculateKingAttacks(square);
        }
    }

    private static long calculateKingAttacks(int square) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;

        // All eight possible king moves
        int[][] moves = {
            {-1, -1}, {-1, 0}, {-1, 1},  // Top row
            {0, -1},           {0, 1},    // Middle row
            {1, -1},  {1, 0},  {1, 1}     // Bottom row
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

    public static long getAllKingAttacks(int square) {
        return kingAttacks[square];
    }

    public static void main(String[] args) {
        int square = Bitboard.e4.index();
        Bitboard board = new Bitboard();
        
        long attacks = getAllKingAttacks(square);
        for (int i = 0; i < 64; i++) {
            if ((attacks & 1L << i) != 0) {
                board.setPiece(Chess.king, Chess.white, i);
            }
        }
        
        // visualise the attacks on an empty board
        System.out.println(Chess.boardToStr.apply(board, true));
    }
}
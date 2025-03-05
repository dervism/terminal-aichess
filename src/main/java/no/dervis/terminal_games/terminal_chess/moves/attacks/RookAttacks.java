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


    // MAGIC

    private static final long[] rookMagicNumbers = new long[64];
    private static final long[][] rookMagicAttackTable = new long[64][1 << 12]; // Lookup table

    static {
        initializeMagicNumbers();
        initializeMagicRookAttacks();
    }

    /**
     * Step 1: Generate Magic Numbers for Rook Moves
     */
    private static void initializeMagicNumbers() {
        for (int square = 0; square < 64; square++) {
            rookMagicNumbers[square] = MagicNumberGenerator.findMagicNumber(square, getBlockerMask(square), Long.bitCount(getBlockerMask(square)), false);
        }
    }

    /**
     * Step 2: Precompute Rook Attack Tables using Magic Bitboards
     */
    private static void initializeMagicRookAttacks() {
        for (int square = 0; square < 64; square++) {
            long mask = getBlockerMask(square);
            int numBits = Long.bitCount(mask);
            int tableSize = 1 << numBits;
            rookMagicAttackTable[square] = new long[tableSize];

            for (int i = 0; i < tableSize; i++) {
                long blockers = MagicNumberGenerator.indexToBlockers(i, mask);
                int magicIndex = (int) ((blockers * rookMagicNumbers[square]) >>> (64 - numBits));
                rookMagicAttackTable[square][magicIndex] = getRookAttacks(square, blockers);
            }
        }
    }

    /**
     * Step 3: Get the relevant blocker mask for a given square.
     */
    public static long getBlockerMask(int square) {
        int rank = square / 8, file = square % 8;
        long mask = 0L;

        mask |= calculateDirectionalMask(rank, file, 1, 0);  // Up
        mask |= calculateDirectionalMask(rank, file, -1, 0); // Down
        mask |= calculateDirectionalMask(rank, file, 0, 1);  // Right
        mask |= calculateDirectionalMask(rank, file, 0, -1); // Left

        return mask;
    }

    /**
     * Step 4: Calculate legal moves in a given direction until a blocker is hit.
     */
    private static long calculateDirectionalMask(int rank, int file, int rankIncrement, int fileIncrement) {
        long mask = 0L;

        for (int r = rank + rankIncrement, f = file + fileIncrement;
             r >= 1 && r <= 6 && f >= 1 && f <= 6; // Exclude edges
             r += rankIncrement, f += fileIncrement) {
            mask |= (1L << (r * 8 + f));
        }

        return mask;
    }

    /**
     * Step 5: Compute dynamic attack sets considering blockers.
     */
    public static long getRookAttacks(int square, long blockers) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;

        attacks |= calculateDirectionalAttacks(rank, file, 1, 0, blockers);  // Up
        attacks |= calculateDirectionalAttacks(rank, file, -1, 0, blockers); // Down
        attacks |= calculateDirectionalAttacks(rank, file, 0, 1, blockers);  // Right
        attacks |= calculateDirectionalAttacks(rank, file, 0, -1, blockers); // Left

        return attacks;
    }

    /**
     * Step 6: Compute legal moves until a blocker is hit.
     */
    private static long calculateDirectionalAttacks(int rank, int file, int rankIncrement, int fileIncrement, long blockers) {
        long attacks = 0L;

        for (int r = rank + rankIncrement, f = file + fileIncrement;
             r >= 0 && r <= 7 && f >= 0 && f <= 7;
             r += rankIncrement, f += fileIncrement) {
            int index = r * 8 + f;
            attacks |= (1L << index);

            if ((blockers & (1L << index)) != 0) { // Stop at first blocker
                break;
            }
        }

        return attacks;
    }

    /**
     * Step 7: Retrieve precomputed attack set using Magic Bitboard hashing.
     */
    public static long getMagicRookAttacks(int square, long occupied) {
        long mask = getBlockerMask(square);
        long blockers = occupied & mask; // Only consider relevant blockers
        int magicIndex = (int) ((blockers * rookMagicNumbers[square]) >>> (64 - Long.bitCount(mask)));

        return rookMagicAttackTable[square][magicIndex];
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

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


    // MAGIC

    private static final long[] bishopMagicNumbers = new long[64]; // Precomputed magic numbers
    private static final long[][] bishopMagicAttackTable = new long[64][1 << 12]; // Lookup table

    static {
        initializeBishopMagicNumbers();
        initializeMagicBishopAttacks();
    }

    private static void initializeBishopMagicNumbers() {
        for (int square = 0; square < 64; square++) {
            bishopMagicNumbers[square] = MagicNumberGenerator.findMagicNumber(square, getBlockerMask(square), Long.bitCount(getBlockerMask(square)), true);
        }
    }

    private static void initializeMagicBishopAttacks() {
        for (int square = 0; square < 64; square++) {
            long mask = getBlockerMask(square);
            int numBits = Long.bitCount(mask);
            int tableSize = 1 << numBits;
            bishopMagicAttackTable[square] = new long[tableSize];

            for (int i = 0; i < tableSize; i++) {
                long blockers = MagicNumberGenerator.indexToBlockers(i, mask);
                int magicIndex = (int) ((blockers * bishopMagicNumbers[square]) >>> (64 - numBits));
                bishopMagicAttackTable[square][magicIndex] = getBishopAttacks(square, blockers);
            }
        }
    }


    /**
     * Returns the attack set using the Magic Bitboard method.
     */
    public static long getMagicBishopAttacks(int square, long occupied) {
        long mask = getBlockerMask(square);
        long blockers = occupied & mask; // Only consider relevant blockers
        int magicIndex = (int) ((blockers * bishopMagicNumbers[square]) >>> (64 - Long.bitCount(mask)));

        return bishopMagicAttackTable[square][magicIndex];
    }

    public static long getBishopAttacks(int square, long blockers) {
        int rank = square / 8, file = square % 8;
        long attacks = 0L;

        attacks |= calculateDirectionalAttacks(rank, file, 1, 1, blockers);   // Up-Right
        attacks |= calculateDirectionalAttacks(rank, file, 1, -1, blockers);  // Up-Left
        attacks |= calculateDirectionalAttacks(rank, file, -1, 1, blockers);  // Down-Right
        attacks |= calculateDirectionalAttacks(rank, file, -1, -1, blockers); // Down-Left

        return attacks;
    }

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

    public static long getBlockerMask(int square) {
        int rank = square / 8, file = square % 8;
        long mask = 0L;

        mask |= calculateDirectionalMask(rank, file, 1, 1);   // Up-Right
        mask |= calculateDirectionalMask(rank, file, 1, -1);  // Up-Left
        mask |= calculateDirectionalMask(rank, file, -1, 1);  // Down-Right
        mask |= calculateDirectionalMask(rank, file, -1, -1); // Down-Left

        return mask;
    }

    private static long calculateDirectionalMask(int rank, int file, int rankIncrement, int fileIncrement) {
        long mask = 0L;

        for (int r = rank + rankIncrement, f = file + fileIncrement;
             r >= 1 && r <= 6 && f >= 1 && f <= 6; // Exclude edges
             r += rankIncrement, f += fileIncrement) {
            mask |= (1L << (r * 8 + f));
        }

        return mask;
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


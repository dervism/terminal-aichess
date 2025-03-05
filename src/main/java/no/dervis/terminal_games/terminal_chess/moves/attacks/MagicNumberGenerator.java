package no.dervis.terminal_games.terminal_chess.moves.attacks;

import java.util.Random;

public class MagicNumberGenerator {
    private static final Random random = new Random();

    /**
     * Generates a magic number for a given square.
     */
    public static long findMagicNumber(int square, long mask, int relevantBits, boolean isBishop) {
        long[] possibleBlockers = generateAllBlockerCombinations(mask);
        long[] attackTable = new long[1 << relevantBits];

        for (long attempt = 0; attempt < 50000000000L; attempt++) { // Try many numbers
            long magic = generateRandomMagic();

            // Skip numbers that don't have enough 1s (avoid weak hashes)
            if (Long.bitCount((magic * possibleBlockers[0]) >>> 56) < 6) {
                continue;
            }

            boolean uniqueMapping = true;
            for (long blockers : possibleBlockers) {
                int magicIndex = (int) ((blockers * magic) >>> (64 - relevantBits));

                if (attackTable[magicIndex] == 0) {
                    attackTable[magicIndex] = getAttackSet(square, blockers, isBishop);
                } else if (attackTable[magicIndex] != getAttackSet(square, blockers, isBishop)) {
                    uniqueMapping = false;
                    break;
                }
            }

            if (uniqueMapping) {
                return magic; // Found a valid magic number!
            }
        }

        throw new RuntimeException("Failed to find magic number for square " + square);
    }

    /**
     * Generates a random magic number with specific constraints.
     */
    private static long generateRandomMagic() {
        return (random.nextLong() & random.nextLong() & random.nextLong()) & 0xFFFFFFFFFFFFFFF0L;
    }

    /**
     * Generates all blocker combinations for a given mask.
     */
    private static long[] generateAllBlockerCombinations(long mask) {
        int numBits = Long.bitCount(mask);
        int numCombinations = 1 << numBits;
        long[] blockers = new long[numCombinations];

        for (int i = 0; i < numCombinations; i++) {
            blockers[i] = indexToBlockers(i, mask);
        }

        return blockers;
    }

    /**
     * Converts an index to a blocker bitboard.
     */
    public static long indexToBlockers(int index, long mask) {
        long blockers = 0L;
        int bitIndex = 0;

        for (int i = 0; i < 64; i++) {
            if ((mask & (1L << i)) != 0) {
                if ((index & (1 << bitIndex)) != 0) {
                    blockers |= (1L << i);
                }
                bitIndex++;
            }
        }

        return blockers;
    }

    /**
     * Computes bishop or rook attack set.
     */
    private static long getAttackSet(int square, long blockers, boolean isBishop) {
        return isBishop ? BishopAttacks.getBishopAttacks(square, blockers)
                : RookAttacks.getRookAttacks(square, blockers);
    }
}
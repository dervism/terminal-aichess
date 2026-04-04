package no.dervis.terminal_games.terminal_chess.moves.attacks;

/**
 * Magic Bitboard implementation for efficient sliding piece attack generation.
 *
 * <p>Instead of iterating along rays to find blockers, magic bitboards use
 * a hash-based lookup: multiply the relevant occupancy bits by a "magic number",
 * shift right, and index into a precomputed attack table. This turns
 * sliding piece attack computation into a single multiply + shift + table lookup.</p>
 *
 * <p>The magic numbers are found deterministically at startup using a fixed-seed
 * sparse random number generator. Because the seed is fixed, the same magics are
 * produced on every run — this is functionally equivalent to hardcoded constants
 * but avoids transcription errors. The approach follows the same pattern used by
 * Stockfish and other production chess engines.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>For each square, compute a <b>relevant occupancy mask</b>: the squares
 *       along the piece's attack rays, excluding board edges (edge squares don't
 *       affect which squares are reachable — the piece always attacks up to and
 *       including an edge blocker).</li>
 *   <li>Find a <b>magic number</b> such that for every subset of the mask
 *       (representing different blocker configurations), the product
 *       {@code (subset * magic) >>> shift} yields a unique index when different
 *       blocker configurations produce different attack sets (constructive
 *       collisions — same attacks for same index — are fine).</li>
 *   <li>Build a lookup table mapping each index to its attack bitboard.</li>
 *   <li>At query time: {@code table[square][(occupancy & mask) * magic >>> shift]}</li>
 * </ol>
 */
public class MagicBitboard {

    // Number of relevant occupancy bits per square for rook
    static final int[] ROOK_BITS = {
        12, 11, 11, 11, 11, 11, 11, 12,
        11, 10, 10, 10, 10, 10, 10, 11,
        11, 10, 10, 10, 10, 10, 10, 11,
        11, 10, 10, 10, 10, 10, 10, 11,
        11, 10, 10, 10, 10, 10, 10, 11,
        11, 10, 10, 10, 10, 10, 10, 11,
        11, 10, 10, 10, 10, 10, 10, 11,
        12, 11, 11, 11, 11, 11, 11, 12
    };

    // Number of relevant occupancy bits per square for bishop
    static final int[] BISHOP_BITS = {
         6, 5, 5, 5, 5, 5, 5, 6,
         5, 5, 5, 5, 5, 5, 5, 5,
         5, 5, 7, 7, 7, 7, 5, 5,
         5, 5, 7, 9, 9, 7, 5, 5,
         5, 5, 7, 9, 9, 7, 5, 5,
         5, 5, 7, 7, 7, 7, 5, 5,
         5, 5, 5, 5, 5, 5, 5, 5,
         6, 5, 5, 5, 5, 5, 5, 6
    };

    // Relevant occupancy masks (attack rays excluding board edges)
    private static final long[] ROOK_MASKS = new long[64];
    private static final long[] BISHOP_MASKS = new long[64];

    // Magic numbers found during initialization
    private static final long[] ROOK_MAGICS = new long[64];
    private static final long[] BISHOP_MAGICS = new long[64];

    // Attack lookup tables: [square][magic_index] -> attack bitboard
    private static final long[][] ROOK_TABLE = new long[64][];
    private static final long[][] BISHOP_TABLE = new long[64][];

    static {
        initMasks();
        initAllTables();
    }

    // ---- Public API ----

    /**
     * Returns the rook attack bitboard for a given square and board occupancy.
     * Single multiply-shift-lookup — O(1).
     */
    public static long rookAttacks(int square, long occupancy) {
        int index = (int) ((occupancy & ROOK_MASKS[square]) * ROOK_MAGICS[square]
                >>> (64 - ROOK_BITS[square]));
        return ROOK_TABLE[square][index];
    }

    /**
     * Returns the bishop attack bitboard for a given square and board occupancy.
     * Single multiply-shift-lookup — O(1).
     */
    public static long bishopAttacks(int square, long occupancy) {
        int index = (int) ((occupancy & BISHOP_MASKS[square]) * BISHOP_MAGICS[square]
                >>> (64 - BISHOP_BITS[square]));
        return BISHOP_TABLE[square][index];
    }

    /**
     * Returns the queen attack bitboard (union of rook and bishop attacks).
     */
    public static long queenAttacks(int square, long occupancy) {
        return rookAttacks(square, occupancy) | bishopAttacks(square, occupancy);
    }

    // ---- Initialization ----

    private static void initMasks() {
        for (int sq = 0; sq < 64; sq++) {
            ROOK_MASKS[sq] = computeRookMask(sq);
            BISHOP_MASKS[sq] = computeBishopMask(sq);
        }
    }

    private static void initAllTables() {
        // Fixed-seed PRNG for deterministic magic number generation.
        // The sparse random outputs (few bits set) are characteristic of good magic numbers
        // because they produce fewer collisions in the hash.
        long seed = 728861L;

        for (int sq = 0; sq < 64; sq++) {
            seed = initTable(sq, ROOK_MASKS[sq], ROOK_BITS[sq],
                    ROOK_MAGICS, ROOK_TABLE, true, seed);
            seed = initTable(sq, BISHOP_MASKS[sq], BISHOP_BITS[sq],
                    BISHOP_MAGICS, BISHOP_TABLE, false, seed);
        }
    }

    /**
     * Finds a magic number for one square and populates its lookup table.
     * Returns the updated PRNG seed.
     */
    private static long initTable(int square, long mask, int bits,
                                  long[] magics, long[][] table,
                                  boolean isRook, long seed) {
        int tableSize = 1 << bits;
        int maskPopCount = Long.bitCount(mask);

        // Enumerate all occupancy subsets and their attacks
        int subsetCount = 1 << maskPopCount;
        long[] occupancies = new long[subsetCount];
        long[] attacks = new long[subsetCount];

        long occupancy = 0L;
        for (int i = 0; i < subsetCount; i++) {
            occupancies[i] = occupancy;
            attacks[i] = isRook ? computeRookAttacks(square, occupancy)
                                : computeBishopAttacks(square, occupancy);
            occupancy = (occupancy - mask) & mask; // Carry-Rippler
        }

        // Trial loop: find a magic with no destructive collisions
        long[] candidate = new long[tableSize];
        outer:
        for (;;) {
            // Generate a sparse random candidate
            seed = xorshift64(seed);
            long m1 = seed;
            seed = xorshift64(seed);
            long m2 = seed;
            seed = xorshift64(seed);
            long m3 = seed;
            long magic = m1 & m2 & m3; // AND of three randoms → sparse bits

            // Quick reject: top bits of mask*magic should have enough bits set
            if (Long.bitCount((mask * magic) & 0xFF00000000000000L) < 6) continue;

            // Reset candidate table
            java.util.Arrays.fill(candidate, 0L);
            boolean[] used = new boolean[tableSize];

            for (int i = 0; i < subsetCount; i++) {
                int index = (int) ((occupancies[i] * magic) >>> (64 - bits));
                if (!used[index]) {
                    used[index] = true;
                    candidate[index] = attacks[i];
                } else if (candidate[index] != attacks[i]) {
                    // Destructive collision — try next magic
                    continue outer;
                }
            }

            // Success: store magic and table
            magics[square] = magic;
            table[square] = candidate.clone();
            return seed;
        }
    }

    private static long xorshift64(long state) {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return state;
    }

    // ---- Mask computation (relevant occupancy bits, excluding board edges) ----

    static long computeRookMask(int square) {
        long mask = 0L;
        int rank = square / 8, file = square % 8;

        for (int r = rank + 1; r < 7; r++) mask |= 1L << (r * 8 + file);
        for (int r = rank - 1; r > 0; r--) mask |= 1L << (r * 8 + file);
        for (int f = file + 1; f < 7; f++) mask |= 1L << (rank * 8 + f);
        for (int f = file - 1; f > 0; f--) mask |= 1L << (rank * 8 + f);

        return mask;
    }

    static long computeBishopMask(int square) {
        long mask = 0L;
        int rank = square / 8, file = square % 8;

        for (int r = rank + 1, f = file + 1; r < 7 && f < 7; r++, f++) mask |= 1L << (r * 8 + f);
        for (int r = rank + 1, f = file - 1; r < 7 && f > 0; r++, f--) mask |= 1L << (r * 8 + f);
        for (int r = rank - 1, f = file + 1; r > 0 && f < 7; r--, f++) mask |= 1L << (r * 8 + f);
        for (int r = rank - 1, f = file - 1; r > 0 && f > 0; r--, f--) mask |= 1L << (r * 8 + f);

        return mask;
    }

    // ---- Attack computation for a given occupancy (used during init only) ----

    static long computeRookAttacks(int square, long occupancy) {
        long attacks = 0L;
        int rank = square / 8, file = square % 8;

        for (int r = rank + 1; r <= 7; r++) {
            long bit = 1L << (r * 8 + file);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int r = rank - 1; r >= 0; r--) {
            long bit = 1L << (r * 8 + file);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int f = file + 1; f <= 7; f++) {
            long bit = 1L << (rank * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int f = file - 1; f >= 0; f--) {
            long bit = 1L << (rank * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }

        return attacks;
    }

    static long computeBishopAttacks(int square, long occupancy) {
        long attacks = 0L;
        int rank = square / 8, file = square % 8;

        for (int r = rank + 1, f = file + 1; r <= 7 && f <= 7; r++, f++) {
            long bit = 1L << (r * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int r = rank + 1, f = file - 1; r <= 7 && f >= 0; r++, f--) {
            long bit = 1L << (r * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int r = rank - 1, f = file + 1; r >= 0 && f <= 7; r--, f++) {
            long bit = 1L << (r * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            long bit = 1L << (r * 8 + f);
            attacks |= bit;
            if ((occupancy & bit) != 0) break;
        }

        return attacks;
    }
}

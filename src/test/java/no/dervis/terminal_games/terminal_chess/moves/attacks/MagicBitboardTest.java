package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.moves.BishopMoveGenerator;
import no.dervis.terminal_games.terminal_chess.moves.RookMoveGenerator;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicBitboardTest {

    @Test
    void rookAttacksMatchRayBased() {
        Random rng = new Random(42);
        for (int square = 0; square < 64; square++) {
            // Test with empty board
            long magicResult = MagicBitboard.rookAttacks(square, 0L);
            long rayResult = RookMoveGenerator.rookAttacks(square, 0L);
            assertEquals(rayResult, magicResult,
                    "Rook attacks differ on empty board at square " + square);

            // Test with random occupancies
            for (int trial = 0; trial < 100; trial++) {
                long occupancy = rng.nextLong();
                magicResult = MagicBitboard.rookAttacks(square, occupancy);
                rayResult = RookMoveGenerator.rookAttacks(square, occupancy);
                assertEquals(rayResult, magicResult,
                        "Rook attacks differ at square " + square + " occ=0x" + Long.toHexString(occupancy));
            }
        }
    }

    @Test
    void bishopAttacksMatchRayBased() {
        Random rng = new Random(42);
        for (int square = 0; square < 64; square++) {
            // Test with empty board
            long magicResult = MagicBitboard.bishopAttacks(square, 0L);
            long rayResult = BishopMoveGenerator.bishopAttacks(square, 0L);
            assertEquals(rayResult, magicResult,
                    "Bishop attacks differ on empty board at square " + square);

            // Test with random occupancies
            for (int trial = 0; trial < 100; trial++) {
                long occupancy = rng.nextLong();
                magicResult = MagicBitboard.bishopAttacks(square, occupancy);
                rayResult = BishopMoveGenerator.bishopAttacks(square, occupancy);
                assertEquals(rayResult, magicResult,
                        "Bishop attacks differ at square " + square + " occ=0x" + Long.toHexString(occupancy));
            }
        }
    }

    @Test
    void queenAttacksCombinesRookAndBishop() {
        Random rng = new Random(123);
        for (int square = 0; square < 64; square++) {
            for (int trial = 0; trial < 50; trial++) {
                long occupancy = rng.nextLong();
                long queenResult = MagicBitboard.queenAttacks(square, occupancy);
                long expected = MagicBitboard.rookAttacks(square, occupancy)
                              | MagicBitboard.bishopAttacks(square, occupancy);
                assertEquals(expected, queenResult,
                        "Queen attacks should be rook | bishop at square " + square);
            }
        }
    }
}

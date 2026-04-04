package no.dervis.terminal_games.terminal_chess.moves.attacks;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttacksTest {

    @Test
    public void testKnightAttacks() {
        // Test center square (e4)
        int centerSquare = Bitboard.e4.index();
        long centerAttacks = KnightAttacks.getAllKnightAttacks(centerSquare);

        // A knight in the center should attack 8 squares
        assertEquals(8, Long.bitCount(centerAttacks));

        // Test corner square (a1)
        int cornerSquare = Bitboard.a1.index();
        long cornerAttacks = KnightAttacks.getAllKnightAttacks(cornerSquare);

        // A knight in the corner should attack 2 squares
        assertEquals(2, Long.bitCount(cornerAttacks));
    }

    @Test
    public void testPawnAttacks() {
        // Test white pawn attacks from e4
        int centerSquare = Bitboard.e4.index();
        long whitePawnAttacks = PawnAttacks.getAllPawnAttacks(centerSquare, Chess.white);

        // A white pawn in the center should attack 2 squares
        assertEquals(2, Long.bitCount(whitePawnAttacks));

        // Test black pawn attacks from e5
        int blackSquare = Bitboard.e5.index();
        long blackPawnAttacks = PawnAttacks.getAllPawnAttacks(blackSquare, Chess.black);

        // A black pawn in the center should attack 2 squares
        assertEquals(2, Long.bitCount(blackPawnAttacks));

        // Edge cases: white pawn on rank 1 attacks one square (b2), black pawn on rank 8 attacks one square (g7).
        // These aren't real pawn positions (pawns promote on rank 1/8), but the table must produce correct
        // diagonal attacks for all squares since it's also used for reverse lookups in check detection.
        assertEquals(1, Long.bitCount(PawnAttacks.getAllPawnAttacks(Bitboard.a1.index(), Chess.white)));
        assertEquals(1, Long.bitCount(PawnAttacks.getAllPawnAttacks(Bitboard.h8.index(), Chess.black)));
        // Truly impossible directions still return 0: white pawn on rank 8 has no square above, black pawn on rank 1 has no square below
        assertEquals(0, Long.bitCount(PawnAttacks.getAllPawnAttacks(Bitboard.a8.index(), Chess.white)));
        assertEquals(0, Long.bitCount(PawnAttacks.getAllPawnAttacks(Bitboard.a1.index(), Chess.black)));
    }

    @Test
    public void testKingAttacks() {
        // Test center square (e4)
        int centerSquare = Bitboard.e4.index();
        long centerAttacks = KingAttacks.getAllKingAttacks(centerSquare);

        // A king in the center should attack 8 squares
        assertEquals(8, Long.bitCount(centerAttacks));

        // Test corner square (a1)
        int cornerSquare = Bitboard.a1.index();
        long cornerAttacks = KingAttacks.getAllKingAttacks(cornerSquare);

        // A king in the corner should attack 3 squares
        assertEquals(3, Long.bitCount(cornerAttacks));
    }
}

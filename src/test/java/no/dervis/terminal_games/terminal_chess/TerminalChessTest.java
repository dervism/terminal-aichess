package no.dervis.terminal_games.terminal_chess;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple2;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerminalChessTest implements Chess {

    // FEN: two white rooks (e3 and g2) can both reach g3
    private static final String TWO_ROOKS_FEN =
            "r3k2r/ppp2ppp/1n3b2/4pq2/8/1BNPR3/PPPB1PRP/7K w kq - 0 1";

    /**
     * "Rg3" is ambiguous (rooks on e3 and g2 can both reach g3).
     * matchSAN should return 2 matches — but the game loop must NOT
     * treat this as a promotion.
     */
    @Test
    void ambiguousRookMoveReturnsTwoMatches() {
        Bitboard board = Bitboard.fromFEN(TWO_ROOKS_FEN);
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> rg3 = TerminalChess.matchSAN("Rg3", moves, board);
        assertEquals(2, rg3.size(), "Rg3 should match two rook moves");

        for (var t : rg3) {
            assertEquals(0, t.right().promotionPiece(), "Rook move should not be a promotion");
            assertEquals(rook, t.right().piece());
        }
    }

    /**
     * "Reg3" disambiguates to the rook on the e-file.
     */
    @Test
    void disambiguatedRookFromEFile() {
        Bitboard board = Bitboard.fromFEN(TWO_ROOKS_FEN);
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> reg3 = TerminalChess.matchSAN("Reg3", moves, board);
        assertEquals(1, reg3.size(), "Reg3 should match exactly one rook move");
        assertEquals(rook, reg3.getFirst().right().piece());
        assertEquals('e', (char) ('a' + reg3.getFirst().right().fromSquare() % 8));
    }

    /**
     * "Rgg3" disambiguates to the rook on the g-file.
     */
    @Test
    void disambiguatedRookFromGFile() {
        Bitboard board = Bitboard.fromFEN(TWO_ROOKS_FEN);
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> rgg3 = TerminalChess.matchSAN("Rgg3", moves, board);
        assertEquals(1, rgg3.size(), "Rgg3 should match exactly one rook move");
        assertEquals(rook, rgg3.getFirst().right().piece());
        assertEquals('g', (char) ('a' + rgg3.getFirst().right().fromSquare() % 8));
    }

    /**
     * The game loop must detect that multiple matches with different
     * from-squares is an ambiguity (not a promotion) and reject the move,
     * asking the user to disambiguate.
     */
    @Test
    void ambiguousMoveIsNotPromotion() {
        Bitboard board = Bitboard.fromFEN(TWO_ROOKS_FEN);
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> rg3 = TerminalChess.matchSAN("Rg3", moves, board);

        // Verify: the matches have DIFFERENT from-squares (ambiguity)
        // and are NOT promotions
        boolean isPromotion = rg3.stream().anyMatch(t -> t.right().promotionPiece() > 0);
        boolean hasDifferentFromSquares = rg3.stream()
                .map(t -> t.right().fromSquare())
                .distinct()
                .count() > 1;

        assertFalse(isPromotion, "Ambiguous rook moves should not be promotions");
        assertTrue(hasDifferentFromSquares, "Ambiguous moves come from different squares");
    }

    /**
     * Real promotion: pawn on e7 moves to e8. "e8=Q" should match exactly one move.
     */
    @Test
    void promotionWithExplicitPiece() {
        Bitboard board = Bitboard.fromFEN("8/4P3/8/8/8/8/8/4K2k w - - 0 1");
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> e8q = TerminalChess.matchSAN("e8=Q", moves, board);
        assertEquals(1, e8q.size(), "e8=Q should match exactly one promotion move");
        assertEquals(queen, e8q.getFirst().right().promotionPiece() % 6);
    }

    /**
     * "e8" without promotion suffix should match all four promotion moves.
     * This IS a valid scenario for the promotion prompt.
     */
    @Test
    void promotionWithoutSuffixMatchesAll() {
        Bitboard board = Bitboard.fromFEN("8/4P3/8/8/8/8/8/4K2k w - - 0 1");
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> e8 = TerminalChess.matchSAN("e8", moves, board);
        assertEquals(4, e8.size(), "e8 should match four promotion moves (N, B, R, Q)");

        // All from the same square — this is promotion, not ambiguity
        long distinctFrom = e8.stream().map(t -> t.right().fromSquare()).distinct().count();
        assertEquals(1, distinctFrom, "All promotion moves should come from the same square");
    }

    /**
     * Unambiguous piece move should return exactly one match.
     */
    @Test
    void unambiguousPieceMove() {
        Bitboard board = Bitboard.fromFEN(TWO_ROOKS_FEN);
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(white);

        List<Tuple2<Integer, Move>> na4 = TerminalChess.matchSAN("Na4", moves, board);
        assertEquals(1, na4.size(), "Na4 should match exactly one knight move");
        assertEquals(knight, na4.getFirst().right().piece());
    }
}

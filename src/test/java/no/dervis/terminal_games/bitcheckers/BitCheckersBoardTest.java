package no.dervis.terminal_games.bitcheckers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.dervis.terminal_games.bitcheckers.BitCheckersBoard.BLACK;
import static no.dervis.terminal_games.bitcheckers.BitCheckersBoard.WHITE;
import static org.junit.jupiter.api.Assertions.*;

class BitCheckersBoardTest {

    private BitCheckersBoard board;

    @BeforeEach
    void setUp() {
        board = new BitCheckersBoard();
        board.initialise();
    }

    // ---- Initial position tests ----

    @Nested
    class InitialPosition {

        @Test
        void blackHas12Pieces() {
            assertEquals(12, board.blackPieceCount());
        }

        @Test
        void whiteHas12Pieces() {
            assertEquals(12, board.whitePieceCount());
        }

        @Test
        void noKingsAtStart() {
            assertEquals(0, board.blackKingCount());
            assertEquals(0, board.whiteKingCount());
        }

        @Test
        void blackToMoveFirst() {
            assertEquals(BLACK, board.turn());
        }

        @Test
        void blackPiecesOnRows0Through2DarkSquares() {
            long black = board.black();
            for (int sq = 0; sq < 24; sq++) {
                int row = sq / 8, col = sq % 8;
                boolean isDark = (row + col) % 2 == 1;
                if (isDark) {
                    assertTrue((black & (1L << sq)) != 0,
                            "Expected black piece on dark square " + sq);
                } else {
                    assertEquals(0, black & (1L << sq),
                            "No black piece on light square " + sq);
                }
            }
        }

        @Test
        void whitePiecesOnRows5Through7DarkSquares() {
            long white = board.white();
            for (int sq = 40; sq < 64; sq++) {
                int row = sq / 8, col = sq % 8;
                boolean isDark = (row + col) % 2 == 1;
                if (isDark) {
                    assertTrue((white & (1L << sq)) != 0,
                            "Expected white piece on dark square " + sq);
                } else {
                    assertEquals(0, white & (1L << sq),
                            "No white piece on light square " + sq);
                }
            }
        }

        @Test
        void middleRowsAreEmpty() {
            long occupied = board.black() | board.white();
            for (int sq = 24; sq < 40; sq++) {
                assertEquals(0, occupied & (1L << sq),
                        "Square " + sq + " should be empty in initial position");
            }
        }
    }

    // ---- Simple move generation ----

    @Nested
    class SimpleMoves {

        @Test
        void blackHas7MovesFromInitialPosition() {
            List<CheckersMove> moves = board.generateMoves();
            assertEquals(7, moves.size(), "Black should have 7 opening moves");
        }

        @Test
        void allInitialMovesAreNonCaptures() {
            List<CheckersMove> moves = board.generateMoves();
            for (CheckersMove m : moves) {
                assertFalse(m.isJump(), "Initial moves should not be captures: " + m);
            }
        }

        @Test
        void blackMovesGoForward() {
            List<CheckersMove> moves = board.generateMoves();
            for (CheckersMove m : moves) {
                assertTrue(m.to() > m.from(),
                        "Black moves should go forward (higher index): " + m);
            }
        }

        @Test
        void whiteHas7MovesAfterOneBlackMove() {
            List<CheckersMove> blackMoves = board.generateMoves();
            board.makeMove(blackMoves.getFirst());
            assertEquals(WHITE, board.turn());

            List<CheckersMove> whiteMoves = board.generateMoves();
            assertEquals(7, whiteMoves.size(), "White should have 7 opening moves");
        }

        @Test
        void whiteMovesGoBackward() {
            // Make one black move to get to white's turn
            board.makeMove(board.generateMoves().getFirst());

            List<CheckersMove> moves = board.generateMoves();
            for (CheckersMove m : moves) {
                assertTrue(m.to() < m.from(),
                        "White moves should go backward (lower index): " + m);
            }
        }
    }

    // ---- Jump (capture) tests ----

    @Nested
    class Jumps {

        @Test
        void singleJumpIsGenerated() {
            board = new BitCheckersBoard();
            // Black man on c3 (sq 18), white man on d4 (sq 27), landing on e5 (sq 36)
            board.setPiece(BLACK, false, 18);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            assertEquals(1, moves.size());
            CheckersMove jump = moves.getFirst();
            assertTrue(jump.isJump());
            assertEquals(18, jump.from());
            assertEquals(36, jump.to());
            assertEquals(1, jump.captureCount());
        }

        @Test
        void forcedCaptureRule() {
            board = new BitCheckersBoard();
            // Black man on a1 (sq 0) — can simple-move to b2 (sq 9)
            // Black man on c3 (sq 18), white man on d4 (sq 27) — jump available
            board.setPiece(BLACK, false, 0);
            board.setPiece(BLACK, false, 18);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            // Only jumps should be returned
            assertTrue(moves.stream().allMatch(CheckersMove::isJump),
                    "When jumps are available, only jumps should be legal");
        }

        @Test
        void multiJumpCapture() {
            board = new BitCheckersBoard();
            // Black man on a1 (sq 0)
            // White men on b2 (sq 9) and d4 (sq 27)
            // Path: a1 -> c3 (jump b2) -> e5 (jump d4)
            board.setPiece(BLACK, false, 0);
            board.setPiece(WHITE, false, 9);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            assertEquals(1, moves.size());
            CheckersMove multiJump = moves.getFirst();
            assertEquals(0, multiJump.from());
            assertEquals(36, multiJump.to());
            assertEquals(2, multiJump.captureCount());
        }

        @Test
        void jumpRemovesOpponentPiece() {
            board = new BitCheckersBoard();
            board.setPiece(BLACK, false, 18);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            CheckersMove jump = board.generateMoves().getFirst();
            board.makeMove(jump);

            assertEquals(0, board.whitePieceCount(), "Captured piece should be removed");
            assertEquals(1, board.blackPieceCount());
        }
    }

    // ---- King movement ----

    @Nested
    class KingMovement {

        @Test
        void kingMovesInAllFourDirections() {
            board = new BitCheckersBoard();
            // Black king on d4 (sq 27)
            board.setPiece(BLACK, true, 27);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            // d4 king can go to c5(34), e5(36), c3(18), e3(20)
            assertEquals(4, moves.size(), "King on d4 should have 4 diagonal moves");

            boolean hasForwardLeft  = moves.stream().anyMatch(m -> m.to() == 34);
            boolean hasForwardRight = moves.stream().anyMatch(m -> m.to() == 36);
            boolean hasBackLeft     = moves.stream().anyMatch(m -> m.to() == 18);
            boolean hasBackRight    = moves.stream().anyMatch(m -> m.to() == 20);

            assertTrue(hasForwardLeft, "King should move forward-left");
            assertTrue(hasForwardRight, "King should move forward-right");
            assertTrue(hasBackLeft, "King should move back-left");
            assertTrue(hasBackRight, "King should move back-right");
        }

        @Test
        void kingCanJumpBackward() {
            board = new BitCheckersBoard();
            // Black king on e5 (sq 36), white man on d4 (sq 27)
            // King should be able to jump backward to c3 (sq 18)
            board.setPiece(BLACK, true, 36);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            assertTrue(moves.stream().anyMatch(m -> m.isJump() && m.to() == 18),
                    "King should be able to jump backward");
        }
    }

    // ---- Promotion ----

    @Nested
    class Promotion {

        @Test
        void blackManPromotesOnRow7() {
            board = new BitCheckersBoard();
            // Black man on d7 (sq 51), empty e8 (sq 60)
            board.setPiece(BLACK, false, 51);
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            // Should be able to move to c8 (58) and e8 (60)
            CheckersMove promo = moves.stream()
                    .filter(m -> m.to() / 8 == 7)
                    .findFirst().orElseThrow();
            board.makeMove(promo);

            long kingBB = board.kings();
            assertTrue((kingBB & (1L << promo.to())) != 0,
                    "Black man should be promoted to king on row 8");
        }

        @Test
        void whiteManPromotesOnRow0() {
            board = new BitCheckersBoard();
            // White man on b2 (sq 9)
            board.setPiece(WHITE, false, 9);
            board.setTurn(WHITE);

            List<CheckersMove> moves = board.generateMoves();
            CheckersMove promo = moves.stream()
                    .filter(m -> m.to() / 8 == 0)
                    .findFirst().orElseThrow();
            board.makeMove(promo);

            long kingBB = board.kings();
            assertTrue((kingBB & (1L << promo.to())) != 0,
                    "White man should be promoted to king on row 1");
        }

        @Test
        void promotionStopsJumpChain() {
            board = new BitCheckersBoard();
            // Black man on e5 (sq 36), white on f6 (sq 45)
            // Black jumps to g7 (sq 54) — promotion rank for black
            // Even if there were another white piece to jump, the chain stops
            board.setPiece(BLACK, false, 36);
            board.setPiece(WHITE, false, 45);
            board.setPiece(WHITE, false, 55); // h7, would allow further jump if not promotion
            board.setTurn(BLACK);

            List<CheckersMove> moves = board.generateMoves();
            // The jump should land on g7 (54) and stop due to promotion
            assertTrue(moves.stream().anyMatch(m -> m.to() == 54 && m.captureCount() == 1),
                    "Jump should stop at promotion rank with 1 capture");
            assertFalse(moves.stream().anyMatch(m -> m.captureCount() == 2),
                    "Double jump through promotion rank should not be allowed");
        }
    }

    // ---- Make / unmake round-trip ----

    @Nested
    class MakeUnmake {

        @Test
        void simpleMoveMakeUnmakeRestoresState() {
            long origBlack = board.black();
            long origWhite = board.white();
            long origKings = board.kings();
            int origTurn = board.turn();

            CheckersMove move = board.generateMoves().getFirst();
            board.makeMove(move);
            board.unmakeMove(move);

            assertEquals(origBlack, board.black(), "Black bitboard should be restored");
            assertEquals(origWhite, board.white(), "White bitboard should be restored");
            assertEquals(origKings, board.kings(), "Kings bitboard should be restored");
            assertEquals(origTurn, board.turn(), "Turn should be restored");
        }

        @Test
        void jumpMakeUnmakeRestoresState() {
            board = new BitCheckersBoard();
            board.setPiece(BLACK, false, 18);
            board.setPiece(WHITE, false, 27);
            board.setTurn(BLACK);

            long origBlack = board.black();
            long origWhite = board.white();
            long origKings = board.kings();

            CheckersMove jump = board.generateMoves().getFirst();
            board.makeMove(jump);
            board.unmakeMove(jump);

            assertEquals(origBlack, board.black());
            assertEquals(origWhite, board.white());
            assertEquals(origKings, board.kings());
            assertEquals(BLACK, board.turn());
        }

        @Test
        void multipleMoveMakeUnmakeSequence() {
            long origBlack = board.black();
            long origWhite = board.white();

            // Play a few moves and then unmake all of them
            CheckersMove m1 = board.generateMoves().getFirst();
            board.makeMove(m1);
            CheckersMove m2 = board.generateMoves().getFirst();
            board.makeMove(m2);
            CheckersMove m3 = board.generateMoves().getFirst();
            board.makeMove(m3);

            board.unmakeMove(m3);
            board.unmakeMove(m2);
            board.unmakeMove(m1);

            assertEquals(origBlack, board.black());
            assertEquals(origWhite, board.white());
            assertEquals(BLACK, board.turn());
        }
    }

    // ---- Game termination ----

    @Nested
    class Termination {

        @Test
        void gameIsTerminalWhenNoPieces() {
            board = new BitCheckersBoard();
            board.setPiece(BLACK, false, 0);
            board.setTurn(BLACK);
            // White has no pieces
            assertTrue(board.isTerminal());
        }

        @Test
        void gameIsNotTerminalAtStart() {
            assertFalse(board.isTerminal());
        }

        @Test
        void terminalScoreWhenCurrentPlayerHasNoPieces() {
            board = new BitCheckersBoard();
            // Only white pieces, black to move — black has no pieces but let's test
            // with a blocked position instead
            board.setPiece(WHITE, false, 0);
            board.setTurn(BLACK);
            // Black has 0 pieces → terminal
            assertTrue(board.isTerminal());
        }
    }

    // ---- Copy ----

    @Test
    void copyProducesIndependentBoard() {
        BitCheckersBoard copy = board.copy();
        assertEquals(board.black(), copy.black());
        assertEquals(board.white(), copy.white());
        assertEquals(board.kings(), copy.kings());
        assertEquals(board.turn(), copy.turn());

        // Mutating copy should not affect original
        copy.makeMove(copy.generateMoves().getFirst());
        assertNotEquals(board.black(), copy.black());
    }

    // ---- Pretty print smoke test ----

    @Test
    void prettyPrintContainsBoardElements() {
        String output = board.prettyPrint();
        assertTrue(output.contains("a"));
        assertTrue(output.contains("h"));
        assertTrue(output.contains("1"));
        assertTrue(output.contains("8"));
    }
}

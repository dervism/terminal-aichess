package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import org.junit.jupiter.api.Test;

import static no.dervis.terminal_games.terminal_chess.board.Board.*;
import static no.dervis.terminal_games.terminal_chess.board.Chess.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorTest {

    @Test
    void generateInitialPositionMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);

        // In initial position, each side should have 20 legal moves
        assertEquals(20, generator.generateMoves(white).size());
        assertEquals(20, generator.generateMoves(black).size());
    }

    @Test
    void filtersPinnedPieceMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        // Print initial board state
        System.out.println("[DEBUG_LOG] Board before setup:");
        System.out.println(boardToStr.apply(board, true));

        // Remove pieces to create a pin scenario
        board.removePiece(pawn, white, f2.index());
        board.removePiece(knight, white, g1.index());
        board.removePiece(bishop, white, f1.index());
        board.removePiece(bishop, black, f8.index());

        // Place black bishop to pin the pawn
        board.setPiece(bishop, black, h3.index());  // Black bishop pins white pawn on g2

        // Print final board state and square indices
        System.out.println("[DEBUG_LOG] Board after setup:");
        System.out.println(boardToStr.apply(board, true));
        System.out.println("[DEBUG_LOG] Square indices:");
        System.out.println("[DEBUG_LOG] King at e1: " + e1.index() + " (rank=" + e1.index()/8 + ", file=" + e1.index()%8 + ")");
        System.out.println("[DEBUG_LOG] Pawn at g2: " + g2.index() + " (rank=" + g2.index()/8 + ", file=" + g2.index()%8 + ")");
        System.out.println("[DEBUG_LOG] Bishop at h3: " + h3.index() + " (rank=" + h3.index()/8 + ", file=" + h3.index()%8 + ")");

        // Verify pieces are in correct positions
        assertTrue((board.whitePieces()[pawn] & (1L << g2.index())) != 0, "White pawn should be on g2");
        assertTrue((board.blackPieces()[bishop] & (1L << h3.index())) != 0, "Black bishop should be on h3");

        Generator generator = new Generator(board);
        var moves = generator.generateMoves(white);

        // Print all generated moves
        System.out.println("[DEBUG_LOG] Generated moves for white:");
        for (int move : moves) {
            int from = move >>> 14;
            int to = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] Move from " + from + " to " + to);
        }

        // The pawn on g2 should not be able to move as it's pinned
        for (int move : moves) {
            int from = move >>> 14;
            if (from == g2.index()) {
                System.out.println("[DEBUG_LOG] Found illegal move of pinned pawn from g2");
            }
            assertTrue(from != g2.index(), "Pinned pawn should not be able to move");
        }
    }

    @Test
    void filtersMovesLeavingKingInCheck() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        // Print initial board state
        System.out.println("[DEBUG_LOG] Board before setup:");
        System.out.println(boardToStr.apply(board, true));

        // Setup position where moving a piece would expose king to check
        board.removePiece(pawn, white, e2.index());
        board.removePiece(pawn, white, f2.index());
        board.removePiece(rook, black, a8.index());
        board.removePiece(queen, black, d8.index());
        board.removePiece(bishop, black, c8.index());
        board.setPiece(rook, black, e4.index());  // Black rook attacks king if f1 bishop moves

        // Print final board state and square indices
        System.out.println("[DEBUG_LOG] Board after setup:");
        System.out.println(boardToStr.apply(board, true));
        System.out.println("[DEBUG_LOG] Square indices:");
        System.out.println("[DEBUG_LOG] King at e1: " + e1.index() + " (rank=" + e1.index()/8 + ", file=" + e1.index()%8 + ")");
        System.out.println("[DEBUG_LOG] Bishop at f1: " + f1.index() + " (rank=" + f1.index()/8 + ", file=" + f1.index()%8 + ")");
        System.out.println("[DEBUG_LOG] Rook at e4: " + e4.index() + " (rank=" + e4.index()/8 + ", file=" + e4.index()%8 + ")");

        // Verify pieces are in correct positions
        assertTrue((board.whitePieces()[bishop] & (1L << f1.index())) != 0, "White bishop should be on f1");
        assertTrue((board.blackPieces()[rook] & (1L << e4.index())) != 0, "Black rook should be on e4");

        Generator generator = new Generator(board);
        var moves = generator.generateMoves(white);

        // Print all generated moves
        System.out.println("[DEBUG_LOG] Generated moves for white:");
        for (int move : moves) {
            int from = move >>> 14;
            int to = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] Move from " + from + " to " + to);
        }

        // The f1 bishop should not be able to move as it would expose the king
        for (int move : moves) {
            int from = move >>> 14;
            if (from == f1.index()) {
                System.out.println("[DEBUG_LOG] Found illegal move of bishop from f1");
            }
            assertTrue(from != f1.index(), "Bishop should not be able to move as it would expose king to check");
        }
    }

    @Test
    void generatesCheckEvasionMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        // Setup position where king is in check
        board.removePiece(pawn, white, e2.index());
        board.removePiece(pawn, white, f2.index());
        board.setPiece(queen, black, e3.index());

        Generator generator = new Generator(board);
        var moves = generator.generateMoves(white);

        // Verify that only moves that get out of check are generated
        for (int move : moves) {
            Bitboard tempBoard = board.copy();
            tempBoard.makeMove(move);
            CheckHelper checkHelper = new CheckHelper(tempBoard);
            assertTrue(!checkHelper.isKingInCheck(white), "Move should get king out of check");
        }
    }
}

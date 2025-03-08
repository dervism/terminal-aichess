package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.dervis.terminal_games.terminal_chess.board.Board.*;
import static no.dervis.terminal_games.terminal_chess.board.Chess.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PawnMoveGeneratorTest {

    @Test
    void testInitialWhitePawnMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        PawnMoveGenerator generator = new PawnMoveGenerator(board);

        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Debug output
        System.out.println("[DEBUG_LOG] Initial white pawn moves:");
        for (Integer move : moves) {
            int fromSquare = move >>> 14;
            int toSquare = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] From: " + fromSquare + " To: " + toSquare);
        }

        // Each pawn can move one or two squares forward from initial position
        assertEquals(16, moves.size());
    }

    @Test
    void testBlockedPawnMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Place a black piece in front of the e2 pawn
        board.setPiece(pawn, black, e3.index());

        System.out.println(boardToStr.apply(board, true));

        System.out.println("[DEBUG_LOG] Board setup for blocked pawn test:");
        System.out.println("[DEBUG_LOG] Black blocking pawn at e3 (index " + e3.index() + ")");

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        System.out.println("[DEBUG_LOG] Generated moves for blocked pawn test:");
        for (Integer move : moves) {
            int fromSquare = move >>> 14;
            int toSquare = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] From: " + fromSquare + " To: " + toSquare);
        }

        // 7 pawns can move one or two squares, blocked pawn can't move
        assertEquals(16, moves.size());
    }

    @Test
    void testPawnCaptures() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up a white pawn with two black pieces it can capture
        board.setPiece(pawn, white, e4.index());
        board.setPiece(pawn, black, d5.index());
        board.setPiece(pawn, black, f5.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Pawn should have 3 legal moves: forward one square and two captures
        assertEquals(3, moves.size());
    }

    @Test
    void testPawnPromotion() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up a white pawn about to promote
        board.setPiece(pawn, white, e7.index());
        board.setPiece(pawn, black, d8.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Should have 8 moves: 4 promotion moves forward and 4 promotion capture moves
        assertEquals(8, moves.size());
    }

    @Test
    void testEnPassantCapture() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up an en passant situation
        board.setPiece(pawn, white, e5.index());
        board.setPiece(pawn, black, f5.index());

        // Set en passant target square (f6)
        long enPassantTarget = 1L << f6.index();

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, enPassantTarget);

        // Should have 2 moves: forward one square and en passant capture
        assertEquals(2, moves.size());
    }

    @Test
    void testEdgeFileCaptures() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up pawns on A and H files
        board.setPiece(pawn, white, a4.index());
        board.setPiece(pawn, white, h4.index());
        board.setPiece(pawn, black, b5.index());
        board.setPiece(pawn, black, g5.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Each pawn should have 2 moves: forward one square and capture
        assertEquals(4, moves.size());
    }

    @Test
    void testCaptureOwnPieces() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up white pawns that could capture each other if it were legal
        board.setPiece(pawn, white, e4.index());
        board.setPiece(pawn, white, d5.index());
        board.setPiece(pawn, white, f5.index());

        System.out.println(boardToStr.apply(board, true));

        System.out.println("[DEBUG_LOG] Board setup for capture own pieces test:");
        System.out.println("[DEBUG_LOG] White pawn at e4 (index " + e4.index() + ")");
        System.out.println("[DEBUG_LOG] White pawn at d5 (index " + d5.index() + ")");
        System.out.println("[DEBUG_LOG] White pawn at f5 (index " + f5.index() + ")");

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        System.out.println("[DEBUG_LOG] Generated moves for capture own pieces test:");
        for (Integer move : moves) {
            int fromSquare = move >>> 14;
            int toSquare = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] From: " + fromSquare + " To: " + toSquare);
        }

        // Only the forward move should be legal
        assertEquals(1, moves.size());
    }

    @Test
    void testDoubleMoveFromNonStartingPosition() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up a white pawn on the third rank
        board.setPiece(pawn, white, e3.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Only single forward move should be possible
        assertEquals(1, moves.size());
    }

    @Test
    void testInvalidEnPassant() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up pawns but with invalid en passant target
        board.setPiece(pawn, white, e5.index());
        board.setPiece(pawn, black, f5.index());

        // Set invalid en passant target (e4)
        long invalidEnPassantTarget = 1L << e4.index();

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, invalidEnPassantTarget);

        // Only forward move should be possible, en passant should be invalid
        assertEquals(1, moves.size());
    }

    @Test
    void testBlackPawnMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up black pawns in various positions
        board.setPiece(pawn, black, e7.index()); // Starting position
        board.setPiece(pawn, black, f6.index()); // Advanced position
        board.setPiece(pawn, white, e6.index()); // Blocking piece
        board.setPiece(pawn, white, g5.index()); // Capture target

        System.out.println(boardToStr.apply(board, true));

        System.out.println("[DEBUG_LOG] Board setup for black pawns:");
        System.out.println("[DEBUG_LOG] Black pawn at e7 (index " + e7.index() + ")");
        System.out.println("[DEBUG_LOG] Black pawn at f6 (index " + f6.index() + ")");
        System.out.println("[DEBUG_LOG] White pawn at e6 (index " + e6.index() + ")");
        System.out.println("[DEBUG_LOG] White pawn at g5 (index " + g5.index() + ")");

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(black, 0L);

        System.out.println("[DEBUG_LOG] Generated black pawn moves:");
        for (Integer move : moves) {
            int fromSquare = move >>> 14;
            int toSquare = (move >>> 7) & 0x3F;
            System.out.println("[DEBUG_LOG] From: " + fromSquare + " To: " + toSquare);
        }

        // f6 pawn: 1 move (single) and 1 capture
        assertEquals(2, moves.size());
    }

    @Test
    void testPawnAtBoardEdge() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up a white pawn at the edge of the board
        board.setPiece(pawn, white, e8.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // No moves should be possible at the edge
        assertEquals(0, moves.size());
    }

    @Test
    void testIllegalBackwardMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        // Clear the board first
        for (int i = 0; i < 64; i++) {
            board.removePiece(pawn, white, i);
            board.removePiece(pawn, black, i);
        }

        // Set up pawns with enemy pieces behind them
        board.setPiece(pawn, white, e4.index());
        board.setPiece(pawn, black, e3.index());
        board.setPiece(pawn, black, d3.index());
        board.setPiece(pawn, black, f3.index());

        PawnMoveGenerator generator = new PawnMoveGenerator(board);
        List<Integer> moves = generator.generatePawnMoves(white, 0L);

        // Only forward move should be possible, no backward captures
        assertEquals(1, moves.size());
    }
}

package no.dervis.terminal_games.terminal_chess.moves.generator;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawRulesTest implements Chess, Board {

    // ---------------------------------------------------------------
    //  Half-move clock (50-move rule foundation)
    // ---------------------------------------------------------------

    @Test
    void halfMoveClock_incrementsOnQuietMoves() {
        // King + Rook vs King — only quiet moves
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/R3K3 w - - 0 1");
        assertEquals(0, board.halfMoveClock());

        // White king moves e1 -> d1 (quiet)
        board.makeMove(Move.createMove(e1.index(), d1.index(), MoveType.NORMAL.ordinal()));
        assertEquals(1, board.halfMoveClock());

        // Black king moves e5 -> d5 (quiet)
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));
        assertEquals(2, board.halfMoveClock());
    }

    @Test
    void halfMoveClock_resetsOnPawnMove() {
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/4P3/4K3 w - - 10 1");
        assertEquals(10, board.halfMoveClock());

        // White pawn e2 -> e4 — pawn move resets the clock
        board.makeMove(Move.createMove(e2.index(), e4.index(), MoveType.NORMAL.ordinal()));
        assertEquals(0, board.halfMoveClock());
    }

    @Test
    void halfMoveClock_resetsOnCapture() {
        // White rook on a1, black knight on a5, kings present
        Bitboard board = Bitboard.fromFEN("8/8/8/n3k3/8/8/8/R3K3 w - - 15 1");
        assertEquals(15, board.halfMoveClock());

        // Ra1xa5 — capture resets the clock
        board.makeMove(Move.createMove(a1.index(), a5.index(), MoveType.ATTACK.ordinal()));
        assertEquals(0, board.halfMoveClock());
    }

    @Test
    void halfMoveClock_resetsOnEnPassant() {
        // White pawn on e5, black pawn just moved d7-d5 (en passant possible)
        Bitboard board = Bitboard.fromFEN("8/8/8/3Ppk2/8/8/8/4K3 w - - 20 1");
        // Manually set up the move history so en passant target is recognized:
        // we simulate black's d7-d5 by adding it to history
        board.setHalfMoveClock(20);

        // exd6 e.p.
        board.makeMove(Move.createMove(d5.index(), e6.index(), MoveType.EN_PASSANT.ordinal()));
        assertEquals(0, board.halfMoveClock(), "En passant should reset halfmove clock");
    }

    @Test
    void halfMoveClock_preservedByCopy() {
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/R3K3 w - - 42 1");
        assertEquals(42, board.halfMoveClock());

        Bitboard copy = board.copy();
        assertEquals(42, copy.halfMoveClock());

        // Mutating original does not affect copy
        board.makeMove(Move.createMove(e1.index(), d1.index(), MoveType.NORMAL.ordinal()));
        assertEquals(43, board.halfMoveClock());
        assertEquals(42, copy.halfMoveClock());
    }

    // ---------------------------------------------------------------
    //  50-move rule via Generator.getGameState()
    // ---------------------------------------------------------------

    @Test
    void fiftyMoveRule_triggersAtClock100() {
        // Rook + King vs King — sufficient material, clock at 100
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/R3K3 w - - 100 1");

        Generator generator = new Generator(board);
        assertEquals(Generator.GameState.FIFTY_MOVE_RULE, generator.getGameState(white));
    }

    @Test
    void fiftyMoveRule_doesNotTriggerAt99() {
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/R3K3 w - - 99 1");

        Generator generator = new Generator(board);
        // At 99 the game is still ongoing (sufficient material, legal moves exist)
        assertNotEquals(Generator.GameState.FIFTY_MOVE_RULE, generator.getGameState(white));
    }

    @Test
    void fiftyMoveRule_reachedByPlayingMoves() {
        // Rook + King vs King — play quiet moves until clock hits 100
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/R3K3 w - - 0 1");
        board.setHalfMoveClock(98);

        // Two more quiet moves → clock = 100
        board.makeMove(Move.createMove(a1.index(), a2.index(), MoveType.NORMAL.ordinal()));
        assertEquals(99, board.halfMoveClock());
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));
        assertEquals(100, board.halfMoveClock());

        Generator generator = new Generator(board);
        assertEquals(Generator.GameState.FIFTY_MOVE_RULE, generator.getGameState(board.turn()));
    }

    // ---------------------------------------------------------------
    //  Position key for threefold repetition
    // ---------------------------------------------------------------

    @Test
    void positionKey_samePositionSameKey() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        String key1 = board.positionKey();
        String key2 = board.positionKey();
        assertEquals(key1, key2, "Same position should produce the same key");
    }

    @Test
    void positionKey_differentSideToMoveDifferentKey() {
        Bitboard boardWhite = Bitboard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
        Bitboard boardBlack = Bitboard.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1");

        assertNotEquals(boardWhite.positionKey(), boardBlack.positionKey(),
                "Different side to move should produce different keys");
    }

    @Test
    void positionKey_differentCastlingRightsDifferentKey() {
        Bitboard withCastling = Bitboard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1");
        Bitboard noCastling  = Bitboard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w - - 0 1");

        assertNotEquals(withCastling.positionKey(), noCastling.positionKey(),
                "Different castling rights should produce different keys");
    }

    @Test
    void positionKey_returnsToSameKeyAfterRoundTrip() {
        // Kg1-f1-g1 should return to the same position key
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/5K2 w - - 0 1");
        String original = board.positionKey();

        // Kf1-e1
        board.makeMove(Move.createMove(f1.index(), e1.index(), MoveType.NORMAL.ordinal()));
        assertNotEquals(original, board.positionKey());

        // Ke5-d5
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));

        // Ke1-f1
        board.makeMove(Move.createMove(e1.index(), f1.index(), MoveType.NORMAL.ordinal()));

        // Kd5-e5
        board.makeMove(Move.createMove(d5.index(), e5.index(), MoveType.NORMAL.ordinal()));

        assertEquals(original, board.positionKey(),
                "Position key should match after pieces return to original squares");
    }

    // ---------------------------------------------------------------
    //  Threefold repetition detection
    // ---------------------------------------------------------------

    @Test
    void threefoldRepetition_detectedAfterThreeOccurrences() {
        // King vs King — shuffle kings back and forth
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/5K2 w - - 0 1");
        List<String> positions = new ArrayList<>();
        positions.add(board.positionKey()); // position 1 (occurrence 1)

        // Move pair 1: Kf1-e1, Ke5-d5
        board.makeMove(Move.createMove(f1.index(), e1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        // Move pair 2: Ke1-f1, Kd5-e5 → back to start (occurrence 2)
        board.makeMove(Move.createMove(e1.index(), f1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(d5.index(), e5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        assertFalse(Generator.isThreefoldRepetition(positions),
                "Only 2 occurrences so far — should not be threefold");

        // Move pair 3: Kf1-e1, Ke5-d5
        board.makeMove(Move.createMove(f1.index(), e1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        // Move pair 4: Ke1-f1, Kd5-e5 → back to start (occurrence 3)
        board.makeMove(Move.createMove(e1.index(), f1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(d5.index(), e5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        assertTrue(Generator.isThreefoldRepetition(positions),
                "Position has occurred 3 times — threefold repetition");
    }

    @Test
    void threefoldRepetition_notTriggeredWithOnlyTwoOccurrences() {
        Bitboard board = Bitboard.fromFEN("8/8/8/4k3/8/8/8/5K2 w - - 0 1");
        List<String> positions = new ArrayList<>();
        positions.add(board.positionKey());

        // One round trip: start → other → start (2 occurrences)
        board.makeMove(Move.createMove(f1.index(), e1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(e5.index(), d5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(e1.index(), f1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());
        board.makeMove(Move.createMove(d5.index(), e5.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        assertFalse(Generator.isThreefoldRepetition(positions),
                "Only 2 occurrences — not yet threefold");
    }

    @Test
    void threefoldRepetition_notTriggeredWithFewerThanFivePositions() {
        List<String> positions = new ArrayList<>();
        positions.add("pos1");
        positions.add("pos1");
        positions.add("pos1");

        assertFalse(Generator.isThreefoldRepetition(positions),
                "Fewer than 5 positions in history — early exit returns false");
    }

    @Test
    void threefoldRepetition_castlingLossBreaksRepetition() {
        // Same piece placement, but castling rights differ after king moves
        Bitboard board = Bitboard.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1");
        List<String> positions = new ArrayList<>();
        positions.add(board.positionKey());

        String keyWithCastling = board.positionKey();

        // White king moves e1-d1
        board.makeMove(Move.createMove(e1.index(), d1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        // Black king moves e8-d8
        board.makeMove(Move.createMove(e8.index(), d8.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        // White king moves d1-e1 (back, but castling rights are now lost)
        board.makeMove(Move.createMove(d1.index(), e1.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        // Black king moves d8-e8 (back, castling rights lost)
        board.makeMove(Move.createMove(d8.index(), e8.index(), MoveType.NORMAL.ordinal()));
        positions.add(board.positionKey());

        String keyWithoutCastling = board.positionKey();

        assertNotEquals(keyWithCastling, keyWithoutCastling,
                "Loss of castling rights means position is different");
        assertFalse(Generator.isThreefoldRepetition(positions),
                "Castling rights changed — these are different positions");
    }
}

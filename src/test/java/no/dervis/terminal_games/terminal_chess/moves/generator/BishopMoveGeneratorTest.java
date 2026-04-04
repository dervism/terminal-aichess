package no.dervis.terminal_games.terminal_chess.moves.generator;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;
import org.junit.jupiter.api.Test;

import static no.dervis.terminal_games.terminal_chess.board.Board.*;
import static no.dervis.terminal_games.terminal_chess.board.Chess.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BishopMoveGeneratorTest {

    @Test
    void generatesNoLegalMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        BishopMoveGenerator generator = new BishopMoveGenerator(board);

        assertEquals(0, generator.generateBishopMoves(white).size());
    }

    @Test
    void generatesMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        board.removePiece(pawn, white, b2.index());
        board.removePiece(pawn, white, d2.index());

        System.out.println(boardToStr.apply(board, true));

        BishopMoveGenerator generator = new BishopMoveGenerator(board);

        assertEquals(7, generator.generateBishopMoves(white).size());
    }

    @Test
    void generatesAttackMoves() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        board.setPiece(pawn, black, f4.index());
        board.removePiece(pawn, white, b2.index());
        board.removePiece(pawn, white, d2.index());

        System.out.println(boardToStr.apply(board, true));

        BishopMoveGenerator generator = new BishopMoveGenerator(board);

        assertEquals(5, generator.generateBishopMoves(white).size());
        long whiteBishops = board.getBishops(white);
        assertTrue((MagicBitboard.bishopAttacks(f4.index(), board.allPieces()) & whiteBishops) != 0);
    }
}
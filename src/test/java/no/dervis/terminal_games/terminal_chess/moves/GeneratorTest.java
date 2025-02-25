package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import org.junit.jupiter.api.Test;

import static no.dervis.terminal_games.terminal_chess.board.Chess.black;
import static no.dervis.terminal_games.terminal_chess.board.Chess.white;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}

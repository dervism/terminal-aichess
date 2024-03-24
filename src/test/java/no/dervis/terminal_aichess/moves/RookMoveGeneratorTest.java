package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RookMoveGeneratorTest implements Board, Chess {

    @Test
    void rookAttacksInitial() {
        Bitboard board = new Bitboard();
        board.setPiece(king, white, e1.index());
        board.setPiece(king, black, e8.index());

        // white queen side
        board.setPiece(rook, white, a1.index());
        board.setPiece(knight, white, b1.index());
        board.setPiece(bishop, white, c1.index());
        board.setPiece(queen, white, d1.index());

        // white king side
        board.setPiece(bishop, white, f1.index());
        board.setPiece(knight, white, g1.index());
        board.setPiece(rook, white, h1.index());

        // black king side
        board.setPiece(bishop, black, f8.index());
        board.setPiece(knight, black, g8.index());
        board.setPiece(rook, black, h8.index());

        // black queen side
        board.setPiece(rook, black, a8.index());
        board.setPiece(knight, black, b8.index());
        board.setPiece(bishop, black, c8.index());
        board.setPiece(queen, black, d8.index());

        System.out.println(boardToStr.apply(board, false));

        Generator g = new Generator(board);
        List<Tuple2<Integer, Move>> whiteMoves = g.generateMoves(white).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .filter(move -> move.right().piece() == rook)
                .toList();
        assertEquals(14, whiteMoves.size());
    }
}
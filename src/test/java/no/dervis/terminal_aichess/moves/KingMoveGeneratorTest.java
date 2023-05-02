package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KingMoveGeneratorTest implements Board, Chess {

    @Test
    void generateNoMovesStartingPosition() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        List<Board.T2<Integer, Move>> whiteKingMoves
                = new KingMoveGenerator(board).generateKingMoves(white)
                .stream()
                .map(move -> new Board.T2<>(move, Move.createMove(move, board)))
                //.peek(System.out::println)
                .toList();
        assertEquals(0, whiteKingMoves.size());

        List<Board.T2<Integer, Move>> blackKingMoves
                = new KingMoveGenerator(board).generateKingMoves(black)
                .stream()
                .map(move -> new Board.T2<>(move, Move.createMove(move, board)))
                //.peek(System.out::println)
                .toList();
        assertEquals(0, blackKingMoves.size());
    }

    @Test
    void generateKingMoves() {
        Bitboard board = new Bitboard();

        board.setPiece(king, white, e1.index());
        board.setPiece(king, black, a1.index());
        board.setPiece(rook, black, e8.index());
        System.out.println(boardToStr.apply(board, false));

        List<Board.T2<Integer, Move>> whiteKingMoves
                = new KingMoveGenerator(board).generateKingMoves(white)
                .stream()
                .map(move -> new Board.T2<>(move, Move.createMove(move, board)))
                .peek(System.out::println)
                .toList();
        assertEquals(5, whiteKingMoves.size());

        List<Board.T2<Integer, Move>> blackKingMoves
                = new KingMoveGenerator(board).generateKingMoves(black)
                .stream()
                .map(move -> new Board.T2<>(move, Move.createMove(move, board)))
                .peek(System.out::println)
                .toList();
        assertEquals(3, blackKingMoves.size());

        // a final test to make sure all moves on the board is generated correctly
        Generator g = new Generator(board);
        List<Board.T2<Integer, Move>> blackMoves = g.generateMoves(black).stream()
                .map(move -> new Board.T2<>(move, Move.createMove(move, board)))
                .peek(System.out::println)
                .toList();
        assertEquals(17, blackMoves.size());
    }

}
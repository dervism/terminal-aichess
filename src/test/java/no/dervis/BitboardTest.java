package no.dervis;

import no.dervis.moves.Generator;
import no.dervis.moves.Move;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class BitboardTest implements Board, Chess{

    @Test
    void setPiece() {
    }

    @Test
    void removePiece() {
    }

    @Test
    void hasPiece() {
    }

    @Test
    void getPiece() {
    }

    @Test
    void makeMove() {
    }

    @Test
    void castlingRights() {
        Bitboard board = new Bitboard();

        board.setPiece(king, white, e1.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(rook, white, a1.index());
        System.out.println(printBoard.apply(board));

        Generator g = new Generator(board);
        List<T2<Integer, Move>> moves = g.generateMoves(white).stream()
                .map(move -> new T2<>(move, Move.createMove(move, board)))
                .peek(System.out::println)
                .toList();
        Assertions.assertEquals(26, moves.size());

        List<T2<Integer, Move>> castleMoves = moves.stream().filter(move -> move.right().moveType() == MoveType.CASTLE_KING_SIDE.ordinal()
                || move.right().moveType() == MoveType.CASTLE_QUEEN_SIDE.ordinal()).toList();
        Assertions.assertEquals(2, castleMoves.size());

        board.makeMove(castleMoves.get(0).left());
        System.out.println(printBoard.apply(board));
        Assertions.assertFalse(board.canCastle(white));

    }
}
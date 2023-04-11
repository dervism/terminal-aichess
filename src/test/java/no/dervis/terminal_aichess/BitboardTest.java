package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.moves.Generator;
import no.dervis.terminal_aichess.moves.Move;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BitboardTest implements Board, Chess {

    @Test
    void setPiece() {
        Bitboard board = new Bitboard();
        board.setPiece(king, white, e1.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(rook, white, a1.index());
        System.out.println(printBoard.apply(board));

        // make sure the correct bits is set
        assertTrue((board.whitePieces()[king] & (1L << e1.index())) != 0);
        assertTrue((board.whitePieces()[rook] & (1L << h1.index())) != 0);
        assertTrue((board.whitePieces()[rook] & (1L << a1.index())) != 0);
    }

    @Test
    void removePiece() {
        Bitboard board = new Bitboard();
        board.setPiece(king, black, e8.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(rook, black, a8.index());
        System.out.println(printBoard.apply(board));

        board.removePiece(king, black, e8.index());
        board.removePiece(rook, black, h8.index());
        board.removePiece(rook, black, a8.index());
        System.out.println(printBoard.apply(board));

        // make sure the correct bits is unset
        assertEquals(0, (board.blackPieces()[king] & (1L << e8.index())));
        assertEquals(0, (board.blackPieces()[rook] & (1L << h8.index())));
        assertEquals(0, (board.blackPieces()[rook] & (1L << a8.index())));
    }

    @Test
    void hasPiece() {
        Bitboard board = new Bitboard();
        board.setPiece(king, black, e8.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(rook, black, a8.index());

        assertTrue(board.hasPiece(king, black, e8.index()));
        assertTrue(board.hasPiece(rook, black, h8.index()));
        assertTrue(board.hasPiece(rook, black, a8.index()));
        assertFalse(board.hasPiece(pawn, black, a8.index()));
        assertFalse(board.hasPiece(knight, black, e4.index()));
    }

    @Test
    void getPiece() {
        Bitboard board = new Bitboard();
        board.setPiece(king, white, e1.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(rook, white, a1.index());

        assertEquals(king, board.getPiece(e1.index()));
        assertEquals(rook, board.getPiece(h1.index()));
        assertEquals(rook, board.getPiece(a1.index()));

        board.setPiece(king, black, e8.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(rook, black, a8.index());

        assertEquals(bking, board.getPiece(e8.index()));
        assertEquals(brook, board.getPiece(h8.index()));
        assertEquals(brook, board.getPiece(a8.index()));
    }

    @Test
    void initialiseBoard() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        for (T3 pawnSquare : rank2) {
            assertEquals(pawn, board.getPiece(pawnSquare.index()));
        }
        assertEquals(knight, board.getPiece(b1.index()));
        assertEquals(knight, board.getPiece(g1.index()));
        assertEquals(bishop, board.getPiece(c1.index()));
        assertEquals(bishop, board.getPiece(f1.index()));
        assertEquals(rook, board.getPiece(a1.index()));
        assertEquals(rook, board.getPiece(h1.index()));
        assertEquals(queen, board.getPiece(d1.index()));
        assertEquals(king, board.getPiece(e1.index()));

        for (T3 pawnSquare : rank7) {
            System.out.println(pawnSquare.index() + "=" + board.getPiece(pawnSquare.index()));
            assertEquals(bpawn, board.getPiece(pawnSquare.index()));
        }
        assertEquals(bknight, board.getPiece(b8.index()));
        assertEquals(bknight, board.getPiece(g8.index()));
        assertEquals(bbishop, board.getPiece(c8.index()));
        assertEquals(bbishop, board.getPiece(f8.index()));
        assertEquals(brook, board.getPiece(a8.index()));
        assertEquals(brook, board.getPiece(h8.index()));
        assertEquals(bqueen, board.getPiece(d8.index()));
        assertEquals(bking, board.getPiece(e8.index()));
    }

    @Test
    void makeMovePawns() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        System.out.println(printBoard.apply(board));

        // initial white pawn moves, 1 and 2 squares forward
        Bitboard wPawnMovesBoard = board.copy();
        List<T2<Integer, Move>> pawnMovesWhite1 = new LinkedList<>();
        for (T3 pawnSquare : rank2) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() + 8 << 7);
            pawnMovesWhite1.add(new T2<>(move, Move.createMove(move, board)));
            wPawnMovesBoard.makeMove(move);
            assertEquals(pawn, wPawnMovesBoard.getPiece(pawnSquare.index() + 8));
            assertEquals(empty, wPawnMovesBoard.getPiece(pawnSquare.index()));
        }

        Bitboard wPawnMovesBoard2 = board.copy();
        List<T2<Integer, Move>> pawnMovesWhite2 = new LinkedList<>();
        for (T3 pawnSquare : rank2) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() + 16 << 7);
            wPawnMovesBoard2.makeMove(move);
            pawnMovesWhite2.add(new T2<>(move, Move.createMove(move, board)));
            assertEquals(pawn, wPawnMovesBoard2.getPiece(pawnSquare.index() + 16));
            assertEquals(empty, wPawnMovesBoard2.getPiece(pawnSquare.index()));
        }

        // initial black pawn moves, 1 and 2 squares forward
        Bitboard bPawnMovesBoard = board.copy();
        List<T2<Integer, Move>> pawnMovesBlack1 = new LinkedList<>();
        for (T3 pawnSquare : rank7) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() - 8 << 7);
            pawnMovesBlack1.add(new T2<>(move, Move.createMove(move, board)));
            bPawnMovesBoard.makeMove(move);
            assertEquals(bpawn, bPawnMovesBoard.getPiece(pawnSquare.index() - 8));
            assertEquals(empty, bPawnMovesBoard.getPiece(pawnSquare.index()));
        }

        Bitboard bPawnMovesBoard2 = board.copy();
        List<T2<Integer, Move>> pawnMovesBlack2 = new LinkedList<>();
        for (T3 pawnSquare : rank7) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() - 16 << 7);
            bPawnMovesBoard2.makeMove(move);
            pawnMovesBlack2.add(new T2<>(move, Move.createMove(move, board)));
            assertEquals(bpawn, bPawnMovesBoard2.getPiece(pawnSquare.index() - 16));
            assertEquals(empty, bPawnMovesBoard2.getPiece(pawnSquare.index()));
        }
    }

    @Test
    void makeMovePawnsEnPassant() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        Bitboard wPawnMovesBoard = board.copy();
        int bmove = (a7.index() << 14) | (a4.index() << 7);
        int wmove = (b2.index() << 14) | (b4.index() << 7);
        wPawnMovesBoard.makeMove(bmove);
        wPawnMovesBoard.makeMove(wmove);
        System.out.println(printBoard.apply(wPawnMovesBoard));

        Generator g = new Generator(wPawnMovesBoard);
        List<T2<Integer, Move>> moves = g.generateMoves(black).stream()
                .map(move -> new T2<>(move, Move.createMove(move, wPawnMovesBoard)))
                .peek(System.out::println)
                .toList();

        assertEquals(1, moves.stream().filter(m -> m.right().moveType() == MoveType.EN_PASSANT.ordinal()).toList().size());
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
        assertEquals(26, moves.size());

        List<T2<Integer, Move>> castleMoves = moves.stream().filter(move -> move.right().moveType() == MoveType.CASTLE_KING_SIDE.ordinal()
                || move.right().moveType() == MoveType.CASTLE_QUEEN_SIDE.ordinal()).toList();
        assertEquals(2, castleMoves.size());

        Bitboard copy = board.copy();
        board.makeMove(castleMoves.get(0).left());
        System.out.println(printBoard.apply(board));
        assertFalse(board.canCastle(white));

    }
}
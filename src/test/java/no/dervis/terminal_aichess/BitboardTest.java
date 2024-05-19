package no.dervis.terminal_aichess;

import no.dervis.terminal_aichess.board.Bitboard;
import no.dervis.terminal_aichess.board.Board;
import no.dervis.terminal_aichess.board.Chess;
import no.dervis.terminal_aichess.moves.Generator;
import no.dervis.terminal_aichess.moves.Move;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class BitboardTest implements Board, Chess {

    private final Predicate<Tuple2<Integer, Move>> onlyCastlingMoves = move -> move.right().moveType() == MoveType.CASTLE_KING_SIDE.ordinal()
            || move.right().moveType() == MoveType.CASTLE_QUEEN_SIDE.ordinal();

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

        for (Tuple3 pawnSquare : rank2) {
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

        for (Tuple3 pawnSquare : rank7) {
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
        List<Tuple2<Integer, Move>> pawnMovesWhite1 = new LinkedList<>();
        for (Tuple3 pawnSquare : rank2) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() + 8 << 7);
            pawnMovesWhite1.add(new Tuple2<>(move, Move.createMove(move, board)));
            wPawnMovesBoard.makeMove(move);
            assertEquals(pawn, wPawnMovesBoard.getPiece(pawnSquare.index() + 8));
            assertEquals(empty, wPawnMovesBoard.getPiece(pawnSquare.index()));
        }

        Bitboard wPawnMovesBoard2 = board.copy();
        List<Tuple2<Integer, Move>> pawnMovesWhite2 = new LinkedList<>();
        for (Tuple3 pawnSquare : rank2) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() + 16 << 7);
            wPawnMovesBoard2.makeMove(move);
            pawnMovesWhite2.add(new Tuple2<>(move, Move.createMove(move, board)));
            assertEquals(pawn, wPawnMovesBoard2.getPiece(pawnSquare.index() + 16));
            assertEquals(empty, wPawnMovesBoard2.getPiece(pawnSquare.index()));
        }

        // initial black pawn moves, 1 and 2 squares forward
        Bitboard bPawnMovesBoard = board.copy();
        List<Tuple2<Integer, Move>> pawnMovesBlack1 = new LinkedList<>();
        for (Tuple3 pawnSquare : rank7) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() - 8 << 7);
            pawnMovesBlack1.add(new Tuple2<>(move, Move.createMove(move, board)));
            bPawnMovesBoard.makeMove(move);
            assertEquals(bpawn, bPawnMovesBoard.getPiece(pawnSquare.index() - 8));
            assertEquals(empty, bPawnMovesBoard.getPiece(pawnSquare.index()));
        }

        Bitboard bPawnMovesBoard2 = board.copy();
        List<Tuple2<Integer, Move>> pawnMovesBlack2 = new LinkedList<>();
        for (Tuple3 pawnSquare : rank7) {
            int move = (pawnSquare.index() << 14) | (pawnSquare.index() - 16 << 7);
            bPawnMovesBoard2.makeMove(move);
            pawnMovesBlack2.add(new Tuple2<>(move, Move.createMove(move, board)));
            assertEquals(bpawn, bPawnMovesBoard2.getPiece(pawnSquare.index() - 16));
            assertEquals(empty, bPawnMovesBoard2.getPiece(pawnSquare.index()));
        }
    }

    @Test
    void makeMovePawnsEnPassant() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();

        Bitboard whitePawnABMoves = board.copy();
        whitePawnABMoves.makeMove(a7, a4);
        whitePawnABMoves.makeMove(b2, b4);
        System.out.println(printBoard.apply(whitePawnABMoves));

        Generator g = new Generator(whitePawnABMoves);
        List<Tuple2<Integer, Move>> movesAB = g.generateMoves(black).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, whitePawnABMoves)))
                .toList();

        assertEquals(1, movesAB.stream().filter(m -> m.right().moveType() == MoveType.EN_PASSANT.ordinal()).toList().size());

        Bitboard whitePawnHGMoves = board.copy();
        whitePawnHGMoves.makeMove(g7, g4);
        whitePawnHGMoves.makeMove(h2, h4);
        System.out.println(printBoard.apply(whitePawnHGMoves));

        List<Tuple2<Integer, Move>> movesHG = g.generateMoves(black).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, whitePawnHGMoves)))
                .toList();
        assertEquals(1, movesHG.stream().filter(m -> m.right().moveType() == MoveType.EN_PASSANT.ordinal()).toList().size());
    }

    @Test
    void whiteCastlingRights() {
        Bitboard board = new Bitboard();

        board.setPiece(king, white, e1.index());
        board.setPiece(king, black, e8.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(rook, white, a1.index());
        board.setPiece(rook, black, a8.index());
        System.out.println(printBoard.apply(board));

        Generator g = new Generator(board);
        List<Tuple2<Integer, Move>> moves = g.generateMoves(white).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .toList();
        assertEquals(26, moves.size());

        List<Tuple2<Integer, Move>> castleMoves = moves
                .stream()
                .filter(onlyCastlingMoves).toList();
        assertEquals(2, castleMoves.size());

        Bitboard kingSideCastle = board.copy();
        kingSideCastle.makeMove(castleMoves.get(0).left());
        System.out.println(printBoard.apply(kingSideCastle));
        assertFalse(kingSideCastle.canCastle(white));
        assertTrue(kingSideCastle.canCastle(black));
        assertEquals(king, kingSideCastle.getPiece(g1.index()));
        assertEquals(rook, kingSideCastle.getPiece(f1.index()));
        assertEquals(rook, kingSideCastle.getPiece(a1.index()));
        assertEquals(empty, kingSideCastle.getPiece(e1.index()));

        Bitboard queenSideCastle = board.copy();
        queenSideCastle.makeMove(castleMoves.get(1).left());
        System.out.println(boardToStr.apply(queenSideCastle, false));
        assertFalse(queenSideCastle.canCastle(white));
        assertTrue(queenSideCastle.canCastle(black));
        assertEquals(king, queenSideCastle.getPiece(c1.index()));
        assertEquals(rook, queenSideCastle.getPiece(d1.index()));
        assertEquals(rook, queenSideCastle.getPiece(h1.index()));
        assertEquals(empty, queenSideCastle.getPiece(a1.index()));
    }

    @Test
    void blackCastlingRights() {
        Bitboard board = new Bitboard();

        board.setPiece(king, white, e1.index());
        board.setPiece(king, black, e8.index());
        board.setPiece(rook, white, h1.index());
        board.setPiece(rook, black, h8.index());
        board.setPiece(rook, white, a1.index());
        board.setPiece(rook, black, a8.index());
        System.out.println(boardToStr.apply(board, false));

        Generator g = new Generator(board);
        List<Tuple2<Integer, Move>> moves = g.generateMoves(black).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .peek(System.out::println)
                .toList();
        assertEquals(26, moves.size());

        List<Tuple2<Integer, Move>> castleMoves = moves
                .stream()
                .filter(onlyCastlingMoves).toList();
        assertEquals(2, castleMoves.size());

        Bitboard kingSideCastle = board.copy();
        kingSideCastle.makeMove(castleMoves.get(0).left());
        System.out.println(printBoard.apply(kingSideCastle));
        assertFalse(kingSideCastle.canCastle(black));
        assertTrue(kingSideCastle.canCastle(white));
        assertEquals(bking, kingSideCastle.getPiece(g8.index()));
        assertEquals(brook, kingSideCastle.getPiece(f8.index()));
        assertEquals(brook, kingSideCastle.getPiece(a8.index()));
        assertEquals(empty, kingSideCastle.getPiece(e8.index()));

        Bitboard queenSideCastle = board.copy();
        queenSideCastle.makeMove(castleMoves.get(1).left());
        System.out.println(boardToStr.apply(queenSideCastle, false));
        assertFalse(queenSideCastle.canCastle(black));
        assertTrue(queenSideCastle.canCastle(white));
        assertEquals(bking, queenSideCastle.getPiece(c8.index()));
        assertEquals(brook, queenSideCastle.getPiece(d8.index()));
        assertEquals(brook, queenSideCastle.getPiece(h8.index()));
        assertEquals(empty, queenSideCastle.getPiece(a8.index()));
    }

    @Test
    void castlingNotAllowed() {
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

        // when all officers are in place, no castling moves should be generated
        Generator g = new Generator(board);
        List<Tuple2<Integer, Move>> whiteMoves = g.generateMoves(white).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .filter(onlyCastlingMoves)
                .toList();
        assertEquals(0, whiteMoves.size());

        List<Tuple2<Integer, Move>> blackMoves = g.generateMoves(black).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .filter(onlyCastlingMoves)
                .toList();
        assertEquals(0, blackMoves.size());

        // remove the pieces on the queen side and verify castle queen side is now possible
        board.removePiece(knight, white, b1.index());
        board.removePiece(bishop, white, c1.index());
        board.removePiece(queen, white, d1.index());

        List<Tuple2<Integer, Move>> queensideCastleMove = g.generateMoves(white).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .filter(onlyCastlingMoves)
                .toList();
        assertEquals(1, queensideCastleMove.size());
        assertEquals(MoveType.CASTLE_QUEEN_SIDE.ordinal(), Move.createMove(queensideCastleMove.get(0).left(), board).moveType());

        board.removePiece(knight, black, b8.index());
        board.removePiece(bishop, black, c8.index());
        board.removePiece(queen, black, d8.index());
        System.out.println(boardToStr.apply(board, false));

        List<Tuple2<Integer, Move>> queensideCastleMoveBlack = g.generateMoves(black).stream()
                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                .filter(onlyCastlingMoves)
                .toList();
        assertEquals(1, queensideCastleMoveBlack.size());
        assertEquals(MoveType.CASTLE_QUEEN_SIDE.ordinal(), Move.createMove(queensideCastleMoveBlack.get(0).left(), board).moveType());
    }
}
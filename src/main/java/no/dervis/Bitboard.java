package no.dervis;

import java.util.LinkedList;

public class Bitboard implements Board, Chess {

    private final long[] whitePieces;
    private final long[] blackPieces;
    private final LinkedList<Integer> history;

    public Bitboard() {
        whitePieces = new long[6];
        blackPieces = new long[6];
        history = new LinkedList<>();
        initialiseBoard();
    }

    private Bitboard(long[] wp, long[] bp, LinkedList<Integer> h) {
        whitePieces = wp;
        blackPieces = bp;
        history = h;
    }

    private void initialiseBoard() {
        // Initialize pawns
        whitePieces[0] = 0x000000000000FF00L;
        blackPieces[0] = 0x00FF000000000000L;

        // Initialize knights
        whitePieces[1] = 0x0000000000000042L;
        blackPieces[1] = 0x4200000000000000L;

        // Initialize bishops
        whitePieces[2] = 0x0000000000000024L;
        blackPieces[2] = 0x2400000000000000L;

        // Initialize rooks
        whitePieces[3] = 0x0000000000000081L;
        blackPieces[3] = 0x8100000000000000L;

        // Initialize queens
        whitePieces[4] = 0x0000000000000008L;
        blackPieces[4] = 0x0800000000000000L;

        // Initialize kings
        whitePieces[5] = 0x0000000000000010L;
        blackPieces[5] = 0x1000000000000000L;
    }

    public Bitboard copy() {
        return new Bitboard(whitePieces.clone(), blackPieces.clone(), new LinkedList<>(history));
    }

    public void setPiece(int pieceType, int color, int rank, int file) {
        int squareIndex = indexFn.apply(rank, file);
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        pieces[pieceType] |= bit;
    }

    public void removePiece(int pieceType, int color, int rank, int file) {
        int squareIndex = indexFn.apply(rank, file);
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        pieces[pieceType] &= ~bit;
    }

    public boolean hasPiece(int pieceType, int color, int rank, int file) {
        int squareIndex = indexFn.apply(rank, file);
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        return (pieces[pieceType] & bit) != 0;
    }



    public long[] whitePieces() {
        return whitePieces;
    }

    public long[] blackPieces() {
        return blackPieces;
    }

    public LinkedList<Integer> history() {
        return history;
    }
}


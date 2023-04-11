package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;

public class KnightMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public KnightMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateKnightMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long knights = color == 0 ? whitePieces[1] : blackPieces[1];
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];

        while (knights != 0) {
            int fromSquare = Long.numberOfTrailingZeros(knights);
            long knightMoves = knightAttacks(fromSquare) & ~friendlyPieces;

            while (knightMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(knightMoves);
                moves.add((fromSquare << 14) | (toSquare << 7));
                knightMoves &= knightMoves - 1;
            }

            knights &= knights - 1;
        }

        return moves;
    }

    private long knightAttacks(int square) {
        long bitboard = 1L << square;

        long attacks = (bitboard << 17) & ~FILE_A;
        attacks |= (bitboard << 10) & ~FILE_A & ~FILE_B;
        attacks |= (bitboard >>> 6) & ~FILE_A & ~FILE_B;
        attacks |= (bitboard >>> 15) & ~FILE_A;

        attacks |= (bitboard << 15) & ~FILE_H;
        attacks |= (bitboard << 6) & ~FILE_G & ~FILE_H;
        attacks |= (bitboard >>> 10) & ~FILE_G & ~FILE_H;
        attacks |= (bitboard >>> 17) & ~FILE_H;

        return attacks;
    }

}

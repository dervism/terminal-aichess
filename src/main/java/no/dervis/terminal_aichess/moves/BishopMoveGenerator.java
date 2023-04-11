package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;

public class BishopMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public BishopMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateBishopMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long bishops = color == 0 ? whitePieces[2] : blackPieces[2];
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];
        long allPieces = friendlyPieces | (color == 0 ? blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]
                : whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]);

        while (bishops != 0) {
            int fromSquare = Long.numberOfTrailingZeros(bishops);
            long bishopMoves = bishopAttacks(fromSquare, allPieces) & ~friendlyPieces;

            while (bishopMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(bishopMoves);
                moves.add((fromSquare << 14) | (toSquare << 7));
                bishopMoves &= bishopMoves - 1;
            }

            bishops &= bishops - 1;
        }

        return moves;
    }

    public static long bishopAttacks(int square, long allPieces) {
        long bitboard = 1L << square;

        long attacks = 0;

        // North-East
        for (int i = square + 9; i < 64 && i % 8 != 0; i += 9) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // North-West
        for (int i = square + 7; i < 64 && i % 8 != 7; i += 7) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // South-East
        for (int i = square - 7; i >= 0 && i % 8 != 0; i -= 7) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // South-West
        for (int i = square - 9; i >= 0 && i % 8 != 7; i -= 9) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        return attacks;
    }

}

package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;

public class RookMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public RookMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateRookMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long rooks = color == 0 ? whitePieces[3] : blackPieces[3];
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];
        long allPieces = friendlyPieces | (color == 0 ? blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]
                : whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]);

        while (rooks != 0) {
            int fromSquare = Long.numberOfTrailingZeros(rooks);
            long rookMoves = rookAttacks(fromSquare, allPieces) & ~friendlyPieces;

            while (rookMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(rookMoves);
                moves.add((fromSquare << 14) | (toSquare << 7));
                rookMoves &= rookMoves - 1;
            }

            rooks &= rooks - 1;
        }

        return moves;
    }

    public static long rookAttacks(int square, long allPieces) {
        long bitboard = 1L << square;

        long attacks = 0;

        // North
        for (int i = square + 8; i < 64; i += 8) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // South
        for (int i = square - 8; i >= 0; i -= 8) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // East
        for (int i = square + 1; i % 8 != 0; i++) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        // West
        for (int i = square - 1; i % 8 != 7 && i >= 0; i--) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }

        return attacks;
    }

}

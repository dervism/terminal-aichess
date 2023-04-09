package no.dervis.moves;

import no.dervis.Bitboard;
import no.dervis.Board;

import java.util.ArrayList;
import java.util.List;

public class PawnMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public PawnMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generatePawnMoves(int color, long enPassantTarget) {
        List<Integer> moves = new ArrayList<>();

        long pawns = color == 0 ? whitePieces[0] : blackPieces[0];
        long emptySquares = ~(whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                | blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]);
        long opponentPieces = color == 0 ? blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]
                : whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5];

        // Generate single pawn moves
        long singleMoves = color == 0 ? (pawns << 8) & emptySquares : (pawns >>> 8) & emptySquares;
        while (singleMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(singleMoves);
            int fromSquare = color == 0 ? toSquare - 8 : toSquare + 8;
            if ((toSquare >= 56 && color == 0) || (toSquare < 8 && color == 1)) { // Promotion
                for (int promotionPiece = 1; promotionPiece <= 4; promotionPiece++) {
                    moves.add((fromSquare << 14) | (toSquare << 7) | (4 << 4) | promotionPiece);
                }
            } else {
                moves.add((fromSquare << 14) | (toSquare << 7));
            }
            singleMoves &= singleMoves - 1;
        }

        // Generate double pawn moves
        long doubleMoves = color == 0 ? ((pawns & 0xFF00L) << 16) & emptySquares & (emptySquares << 8)
                : ((pawns & 0xFF000000000000L) >>> 16) & emptySquares & (emptySquares >>> 8);
        while (doubleMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(doubleMoves);
            int fromSquare = color == 0 ? toSquare - 16 : toSquare + 16;
            moves.add((fromSquare << 14) | (toSquare << 7));
            doubleMoves &= doubleMoves - 1;
        }

        long[] pawnCaptures = color == 0
                ? new long[]{(pawns << 7) & ~FILE_A, (pawns << 9) & ~FILE_H}
                : new long[]{(pawns >>> 9) & ~FILE_A, (pawns >>> 7) & ~FILE_H};
        for (int direction = 0; direction < 2; direction++) {
            long captures = pawnCaptures[direction] & opponentPieces;
            while (captures != 0) {
                int toSquare = Long.numberOfTrailingZeros(captures);
                int fromSquare = color == 0 ? toSquare - (direction == 0 ? 7 : 9) : toSquare + (direction == 0 ? 9 : 7);
                if ((toSquare >= 56 && color == 0) || (toSquare < 8 && color == 1)) { // Promotion
                    for (int promotionPiece = 1; promotionPiece <= 4; promotionPiece++) {
                        moves.add((fromSquare << 14) | (toSquare << 7) | (4 << 4) | promotionPiece);
                    }
                } else {
                    moves.add((fromSquare << 14) | (toSquare << 7));
                }
                captures &= captures - 1;
            }
        }

        // Generate en passant captures
        if (enPassantTarget != 0) {
            long targetBitboard = 1L << enPassantTarget;
            long enPassantCaptures = color == 0
                    ? ((pawns << 7) & ~FILE_A | (pawns << 9) & ~FILE_H) & targetBitboard
                    : ((pawns >>> 9) & ~FILE_A | (pawns >>> 7) & ~FILE_H) & targetBitboard;
            while (enPassantCaptures != 0) {
                int toSquare = Long.numberOfTrailingZeros(enPassantCaptures);
                int fromSquare = color == 0 ? toSquare - (toSquare % 8 == 0 ? 7 : 9) : toSquare + (toSquare % 8 == 0 ? 9 : 7);
                moves.add((fromSquare << 14) | (toSquare << 7) | (1 << 4));
                enPassantCaptures &= enPassantCaptures - 1;
            }
        }

        return moves;
    }

}

package no.dervis.moves;

import no.dervis.Bitboard;
import no.dervis.Board;
import no.dervis.Chess;

import java.util.ArrayList;
import java.util.List;

public class KingMoveGenerator implements Board, Chess {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public KingMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    private static final int[] KING_MOVES = {-9, -8, -7, -1, 1, 7, 8, 9};

    public List<Integer> generateKingMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long kingBoard = color == 0 ? whitePieces[king] : blackPieces[king];
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];

        int fromSquare = Long.numberOfTrailingZeros(kingBoard);

        for (int moveDelta : KING_MOVES) {
            int toSquare = fromSquare + moveDelta;

            // Check if the move is inside the board and not a wrap-around move
            if (toSquare >= 0 && toSquare < 64 && Math.abs((fromSquare % 8) - (toSquare % 8)) <= 2) {
                if ((friendlyPieces & (1L << toSquare)) == 0) {
                    moves.add((fromSquare << 14) | (toSquare << 7));
                }
            }
        }

        return moves;
    }


}

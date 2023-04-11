package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;

import java.util.ArrayList;
import java.util.List;

public class KingMoveGenerator implements Board, Chess {

    private final long[] whitePieces;
    private final long[] blackPieces;
    private int castlingRights;

    public KingMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
        this.castlingRights = board.castlingRights();
    }

    private static final int[] KING_MOVES = {-9, -8, -7, -1, 1, 7, 8, 9};

    public List<Integer> generateKingMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long kingBoard = color == 0 ? whitePieces[king] : blackPieces[king];
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];
        long allPieces = friendlyPieces | (color == 0 ? blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]
                : whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]);


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

        // Castling moves
        if (color == 0) {
            if ((castlingRights & 0b0001) != 0 && (allPieces & 0b0110_0000_0000_0000L) == 0) {
                moves.add((fromSquare << 14) | (fromSquare + 2 << 7) | (MoveType.CASTLE_KING_SIDE.ordinal() << 4));
            }
            if ((castlingRights & 0b0010) != 0 && (allPieces & 0b0000_1110_0000_0000L) == 0) {
                moves.add((fromSquare << 14) | (fromSquare - 2 << 7) | (MoveType.CASTLE_QUEEN_SIDE.ordinal() << 4));
            }
        } else {
            if ((castlingRights & 0b0100) != 0 && (allPieces & 0b0110_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L) == 0) {
                moves.add((fromSquare << 14) | (fromSquare + 2 << 7) | (MoveType.CASTLE_KING_SIDE.ordinal() << 4));
            }
            if ((castlingRights & 0b1000) != 0 && (allPieces & 0b0000_1110_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L) == 0) {
                moves.add((fromSquare << 14) | (fromSquare - 2 << 7) | (MoveType.CASTLE_QUEEN_SIDE.ordinal() << 4));
            }
        }

        return moves;
    }


}

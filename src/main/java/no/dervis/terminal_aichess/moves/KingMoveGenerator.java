package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;

import java.util.ArrayList;
import java.util.List;

public class KingMoveGenerator implements Board, Chess {

    private final long[] whitePieces;
    private final long[] blackPieces;
    private final int castlingRights;

    public KingMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
        this.castlingRights = board.castlingRights();
    }

    private static final int[] KING_MOVES = {-9, -8, -7, -1, 1, 7, 8, 9};

    public List<Integer> generateKingMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long kingBoard = color == 0 ? whitePieces[king] : blackPieces[king];
        long friendlyPieces = 0, enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            friendlyPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        long allPieces = friendlyPieces | enemyPieces;

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
            if ((castlingRights & 0b0001) != 0 && (allPieces & 0b110_0000L) == 0 && fromSquare == e1.index() && (whitePieces[rook] & h1.index()) != 0) {
                moves.add((fromSquare << 14) | (fromSquare + 2 << 7) | (MoveType.CASTLE_KING_SIDE.ordinal() << 4));
            }
            if ((castlingRights & 0b0010) != 0 && (allPieces & 0b1110L) == 0 && fromSquare == e1.index() && (whitePieces[rook] & a1.index()) != 0) {
                moves.add((fromSquare << 14) | (fromSquare - 2 << 7) | (MoveType.CASTLE_QUEEN_SIDE.ordinal() << 4));
            }
        } else {
            if ((castlingRights & 0b0100) != 0 && (allPieces & 0x6000000000000000L) == 0 && fromSquare == e8.index() && (blackPieces[rook] & h8.index()) != 0) {
                moves.add((fromSquare << 14) | (fromSquare + 2 << 7) | (MoveType.CASTLE_KING_SIDE.ordinal() << 4));
            }
            if ((castlingRights & 0b1000) != 0 && (allPieces & 0xe00000000000000L) == 0 && fromSquare == e8.index() && (blackPieces[rook] & a8.index()) != 0) {
                moves.add((fromSquare << 14) | (fromSquare - 2 << 7) | (MoveType.CASTLE_QUEEN_SIDE.ordinal() << 4));
            }
        }

        return moves;
    }


}

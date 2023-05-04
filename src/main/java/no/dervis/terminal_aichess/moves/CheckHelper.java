package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;
import no.dervis.terminal_aichess.Chess;

public class CheckHelper implements Board, Chess {

    private final Bitboard board;
    private final long[] whitePieces;
    private final long[] blackPieces;

    public CheckHelper(Bitboard board) {
        this.board = board;
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public boolean isKingInCheck(int color) {
        int opponentColor = 1 - color;

        long kingBitboard = color == 0 ? whitePieces[king] : blackPieces[king];
        int kingSquare = Long.numberOfTrailingZeros(kingBitboard);

        return false;
    }

    public boolean isSquareAttackedByPawn(int square, int attackingColor) {
        long attackedSquareBitboard = 1L << square;
        long pawnAttacks = 0L;

        if (attackingColor == white) { // white pawns
            if ((square % 8) != 0) {
                pawnAttacks |= attackedSquareBitboard >>> 9;
            }
            if ((square % 8) != 7) {
                pawnAttacks |= attackedSquareBitboard >>> 7;
            }
            return (pawnAttacks & whitePieces[pawn]) != 0;
        } else { // black pawns
            if ((square % 8) != 0) {
                pawnAttacks |= attackedSquareBitboard << 7;
            }
            if ((square % 8) != 7) {
                pawnAttacks |= attackedSquareBitboard << 9;
            }
            return (pawnAttacks & blackPieces[pawn]) != 0;
        }
    }

}

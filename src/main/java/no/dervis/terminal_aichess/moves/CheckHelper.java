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
        long attackBitboard = 0L;

        if (attackingColor == 0) { // white pawns
            if ((square - 7) >= 0 && (square % 8) != 0) {
                attackBitboard |= 1L << (square - 7);
            }
            if ((square - 9) >= 0 && (square % 8) != 7) {
                attackBitboard |= 1L << (square - 9);
            }
            return (attackBitboard & whitePieces[pawn]) != 0;
        } else { // black pawns
            if ((square + 7) < 64 && (square % 8) != 7) {
                attackBitboard |= 1L << (square + 7);
            }
            if ((square + 9) < 64 && (square % 8) != 0) {
                attackBitboard |= 1L << (square + 9);
            }
            return (attackBitboard & blackPieces[pawn]) != 0;
        }
    }

}

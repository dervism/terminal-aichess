package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Chess;

public class CheckHelper implements Chess {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public CheckHelper(long[] whitePieces, long[] blackPieces) {
        this.whitePieces = whitePieces;
        this.blackPieces = blackPieces;
    }

    public boolean isKingInCheck(int color) {
        int opponentColor = 1 - color;

        long kingBitboard = color == 0 ? whitePieces[king] : blackPieces[king];
        int kingSquare = Long.numberOfTrailingZeros(kingBitboard);

        return isSquareAttacked(kingSquare, opponentColor);
    }

    private boolean isSquareAttacked(int square, int color) {
        if (isAttackedByPawns(square, color)) return true;
        if (isAttackedByKnights(square, color)) return true;
        if (isAttackedByBishops(square, color)) return true;
        if (isAttackedByRooks(square, color)) return true;

        return isAttackedByQueens(square, color);
        // No need to check for king attacks since it's an invalid board state
    }

    private boolean isAttackedByPawns(int square, int color) {
        return false;
    }

    private boolean isAttackedByKnights(int square, int color) {
        return false;
    }

    private boolean isAttackedByBishops(int square, int color) {
        return false;
    }

    private boolean isAttackedByRooks(int square, int color) {
        return false;
    }

    private boolean isAttackedByQueens(int square, int color) {
        return isAttackedByBishops(square, color) || isAttackedByRooks(square, color);
    }
}

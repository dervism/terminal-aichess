package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.board.Bitboard;
import no.dervis.terminal_aichess.board.Board;
import no.dervis.terminal_aichess.board.Chess;
import no.dervis.terminal_aichess.moves.attacks.BishopAttacks;
import no.dervis.terminal_aichess.moves.attacks.QueenAttacks;
import no.dervis.terminal_aichess.moves.attacks.RookAttacks;

import static no.dervis.terminal_aichess.moves.BishopMoveGenerator.bishopAttacks;
import static no.dervis.terminal_aichess.moves.RookMoveGenerator.rookAttacks;

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

        return isSquareAttackedByPawn(kingSquare, opponentColor) ||
                isSquareAttackedByKnight(kingSquare, opponentColor) ||
                isSquareAttackedBySlidingPiece(kingSquare, opponentColor);
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

    public boolean isSquareAttackedByKnight(int square, int attackingColor) {
        long attackedSquareBitboard = 1L << square;
        long[] attackingKnights = attackingColor == 0 ? whitePieces : blackPieces;
        long knightAttacks = 0L;

        // Compute knight attacks for the given square
        knightAttacks |= (attackedSquareBitboard & ~FILE_A) << 15;
        knightAttacks |= (attackedSquareBitboard & ~FILE_A & ~FILE_B) << 6;
        knightAttacks |= (attackedSquareBitboard & ~FILE_H) << 17;
        knightAttacks |= (attackedSquareBitboard & ~FILE_H & ~FILE_G) << 10;

        knightAttacks |= (attackedSquareBitboard & ~FILE_A) >>> 17;
        knightAttacks |= (attackedSquareBitboard & ~FILE_A & ~FILE_B) >>> 10;
        knightAttacks |= (attackedSquareBitboard & ~FILE_H) >>> 15;
        knightAttacks |= (attackedSquareBitboard & ~FILE_H & ~FILE_G) >>> 6;

        // Check if any of the attacking knights can attack the given square
        return (knightAttacks & attackingKnights[knight]) != 0;
    }

    public boolean isSquareAttackedBySlidingPiece(int square, int attackingColor) {
        long attackingPieces = attackingColor == 0
                ? whitePieces[rook] | whitePieces[bishop] | whitePieces[queen]
                : blackPieces[rook] | blackPieces[bishop] | blackPieces[queen];

        long opponentPieces = board.allPieces(1 - attackingColor);
        //long slidingAttacks = bishopAttacks(square, allPieces) | rookAttacks(square, allPieces);

        long rookAttackBitboard = RookAttacks.getRookAttacks(square);
        long bishopAttackBitboard = BishopAttacks.getBishopAttacks(square);
        long queenAttackBitboard = QueenAttacks.getQueenAttacks(square);

        long slidingAttacks = queenAttackBitboard;

        slidingAttacks &= ~opponentPieces;

        return (slidingAttacks & attackingPieces) != 0;
    }

    public boolean isSquareAttackedByBishop(int square, int attackingColor) {
        long attackingBishops = attackingColor == 0 ? whitePieces[bishop] : blackPieces[bishop];
        long bishopAttacks = bishopAttacks(square, board.allPieces(attackingColor));

        // check if any of the attacking bishops can attack the given square
        return (bishopAttacks & attackingBishops) != 0;
    }

    public boolean isSquareAttackedByRook(int square, int attackingColor) {
        long attackingRooks = attackingColor == 0 ? whitePieces[rook] : blackPieces[rook];
        long rookAttacks = rookAttacks(square, board.allPieces(attackingColor));

        // check if any of the attacking bishops can attack the given square
        return (rookAttacks & attackingRooks) != 0;
    }

}

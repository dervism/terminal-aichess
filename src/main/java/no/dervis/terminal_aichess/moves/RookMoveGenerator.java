package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static no.dervis.terminal_aichess.Chess.rook;

public class RookMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    private static final int NORTH_OFFSET = 8;
    private static final int SOUTH_OFFSET = -8;
    private static final int EAST_OFFSET = 1;
    private static final int WEST_OFFSET = -1;
    private static final int BOARD_SIZE = 64;
    private static final int ROW_SIZE = 8;

    public RookMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateRookMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long rooksBitboard = color == 0 ? whitePieces[rook] : blackPieces[rook];
        long friendlyPieces = 0, enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            friendlyPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        long allPiecesBitboard = friendlyPieces | enemyPieces;

        while (rooksBitboard != 0) {
            int fromSquare = Long.numberOfTrailingZeros(rooksBitboard);
            long rookMovesBitboard = rookAttacks(fromSquare, allPiecesBitboard) & ~friendlyPieces;

            while (rookMovesBitboard != 0) {
                int toSquare = Long.numberOfTrailingZeros(rookMovesBitboard);
                moves.add((fromSquare << 14) | (toSquare << 7));
                rookMovesBitboard &= rookMovesBitboard - 1;
            }
            rooksBitboard &= rooksBitboard - 1;
        }

        return moves;
    }

    public static long rookAttacks(int square, long allPieces) {
        return calculateAttacksInDirection(square, allPieces, NORTH_OFFSET, i -> true)
                | calculateAttacksInDirection(square, allPieces, SOUTH_OFFSET, i -> true)
                | calculateAttacksInDirection(square, allPieces, EAST_OFFSET, i -> i % ROW_SIZE != 0)
                | calculateAttacksInDirection(square, allPieces, WEST_OFFSET, i -> i % ROW_SIZE != 7);
    }


    private static long calculateAttacksInDirection(int square, long allPieces, int directionOffset, Predicate<Integer> isEdge) {
        long attacks = 0;
        for (int i = square + directionOffset; i < BOARD_SIZE && i >= 0 && isEdge.test(i); i += directionOffset) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }
        return attacks;
    }






    // this is the old attacks calculation method before refactoring it above
    private static long rookAttacks__(int square, long allPieces) {
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

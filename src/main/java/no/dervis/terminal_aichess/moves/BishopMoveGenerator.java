package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;

public class BishopMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    private static final int BOARD_SIZE = 64;
    private static final int NORTH_EAST = 9;
    private static final int NORTH_WEST = 7;
    private static final int SOUTH_EAST = -7;
    private static final int SOUTH_WEST = -9;
    private static final int RIGHT_EDGE = 0;
    private static final int LEFT_EDGE = 7;

    public BishopMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateBishopMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long bishops = color == 0 ? whitePieces[2] : blackPieces[2];
        long friendlyPieces = 0, enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            friendlyPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        long allPieces = friendlyPieces | enemyPieces;

        while (bishops != 0) {
            int fromSquare = Long.numberOfTrailingZeros(bishops);
            long bishopMoves = bishopAttacks(fromSquare, allPieces) & ~friendlyPieces;

            while (bishopMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(bishopMoves);
                moves.add((fromSquare << 14) | (toSquare << 7));
                bishopMoves &= bishopMoves - 1;
            }

            bishops &= bishops - 1;
        }

        return moves;
    }

    public static long bishopAttacks(int square, long allPieces) {
        long attacks = 0;

        attacks |= calculateAttacksInDirection(square, allPieces, NORTH_EAST, RIGHT_EDGE);
        attacks |= calculateAttacksInDirection(square, allPieces, NORTH_WEST, LEFT_EDGE);
        attacks |= calculateAttacksInDirection(square, allPieces, SOUTH_EAST, RIGHT_EDGE);
        attacks |= calculateAttacksInDirection(square, allPieces, SOUTH_WEST, LEFT_EDGE);

        return attacks;
    }

    private static long calculateAttacksInDirection(int square, long allPieces, int direction, int edge) {
        long attacks = 0;
        for (int i = square + direction;
             isWithinBoardLimit(i) && i % 8 != edge;
             i += direction) {
            attacks |= 1L << i;
            if ((allPieces & (1L << i)) != 0) {
                break;
            }
        }
        return attacks;
    }

    private static boolean isWithinBoardLimit(int square){
        return square >= 0 && square < BOARD_SIZE;
    }

}

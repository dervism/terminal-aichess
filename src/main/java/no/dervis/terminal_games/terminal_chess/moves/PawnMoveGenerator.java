package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;

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
        long emptySquares = ~0, enemyPieces = 0;
        for (int i = 0; i < 6; i++) {
            emptySquares &= ~(whitePieces[i] | blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        // Generate single pawn moves
        long singleMoves = color == 0 ? (pawns << 8) & emptySquares : (pawns >>> 8) & emptySquares;
        while (singleMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(singleMoves);
            int fromSquare = color == 0 ? toSquare - 8 : toSquare + 8;
            singleMoves = isPromotionMove(color, moves, singleMoves, toSquare, fromSquare);
        }

        generateDoubleMoves(color, pawns, emptySquares, moves);

        generateCaptures(color, pawns, enemyPieces, moves);

        generateEnPassantMoves(color, enPassantTarget, pawns, moves);

        return moves;
    }

    private static void generateDoubleMoves(int color, long pawns, long emptySquares, List<Integer> moves) {
        long doubleMoves = color == 0
                ? ((pawns & 0xFF00L) << 16) & emptySquares & (emptySquares << 8)
                : ((pawns & 0xFF000000000000L) >>> 16) & emptySquares & (emptySquares >>> 8);

        while (doubleMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(doubleMoves);
            int fromSquare = color == 0 ? toSquare - 16 : toSquare + 16;
            moves.add((fromSquare << 14) | (toSquare << 7));
            doubleMoves &= doubleMoves - 1;
        }
    }

    private static void generateCaptures(int color, long pawns, long enemyPieces, List<Integer> moves) {
        long[] pawnCaptures = color == 0
                ? new long[]{(pawns << 7) & ~FILE_A, (pawns << 9) & ~FILE_H}
                : new long[]{(pawns >>> 9) & ~FILE_A, (pawns >>> 7) & ~FILE_H};
        for (int direction = 0; direction < 2; direction++) {
            long captures = pawnCaptures[direction] & enemyPieces;
            while (captures != 0) {
                int toSquare = Long.numberOfTrailingZeros(captures);
                int fromSquare = color == 0 ? toSquare - (direction == 0 ? 7 : 9) : toSquare + (direction == 0 ? 9 : 7);
                captures = isPromotionMove(color, moves, captures, toSquare, fromSquare);
            }
        }
    }

    private static void generateEnPassantMoves(int color, long enPassantTarget, long pawns, List<Integer> moves) {
        if (enPassantTarget != 0) {
            long enPassantCaptures = color == 0
                    ? ((pawns << 7) & ~FILE_A | (pawns << 9) & ~FILE_H) & enPassantTarget
                    : ((pawns >>> 9) & ~FILE_A | (pawns >>> 7) & ~FILE_H) & enPassantTarget;
            while (enPassantCaptures != 0) {
                int toSquare = Long.numberOfTrailingZeros(enPassantCaptures);
                int fromSquare = color == 0 ? toSquare - (toSquare % 8 == 0 ? 7 : 9) : toSquare + (toSquare % 8 == 0 ? 9 : 7);
                moves.add((fromSquare << 14) | (toSquare << 7) | (1 << 4));
                enPassantCaptures &= enPassantCaptures - 1;
            }
        }
    }

    private static long isPromotionMove(int color, List<Integer> moves, long captures, int toSquare, int fromSquare) {
        if ((toSquare >= 56 && color == 0) || (toSquare < 8 && color == 1)) { // Promotion
            for (int promotionPiece = 1; promotionPiece <= 4; promotionPiece++) {
                moves.add((fromSquare << 14) | (toSquare << 7) | (4 << 4) | promotionPiece);
            }
        } else {
            moves.add((fromSquare << 14) | (toSquare << 7));
        }

        captures &= captures - 1;
        return captures;
    }

}

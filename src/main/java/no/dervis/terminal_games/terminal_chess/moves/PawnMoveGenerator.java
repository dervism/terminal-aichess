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
        long ownPieces = 0, enemyPieces = 0;
        for (int i = 0; i < 6; i++) {
            ownPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }
        long emptySquares = ~(ownPieces | enemyPieces);

        // Generate single pawn moves
        long allPieces = ownPieces | enemyPieces;
        System.out.println("[DEBUG_LOG] Pawns: " + Long.toBinaryString(pawns));
        System.out.println("[DEBUG_LOG] All pieces: " + Long.toBinaryString(allPieces));

        // Generate single pawn moves
        System.out.println("[DEBUG_LOG] Pawns before shift: 0x" + Long.toHexString(pawns));

        // Process each pawn for potential moves
        long remainingPawns = pawns;
        long singleMoves = 0L;
        while (remainingPawns != 0) {
            int fromSquare = Long.numberOfTrailingZeros(remainingPawns);
            int toSquare = color == 0 ? fromSquare + 8 : fromSquare - 8;

            // Check if destination square is within bounds and empty
            if (toSquare >= 0 && toSquare < 64) {
                long toSquareMask = 1L << toSquare;
                // Check if destination is empty and not blocked by own pieces
                if ((toSquareMask & allPieces) == 0 && (toSquareMask & ownPieces) == 0) {
                    singleMoves |= toSquareMask;
                    isPromotionMove(color, moves, toSquareMask, toSquare, fromSquare);
                }
            }
            remainingPawns &= remainingPawns - 1;
        }
        System.out.println("[DEBUG_LOG] Single moves: " + Long.toBinaryString(singleMoves));

        // Generate double moves from starting position
        // White pawns start on rank 2 (0xFF00), black pawns on rank 7 (0xFF00000000000000)
        long startRank = color == 0 ? 0xFF00L : 0xFF00000000000000L;
        System.out.println("[DEBUG_LOG] Start rank value: 0x" + Long.toHexString(startRank));
        System.out.println("[DEBUG_LOG] Pawns at start: 0x" + Long.toHexString(pawns & startRank));
        System.out.println("[DEBUG_LOG] Start rank: " + Long.toBinaryString(startRank));
        System.out.println("[DEBUG_LOG] Pawns on start rank: " + Long.toBinaryString(pawns & startRank));

        long startPawns = pawns & startRank;
        while (startPawns != 0) {
            int fromSquare = Long.numberOfTrailingZeros(startPawns);
            int midSquare = color == 0 ? fromSquare + 8 : fromSquare - 8;
            int toSquare = color == 0 ? fromSquare + 16 : fromSquare - 16;

            System.out.println("[DEBUG_LOG] Double move check: from=" + fromSquare + 
                             " mid=" + midSquare + " to=" + toSquare);

            // Check if both squares in the path are empty and within bounds
            if (toSquare >= 0 && toSquare < 64 && midSquare >= 0 && midSquare < 64) {
                long midSquareMask = 1L << midSquare;
                long toSquareMask = 1L << toSquare;
                boolean midEmpty = (midSquareMask & allPieces) == 0;
                boolean toEmpty = (toSquareMask & allPieces) == 0;
                System.out.println("[DEBUG_LOG] Mid square empty: " + midEmpty + 
                                 " To square empty: " + toEmpty);

                if (midEmpty && toEmpty) {
                    moves.add((fromSquare << 14) | (toSquare << 7));
                }
            }
            startPawns &= startPawns - 1;
        }

        generateCaptures(color, pawns, enemyPieces, ownPieces, moves);

        generateEnPassantMoves(color, enPassantTarget, pawns, ownPieces, moves);

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

    private static void generateCaptures(int color, long pawns, long enemyPieces, long ownPieces, List<Integer> moves) {
        // Calculate potential capture squares, excluding own pieces
        long leftCaptures = color == 0
            ? (pawns << 7) & ~FILE_A & enemyPieces & ~ownPieces  // White captures up-left
            : (pawns >>> 9) & ~FILE_A & enemyPieces & ~ownPieces; // Black captures down-left
        long rightCaptures = color == 0
            ? (pawns << 9) & ~FILE_H & enemyPieces & ~ownPieces  // White captures up-right
            : (pawns >>> 7) & ~FILE_H & enemyPieces & ~ownPieces; // Black captures down-right

        // Process left captures
        while (leftCaptures != 0) {
            int toSquare = Long.numberOfTrailingZeros(leftCaptures);
            int fromSquare = color == 0 ? toSquare - 7 : toSquare + 9;
            isPromotionMove(color, moves, 1L << toSquare, toSquare, fromSquare);
            leftCaptures &= leftCaptures - 1;
        }

        // Process right captures
        while (rightCaptures != 0) {
            int toSquare = Long.numberOfTrailingZeros(rightCaptures);
            int fromSquare = color == 0 ? toSquare - 9 : toSquare + 7;
            isPromotionMove(color, moves, 1L << toSquare, toSquare, fromSquare);
            rightCaptures &= rightCaptures - 1;
        }
    }

    private static void generateEnPassantMoves(int color, long enPassantTarget, long pawns, long ownPieces, List<Integer> moves) {
        if (enPassantTarget != 0) {
            // Process each pawn for potential en passant captures
            long remainingPawns = pawns;
            while (remainingPawns != 0) {
                int fromSquare = Long.numberOfTrailingZeros(remainingPawns);
                int file = fromSquare % 8;

                // Check both diagonal captures
                for (int direction = 0; direction < 2; direction++) {
                    // Skip if pawn is on edge and would wrap around
                    if ((direction == 0 && file == 0) || (direction == 1 && file == 7)) {
                        continue;
                    }

                    // Calculate en passant capture square
                    int toSquare = color == 0
                        ? fromSquare + (direction == 0 ? 7 : 9)  // White captures up-left or up-right
                        : fromSquare - (direction == 0 ? 9 : 7); // Black captures down-left or down-right

                    // Verify capture is valid and targets en passant square
                    if (toSquare >= 0 && toSquare < 64) {
                        long toSquareMask = 1L << toSquare;
                        if ((toSquareMask & enPassantTarget) != 0) {
                            moves.add((fromSquare << 14) | (toSquare << 7) | (1 << 4));
                        }
                    }
                }
                remainingPawns &= remainingPawns - 1;
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

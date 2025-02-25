package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.attacks.BishopAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.RookAttacks;

import java.util.stream.IntStream;

import static no.dervis.terminal_games.terminal_chess.moves.BishopMoveGenerator.bishopAttacks;
import static no.dervis.terminal_games.terminal_chess.moves.RookMoveGenerator.rookAttacks;

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

        long rookAttackBitboard = getSlidingAttacks(square, RookAttacks.getRookAttacks(square));
        long bishopAttackBitboard = getSlidingAttacks(square, BishopAttacks.getBishopAttacks(square));
        long slidingAttacks = rookAttackBitboard | bishopAttackBitboard;
        return (slidingAttacks & attackingPieces) != 0;
    }

    private long getSlidingAttacks(int square, long attackMask) {
        long attacks = 0L;
        long blockers = board.allPieces() & attackMask;

        // Calculate the sliding attacks, taking into account the blockers
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.NORTH);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.SOUTH);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.EAST);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.WEST);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.NORTH_EAST);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.NORTH_WEST);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.SOUTH_EAST);
        attacks |= getDirectionalSlidingAttacks(square, attackMask, blockers, Direction.SOUTH_WEST);

        return attacks;
    }

    private long getDirectionalSlidingAttacks(int square, long attackMask, long blockers, Direction direction) {
        return IntStream.iterate(
                getNextSquareInDirection(square, direction),
                        s -> s != -1 && ((1L << s) & attackMask) != 0,
                        s -> getNextSquareInDirection(s, direction))
                .mapToLong(s -> 1L << s)
                .takeWhile(bitboard -> (bitboard & blockers) == 0)
                .reduce(0L, (a, b) -> a | b) |
                IntStream.iterate(
                        getNextSquareInDirection(square, direction),
                                s -> s != -1 && ((1L << s) & attackMask) != 0,
                                s -> getNextSquareInDirection(s, direction))
                        .mapToLong(s -> 1L << s)
                        .dropWhile(bitboard -> (bitboard & blockers) == 0)
                        .findFirst()
                        .orElse(0L);
    }

    private int getNextSquareInDirection(int square, Direction direction) {
        return switch (direction) {
            case NORTH -> (square >= 56) ? -1 : square + 8;
            case SOUTH -> (square < 8) ? -1 : square - 8;
            case EAST -> ((square % 8) == 7) ? -1 : square + 1;
            case WEST -> ((square % 8) == 0) ? -1 : square - 1;
            case NORTH_EAST -> ((square >= 56) || ((square % 8) == 7)) ? -1 : square + 9;
            case NORTH_WEST -> ((square >= 56) || ((square % 8) == 0)) ? -1 : square + 7;
            case SOUTH_EAST -> ((square < 8) || ((square % 8) == 7)) ? -1 : square - 7;
            case SOUTH_WEST -> ((square < 8) || ((square % 8) == 0)) ? -1 : square - 9;
        };
    }

    enum Direction {
        NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST
    }

    public boolean isSquareAttackedByBishop(int square, int attackingColor) {
        long attackingBishops = attackingColor == 0 ? whitePieces[bishop] : blackPieces[bishop];
        long bishopAttacks = bishopAttacks(square, board.allPieces());

        // check if any of the attacking bishops can attack the given square
        return (bishopAttacks & attackingBishops) != 0;
    }

    public boolean isSquareAttackedByRook(int square, int attackingColor) {
        long attackingRooks = attackingColor == 0 ? whitePieces[rook] : blackPieces[rook];
        long rookAttacks = rookAttacks(square, board.allPieces());

        // check if any of the attacking rooks can attack the given square
        return (rookAttacks & attackingRooks) != 0;
    }

    private boolean isOnSameRankOrFile(int square1, int square2) {
        return (square1 / 8 == square2 / 8) || (square1 % 8 == square2 % 8);
    }

    private boolean isOnSameDiagonal(int square1, int square2) {
        int rank1 = square1 / 8;
        int file1 = square1 % 8;
        int rank2 = square2 / 8;
        int file2 = square2 % 8;

        // Check if squares are on the same diagonal
        int rankDiff = rank2 - rank1;
        int fileDiff = file2 - file1;

        // Squares are on the same diagonal if the absolute differences in rank and file are equal
        // This works for both positive (/) and negative (\) diagonals
        return Math.abs(rankDiff) == Math.abs(fileDiff);
    }

    private boolean isOnSameDiagonalAndDirection(int square1, int square2, int square3) {
        int rank1 = square1 / 8;
        int file1 = square1 % 8;
        int rank2 = square2 / 8;
        int file2 = square2 % 8;
        int rank3 = square3 / 8;
        int file3 = square3 % 8;

        System.out.println("[DEBUG_LOG] Checking diagonal alignment:");
        System.out.println("[DEBUG_LOG] Square1 (rank=" + rank1 + ", file=" + file1 + ")");
        System.out.println("[DEBUG_LOG] Square2 (rank=" + rank2 + ", file=" + file2 + ")");
        System.out.println("[DEBUG_LOG] Square3 (rank=" + rank3 + ", file=" + file3 + ")");

        // Check if all squares are on the same diagonal
        if (!isOnSameDiagonal(square1, square3)) {
            System.out.println("[DEBUG_LOG] Squares 1 and 3 are not on the same diagonal");
            return false;
        }

        // Calculate diagonal direction
        int rankDir = Integer.compare(rank3 - rank1, 0);
        int fileDir = Integer.compare(file3 - file1, 0);

        // Calculate relative position of square2
        int rankDiff2 = rank2 - rank1;
        int fileDiff2 = file2 - file1;

        // Check if square2 is between square1 and square3 on the diagonal
        boolean isOnDiagonal = Math.abs(rankDiff2) == Math.abs(fileDiff2);
        boolean isInDirection = Integer.compare(rankDiff2, 0) == rankDir && 
                              Integer.compare(fileDiff2, 0) == fileDir;
        boolean isBetween = Math.abs(rankDiff2) <= Math.abs(rank3 - rank1) &&
                          Math.abs(fileDiff2) <= Math.abs(file3 - file1);

        System.out.println("[DEBUG_LOG] Diagonal check: isOnDiagonal=" + isOnDiagonal + 
                          ", isInDirection=" + isInDirection + ", isBetween=" + isBetween);

        return isOnDiagonal && isInDirection && isBetween;
    }

    private boolean isBetween(int kingSquare, int pieceSquare, int pinningSquare) {
        System.out.println("[DEBUG_LOG] Checking if piece at " + pieceSquare + " is between king at " + kingSquare + " and pinning piece at " + pinningSquare);

        int pieceRank = pieceSquare / 8;
        int pieceFile = pieceSquare % 8;
        int kingRank = kingSquare / 8;
        int kingFile = kingSquare % 8;
        int pinningRank = pinningSquare / 8;
        int pinningFile = pinningSquare % 8;

        System.out.println("[DEBUG_LOG] Piece position: rank=" + pieceRank + ", file=" + pieceFile);
        System.out.println("[DEBUG_LOG] King position: rank=" + kingRank + ", file=" + kingFile);
        System.out.println("[DEBUG_LOG] Pinning piece position: rank=" + pinningRank + ", file=" + pinningFile);

        // For orthogonal movement
        if (kingRank == pieceRank && pieceRank == pinningRank) {
            // Same rank
            int min = Math.min(kingFile, pinningFile);
            int max = Math.max(kingFile, pinningFile);
            boolean result = pieceFile >= min && pieceFile <= max;
            System.out.println("[DEBUG_LOG] Same rank check: min=" + min + ", max=" + max + ", piece file=" + pieceFile + ", result=" + result);
            return result;
        }
        if (kingFile == pieceFile && pieceFile == pinningFile) {
            // Same file
            int min = Math.min(kingRank, pinningRank);
            int max = Math.max(kingRank, pinningRank);
            boolean result = pieceRank >= min && pieceRank <= max;
            System.out.println("[DEBUG_LOG] Same file check: min=" + min + ", max=" + max + ", piece rank=" + pieceRank + ", result=" + result);
            return result;
        }

        // For diagonal movement
        if (isOnSameDiagonal(kingSquare, pinningSquare)) {
            boolean result = isOnSameDiagonalAndDirection(kingSquare, pieceSquare, pinningSquare);
            if (!result) {
                System.out.println("[DEBUG_LOG] Piece is not on the same diagonal or not in the right direction");
                return false;
            }

            // Check if piece is between king and pinning piece
            boolean withinBounds = Math.abs(pieceRank - kingRank) <= Math.abs(pinningRank - kingRank) &&
                                 Math.abs(pieceFile - kingFile) <= Math.abs(pinningFile - kingFile);

            System.out.println("[DEBUG_LOG] Diagonal check: withinBounds=" + withinBounds);
            return withinBounds;
        }

        System.out.println("[DEBUG_LOG] Piece is not aligned with king and potential pinning piece");
        return false;
    }

    private long getBetweenBits(int square1, int square2) {
        long result = 0L;
        int minRank = Math.min(square1 / 8, square2 / 8);
        int maxRank = Math.max(square1 / 8, square2 / 8);
        int minFile = Math.min(square1 % 8, square2 % 8);
        int maxFile = Math.max(square1 % 8, square2 % 8);

        if (square1 / 8 == square2 / 8) {
            // Same rank
            for (int file = minFile + 1; file < maxFile; file++) {
                result |= 1L << (square1 / 8 * 8 + file);
            }
        } else if (square1 % 8 == square2 % 8) {
            // Same file
            for (int rank = minRank + 1; rank < maxRank; rank++) {
                result |= 1L << (rank * 8 + square1 % 8);
            }
        } else if (isOnSameDiagonal(square1, square2)) {
            // Diagonal
            int rankStep = (square2 / 8 - square1 / 8) > 0 ? 1 : -1;
            int fileStep = (square2 % 8 - square1 % 8) > 0 ? 1 : -1;
            int rank = square1 / 8 + rankStep;
            int file = square1 % 8 + fileStep;
            while (rank != square2 / 8 || file != square2 % 8) {
                result |= 1L << (rank * 8 + file);
                rank += rankStep;
                file += fileStep;
            }
        }
        return result;
    }
}

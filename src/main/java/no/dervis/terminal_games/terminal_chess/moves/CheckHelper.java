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

}

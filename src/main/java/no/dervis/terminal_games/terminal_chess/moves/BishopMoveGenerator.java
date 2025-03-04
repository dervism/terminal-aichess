package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BishopMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    private static final int NORTH_EAST = 9;
    private static final int NORTH_WEST = 7;
    private static final int SOUTH_EAST = -7;
    private static final int SOUTH_WEST = -9;

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
        AtomicInteger ai = new AtomicInteger(square + direction);
        long attacks = Stream
                .iterate(ai.get(), i -> isWithinBoardLimit(i) && i % 8 != edge, _ -> ai.addAndGet(direction))
                .mapToLong(i -> 1L << i)
                .takeWhile(i -> (allPieces & i) == 0)
                .reduce(0L, (attacked, squareBit) -> attacked | squareBit);

        if (isWithinBoardLimit(ai.get()) && ai.get() % 8 != edge) {
            attacks |= 1L << ai.get();
        }

        return attacks;
    }

    public static boolean isWithinBoardLimit(int square){
        return square >= 0 && square < 64;
    }

}

package no.dervis.terminal_games.terminal_chess.moves.generator;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess.MoveType;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;

import java.util.ArrayList;
import java.util.List;

public class BishopMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

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
                int type = ((1L << toSquare) & enemyPieces) != 0 ? MoveType.ATTACK.ordinal() : 0;
                moves.add((fromSquare << 14) | (toSquare << 7) | (type << 4));
                bishopMoves &= bishopMoves - 1;
            }

            bishops &= bishops - 1;
        }

        return moves;
    }

    public static long bishopAttacks(int square, long allPieces) {
        return MagicBitboard.bishopAttacks(square, allPieces);
    }

}

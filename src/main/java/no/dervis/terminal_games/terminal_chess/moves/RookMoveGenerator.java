package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.moves.attacks.MagicBitboard;

import java.util.ArrayList;
import java.util.List;

import static no.dervis.terminal_games.terminal_chess.board.Chess.rook;

public class RookMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

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
        return MagicBitboard.rookAttacks(square, allPieces);
    }

}

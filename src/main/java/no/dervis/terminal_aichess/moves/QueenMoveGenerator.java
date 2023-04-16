package no.dervis.terminal_aichess.moves;

import no.dervis.terminal_aichess.Bitboard;
import no.dervis.terminal_aichess.Board;

import java.util.ArrayList;
import java.util.List;

import static no.dervis.terminal_aichess.moves.BishopMoveGenerator.bishopAttacks;
import static no.dervis.terminal_aichess.moves.RookMoveGenerator.rookAttacks;

public class QueenMoveGenerator implements Board {

    private final long[] whitePieces;
    private final long[] blackPieces;

    public QueenMoveGenerator(Bitboard board) {
        this.whitePieces = board.whitePieces();
        this.blackPieces = board.blackPieces();
    }

    public List<Integer> generateQueenMoves(int color) {
        List<Integer> moves = new ArrayList<>();

        long queens = color == 0 ? whitePieces[4] : blackPieces[4];
        long friendlyPieces = 0, enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            friendlyPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        long allPieces = friendlyPieces | enemyPieces;

        while (queens != 0) {
            int fromSquare = Long.numberOfTrailingZeros(queens);
            long queenMoves = (bishopAttacks(fromSquare, allPieces) | rookAttacks(fromSquare, allPieces)) & ~friendlyPieces;

            while (queenMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(queenMoves);
                moves.add((fromSquare << 14) | (toSquare << 7));
                queenMoves &= queenMoves - 1;
            }

            queens &= queens - 1;
        }

        return moves;
    }


}

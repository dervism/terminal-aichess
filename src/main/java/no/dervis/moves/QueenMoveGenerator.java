package no.dervis.moves;

import no.dervis.Bitboard;
import no.dervis.Board;

import java.util.ArrayList;
import java.util.List;

import static no.dervis.moves.BishopMoveGenerator.bishopAttacks;
import static no.dervis.moves.RookMoveGenerator.rookAttacks;

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
        long friendlyPieces = color == 0 ? whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]
                : blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5];
        long allPieces = friendlyPieces | (color == 0 ? blackPieces[0] | blackPieces[1] | blackPieces[2] | blackPieces[3] | blackPieces[4] | blackPieces[5]
                : whitePieces[0] | whitePieces[1] | whitePieces[2] | whitePieces[3] | whitePieces[4] | whitePieces[5]);

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

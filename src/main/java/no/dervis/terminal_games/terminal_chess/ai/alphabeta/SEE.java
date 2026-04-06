package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KingAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KnightAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.PawnAttacks;

/**
 * Static Exchange Evaluation (SEE).
 *
 * <p>Evaluates the material outcome of a sequence of captures on a single
 * square. Used for move ordering (rank captures more accurately than MVV-LVA)
 * and for pruning losing captures in quiescence search.</p>
 *
 * <p>The algorithm simulates the exchange by finding the least valuable
 * attacker for each side, "removing" it from the board (clearing its bit
 * in the occupancy), and revealing any X-ray attackers behind it.</p>
 */
public class SEE implements Chess {

    // Piece values for SEE (centipawns, matching Evaluation)
    private static final int[] SEE_VALUES = {100, 320, 330, 500, 950, 20000};

    /**
     * Returns the SEE score of a capture move in centipawns.
     *
     * @param board the current board position
     * @param move  the encoded capture move
     * @return the estimated material gain (positive = winning exchange)
     */
    public static int see(Bitboard board, int move) {
        int from = move >>> 14;
        int to = (move >>> 7) & 0x3F;
        int moveType = (move >>> 4) & 0x7;

        int attackerPiece = board.getPiece(from);
        if (attackerPiece == -1) return 0;

        int attackerType = attackerPiece % 6;
        int attackerColor = attackerPiece / 6;

        // Determine the initial captured piece value
        int capturedValue;
        if (moveType == MoveType.EN_PASSANT.ordinal()) {
            capturedValue = SEE_VALUES[pawn];
        } else {
            int capturedPiece = board.getPiece(to);
            if (capturedPiece == -1) return 0; // not a capture
            capturedValue = SEE_VALUES[capturedPiece % 6];
        }

        // Build occupancy and piece bitboards
        long occupancy = board.allPieces();
        long[] colorPieces = {board.allWhitePieces(), board.allBlackPieces()};

        // Remove the initial attacker from occupancy
        occupancy ^= (1L << from);
        colorPieces[attackerColor] ^= (1L << from);

        // Get all attackers to the target square (updated as pieces are "removed")
        long attackers = allAttackersTo(board, to, occupancy);

        // Gain array: gain[0] = initial capture value
        int[] gain = new int[32];
        int depth = 0;
        gain[0] = capturedValue;

        int sideToMove = 1 - attackerColor; // opponent moves next
        int lastAttackerValue = SEE_VALUES[attackerType];

        while (true) {
            depth++;
            gain[depth] = lastAttackerValue - gain[depth - 1]; // negamax-style

            // Prune: if even capturing for free doesn't help, stop
            if (Math.max(-gain[depth - 1], gain[depth]) < 0) break;

            // Find the least valuable attacker for sideToMove
            long sideAttackers = attackers & colorPieces[sideToMove];
            if (sideAttackers == 0) break;

            int lva = leastValuableAttacker(board, sideAttackers, sideToMove);
            if (lva == -1) break;

            int lvaSquare = Long.numberOfTrailingZeros(
                    getPieceBB(board, lva, sideToMove) & sideAttackers & occupancy);
            if (lvaSquare >= 64) break;

            lastAttackerValue = SEE_VALUES[lva];

            // Remove this attacker from occupancy (reveals X-ray attackers)
            long lvabit = 1L << lvaSquare;
            occupancy ^= lvabit;
            colorPieces[sideToMove] ^= lvabit;

            // Recalculate attackers with updated occupancy (for X-ray discovery)
            attackers = allAttackersTo(board, to, occupancy);

            sideToMove = 1 - sideToMove;
        }

        // Unwind the gain array using negamax
        while (--depth > 0) {
            gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
        }

        return gain[0];
    }

    /**
     * Returns true if the capture is likely losing (SEE < 0).
     */
    public static boolean isLosingCapture(Bitboard board, int move) {
        return see(board, move) < 0;
    }

    /**
     * Computes a bitboard of all pieces (both colors) that attack a given square,
     * given the current occupancy (pieces may have been "removed" during SEE).
     */
    private static long allAttackersTo(Bitboard board, int square, long occupancy) {
        long attackers = 0L;

        // Pawn attacks (from white's perspective: white pawns attack upward)
        attackers |= PawnAttacks.getAllPawnAttacks(square, black) & board.getPawns(white) & occupancy;
        attackers |= PawnAttacks.getAllPawnAttacks(square, white) & board.getPawns(black) & occupancy;

        // Knight attacks
        attackers |= KnightAttacks.getAllKnightAttacks(square)
                & (board.getKnights(white) | board.getKnights(black)) & occupancy;

        // King attacks
        attackers |= KingAttacks.getAllKingAttacks(square)
                & (board.kingPiece(white) | board.kingPiece(black)) & occupancy;

        // Sliding piece attacks (use occupancy for blocking)
        long rookQueens = (board.getRooks(white) | board.getRooks(black)
                | board.getQueens(white) | board.getQueens(black)) & occupancy;
        attackers |= MagicBitboard.rookAttacks(square, occupancy) & rookQueens;

        long bishopQueens = (board.getBishops(white) | board.getBishops(black)
                | board.getQueens(white) | board.getQueens(black)) & occupancy;
        attackers |= MagicBitboard.bishopAttacks(square, occupancy) & bishopQueens;

        return attackers;
    }

    /**
     * Returns the piece type of the least valuable attacker in the given
     * attacker bitboard for the specified color. Returns -1 if none found.
     */
    private static int leastValuableAttacker(Bitboard board, long attackers, int color) {
        for (int pt = pawn; pt <= king; pt++) {
            if ((getPieceBB(board, pt, color) & attackers) != 0) {
                return pt;
            }
        }
        return -1;
    }

    private static long getPieceBB(Bitboard board, int pieceType, int color) {
        return Evaluation.getPieceBB(board, pieceType, color);
    }
}

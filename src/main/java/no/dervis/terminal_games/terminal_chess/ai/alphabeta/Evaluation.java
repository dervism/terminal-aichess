package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KingAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KnightAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.PawnAttacks;

/**
 * Static board evaluation function for the chess AI.
 *
 * <p>Evaluates a position using:</p>
 * <ul>
 *   <li>Material balance</li>
 *   <li>Piece-square tables (middlegame/endgame with game-phase interpolation)</li>
 *   <li>Bishop pair bonus</li>
 *   <li>Minor piece imbalance (knights prefer closed, bishops prefer open)</li>
 *   <li>Pawn structure (doubled, isolated, passed, backward pawns)</li>
 *   <li>Rook on open/semi-open files</li>
 *   <li>Connected rooks</li>
 *   <li>Rook on 7th rank</li>
 *   <li>Knight outposts</li>
 *   <li>King safety (pawn shield + king danger zone)</li>
 *   <li>Pawn storms (opposite-side castling)</li>
 *   <li>Piece mobility</li>
 *   <li>Space advantage</li>
 *   <li>King-pawn proximity (endgame)</li>
 *   <li>Tempo bonus</li>
 * </ul>
 *
 * <p>All scores are computed from white's perspective and then adjusted
 * for the side to move (positive = good for the side to move).</p>
 */
public class Evaluation implements Chess, Board {

    // ----- Material values (centipawns) -----
    static final int[] MG_PIECE_VALUE = {100, 320, 330, 500, 950, 0};
    static final int[] EG_PIECE_VALUE = {130, 310, 320, 530, 1000, 0};

    // ----- Game phase weights -----
    private static final int PHASE_KNIGHT = 1;
    private static final int PHASE_BISHOP = 1;
    private static final int PHASE_ROOK = 2;
    private static final int PHASE_QUEEN = 4;
    private static final int TOTAL_PHASE = 24;

    // ----- Evaluation bonuses / penalties -----
    private static final int BISHOP_PAIR_BONUS_MG = 45;
    private static final int BISHOP_PAIR_BONUS_EG = 55;
    private static final int DOUBLED_PAWN_PENALTY = -12;
    private static final int ISOLATED_PAWN_PENALTY_MG = -15;
    private static final int ISOLATED_PAWN_PENALTY_EG = -20;
    private static final int ROOK_OPEN_FILE_BONUS = 25;
    private static final int ROOK_SEMI_OPEN_FILE_BONUS = 12;
    private static final int TEMPO_BONUS = 15;

    // Passed pawn bonus by rank advancement (0-7)
    private static final int[] PASSED_PAWN_BONUS_MG = {0,  5, 10, 20, 40, 80, 150, 0};
    private static final int[] PASSED_PAWN_BONUS_EG = {0, 10, 20, 45, 90, 180, 350, 0};

    // Mobility weights per square (centipawns per available square)
    private static final int[] MOBILITY_MG = {0, 4, 3, 2, 1, 0};
    private static final int[] MOBILITY_EG = {0, 2, 3, 3, 2, 0};

    // King safety: pawn shield bonus per pawn near king
    private static final int PAWN_SHIELD_BONUS = 15;
    private static final int PAWN_SHIELD_BONUS_3RD = 8;

    // --- New evaluation constants ---

    // Connected rooks
    private static final int CONNECTED_ROOKS_BONUS_MG = 15;
    private static final int CONNECTED_ROOKS_BONUS_EG = 10;

    // Knight outposts
    private static final int KNIGHT_OUTPOST_UNSUPPORTED_MG = 20;
    private static final int KNIGHT_OUTPOST_UNSUPPORTED_EG = 10;
    private static final int KNIGHT_OUTPOST_SUPPORTED_MG = 30;
    private static final int KNIGHT_OUTPOST_SUPPORTED_EG = 15;
    private static final int KNIGHT_OUTPOST_CENTER_BONUS_UNSUPPORTED = 3;
    private static final int KNIGHT_OUTPOST_CENTER_BONUS_SUPPORTED = 5;

    // King danger zone
    private static final int[] KING_DANGER_ATTACK_WEIGHT = {0, 2, 2, 3, 5, 0}; // N, B, R, Q indexed by piece type
    private static final int KING_DANGER_MAX = 500;

    // Space advantage
    private static final int SPACE_BONUS_PER_SQUARE = 2;

    // Pawn storms (bonus by rank advancement: index = rank 0-7)
    private static final int[] PAWN_STORM_BONUS = {0, 0, 0, 10, 25, 50, 0, 0};

    // Rook on 7th rank
    private static final int ROOK_7TH_BONUS_MG = 25;
    private static final int ROOK_7TH_BONUS_EG = 35;

    // Backward pawns
    private static final int BACKWARD_PAWN_PENALTY_MG = -10;
    private static final int BACKWARD_PAWN_PENALTY_EG = -15;

    // King-pawn proximity (endgame)
    private static final int KING_PAWN_PROXIMITY_BONUS = 5;

    // Minor piece imbalance
    private static final int KNIGHT_PAWN_ADJ_MG = 3;
    private static final int KNIGHT_PAWN_ADJ_EG = 2;
    private static final int BISHOP_PAWN_ADJ_MG = -3;
    private static final int BISHOP_PAWN_ADJ_EG = -2;

    // Passed pawn advanced bonuses
    private static final int UNBLOCKED_PASSER_BONUS_MG = 5;   // per advancement rank
    private static final int UNBLOCKED_PASSER_BONUS_EG = 15;  // per advancement rank
    private static final int FREE_PATH_PASSER_BONUS_MG = 15;
    private static final int FREE_PATH_PASSER_BONUS_EG = 50;
    private static final int ROOK_BEHIND_PASSER_BONUS_MG = 20;
    private static final int ROOK_BEHIND_PASSER_BONUS_EG = 30;
    private static final int PROTECTED_PASSER_BONUS_MG = 15;
    private static final int PROTECTED_PASSER_BONUS_EG = 25;
    private static final int UNSTOPPABLE_PASSER_BONUS = 400; // EG only

    // ----- Masks -----

    private static final long[] ADJACENT_FILES = {
        FILE_B,              // file A
        FILE_A | FILE_C,     // file B
        FILE_B | FILE_D,     // file C
        FILE_C | FILE_E,     // file D
        FILE_D | FILE_F,     // file E
        FILE_E | FILE_G,     // file F
        FILE_F | FILE_H,     // file G
        FILE_G               // file H
    };

    private static final long[] FILES = {
        FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H
    };

    private static final long RANK_1 = 0xFFL;
    private static final long RANK_2 = 0xFF00L;
    private static final long RANK_3 = 0xFF0000L;
    private static final long RANK_4 = 0xFF000000L;
    private static final long RANK_5 = 0xFF00000000L;
    private static final long RANK_6 = 0xFF0000000000L;
    private static final long RANK_7 = 0xFF000000000000L;
    private static final long RANK_8 = 0xFF00000000000000L;

    private static final long[] RANKS = {
        RANK_1, RANK_2, RANK_3, RANK_4, RANK_5, RANK_6, RANK_7, RANK_8
    };

    // Center files C-F for space evaluation
    private static final long CENTER_FILES = FILE_C | FILE_D | FILE_E | FILE_F;

    // ----- Piece-Square Tables -----
    // All from white's perspective: index 0 = a1 (rank 1), index 63 = h8 (rank 8)
    // For black: access via pst[sq ^ 56] (mirror rank)

    private static final int[] PAWN_MG = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10,-20,-20, 10, 10,  5,
         5, -5,-10,  0,  0,-10, -5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5,  5, 10, 25, 25, 10,  5,  5,
        10, 10, 20, 30, 30, 20, 10, 10,
        50, 50, 50, 50, 50, 50, 50, 50,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] PAWN_EG = {
         0,  0,  0,  0,  0,  0,  0,  0,
        10, 10, 10, 10, 10, 10, 10, 10,
        10, 10, 10, 10, 10, 10, 10, 10,
        20, 20, 20, 20, 20, 20, 20, 20,
        30, 30, 30, 30, 30, 30, 30, 30,
        50, 50, 50, 50, 50, 50, 50, 50,
        80, 80, 80, 80, 80, 80, 80, 80,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_MG = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] KNIGHT_EG = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_MG = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] BISHOP_EG = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_MG = {
         0,  0,  0,  5,  5,  0,  0,  0,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         5, 10, 10, 10, 10, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] ROOK_EG = {
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] QUEEN_MG = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] QUEEN_EG = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5, 10, 10,  5,  0, -5,
         -5,  0,  5, 10, 10,  5,  0, -5,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_MG = {
         20, 30, 10,  0,  0, 10, 30, 20,
         20, 20,  0,  0,  0,  0, 20, 20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30
    };

    private static final int[] KING_EG = {
        -50,-30,-30,-30,-30,-30,-30,-50,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -50,-40,-30,-20,-20,-30,-40,-50
    };

    private static final int[][] MG_PST = {PAWN_MG, KNIGHT_MG, BISHOP_MG, ROOK_MG, QUEEN_MG, KING_MG};
    private static final int[][] EG_PST = {PAWN_EG, KNIGHT_EG, BISHOP_EG, ROOK_EG, QUEEN_EG, KING_EG};

    // ========== Public API ==========

    /**
     * Evaluates the board position. Returns a score in centipawns from
     * the perspective of the side to move (positive = advantage).
     */
    public static int evaluate(Bitboard board) {
        int mgWhite = 0, egWhite = 0;
        int mgBlack = 0, egBlack = 0;

        long allPieces = board.allPieces();
        long whitePieces = board.allWhitePieces();
        long blackPieces = board.allBlackPieces();
        long wPawns = board.getPawns(white);
        long bPawns = board.getPawns(black);

        // Game phase
        int phase = computePhase(board);

        // 1. Material + PST
        for (int pt = 0; pt < 6; pt++) {
            long wBB = getPieceBB(board, pt, white);
            while (wBB != 0) {
                int sq = Long.numberOfTrailingZeros(wBB);
                mgWhite += MG_PIECE_VALUE[pt] + MG_PST[pt][sq];
                egWhite += EG_PIECE_VALUE[pt] + EG_PST[pt][sq];
                wBB &= wBB - 1;
            }
            long bBB = getPieceBB(board, pt, black);
            while (bBB != 0) {
                int sq = Long.numberOfTrailingZeros(bBB);
                mgBlack += MG_PIECE_VALUE[pt] + MG_PST[pt][sq ^ 56];
                egBlack += EG_PIECE_VALUE[pt] + EG_PST[pt][sq ^ 56];
                bBB &= bBB - 1;
            }
        }

        // 2. Bishop pair
        if (Long.bitCount(board.getBishops(white)) >= 2) {
            mgWhite += BISHOP_PAIR_BONUS_MG;
            egWhite += BISHOP_PAIR_BONUS_EG;
        }
        if (Long.bitCount(board.getBishops(black)) >= 2) {
            mgBlack += BISHOP_PAIR_BONUS_MG;
            egBlack += BISHOP_PAIR_BONUS_EG;
        }

        // 3. Minor piece imbalance
        int totalPawns = Long.bitCount(wPawns | bPawns);
        int pawnDelta = totalPawns - 8;

        int wKnightCount = Long.bitCount(board.getKnights(white));
        int wBishopCount = Long.bitCount(board.getBishops(white));
        mgWhite += wKnightCount * pawnDelta * KNIGHT_PAWN_ADJ_MG;
        egWhite += wKnightCount * pawnDelta * KNIGHT_PAWN_ADJ_EG;
        mgWhite += wBishopCount * pawnDelta * BISHOP_PAWN_ADJ_MG;
        egWhite += wBishopCount * pawnDelta * BISHOP_PAWN_ADJ_EG;

        int bKnightCount = Long.bitCount(board.getKnights(black));
        int bBishopCount = Long.bitCount(board.getBishops(black));
        mgBlack += bKnightCount * pawnDelta * KNIGHT_PAWN_ADJ_MG;
        egBlack += bKnightCount * pawnDelta * KNIGHT_PAWN_ADJ_EG;
        mgBlack += bBishopCount * pawnDelta * BISHOP_PAWN_ADJ_MG;
        egBlack += bBishopCount * pawnDelta * BISHOP_PAWN_ADJ_EG;

        // King squares (used by multiple evaluations below)
        int wKingSq = Long.numberOfTrailingZeros(board.kingPiece(white));
        int bKingSq = Long.numberOfTrailingZeros(board.kingPiece(black));

        // 4. Pawn structure (doubled, isolated, backward)
        int[] wPawnStructure = evaluatePawnStructure(wPawns, bPawns, white);
        int[] bPawnStructure = evaluatePawnStructure(bPawns, wPawns, black);
        mgWhite += wPawnStructure[0]; egWhite += wPawnStructure[1];
        mgBlack += bPawnStructure[0]; egBlack += bPawnStructure[1];

        // 4b. Passed pawns (base bonus + unblocked/free path/rook support/unstoppable)
        int[] wPassedPawns = evaluatePassedPawns(board, wPawns, bPawns,
                board.getRooks(white), bKingSq, white, allPieces);
        int[] bPassedPawns = evaluatePassedPawns(board, bPawns, wPawns,
                board.getRooks(black), wKingSq, black, allPieces);
        mgWhite += wPassedPawns[0]; egWhite += wPassedPawns[1];
        mgBlack += bPassedPawns[0]; egBlack += bPassedPawns[1];

        // 5. Rook on open/semi-open files
        int[] wRookFiles = evaluateRookFiles(board.getRooks(white), wPawns, bPawns);
        int[] bRookFiles = evaluateRookFiles(board.getRooks(black), bPawns, wPawns);
        mgWhite += wRookFiles[0]; egWhite += wRookFiles[1];
        mgBlack += bRookFiles[0]; egBlack += bRookFiles[1];

        // 6. Connected rooks
        int[] wConnRooks = evaluateConnectedRooks(board.getRooks(white), allPieces);
        int[] bConnRooks = evaluateConnectedRooks(board.getRooks(black), allPieces);
        mgWhite += wConnRooks[0]; egWhite += wConnRooks[1];
        mgBlack += bConnRooks[0]; egBlack += bConnRooks[1];

        // 7. Rook on 7th rank
        int[] wRook7th = evaluateRookOn7th(board.getRooks(white), bKingSq, bPawns, white);
        int[] bRook7th = evaluateRookOn7th(board.getRooks(black), wKingSq, wPawns, black);
        mgWhite += wRook7th[0]; egWhite += wRook7th[1];
        mgBlack += bRook7th[0]; egBlack += bRook7th[1];

        // 8. Knight outposts
        int[] wOutposts = evaluateKnightOutposts(board.getKnights(white), wPawns, bPawns, white);
        int[] bOutposts = evaluateKnightOutposts(board.getKnights(black), bPawns, wPawns, black);
        mgWhite += wOutposts[0]; egWhite += wOutposts[1];
        mgBlack += bOutposts[0]; egBlack += bOutposts[1];

        // 9. King safety: pawn shield (MG only)
        mgWhite += evaluateKingSafety(board, white, wPawns);
        mgBlack += evaluateKingSafety(board, black, bPawns);

        // 10. King danger zone (MG only)
        mgWhite -= evaluateKingDanger(board, black, wKingSq, whitePieces, allPieces);  // enemy attacks on white king
        mgBlack -= evaluateKingDanger(board, white, bKingSq, blackPieces, allPieces);  // enemy attacks on black king

        // 11. Pawn storms (MG only)
        int wKingFile = wKingSq % 8;
        int bKingFile = bKingSq % 8;
        mgWhite += evaluatePawnStorms(wPawns, bKingSq, bKingFile, wKingFile, white);
        mgBlack += evaluatePawnStorms(bPawns, wKingSq, wKingFile, bKingFile, black);

        // 12. Mobility
        int[] wMobility = evaluateMobility(board, white, whitePieces, allPieces);
        int[] bMobility = evaluateMobility(board, black, blackPieces, allPieces);
        mgWhite += wMobility[0]; egWhite += wMobility[1];
        mgBlack += bMobility[0]; egBlack += bMobility[1];

        // 13. Space advantage (MG only)
        mgWhite += evaluateSpace(wPawns, bPawns, white);
        mgBlack += evaluateSpace(bPawns, wPawns, black);

        // 14. King-pawn proximity (EG only)
        egWhite += evaluateKingPawnProximity(wPawns, bPawns, wKingSq, bKingSq, white);
        egBlack += evaluateKingPawnProximity(bPawns, wPawns, bKingSq, wKingSq, black);

        // Interpolate between middlegame and endgame
        int mgScore = mgWhite - mgBlack;
        int egScore = egWhite - egBlack;
        int score = (mgScore * phase + egScore * (TOTAL_PHASE - phase)) / TOTAL_PHASE;

        // Tempo bonus for the side to move
        score += (board.turn() == white) ? TEMPO_BONUS : -TEMPO_BONUS;

        // Return from side-to-move perspective
        return (board.turn() == white) ? score : -score;
    }

    // ========== Component evaluations ==========

    private static int computePhase(Bitboard board) {
        int phase = 0;
        phase += Long.bitCount(board.getKnights(white) | board.getKnights(black)) * PHASE_KNIGHT;
        phase += Long.bitCount(board.getBishops(white) | board.getBishops(black)) * PHASE_BISHOP;
        phase += Long.bitCount(board.getRooks(white) | board.getRooks(black)) * PHASE_ROOK;
        phase += Long.bitCount(board.getQueens(white) | board.getQueens(black)) * PHASE_QUEEN;
        return Math.min(phase, TOTAL_PHASE);
    }

    /**
     * Returns {mgScore, egScore} for pawn structure, including backward pawns.
     */
    private static int[] evaluatePawnStructure(long friendlyPawns, long enemyPawns, int color) {
        int mg = 0, eg = 0;
        long pawns = friendlyPawns;

        while (pawns != 0) {
            int sq = Long.numberOfTrailingZeros(pawns);
            int file = sq % 8;
            int rank = sq / 8;

            // Doubled pawns
            if (Long.bitCount(friendlyPawns & FILES[file]) > 1) {
                mg += DOUBLED_PAWN_PENALTY;
                eg += DOUBLED_PAWN_PENALTY;
            }

            // Isolated pawns
            if ((friendlyPawns & ADJACENT_FILES[file]) == 0) {
                mg += ISOLATED_PAWN_PENALTY_MG;
                eg += ISOLATED_PAWN_PENALTY_EG;
            }

            // Backward pawns: stop square attacked by enemy pawn and no friendly pawn
            // on adjacent files at same or lower rank can support it
            if (isBackwardPawn(sq, file, rank, friendlyPawns, enemyPawns, color)) {
                mg += BACKWARD_PAWN_PENALTY_MG;
                eg += BACKWARD_PAWN_PENALTY_EG;
            }

            pawns &= pawns - 1;
        }
        return new int[]{mg, eg};
    }

    /**
     * Returns {mgScore, egScore} for passed pawns with advanced evaluation:
     * base bonus, unblockaded bonus, free path, rook behind passer,
     * protected passer, and unstoppable passer detection.
     */
    private static int[] evaluatePassedPawns(Bitboard board, long friendlyPawns, long enemyPawns,
                                              long friendlyRooks, int enemyKingSq,
                                              int color, long allPieces) {
        int mg = 0, eg = 0;
        long pawns = friendlyPawns;

        while (pawns != 0) {
            int sq = Long.numberOfTrailingZeros(pawns);
            int file = sq % 8;
            int rank = sq / 8;

            if (isPassedPawn(sq, file, rank, enemyPawns, color)) {
                int advancement = (color == white) ? rank - 1 : 6 - rank;
                if (advancement >= 0 && advancement < PASSED_PAWN_BONUS_MG.length) {
                    mg += PASSED_PAWN_BONUS_MG[advancement];
                    eg += PASSED_PAWN_BONUS_EG[advancement];
                }

                // Unblockaded: stop square is empty
                int stopSq = (color == white) ? sq + 8 : sq - 8;
                if (stopSq >= 0 && stopSq <= 63 && board.getPiece(stopSq) == -1) {
                    mg += advancement * UNBLOCKED_PASSER_BONUS_MG;
                    eg += advancement * UNBLOCKED_PASSER_BONUS_EG;

                    // Free path: all squares from stop square to promotion are empty
                    boolean freePath = true;
                    int checkSq = stopSq;
                    while (checkSq >= 0 && checkSq <= 63) {
                        if ((allPieces & (1L << checkSq)) != 0) {
                            freePath = false;
                            break;
                        }
                        checkSq = (color == white) ? checkSq + 8 : checkSq - 8;
                    }
                    if (freePath) {
                        mg += FREE_PATH_PASSER_BONUS_MG;
                        eg += FREE_PATH_PASSER_BONUS_EG;
                    }

                    // Unstoppable passer (EG): enemy king outside the square of the pawn
                    int movesToPromo = (color == white) ? 7 - rank : rank;
                    int promoSq = (color == white) ? (56 + file) : file;
                    int kingDist = chebyshevDistance(enemyKingSq, promoSq);
                    if (freePath && movesToPromo < kingDist) {
                        eg += UNSTOPPABLE_PASSER_BONUS;
                    }
                }

                // Rook behind passer: friendly rook on same file behind the pawn
                long fileOfPawn = FILES[file];
                long behindMask = 0L;
                if (color == white) {
                    for (int r = 0; r < rank; r++) behindMask |= RANKS[r];
                } else {
                    for (int r = rank + 1; r <= 7; r++) behindMask |= RANKS[r];
                }
                if ((friendlyRooks & fileOfPawn & behindMask) != 0) {
                    mg += ROOK_BEHIND_PASSER_BONUS_MG;
                    eg += ROOK_BEHIND_PASSER_BONUS_EG;
                }

                // Protected passer: defended by another friendly pawn
                long pawnDefenders = PawnAttacks.getAllPawnAttacks(sq, 1 - color);
                if ((pawnDefenders & friendlyPawns) != 0) {
                    mg += PROTECTED_PASSER_BONUS_MG;
                    eg += PROTECTED_PASSER_BONUS_EG;
                }
            }

            pawns &= pawns - 1;
        }
        return new int[]{mg, eg};
    }

    private static boolean isPassedPawn(int sq, int file, int rank, long enemyPawns, int color) {
        long fileMask = FILES[file] | ADJACENT_FILES[file];
        long rankMask = 0L;
        if (color == white) {
            for (int r = rank + 1; r <= 7; r++) rankMask |= RANKS[r];
        } else {
            for (int r = rank - 1; r >= 0; r--) rankMask |= RANKS[r];
        }
        return (enemyPawns & fileMask & rankMask) == 0;
    }

    private static boolean isBackwardPawn(int sq, int file, int rank, long friendlyPawns,
                                          long enemyPawns, int color) {
        // A backward pawn has no friendly pawn on adjacent files at same or lower rank
        // AND its stop square is attacked by an enemy pawn
        long adjFiles = ADJACENT_FILES[file];

        // Check for friendly pawns on adjacent files that are at same rank or behind
        long supportMask = adjFiles;
        if (color == white) {
            // Ranks from rank 1 up to current rank
            long rankMask = 0L;
            for (int r = 0; r <= rank; r++) rankMask |= RANKS[r];
            supportMask &= rankMask;
        } else {
            // Ranks from rank 8 down to current rank
            long rankMask = 0L;
            for (int r = 7; r >= rank; r--) rankMask |= RANKS[r];
            supportMask &= rankMask;
        }

        if ((friendlyPawns & supportMask) != 0) return false;

        // Check if the stop square is attacked by enemy pawn
        int stopSquare = (color == white) ? sq + 8 : sq - 8;
        if (stopSquare < 0 || stopSquare > 63) return false;

        // Enemy pawn attacks on the stop square
        // A square is attacked by an enemy pawn if an enemy pawn is diagonally behind it
        long enemyPawnAttacks = PawnAttacks.getAllPawnAttacks(stopSquare, color);
        return (enemyPawnAttacks & enemyPawns) != 0;
    }

    /**
     * Returns {mgScore, egScore} for rook file bonuses.
     */
    private static int[] evaluateRookFiles(long rooks, long friendlyPawns, long enemyPawns) {
        int mg = 0, eg = 0;
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            int file = sq % 8;
            long fileMask = FILES[file];

            if ((friendlyPawns & fileMask) == 0) {
                if ((enemyPawns & fileMask) == 0) {
                    mg += ROOK_OPEN_FILE_BONUS;
                    eg += ROOK_OPEN_FILE_BONUS;
                } else {
                    mg += ROOK_SEMI_OPEN_FILE_BONUS;
                    eg += ROOK_SEMI_OPEN_FILE_BONUS;
                }
            }
            rooks &= rooks - 1;
        }
        return new int[]{mg, eg};
    }

    /**
     * Returns {mgScore, egScore} for connected rooks.
     * Two rooks seeing each other along a rank or file (no pieces between).
     */
    private static int[] evaluateConnectedRooks(long rooks, long allPieces) {
        if (Long.bitCount(rooks) < 2) return new int[]{0, 0};

        int sq1 = Long.numberOfTrailingZeros(rooks);
        long remaining = rooks & (rooks - 1);
        int sq2 = Long.numberOfTrailingZeros(remaining);

        // Check if rook at sq1 can see sq2 via rook attacks
        long rookAttacksFromSq1 = MagicBitboard.rookAttacks(sq1, allPieces);
        if ((rookAttacksFromSq1 & (1L << sq2)) != 0) {
            return new int[]{CONNECTED_ROOKS_BONUS_MG, CONNECTED_ROOKS_BONUS_EG};
        }
        return new int[]{0, 0};
    }

    /**
     * Returns {mgScore, egScore} for rook on 7th rank.
     * Bonus when enemy king is on back rank or enemy pawns are on that rank.
     */
    private static int[] evaluateRookOn7th(long rooks, int enemyKingSq, long enemyPawns, int color) {
        int mg = 0, eg = 0;
        long seventhRank = (color == white) ? RANK_7 : RANK_2;
        int enemyBackRank = (color == white) ? 7 : 0;

        long rooksOn7th = rooks & seventhRank;
        if (rooksOn7th == 0) return new int[]{0, 0};

        int enemyKingRank = enemyKingSq / 8;
        boolean kingOnBackRank = (enemyKingRank == enemyBackRank);
        boolean pawnsOnRank = (enemyPawns & seventhRank) != 0;

        if (kingOnBackRank || pawnsOnRank) {
            int count = Long.bitCount(rooksOn7th);
            mg += count * ROOK_7TH_BONUS_MG;
            eg += count * ROOK_7TH_BONUS_EG;
        }
        return new int[]{mg, eg};
    }

    /**
     * Returns {mgScore, egScore} for knight outposts.
     * Knight in enemy half, no enemy pawn on adjacent files at same or more advanced ranks.
     */
    private static int[] evaluateKnightOutposts(long knights, long friendlyPawns, long enemyPawns, int color) {
        int mg = 0, eg = 0;

        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            int rank = sq / 8;
            int file = sq % 8;

            // Must be in enemy half (rank >= 4 for white, rank <= 3 for black)
            boolean inEnemyHalf = (color == white) ? rank >= 4 : rank <= 3;
            if (inEnemyHalf) {
                // Check no enemy pawn can attack this square from more advanced ranks
                long adjFiles = ADJACENT_FILES[file];
                long advancedRanks = 0L;
                if (color == white) {
                    for (int r = rank; r <= 7; r++) advancedRanks |= RANKS[r];
                } else {
                    for (int r = rank; r >= 0; r--) advancedRanks |= RANKS[r];
                }

                if ((enemyPawns & adjFiles & advancedRanks) == 0) {
                    boolean inCenter = (file >= 2 && file <= 5); // C-F files
                    // Check if supported by own pawn
                    // A pawn supports the knight if it attacks the knight's square
                    // PawnAttacks for the enemy color from this square gives squares where
                    // friendly pawns would need to be to attack this square
                    long pawnSupportSquares = PawnAttacks.getAllPawnAttacks(sq, 1 - color);
                    boolean supported = (pawnSupportSquares & friendlyPawns) != 0;

                    if (supported) {
                        mg += KNIGHT_OUTPOST_SUPPORTED_MG + (inCenter ? KNIGHT_OUTPOST_CENTER_BONUS_SUPPORTED : 0);
                        eg += KNIGHT_OUTPOST_SUPPORTED_EG;
                    } else {
                        mg += KNIGHT_OUTPOST_UNSUPPORTED_MG + (inCenter ? KNIGHT_OUTPOST_CENTER_BONUS_UNSUPPORTED : 0);
                        eg += KNIGHT_OUTPOST_UNSUPPORTED_EG;
                    }
                }
            }

            knights &= knights - 1;
        }
        return new int[]{mg, eg};
    }

    private static int evaluateKingSafety(Bitboard board, int color, long friendlyPawns) {
        int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(color));
        int kingFile = kingSquare % 8;
        int kingRank = kingSquare / 8;
        int score = 0;

        int shieldRank2 = (color == white) ? kingRank + 1 : kingRank - 1;
        int shieldRank3 = (color == white) ? kingRank + 2 : kingRank - 2;

        for (int f = Math.max(0, kingFile - 1); f <= Math.min(7, kingFile + 1); f++) {
            if (shieldRank2 >= 0 && shieldRank2 <= 7) {
                if ((friendlyPawns & (1L << (shieldRank2 * 8 + f))) != 0) {
                    score += PAWN_SHIELD_BONUS;
                }
            }
            if (shieldRank3 >= 0 && shieldRank3 <= 7) {
                if ((friendlyPawns & (1L << (shieldRank3 * 8 + f))) != 0) {
                    score += PAWN_SHIELD_BONUS_3RD;
                }
            }
        }

        return score;
    }

    /**
     * Evaluates king danger zone for the defending side.
     * Returns a penalty (positive value) based on how many enemy pieces attack
     * the king zone (king square + adjacent squares).
     *
     * @param board       the board
     * @param attackColor the attacking side (enemy of the king we're evaluating)
     * @param kingSq      the square of the king being attacked
     * @param friendly    friendly pieces bitboard (of the side being attacked)
     * @param allPieces   all pieces
     * @return penalty value (positive = bad for the king's side), 0 if < 2 attackers
     */
    private static int evaluateKingDanger(Bitboard board, int attackColor,
                                          int kingSq, long friendly, long allPieces) {
        // King zone = king square + all adjacent squares
        long kingZone = KingAttacks.getAllKingAttacks(kingSq) | (1L << kingSq);

        int attackWeight = 0;
        int attackerCount = 0;

        // Knights
        long knights = board.getKnights(attackColor);
        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            if ((KnightAttacks.getAllKnightAttacks(sq) & kingZone) != 0) {
                attackWeight += KING_DANGER_ATTACK_WEIGHT[knight];
                attackerCount++;
            }
            knights &= knights - 1;
        }

        // Bishops
        long bishops = board.getBishops(attackColor);
        while (bishops != 0) {
            int sq = Long.numberOfTrailingZeros(bishops);
            if ((MagicBitboard.bishopAttacks(sq, allPieces) & kingZone) != 0) {
                attackWeight += KING_DANGER_ATTACK_WEIGHT[bishop];
                attackerCount++;
            }
            bishops &= bishops - 1;
        }

        // Rooks
        long rooks = board.getRooks(attackColor);
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            if ((MagicBitboard.rookAttacks(sq, allPieces) & kingZone) != 0) {
                attackWeight += KING_DANGER_ATTACK_WEIGHT[rook];
                attackerCount++;
            }
            rooks &= rooks - 1;
        }

        // Queens
        long queens = board.getQueens(attackColor);
        while (queens != 0) {
            int sq = Long.numberOfTrailingZeros(queens);
            if ((MagicBitboard.queenAttacks(sq, allPieces) & kingZone) != 0) {
                attackWeight += KING_DANGER_ATTACK_WEIGHT[queen];
                attackerCount++;
            }
            queens &= queens - 1;
        }

        if (attackerCount < 2) return 0;

        // Quadratic penalty: weight^2 / 4, capped
        int penalty = Math.min(attackWeight * attackWeight / 4, KING_DANGER_MAX);
        return penalty;
    }

    /**
     * Evaluates pawn storms when kings are castled on opposite sides.
     * Bonus for own pawns advanced on the 3 files around the enemy king.
     */
    private static int evaluatePawnStorms(long friendlyPawns, int enemyKingSq,
                                          int enemyKingFile, int friendlyKingFile, int color) {
        // Only when kings are on opposite sides (>= 4 files apart)
        if (Math.abs(friendlyKingFile - enemyKingFile) < 4) return 0;

        int score = 0;
        for (int f = Math.max(0, enemyKingFile - 1); f <= Math.min(7, enemyKingFile + 1); f++) {
            long filePawns = friendlyPawns & FILES[f];
            while (filePawns != 0) {
                int sq = Long.numberOfTrailingZeros(filePawns);
                int rank = sq / 8;
                // For white, higher rank = more advanced; for black, lower rank = more advanced
                int advancementRank = (color == white) ? rank : 7 - rank;
                if (advancementRank >= 0 && advancementRank < PAWN_STORM_BONUS.length) {
                    score += PAWN_STORM_BONUS[advancementRank];
                }
                filePawns &= filePawns - 1;
            }
        }
        return score;
    }

    /**
     * Returns {mgScore, egScore} for piece mobility.
     */
    private static int[] evaluateMobility(Bitboard board, int color,
                                          long friendly, long allPieces) {
        int mg = 0, eg = 0;

        long knights = board.getKnights(color);
        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            int count = Long.bitCount(KnightAttacks.getAllKnightAttacks(sq) & ~friendly);
            mg += count * MOBILITY_MG[knight];
            eg += count * MOBILITY_EG[knight];
            knights &= knights - 1;
        }

        long bishops = board.getBishops(color);
        while (bishops != 0) {
            int sq = Long.numberOfTrailingZeros(bishops);
            int count = Long.bitCount(MagicBitboard.bishopAttacks(sq, allPieces) & ~friendly);
            mg += count * MOBILITY_MG[bishop];
            eg += count * MOBILITY_EG[bishop];
            bishops &= bishops - 1;
        }

        long rooks = board.getRooks(color);
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            int count = Long.bitCount(MagicBitboard.rookAttacks(sq, allPieces) & ~friendly);
            mg += count * MOBILITY_MG[rook];
            eg += count * MOBILITY_EG[rook];
            rooks &= rooks - 1;
        }

        long queens = board.getQueens(color);
        while (queens != 0) {
            int sq = Long.numberOfTrailingZeros(queens);
            int count = Long.bitCount(MagicBitboard.queenAttacks(sq, allPieces) & ~friendly);
            mg += count * MOBILITY_MG[queen];
            eg += count * MOBILITY_EG[queen];
            queens &= queens - 1;
        }

        return new int[]{mg, eg};
    }

    /**
     * Evaluates space advantage (MG only).
     * Count safe squares in center files (C-F) on own side of board,
     * excluding squares attacked by enemy pawns.
     */
    private static int evaluateSpace(long friendlyPawns, long enemyPawns, int color) {
        // Own side of board: ranks 2-4 for white, ranks 5-7 for black
        long ownSide;
        if (color == white) {
            ownSide = RANK_2 | RANK_3 | RANK_4;
        } else {
            ownSide = RANK_5 | RANK_6 | RANK_7;
        }

        long spaceArea = CENTER_FILES & ownSide;

        // Compute enemy pawn attacks using bitwise shifts (optimized)
        long enemyPawnAttacks;
        if (color == white) {
            // Enemy is black: black pawns attack down-left (>>9) and down-right (>>7)
            enemyPawnAttacks = ((enemyPawns & ~FILE_A) >>> 9) | ((enemyPawns & ~FILE_H) >>> 7);
        } else {
            // Enemy is white: white pawns attack up-left (<<7) and up-right (<<9)
            enemyPawnAttacks = ((enemyPawns & ~FILE_A) << 7) | ((enemyPawns & ~FILE_H) << 9);
        }

        long safeSquares = spaceArea & ~enemyPawnAttacks & ~friendlyPawns;
        return Long.bitCount(safeSquares) * SPACE_BONUS_PER_SQUARE;
    }

    /**
     * Evaluates king-pawn proximity for passed pawns (EG only).
     * Bonus for own king being close to own passed pawns,
     * penalty for enemy king being close to own passed pawns.
     */
    private static int evaluateKingPawnProximity(long friendlyPawns, long enemyPawns,
                                                 int friendlyKingSq, int enemyKingSq, int color) {
        int score = 0;
        long pawns = friendlyPawns;

        while (pawns != 0) {
            int sq = Long.numberOfTrailingZeros(pawns);
            int file = sq % 8;
            int rank = sq / 8;

            if (isPassedPawn(sq, file, rank, enemyPawns, color)) {
                int friendlyDist = chebyshevDistance(friendlyKingSq, sq);
                int enemyDist = chebyshevDistance(enemyKingSq, sq);
                // Bonus for own king close, penalty for enemy king close
                // Max distance is 7, so (7 - dist) gives closeness score
                score += (7 - friendlyDist) * KING_PAWN_PROXIMITY_BONUS;
                score -= (7 - enemyDist) * KING_PAWN_PROXIMITY_BONUS;
            }

            pawns &= pawns - 1;
        }
        return score;
    }

    // ========== Helpers ==========

    static long getPieceBB(Bitboard board, int pieceType, int color) {
        return switch (pieceType) {
            case pawn -> board.getPawns(color);
            case knight -> board.getKnights(color);
            case bishop -> board.getBishops(color);
            case rook -> board.getRooks(color);
            case queen -> board.getQueens(color);
            case king -> board.kingPiece(color);
            default -> 0L;
        };
    }

    private static int chebyshevDistance(int sq1, int sq2) {
        int file1 = sq1 % 8, rank1 = sq1 / 8;
        int file2 = sq2 % 8, rank2 = sq2 / 8;
        return Math.max(Math.abs(file1 - file2), Math.abs(rank1 - rank2));
    }
}

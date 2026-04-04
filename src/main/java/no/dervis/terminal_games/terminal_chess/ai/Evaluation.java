package no.dervis.terminal_games.terminal_chess.ai;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.board.MagicBitboard;
import no.dervis.terminal_games.terminal_chess.moves.attacks.KnightAttacks;

/**
 * Static board evaluation function for the chess AI.
 *
 * <p>Evaluates a position using:</p>
 * <ul>
 *   <li>Material balance</li>
 *   <li>Piece-square tables (middlegame/endgame with game-phase interpolation)</li>
 *   <li>Pawn structure (doubled, isolated, passed pawns)</li>
 *   <li>Bishop pair bonus</li>
 *   <li>Rook on open/semi-open files</li>
 *   <li>King safety (pawn shield)</li>
 *   <li>Piece mobility</li>
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
    private static final int TOTAL_PHASE = 24; // 2*(1+1+2+4) per side = 16, but standard is 24

    // ----- Evaluation bonuses / penalties -----
    private static final int BISHOP_PAIR_BONUS_MG = 45;
    private static final int BISHOP_PAIR_BONUS_EG = 55;
    private static final int DOUBLED_PAWN_PENALTY = -12;
    private static final int ISOLATED_PAWN_PENALTY_MG = -15;
    private static final int ISOLATED_PAWN_PENALTY_EG = -20;
    private static final int ROOK_OPEN_FILE_BONUS = 25;
    private static final int ROOK_SEMI_OPEN_FILE_BONUS = 12;
    private static final int TEMPO_BONUS = 15;

    // Passed pawn bonus by rank (from the pawn's perspective)
    // Index = number of ranks advanced from starting position (0-6)
    private static final int[] PASSED_PAWN_BONUS_MG = {0,  5, 10, 20, 35, 60, 100, 0};
    private static final int[] PASSED_PAWN_BONUS_EG = {0, 10, 20, 40, 70, 120, 200, 0};

    // Mobility weights per square (centipawns per available square)
    private static final int[] MOBILITY_MG = {0, 4, 3, 2, 1, 0}; // pawn, knight, bishop, rook, queen, king
    private static final int[] MOBILITY_EG = {0, 2, 3, 3, 2, 0};

    // King safety: pawn shield bonus per pawn on 2nd/3rd rank near king
    private static final int PAWN_SHIELD_BONUS = 15;
    private static final int PAWN_SHIELD_BONUS_3RD = 8;

    // ----- Adjacent file masks for pawn structure -----
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

    // ----- Piece-Square Tables -----
    // All from white's perspective: index 0 = a1 (rank 1), index 63 = h8 (rank 8)
    // For black: access via pst[sq ^ 56] (mirror rank)

    // Pawn middlegame PST
    private static final int[] PAWN_MG = {
         0,  0,  0,  0,  0,  0,  0,  0, // rank 1 (no pawns here)
         5, 10, 10,-20,-20, 10, 10,  5, // rank 2
         5, -5,-10,  0,  0,-10, -5,  5, // rank 3
         0,  0,  0, 20, 20,  0,  0,  0, // rank 4
         5,  5, 10, 25, 25, 10,  5,  5, // rank 5
        10, 10, 20, 30, 30, 20, 10, 10, // rank 6
        50, 50, 50, 50, 50, 50, 50, 50, // rank 7 (about to promote)
         0,  0,  0,  0,  0,  0,  0,  0  // rank 8
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

    // Knight PST
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

    // Bishop PST
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

    // Rook PST
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

    // Queen PST
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

    // King middlegame: favor castled position, stay safe
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

    // King endgame: centralize
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

    // Indexed by piece type: pawn=0, knight=1, bishop=2, rook=3, queen=4, king=5
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

        // Game phase
        int phase = computePhase(board);

        // Material + PST
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
                mgBlack += MG_PIECE_VALUE[pt] + MG_PST[pt][sq ^ 56]; // mirror rank
                egBlack += EG_PIECE_VALUE[pt] + EG_PST[pt][sq ^ 56];
                bBB &= bBB - 1;
            }
        }

        // Bishop pair
        if (Long.bitCount(board.getBishops(white)) >= 2) {
            mgWhite += BISHOP_PAIR_BONUS_MG;
            egWhite += BISHOP_PAIR_BONUS_EG;
        }
        if (Long.bitCount(board.getBishops(black)) >= 2) {
            mgBlack += BISHOP_PAIR_BONUS_MG;
            egBlack += BISHOP_PAIR_BONUS_EG;
        }

        // Pawn structure
        long wPawns = board.getPawns(white);
        long bPawns = board.getPawns(black);

        int[] wPawnStructure = evaluatePawnStructure(wPawns, bPawns, white);
        int[] bPawnStructure = evaluatePawnStructure(bPawns, wPawns, black);
        mgWhite += wPawnStructure[0]; egWhite += wPawnStructure[1];
        mgBlack += bPawnStructure[0]; egBlack += bPawnStructure[1];

        // Rook on open/semi-open files
        int[] wRookFiles = evaluateRookFiles(board.getRooks(white), wPawns, bPawns);
        int[] bRookFiles = evaluateRookFiles(board.getRooks(black), bPawns, wPawns);
        mgWhite += wRookFiles[0]; egWhite += wRookFiles[1];
        mgBlack += bRookFiles[0]; egBlack += bRookFiles[1];

        // King safety (pawn shield) — only relevant in middlegame
        int wKingSafety = evaluateKingSafety(board, white, wPawns);
        int bKingSafety = evaluateKingSafety(board, black, bPawns);
        mgWhite += wKingSafety;
        mgBlack += bKingSafety;

        // Mobility
        int[] wMobility = evaluateMobility(board, white, whitePieces, allPieces);
        int[] bMobility = evaluateMobility(board, black, blackPieces, allPieces);
        mgWhite += wMobility[0]; egWhite += wMobility[1];
        mgBlack += bMobility[0]; egBlack += bMobility[1];

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
     * Returns {mgScore, egScore} for pawn structure.
     */
    private static int[] evaluatePawnStructure(long friendlyPawns, long enemyPawns, int color) {
        int mg = 0, eg = 0;
        long pawns = friendlyPawns;

        while (pawns != 0) {
            int sq = Long.numberOfTrailingZeros(pawns);
            int file = sq % 8;
            int rank = sq / 8;

            // Doubled pawns: more than one pawn on this file
            if (Long.bitCount(friendlyPawns & FILES[file]) > 1) {
                mg += DOUBLED_PAWN_PENALTY;
                eg += DOUBLED_PAWN_PENALTY;
            }

            // Isolated pawns: no friendly pawn on adjacent files
            if ((friendlyPawns & ADJACENT_FILES[file]) == 0) {
                mg += ISOLATED_PAWN_PENALTY_MG;
                eg += ISOLATED_PAWN_PENALTY_EG;
            }

            // Passed pawns: no enemy pawn can block or capture on path to promotion
            if (isPassedPawn(sq, file, rank, enemyPawns, color)) {
                int advancement = (color == white) ? rank - 1 : 6 - rank;
                if (advancement >= 0 && advancement < PASSED_PAWN_BONUS_MG.length) {
                    mg += PASSED_PAWN_BONUS_MG[advancement];
                    eg += PASSED_PAWN_BONUS_EG[advancement];
                }
            }

            pawns &= pawns - 1;
        }
        return new int[]{mg, eg};
    }

    private static boolean isPassedPawn(int sq, int file, int rank, long enemyPawns, int color) {
        // Build a mask of files that could block/capture: same file + adjacent files
        long fileMask = FILES[file] | ADJACENT_FILES[file];

        // Only consider ranks ahead of the pawn
        long rankMask = 0L;
        if (color == white) {
            for (int r = rank + 1; r <= 7; r++) rankMask |= 0xFFL << (r * 8);
        } else {
            for (int r = rank - 1; r >= 0; r--) rankMask |= 0xFFL << (r * 8);
        }

        return (enemyPawns & fileMask & rankMask) == 0;
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

    private static int evaluateKingSafety(Bitboard board, int color, long friendlyPawns) {
        int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(color));
        int kingFile = kingSquare % 8;
        int kingRank = kingSquare / 8;
        int score = 0;

        // Check pawn shield on 2nd and 3rd ranks relative to king
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
     * Returns {mgScore, egScore} for piece mobility.
     */
    private static int[] evaluateMobility(Bitboard board, int color,
                                          long friendly, long allPieces) {
        int mg = 0, eg = 0;

        // Knight mobility
        long knights = board.getKnights(color);
        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            int count = Long.bitCount(KnightAttacks.getAllKnightAttacks(sq) & ~friendly);
            mg += count * MOBILITY_MG[knight];
            eg += count * MOBILITY_EG[knight];
            knights &= knights - 1;
        }

        // Bishop mobility
        long bishops = board.getBishops(color);
        while (bishops != 0) {
            int sq = Long.numberOfTrailingZeros(bishops);
            int count = Long.bitCount(MagicBitboard.bishopAttacks(sq, allPieces) & ~friendly);
            mg += count * MOBILITY_MG[bishop];
            eg += count * MOBILITY_EG[bishop];
            bishops &= bishops - 1;
        }

        // Rook mobility
        long rooks = board.getRooks(color);
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            int count = Long.bitCount(MagicBitboard.rookAttacks(sq, allPieces) & ~friendly);
            mg += count * MOBILITY_MG[rook];
            eg += count * MOBILITY_EG[rook];
            rooks &= rooks - 1;
        }

        // Queen mobility
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
}

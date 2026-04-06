package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced chess AI using iterative deepening with alpha-beta pruning.
 *
 * <h3>Search features</h3>
 * <ul>
 *   <li>Iterative deepening with aspiration windows</li>
 *   <li>Negamax alpha-beta pruning</li>
 *   <li>Principal Variation Search (PVS)</li>
 *   <li>Quiescence search with delta pruning and SEE pruning</li>
 *   <li>Transposition table with incremental Zobrist hashing</li>
 *   <li>Check extensions and singular extensions</li>
 *   <li>Null-move pruning</li>
 *   <li>Reverse futility pruning, futility pruning, razoring</li>
 *   <li>Late Move Reductions (logarithmic, history-informed) and Late Move Pruning</li>
 *   <li>SEE pruning of losing captures in main search</li>
 *   <li>Internal Iterative Deepening</li>
 *   <li>Move ordering: TT move → promotions → SEE captures → killers → countermove → history</li>
 *   <li>Lazy move picking (sort one at a time)</li>
 *   <li>Configurable time management</li>
 * </ul>
 */
public class ChessAI implements Chess, Engine {

    // ===== Constants =====
    private static final int MAX_PLY = 128;
    private static final int INFINITY = 50_000;
    private static final int MATE_SCORE = 49_000;
    private static final int MATE_THRESHOLD = 48_000;

    // Transposition table flags
    private static final byte TT_EXACT = 0;
    private static final byte TT_ALPHA = 1; // upper bound (failed low)
    private static final byte TT_BETA = 2;  // lower bound (failed high)

    // Move ordering score thresholds
    private static final int TT_MOVE_SCORE    = 10_000_000;
    private static final int PROMOTION_SCORE  =  5_000_000;
    private static final int CAPTURE_BASE     =  1_000_000;
    private static final int KILLER_SCORE_0   =    900_000;
    private static final int KILLER_SCORE_1   =    800_000;
    private static final int COUNTERMOVE_SCORE =   700_000;

    // Aspiration window
    private static final int ASP_INITIAL_WINDOW = 25;

    // Reverse Futility Pruning
    private static final int RFP_MAX_DEPTH = 6;
    private static final int RFP_MARGIN_PER_DEPTH = 80;

    // Futility Pruning
    private static final int FP_MAX_DEPTH = 2;
    private static final int[] FP_MARGIN = {0, 200, 350};

    // Late Move Pruning
    private static final int LMP_MAX_DEPTH = 3;
    private static final int[] LMP_MOVE_THRESHOLD = {0, 5, 8, 12};

    // Razoring
    private static final int RAZOR_MAX_DEPTH = 2;
    private static final int[] RAZOR_MARGIN = {0, 300, 500};

    // Delta pruning (quiescence)
    private static final int DELTA_MARGIN = 200;

    // Internal Iterative Deepening
    private static final int IID_MIN_DEPTH = 4;
    private static final int IID_REDUCTION = 2;

    // Singular Extensions
    private static final int SE_MIN_DEPTH = 8;
    private static final int SE_MARGIN_MULTIPLIER = 2;

    // SEE pruning in main search
    private static final int SEE_PRUNE_DEPTH = 6;

    // History clamping
    private static final int HISTORY_MAX = 16384;

    // LMR reduction table (logarithmic): [depth][moveNumber]
    private static final int[][] LMR_TABLE = new int[64][64];
    static {
        for (int d = 1; d < 64; d++) {
            for (int m = 1; m < 64; m++) {
                LMR_TABLE[d][m] = Math.max(0, (int) (0.77 + Math.log(d) * Math.log(m) / 2.36));
            }
        }
    }

    // ===== Transposition Table =====
    private static final int TT_SIZE = 1 << 22; // 4M entries
    private static final int TT_MASK = TT_SIZE - 1;

    private final long[] ttKey   = new long[TT_SIZE];
    private final int[]  ttMove  = new int[TT_SIZE];
    private final short[] ttScore = new short[TT_SIZE];
    private final byte[] ttDepth = new byte[TT_SIZE];
    private final byte[] ttFlag  = new byte[TT_SIZE];

    // ===== Search State =====
    private final int[][] killers = new int[MAX_PLY][2];
    private final int[][][] history = new int[2][64][64];
    private final int[][][] countermoves = new int[2][64][64];

    private long startTime;
    private long timeLimitMs;
    private boolean stopped;
    private int nodesSearched;

    // Best move from the last completed iteration (safe fallback if search is interrupted)
    private int bestMoveRoot;

    private final boolean verbose;

    public ChessAI() {
        this(true);
    }

    public ChessAI(boolean verbose) {
        this.verbose = verbose;
    }

    // ===== Public API =====

    /**
     * Finds the best move for the current side to move.
     *
     * @param board       the current board position
     * @param timeLimitMs maximum time in milliseconds for the search
     * @return the best move (encoded int), or 0 if no legal moves
     */
    public int findBestMove(Bitboard board, long timeLimitMs) {
        this.startTime = System.currentTimeMillis();
        this.timeLimitMs = timeLimitMs;
        this.stopped = false;
        this.nodesSearched = 0;
        this.bestMoveRoot = 0;

        // Clear per-search tables
        clearKillers();
        clearHistory();
        clearCountermoves();

        int color = board.turn();
        Generator gen = new Generator(board);
        List<Integer> rootMoves = gen.generateMoves(color);

        if (rootMoves.isEmpty()) return 0;
        if (rootMoves.size() == 1) return rootMoves.getFirst();

        bestMoveRoot = rootMoves.getFirst();
        int completedDepth = 0;
        int prevScore = 0;

        // Iterative deepening with aspiration windows
        for (int depth = 1; depth <= MAX_PLY; depth++) {
            int score;
            if (depth <= 3) {
                score = searchRoot(board, rootMoves, depth, color, -INFINITY, INFINITY);
            } else {
                int delta = ASP_INITIAL_WINDOW;
                int aspAlpha = prevScore - delta;
                int aspBeta = prevScore + delta;
                while (true) {
                    score = searchRoot(board, rootMoves, depth, color, aspAlpha, aspBeta);
                    if (stopped) break;
                    if (score <= aspAlpha) {
                        aspAlpha = Math.max(aspAlpha - delta, -INFINITY);
                        delta *= 2;
                    } else if (score >= aspBeta) {
                        aspBeta = Math.min(aspBeta + delta, INFINITY);
                        delta *= 2;
                    } else {
                        break;
                    }
                    if (delta > 1000) {
                        aspAlpha = -INFINITY;
                        aspBeta = INFINITY;
                    }
                }
            }

            if (stopped) break;

            prevScore = score;
            completedDepth = depth;

            long elapsed = System.currentTimeMillis() - startTime;
            String scoreStr = isMateScore(score)
                    ? "mate " + mateInMoves(score)
                    : "cp " + score;

            if (verbose) {
                String pvLine = extractPV(board, depth);
            System.out.printf("\r[SingleAI] info depth %d score %s nodes %d time %dms pv %s          ",
                    depth, scoreStr, nodesSearched, elapsed, pvLine);
            System.out.flush();
            }

            // Time management: if more than half the time is used, don't start a new depth
            if (elapsed > timeLimitMs / 2) break;

            // Stop if we found a forced mate
            if (Math.abs(score) > MATE_THRESHOLD) break;
        }

        if (verbose) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("%n[SingleAI] info depth %d total nodes %,d time %dms%n",
                completedDepth, nodesSearched, elapsed);
        }

        return bestMoveRoot;
    }

    // ===== Search Core =====

    /**
     * Root search with move ordering and best move tracking.
     */
    private int searchRoot(Bitboard board, List<Integer> moves, int depth, int color,
                           int alpha, int beta) {
        long hash = board.getHash();
        int ttBest = probeTTMove(hash);

        // Score moves and use lazy picking
        int[] scores = scoreMoves(moves, board, ttBest, 0, color, 0);

        int bestMove = moves.getFirst();
        int bestScore = -INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            pickMove(moves, scores, i);
            int move = moves.get(i);
            Bitboard copy = board.copy();
            copy.makeMove(move);

            int score;
            if (i == 0) {
                score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true, move, 0);
            } else {
                // PVS: null-window search for non-first moves
                score = -alphaBeta(copy, depth - 1, -alpha - 1, -alpha, 1 - color, 1, false, move, 0);
                if (score > alpha && score < beta) {
                    score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true, move, 0);
                }
            }

            if (stopped) return bestScore;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                if (score > alpha) {
                    alpha = score;
                }
            }
        }

        bestMoveRoot = bestMove;
        storeTT(hash, depth, bestScore, TT_EXACT, bestMove);
        return bestScore;
    }

    /**
     * Negamax alpha-beta search with all pruning and extension techniques.
     */
    private int alphaBeta(Bitboard board, int depth, int alpha, int beta,
                          int color, int ply, boolean pvNode, int prevMove, int excludedMove) {
        // Time check every 4096 nodes
        if ((nodesSearched & 4095) == 0) checkTime();
        if (stopped) return 0;

        nodesSearched++;

        // Transposition table probe (using incremental hash)
        long hash = board.getHash();
        int ttHitMove = 0;
        int ttIndex = (int) (hash & TT_MASK);
        if (ttKey[ttIndex] == hash) {
            ttHitMove = ttMove[ttIndex];
            if (ttDepth[ttIndex] >= depth && !pvNode && excludedMove == 0) {
                int ttVal = ttScore[ttIndex];
                byte flag = ttFlag[ttIndex];
                if (flag == TT_EXACT) return ttVal;
                if (flag == TT_ALPHA && ttVal <= alpha) return alpha;
                if (flag == TT_BETA && ttVal >= beta) return beta;
            }
        }

        // Internal Iterative Deepening: at PV nodes with no TT move,
        // do a shallow search to find a move for ordering.
        if (pvNode && ttHitMove == 0 && depth >= IID_MIN_DEPTH) {
            alphaBeta(board, depth - IID_REDUCTION, alpha, beta, color, ply, true, prevMove, 0);
            if (ttKey[ttIndex] == hash) {
                ttHitMove = ttMove[ttIndex];
            }
        }

        // Check extension: extend search when in check
        long kingBB = board.kingPiece(color);
        if (kingBB == 0) return -MATE_SCORE + ply;
        int kingSquare = Long.numberOfTrailingZeros(kingBB);
        boolean inCheck = Generator.isKingInCheck(board, color, kingSquare);
        if (inCheck) depth++;

        // Leaf node → quiescence search
        if (depth <= 0) return quiescence(board, alpha, beta, color, ply);

        // Null-move pruning
        if (!pvNode && !inCheck && depth >= 3 && hasNonPawnMaterial(board, color)) {
            int R = 2 + (depth >= 6 ? 1 : 0);
            Bitboard nullCopy = board.copy();
            nullCopy.makeNullMove();
            int nullScore = -alphaBeta(nullCopy, depth - 1 - R, -beta, -beta + 1,
                    1 - color, ply + 1, false, 0, 0);
            if (stopped) return 0;
            if (nullScore >= beta) {
                if (nullScore > MATE_THRESHOLD) nullScore = beta;
                return nullScore;
            }
        }

        // Static evaluation for pruning decisions (not computed when in check)
        int staticEval = inCheck ? -INFINITY : Evaluation.evaluate(board);

        // Reverse Futility Pruning: position so good that full search won't change result
        if (!pvNode && !inCheck && depth <= RFP_MAX_DEPTH
                && staticEval - RFP_MARGIN_PER_DEPTH * depth >= beta
                && Math.abs(beta) < MATE_THRESHOLD) {
            return staticEval;
        }

        // Razoring: static eval far below alpha — verify with quiescence
        if (!pvNode && !inCheck && depth <= RAZOR_MAX_DEPTH
                && staticEval + RAZOR_MARGIN[depth] < alpha
                && Math.abs(alpha) < MATE_THRESHOLD) {
            int razorScore = quiescence(board, alpha, beta, color, ply);
            if (razorScore <= alpha) return razorScore;
        }

        // Futility pruning flag (applied per-move in the loop)
        boolean canFutilityPrune = !pvNode && !inCheck
                && depth <= FP_MAX_DEPTH
                && staticEval + FP_MARGIN[depth] < alpha
                && Math.abs(alpha) < MATE_THRESHOLD;

        // Generate legal moves
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(color);

        // Terminal node detection
        if (moves.isEmpty()) {
            if (inCheck) return -MATE_SCORE + ply;
            return 0;
        }

        // Score moves (lazy picking in the loop)
        int[] moveScores = scoreMoves(moves, board, ttHitMove, ply, color, prevMove);

        // Singular Extensions: if TT move is significantly better than alternatives, extend it
        boolean singularExtension = false;
        if (depth >= SE_MIN_DEPTH
                && ttHitMove != 0
                && excludedMove == 0
                && ttKey[ttIndex] == hash
                && ttDepth[ttIndex] >= depth - 3
                && (ttFlag[ttIndex] == TT_BETA || ttFlag[ttIndex] == TT_EXACT)) {
            int ttVal = ttScore[ttIndex];
            if (Math.abs(ttVal) < MATE_THRESHOLD) {
                int singularBeta = ttVal - SE_MARGIN_MULTIPLIER * depth;
                int singularDepth = (depth - 1) / 2;
                int singularScore = alphaBeta(board, singularDepth, singularBeta - 1, singularBeta,
                        color, ply, false, prevMove, ttHitMove);
                if (!stopped && singularScore < singularBeta) {
                    singularExtension = true;
                }
            }
        }

        int bestScore = -INFINITY;
        int bestMove = moves.getFirst();
        byte flag = TT_ALPHA;

        for (int i = 0; i < moves.size(); i++) {
            // Lazy move picking: select best remaining move
            pickMove(moves, moveScores, i);
            int move = moves.get(i);

            // Skip excluded move (for singular extension probe)
            if (move == excludedMove) continue;

            boolean isCapture = isCapture(move, board);
            int moveType = (move >>> 4) & 0x7;
            boolean isPromotion = (move & 0xF) > 0;

            // Futility pruning: skip quiet moves when static eval + margin < alpha
            if (canFutilityPrune && !isCapture && !isPromotion && i > 0) {
                continue;
            }

            // Late Move Pruning: at low depths, skip late quiet moves entirely
            if (!pvNode && !inCheck && depth <= LMP_MAX_DEPTH && !isCapture && !isPromotion
                    && i >= LMP_MOVE_THRESHOLD[depth]) {
                continue;
            }

            // SEE pruning: skip losing captures at low depths in the main search
            if (!pvNode && !inCheck && depth <= SEE_PRUNE_DEPTH
                    && isCapture && !isPromotion && i > 0
                    && SEE.isLosingCapture(board, move)) {
                // At very low depths, skip entirely; at higher depths, reduce
                if (depth <= 2) continue;
                // else fall through to LMR with extra reduction
            }

            Bitboard copy = board.copy();
            copy.makeMove(move);

            int score;
            int newDepth = depth - 1;

            // Singular extension: extend the TT move if it was found to be singular
            if (singularExtension && move == ttHitMove) {
                newDepth += 1;
            }

            // Late Move Reductions (LMR) — logarithmic, history-informed
            boolean doLMR = !pvNode && i >= 3 && depth >= 3 && !isCapture && !isPromotion && !inCheck;

            if (doLMR) {
                int d = Math.min(depth, 63);
                int m = Math.min(i, 63);
                int reduction = LMR_TABLE[d][m];

                // History-informed: adjust reduction based on history score
                int from = move >>> 14;
                int to = (move >>> 7) & 0x3F;
                int histScore = history[color][from][to];
                if (histScore < -1000) {
                    reduction += 1;
                } else if (histScore > 4000) {
                    reduction = Math.max(0, reduction - 1);
                }
                reduction = Math.min(reduction, newDepth - 1);
                reduction = Math.max(0, reduction);

                score = -alphaBeta(copy, newDepth - reduction, -alpha - 1, -alpha,
                        1 - color, ply + 1, false, move, 0);

                // Re-search at full depth if it fails high
                if (score > alpha) {
                    score = -alphaBeta(copy, newDepth, -beta, -alpha,
                            1 - color, ply + 1, pvNode, move, 0);
                }
            } else if (!pvNode || i > 0) {
                // PVS: null-window search for non-PV nodes or non-first moves
                score = -alphaBeta(copy, newDepth, -alpha - 1, -alpha,
                        1 - color, ply + 1, false, move, 0);
                if (score > alpha && score < beta) {
                    score = -alphaBeta(copy, newDepth, -beta, -alpha,
                            1 - color, ply + 1, true, move, 0);
                }
            } else {
                // Full window search for the first move in PV nodes
                score = -alphaBeta(copy, newDepth, -beta, -alpha,
                        1 - color, ply + 1, true, move, 0);
            }

            if (stopped) return 0;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;

                if (score > alpha) {
                    alpha = score;
                    flag = TT_EXACT;

                    if (score >= beta) {
                        // Beta cutoff
                        if (!isCapture) {
                            updateKillers(ply, move);
                            int from = move >>> 14;
                            int to = (move >>> 7) & 0x3F;
                            history[color][from][to] = Math.min(
                                    history[color][from][to] + depth * depth, HISTORY_MAX);

                            // Countermove: record this move as the refutation of prevMove
                            if (prevMove != 0) {
                                int prevFrom = prevMove >>> 14;
                                int prevTo = (prevMove >>> 7) & 0x3F;
                                int prevColor = 1 - color;
                                countermoves[prevColor][prevFrom][prevTo] = move;
                            }

                            // History malus: penalize all previously searched quiet moves
                            for (int j = 0; j < i; j++) {
                                int prev = moves.get(j);
                                if (prev != excludedMove && !isCapture(prev, board)) {
                                    int pf = prev >>> 14;
                                    int pt = (prev >>> 7) & 0x3F;
                                    history[color][pf][pt] = Math.max(
                                            history[color][pf][pt] - depth * depth, -HISTORY_MAX);
                                }
                            }
                        }
                        storeTT(hash, depth, beta, TT_BETA, bestMove);
                        return beta;
                    }
                }
            }
        }

        storeTT(hash, depth, bestScore, flag, bestMove);
        return bestScore;
    }

    /**
     * Quiescence search: only considers captures (and check evasions) to
     * avoid the horizon effect. Uses stand-pat score as the lower bound.
     * Includes delta pruning and SEE pruning of losing captures.
     */
    private int quiescence(Bitboard board, int alpha, int beta, int color, int ply) {
        if ((nodesSearched & 4095) == 0) checkTime();
        if (stopped) return 0;

        nodesSearched++;

        // TT probe in quiescence
        long hash = board.getHash();
        int ttIndex = (int) (hash & TT_MASK);
        if (ttKey[ttIndex] == hash && ttDepth[ttIndex] >= 0) {
            int ttVal = ttScore[ttIndex];
            byte ttF = ttFlag[ttIndex];
            if (ttF == TT_EXACT) return ttVal;
            if (ttF == TT_BETA && ttVal >= beta) return beta;
            if (ttF == TT_ALPHA && ttVal <= alpha) return alpha;
        }

        // Stand pat: static evaluation as a baseline
        int standPat = Evaluation.evaluate(board);

        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        // Check if in check — if so, generate all moves (must escape)
        long kingBB = board.kingPiece(color);
        if (kingBB == 0) return -MATE_SCORE + ply;
        int kingSquare = Long.numberOfTrailingZeros(kingBB);
        boolean inCheck = Generator.isKingInCheck(board, color, kingSquare);

        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(color);

        if (moves.isEmpty()) {
            if (inCheck) return -MATE_SCORE + ply;
            return 0;
        }

        // Filter to captures (and promotions) unless in check
        List<Integer> captureMoves;
        if (inCheck) {
            captureMoves = moves;
        } else {
            captureMoves = new ArrayList<>();
            for (int m : moves) {
                if (isCapture(m, board) || (m & 0xF) > 0) {
                    captureMoves.add(m);
                }
            }
        }

        // Score and use lazy picking
        int[] moveScores = scoreMoves(captureMoves, board, 0, ply, color, 0);

        for (int i = 0; i < captureMoves.size(); i++) {
            pickMove(captureMoves, moveScores, i);
            int move = captureMoves.get(i);

            // SEE pruning: skip losing captures in quiescence (unless in check)
            if (!inCheck && isCapture(move, board) && SEE.isLosingCapture(board, move)) {
                continue;
            }

            // Delta pruning: skip captures where material gain + margin can't raise alpha
            if (!inCheck && isCapture(move, board)) {
                int to = (move >>> 7) & 0x3F;
                int capturedPiece = board.getPiece(to);
                int capturedValue;
                if (capturedPiece != -1) {
                    capturedValue = Evaluation.MG_PIECE_VALUE[capturedPiece % 6];
                } else {
                    capturedValue = Evaluation.MG_PIECE_VALUE[pawn]; // en passant
                }
                if (standPat + capturedValue + DELTA_MARGIN < alpha) {
                    continue;
                }
            }

            Bitboard copy = board.copy();
            copy.makeMove(move);

            int score = -quiescence(copy, -beta, -alpha, 1 - color, ply + 1);

            if (stopped) return 0;

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }

        return alpha;
    }

    // ===== Move Ordering =====

    /**
     * Scores each move for ordering. Higher score = searched first.
     */
    private int[] scoreMoves(List<Integer> moves, Bitboard board,
                             int ttBestMove, int ply, int color, int prevMove) {
        int[] scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);

            // TT move gets highest priority
            if (move == ttBestMove && ttBestMove != 0) {
                scores[i] = TT_MOVE_SCORE;
                continue;
            }

            int promotionPiece = move & 0xF;
            int to = (move >>> 7) & 0x3F;
            int captured = board.getPiece(to);

            // Queen promotion: very high priority
            if (promotionPiece == queen) {
                scores[i] = PROMOTION_SCORE;
                continue;
            }
            // Under-promotions: still score reasonably
            if (promotionPiece > 0) {
                scores[i] = PROMOTION_SCORE - 100_000;
                continue;
            }

            if (captured != -1 || ((move >>> 4) & 0x7) == MoveType.EN_PASSANT.ordinal()) {
                // Capture: use SEE for accurate exchange evaluation
                int seeScore = SEE.see(board, move);
                if (seeScore >= 0) {
                    scores[i] = CAPTURE_BASE + seeScore;
                } else {
                    scores[i] = seeScore; // negative: searched last
                }
                continue;
            } else {
                // Killer moves
                if (ply < MAX_PLY) {
                    if (move == killers[ply][0]) {
                        scores[i] = KILLER_SCORE_0;
                        continue;
                    }
                    if (move == killers[ply][1]) {
                        scores[i] = KILLER_SCORE_1;
                        continue;
                    }
                }

                // Countermove bonus
                if (prevMove != 0) {
                    int prevFrom = prevMove >>> 14;
                    int prevTo = (prevMove >>> 7) & 0x3F;
                    int prevColor = 1 - color;
                    if (move == countermoves[prevColor][prevFrom][prevTo]) {
                        scores[i] = COUNTERMOVE_SCORE;
                        continue;
                    }
                }

                // History heuristic
                int from = move >>> 14;
                scores[i] = history[color][from][to];
            }
        }
        return scores;
    }

    /**
     * Lazy move picking: swap the best-scoring move into position i.
     * Only O(n) per call instead of O(n²) for full sort.
     */
    private void pickMove(List<Integer> moves, int[] scores, int i) {
        int bestIdx = i;
        for (int j = i + 1; j < moves.size(); j++) {
            if (scores[j] > scores[bestIdx]) bestIdx = j;
        }
        if (bestIdx != i) {
            int tmpMove = moves.get(i);
            moves.set(i, moves.get(bestIdx));
            moves.set(bestIdx, tmpMove);

            int tmpScore = scores[i];
            scores[i] = scores[bestIdx];
            scores[bestIdx] = tmpScore;
        }
    }

    // ===== Transposition Table =====

    private void storeTT(long hash, int depth, int score, byte flag, int move) {
        int index = (int) (hash & TT_MASK);
        if (depth >= ttDepth[index] || ttKey[index] == hash) {
            ttKey[index] = hash;
            ttMove[index] = move;
            ttScore[index] = (short) Math.max(-32000, Math.min(32000, score));
            ttDepth[index] = (byte) depth;
            ttFlag[index] = flag;
        }
    }

    private int probeTTMove(long hash) {
        int index = (int) (hash & TT_MASK);
        if (ttKey[index] == hash) return ttMove[index];
        return 0;
    }

    // ===== PV Extraction =====

    private String extractPV(Bitboard board, int maxLength) {
        StringBuilder sb = new StringBuilder();
        Bitboard copy = board.copy();
        long[] seenHashes = new long[maxLength];
        int pvLength = 0;

        for (int i = 0; i < maxLength; i++) {
            long hash = copy.getHash();

            for (int j = 0; j < pvLength; j++) {
                if (seenHashes[j] == hash) return sb.toString().trim();
            }
            seenHashes[pvLength++] = hash;

            int ttIdx = (int) (hash & TT_MASK);
            if (ttKey[ttIdx] != hash || ttMove[ttIdx] == 0) break;

            int move = ttMove[ttIdx];
            Move decoded = Move.createMove(move, copy);
            if (sb.length() > 0) sb.append(' ');
            sb.append(decoded.toAlgebraic());

            copy.makeMove(move);
        }

        return sb.toString().trim();
    }

    // ===== Helpers =====

    private boolean isCapture(int move, Bitboard board) {
        int to = (move >>> 7) & 0x3F;
        if (board.getPiece(to) != -1) return true;
        int moveType = (move >>> 4) & 0x7;
        return moveType == MoveType.EN_PASSANT.ordinal();
    }

    private void updateKillers(int ply, int move) {
        if (ply >= MAX_PLY) return;
        if (killers[ply][0] != move) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = move;
        }
    }

    private void clearKillers() {
        for (int[] killer : killers) {
            killer[0] = 0;
            killer[1] = 0;
        }
    }

    private void clearHistory() {
        for (int[][] c : history)
            for (int[] from : c)
                java.util.Arrays.fill(from, 0);
    }

    private void clearCountermoves() {
        for (int[][] c : countermoves)
            for (int[] from : c)
                java.util.Arrays.fill(from, 0);
    }

    private void checkTime() {
        if (System.currentTimeMillis() - startTime >= timeLimitMs) {
            stopped = true;
        }
    }

    private static boolean hasNonPawnMaterial(Bitboard board, int color) {
        return (Evaluation.getPieceBB(board, knight, color)
              | Evaluation.getPieceBB(board, bishop, color)
              | Evaluation.getPieceBB(board, rook, color)
              | Evaluation.getPieceBB(board, queen, color)) != 0;
    }

    private static boolean isMateScore(int score) {
        return Math.abs(score) > MATE_THRESHOLD;
    }

    private static int mateInMoves(int score) {
        int plies = MATE_SCORE - Math.abs(score);
        int moves = (plies + 1) / 2;
        return score > 0 ? moves : -moves;
    }
}

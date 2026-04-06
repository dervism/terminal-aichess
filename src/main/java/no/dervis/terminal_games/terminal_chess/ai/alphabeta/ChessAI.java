package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.util.List;
import java.util.Random;

/**
 * Advanced chess AI using iterative deepening with alpha-beta pruning.
 *
 * <h3>Search features</h3>
 * <ul>
 *   <li>Iterative deepening with aspiration windows</li>
 *   <li>Negamax alpha-beta pruning</li>
 *   <li>Principal Variation Search (PVS)</li>
 *   <li>Quiescence search (captures + check evasions)</li>
 *   <li>Transposition table with Zobrist hashing</li>
 *   <li>Check extensions</li>
 *   <li>Late Move Reductions (LMR)</li>
 *   <li>Move ordering: TT move → MVV-LVA captures → killer moves → history heuristic</li>
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
    private static final int CAPTURE_BASE     =  1_000_000;
    private static final int KILLER_SCORE_0   =    900_000;
    private static final int KILLER_SCORE_1   =    800_000;

    // MVV-LVA table: [victim][attacker] -> ordering score
    // Higher victim value and lower attacker value = higher score
    private static final int[][] MVV_LVA = new int[6][6];
    static {
        int[] values = {1, 3, 3, 5, 9, 10}; // relative piece ordering weights
        for (int victim = 0; victim < 6; victim++) {
            for (int attacker = 0; attacker < 6; attacker++) {
                MVV_LVA[victim][attacker] = values[victim] * 10 - values[attacker];
            }
        }
    }

    // ===== Transposition Table =====
    private static final int TT_SIZE = 1 << 20; // ~1M entries
    private static final int TT_MASK = TT_SIZE - 1;

    private final long[] ttKey   = new long[TT_SIZE];
    private final int[]  ttMove  = new int[TT_SIZE];
    private final short[] ttScore = new short[TT_SIZE];
    private final byte[] ttDepth = new byte[TT_SIZE];
    private final byte[] ttFlag  = new byte[TT_SIZE];

    // ===== Zobrist Hashing =====
    // [piece_index 0..11][square 0..63]
    private static final long[][] ZOBRIST_PIECE = new long[12][64];
    private static final long ZOBRIST_SIDE;
    private static final long[] ZOBRIST_CASTLING = new long[16];

    static {
        Random rng = new Random(0xDEADBEEFL);
        for (int p = 0; p < 12; p++)
            for (int s = 0; s < 64; s++)
                ZOBRIST_PIECE[p][s] = rng.nextLong();
        ZOBRIST_SIDE = rng.nextLong();
        for (int c = 0; c < 16; c++)
            ZOBRIST_CASTLING[c] = rng.nextLong();
    }

    // ===== Search State =====
    private final int[][] killers = new int[MAX_PLY][2];
    private final int[][][] history = new int[2][64][64];

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

        int color = board.turn();
        Generator gen = new Generator(board);
        List<Integer> rootMoves = gen.generateMoves(color);

        if (rootMoves.isEmpty()) return 0;
        if (rootMoves.size() == 1) return rootMoves.getFirst();

        bestMoveRoot = rootMoves.getFirst();
        int bestScore = -INFINITY;
        int completedDepth = 0;

        // Iterative deepening
        for (int depth = 1; depth <= MAX_PLY; depth++) {
            int score = searchRoot(board, rootMoves, depth, color);

            if (stopped) break;

            bestScore = score;
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
    private int searchRoot(Bitboard board, List<Integer> moves, int depth, int color) {
        long hash = computeHash(board);
        int ttMove = probeTTMove(hash);

        // Score and sort moves
        int[] scores = scoreMoves(moves, board, ttMove, 0, color);
        sortMoves(moves, scores);

        int alpha = -INFINITY;
        int beta = INFINITY;
        int bestMove = moves.getFirst();
        int bestScore = -INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            Bitboard copy = board.copy();
            copy.makeMove(move);

            int score;
            if (i == 0) {
                score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true);
            } else {
                // PVS: null-window search for non-first moves
                score = -alphaBeta(copy, depth - 1, -alpha - 1, -alpha, 1 - color, 1, false);
                if (score > alpha && score < beta) {
                    score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true);
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
     * Negamax alpha-beta search with PVS, LMR, check extensions, and TT.
     */
    private int alphaBeta(Bitboard board, int depth, int alpha, int beta,
                          int color, int ply, boolean pvNode) {
        // Time check every 4096 nodes
        if ((nodesSearched & 4095) == 0) checkTime();
        if (stopped) return 0;

        nodesSearched++;

        // Transposition table probe
        long hash = computeHash(board);
        int ttHitMove = 0;
        int ttIndex = (int) (hash & TT_MASK);
        if (ttKey[ttIndex] == hash) {
            ttHitMove = ttMove[ttIndex];
            if (ttDepth[ttIndex] >= depth && !pvNode) {
                int ttVal = ttScore[ttIndex];
                byte flag = ttFlag[ttIndex];
                if (flag == TT_EXACT) return ttVal;
                if (flag == TT_ALPHA && ttVal <= alpha) return alpha;
                if (flag == TT_BETA && ttVal >= beta) return beta;
            }
        }

        // Check extension: extend search when in check
        long kingBB = board.kingPiece(color);
        if (kingBB == 0) return -MATE_SCORE + ply; // king captured (shouldn't happen)
        int kingSquare = Long.numberOfTrailingZeros(kingBB);
        boolean inCheck = Generator.isKingInCheck(board, color, kingSquare);
        if (inCheck) depth++;

        // Leaf node → quiescence search
        if (depth <= 0) return quiescence(board, alpha, beta, color, ply);

        // Generate legal moves
        Generator gen = new Generator(board);
        List<Integer> moves = gen.generateMoves(color);

        // Terminal node detection
        if (moves.isEmpty()) {
            if (inCheck) return -MATE_SCORE + ply; // checkmate (penalize distance)
            return 0; // stalemate
        }

        // Score and sort moves
        int[] moveScores = scoreMoves(moves, board, ttHitMove, ply, color);
        sortMoves(moves, moveScores);

        int bestScore = -INFINITY;
        int bestMove = moves.getFirst();
        byte flag = TT_ALPHA;

        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            Bitboard copy = board.copy();
            copy.makeMove(move);

            boolean isCapture = isCapture(move, board);

            int score;
            int newDepth = depth - 1;

            // Late Move Reductions (LMR)
            // Reduce non-PV, non-capture, non-check, later moves
            boolean doLMR = !pvNode && i >= 3 && depth >= 3 && !isCapture && !inCheck;

            if (doLMR) {
                // Reduced-depth null-window search
                int reduction = 1 + (i >= 6 ? 1 : 0);
                score = -alphaBeta(copy, newDepth - reduction, -alpha - 1, -alpha,
                        1 - color, ply + 1, false);

                // Re-search at full depth if it fails high
                if (score > alpha) {
                    score = -alphaBeta(copy, newDepth, -beta, -alpha,
                            1 - color, ply + 1, pvNode);
                }
            } else if (!pvNode || i > 0) {
                // PVS: null-window search for non-PV nodes or non-first moves
                score = -alphaBeta(copy, newDepth, -alpha - 1, -alpha,
                        1 - color, ply + 1, false);
                if (score > alpha && score < beta) {
                    score = -alphaBeta(copy, newDepth, -beta, -alpha,
                            1 - color, ply + 1, true);
                }
            } else {
                // Full window search for the first move in PV nodes
                score = -alphaBeta(copy, newDepth, -beta, -alpha,
                        1 - color, ply + 1, true);
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
                            history[color][from][to] += depth * depth;
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
     */
    private int quiescence(Bitboard board, int alpha, int beta, int color, int ply) {
        if ((nodesSearched & 4095) == 0) checkTime();
        if (stopped) return 0;

        nodesSearched++;

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

        // Filter to captures only (unless in check — then consider all evasions)
        if (!inCheck) {
            moves = moves.stream()
                    .filter(m -> isCapture(m, board))
                    .toList();
        }

        // Score and sort captures by MVV-LVA
        int[] moveScores = scoreMoves(moves, board, 0, ply, color);
        // Use a mutable copy for sorting
        var mutableMoves = new java.util.ArrayList<>(moves);
        sortMoves(mutableMoves, moveScores);

        for (int move : mutableMoves) {
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
                             int ttBestMove, int ply, int color) {
        int[] scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);

            // TT move gets highest priority
            if (move == ttBestMove && ttBestMove != 0) {
                scores[i] = TT_MOVE_SCORE;
                continue;
            }

            int to = (move >>> 7) & 0x3F;
            int captured = board.getPiece(to);

            if (captured != -1) {
                // Capture: MVV-LVA
                int from = move >>> 14;
                int attacker = board.getPiece(from) % 6;
                int victim = captured % 6;
                scores[i] = CAPTURE_BASE + MVV_LVA[victim][attacker];
            } else {
                // En passant is also a capture
                int moveType = (move >>> 4) & 0x7;
                if (moveType == MoveType.EN_PASSANT.ordinal()) {
                    scores[i] = CAPTURE_BASE + MVV_LVA[pawn][pawn];
                    continue;
                }

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

                // History heuristic
                int from = move >>> 14;
                scores[i] = history[color][from][to];
            }
        }
        return scores;
    }

    /**
     * Partial selection sort: picks the best-scoring move to the front on each pass.
     * Efficient because alpha-beta cutoffs mean we rarely examine all moves.
     */
    private void sortMoves(List<Integer> moves, int[] scores) {
        for (int i = 0; i < moves.size() - 1; i++) {
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
    }

    // ===== Transposition Table =====

    private void storeTT(long hash, int depth, int score, byte flag, int move) {
        int index = (int) (hash & TT_MASK);
        // Replace if new entry is deeper or same position
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

    // ===== Zobrist Hashing =====

    private long computeHash(Bitboard board) {
        long hash = 0L;

        for (int color = 0; color < 2; color++) {
            for (int pt = 0; pt < 6; pt++) {
                long bb = Evaluation.getPieceBB(board, pt, color);
                while (bb != 0) {
                    int sq = Long.numberOfTrailingZeros(bb);
                    hash ^= ZOBRIST_PIECE[color * 6 + pt][sq];
                    bb &= bb - 1;
                }
            }
        }

        if (board.turn() == black) hash ^= ZOBRIST_SIDE;
        hash ^= ZOBRIST_CASTLING[board.castlingRights()];

        return hash;
    }

    // ===== PV Extraction =====

    /**
     * Extracts the principal variation by walking the transposition table.
     * Returns a string in standard algebraic notation, e.g. "e4 e5 Nf3 Nc6 Bc4 Bc5".
     */
    private String extractPV(Bitboard board, int maxLength) {
        StringBuilder sb = new StringBuilder();
        Bitboard copy = board.copy();
        long[] seenHashes = new long[maxLength];
        int pvLength = 0;

        for (int i = 0; i < maxLength; i++) {
            long hash = computeHash(copy);

            // Cycle detection
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
        for (int[][] color : history)
            for (int[] from : color)
                java.util.Arrays.fill(from, 0);
    }

    private void checkTime() {
        if (System.currentTimeMillis() - startTime >= timeLimitMs) {
            stopped = true;
        }
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

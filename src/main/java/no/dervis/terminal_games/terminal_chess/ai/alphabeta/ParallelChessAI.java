package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Improved parallel chess AI using Lazy SMP with better thread utilisation.
 *
 * <p>Key features:</p>
 * <ul>
 *   <li><b>Depth diversity</b> — threads start at staggered depths (1, 2, 3, 1, 2, 3, ...)
 *       so they explore different parts of the search tree sooner.</li>
 *   <li><b>Root move shuffling</b> — helper threads shuffle their root move list so
 *       different threads prioritise different candidate moves, reducing redundant work.</li>
 *   <li><b>Independent time management</b> — each thread independently decides when to stop
 *       starting new depth iterations, rather than thread 0 killing everyone via the shared
 *       stop flag. The hard time limit still stops all threads.</li>
 * </ul>
 */
public class ParallelChessAI implements Chess, Engine {

    // ===== Constants =====
    private static final int MAX_PLY = 128;
    private static final int INFINITY = 50_000;
    private static final int MATE_SCORE = 49_000;
    private static final int MATE_THRESHOLD = 48_000;

    // Transposition table flags
    private static final byte TT_EXACT = 0;
    private static final byte TT_ALPHA = 1;
    private static final byte TT_BETA = 2;

    // Move ordering score thresholds
    private static final int TT_MOVE_SCORE    = 10_000_000;
    private static final int CAPTURE_BASE     =  1_000_000;
    private static final int KILLER_SCORE_0   =    900_000;
    private static final int KILLER_SCORE_1   =    800_000;

    // MVV-LVA table
    private static final int[][] MVV_LVA = new int[6][6];
    static {
        int[] values = {1, 3, 3, 5, 9, 10};
        for (int victim = 0; victim < 6; victim++)
            for (int attacker = 0; attacker < 6; attacker++)
                MVV_LVA[victim][attacker] = values[victim] * 10 - values[attacker];
    }

    // Depth diversity table: maps threadId to a starting depth offset.
    // Threads cycle through offsets 0, 1, 2 so they begin searching at
    // different depths, reducing redundant work in the shared TT.
    private static final int[] DEPTH_OFFSETS = {0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0};

    // ===== Shared Transposition Table =====
    private static final int TT_SIZE = 1 << 20;
    private static final int TT_MASK = TT_SIZE - 1;

    private final long[]  ttKey   = new long[TT_SIZE];
    private final int[]   ttMove  = new int[TT_SIZE];
    private final short[] ttScore = new short[TT_SIZE];
    private final byte[]  ttDepth = new byte[TT_SIZE];
    private final byte[]  ttFlag  = new byte[TT_SIZE];

    // ===== Zobrist Hashing (static, read-only) =====
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

    // ===== Shared Search Control =====
    private volatile boolean stopped;
    private long startTime;
    private long timeLimitMs;

    private final int numThreads;
    private SearchWorker[] workers;

    private final boolean verbose;

    // ===== Constructors =====

    public ParallelChessAI() {
        this(Runtime.getRuntime().availableProcessors(), true);
    }

    public ParallelChessAI(boolean verbose) {
        this(Runtime.getRuntime().availableProcessors(), verbose);
    }

    public ParallelChessAI(int numThreads) {
        this(numThreads, true);
    }

    public ParallelChessAI(int numThreads, boolean verbose) {
        this.numThreads = Math.max(1, numThreads);
        this.verbose = verbose;
    }

    // ===== Public API =====

    public int findBestMove(Bitboard board, long timeLimitMs) {
        this.startTime = System.currentTimeMillis();
        this.timeLimitMs = timeLimitMs;
        this.stopped = false;

        int color = board.turn();
        Generator gen = new Generator(board);
        List<Integer> rootMoves = gen.generateMoves(color);

        if (rootMoves.isEmpty()) return 0;
        if (rootMoves.size() == 1) return rootMoves.getFirst();

        // Launch worker threads
        workers = new SearchWorker[numThreads];
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            // Each helper thread gets a shuffled copy of root moves
            List<Integer> threadMoves = new ArrayList<>(rootMoves);
            if (i > 0) {
                Collections.shuffle(threadMoves, new Random(i * 7919L));
            }
            workers[i] = new SearchWorker(board.copy(), threadMoves, i);
            threads[i] = new Thread(workers[i], "ismp-" + i);
            threads[i].setDaemon(true);
        }

        for (Thread t : threads) t.start();

        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        // Collect result: prefer the worker that completed the deepest search
        int bestMove = rootMoves.getFirst();
        int bestDepth = 0;
        int bestScore = -INFINITY;
        long totalNodes = 0;

        for (SearchWorker w : workers) {
            totalNodes += w.nodesSearched;
            if (w.completedDepth > bestDepth ||
                (w.completedDepth == bestDepth && w.bestScore > bestScore)) {
                bestDepth = w.completedDepth;
                bestScore = w.bestScore;
                bestMove = w.bestMove;
            }
        }

        if (verbose) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("[ParallelAI] info threads %d depth %d total nodes %,d time %dms%n",
                numThreads, bestDepth, totalNodes, elapsed);
        }

        return bestMove;
    }

    // ===== Shared TT Access =====

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

    private String extractPV(Bitboard board, int maxLength) {
        StringBuilder sb = new StringBuilder();
        Bitboard copy = board.copy();
        long[] seenHashes = new long[maxLength];
        int pvLength = 0;

        for (int i = 0; i < maxLength; i++) {
            long hash = computeHash(copy);
            for (int j = 0; j < pvLength; j++) {
                if (seenHashes[j] == hash) return sb.toString().trim();
            }
            seenHashes[pvLength++] = hash;

            int ttIdx = (int) (hash & TT_MASK);
            if (ttKey[ttIdx] != hash || ttMove[ttIdx] == 0) break;

            int move = ttMove[ttIdx];
            Move decoded = Move.createMove(move, copy);
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(decoded.toAlgebraic());
            copy.makeMove(move);
        }
        return sb.toString().trim();
    }

    // ===== Helpers =====

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

    // =========================================================================
    // SearchWorker — one per thread, owns per-thread search state
    // =========================================================================

    private class SearchWorker implements Runnable {

        private final Bitboard board;
        private final List<Integer> rootMoves;
        private final int threadId;

        // Per-thread state
        private final int[][] killers = new int[MAX_PLY][2];
        private final int[][][] history = new int[2][64][64];
        int nodesSearched;
        int bestMove;
        int bestScore = -INFINITY;
        int completedDepth;

        SearchWorker(Bitboard board, List<Integer> rootMoves, int threadId) {
            this.board = board;
            this.rootMoves = rootMoves;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            int color = board.turn();
            bestMove = rootMoves.getFirst();

            // Depth diversity: threads start at staggered depths so they
            // populate the shared TT with entries from different depth levels.
            int offset = DEPTH_OFFSETS[threadId % DEPTH_OFFSETS.length];
            int startDepth = 1 + offset;

            for (int depth = startDepth; depth <= MAX_PLY; depth++) {
                int score = searchRoot(depth, color);

                if (stopped) break;

                bestScore = score;
                completedDepth = depth;

                // Only thread 0 prints progress
                if (threadId == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long totalNodes = 0;
                    for (SearchWorker w : workers) totalNodes += w.nodesSearched;
                    long nps = elapsed > 0 ? (totalNodes * 1000) / elapsed : 0;

                    String scoreStr = isMateScore(score)
                        ? "mate " + mateInMoves(score)
                        : "cp " + score;
                    String pvLine = extractPV(board, depth);
                    System.out.printf("\r[ParallelAI] info depth %d score %s nodes %d nps %d time %dms pv %s          ",
                            depth, scoreStr, totalNodes, nps, elapsed, pvLine);
                    System.out.flush();
                }

                // Each thread independently decides whether to start a new depth.
                // This prevents thread 0 from killing a helper that is about to
                // complete a deeper search.
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > timeLimitMs / 2) break;

                if (Math.abs(score) > MATE_THRESHOLD) break;
            }

            if (threadId == 0 && verbose) System.out.println();
        }

        // ----- Root Search -----

        private int searchRoot(int depth, int color) {
            long hash = computeHash(board);
            int ttBestMove = probeTTMove(hash);

            // Thread 0 uses TT-informed move ordering.
            // Helper threads already have shuffled root moves; they still
            // benefit from TT/killer/history scoring within the shuffle.
            int[] scores = scoreMoves(rootMoves, board, ttBestMove, 0, color);
            sortMoves(rootMoves, scores);

            int alpha = -INFINITY;
            int beta = INFINITY;
            int localBest = rootMoves.getFirst();
            int localBestScore = -INFINITY;

            for (int i = 0; i < rootMoves.size(); i++) {
                int move = rootMoves.get(i);
                Bitboard copy = board.copy();
                copy.makeMove(move);

                int score;
                if (i == 0) {
                    score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true);
                } else {
                    score = -alphaBeta(copy, depth - 1, -alpha - 1, -alpha, 1 - color, 1, false);
                    if (score > alpha && score < beta) {
                        score = -alphaBeta(copy, depth - 1, -beta, -alpha, 1 - color, 1, true);
                    }
                }

                if (stopped) return localBestScore;

                if (score > localBestScore) {
                    localBestScore = score;
                    localBest = move;
                    if (score > alpha) alpha = score;
                }
            }

            bestMove = localBest;
            storeTT(hash, depth, localBestScore, TT_EXACT, localBest);
            return localBestScore;
        }

        // ----- Alpha-Beta -----

        private int alphaBeta(Bitboard board, int depth, int alpha, int beta,
                              int color, int ply, boolean pvNode) {
            if ((nodesSearched & 4095) == 0) checkTime();
            if (stopped) return 0;

            nodesSearched++;

            // TT probe
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

            // Check extension
            long kingBB = board.kingPiece(color);
            if (kingBB == 0) return -MATE_SCORE + ply;
            int kingSquare = Long.numberOfTrailingZeros(kingBB);
            boolean inCheck = Generator.isKingInCheck(board, color, kingSquare);
            if (inCheck) depth++;

            if (depth <= 0) return quiescence(board, alpha, beta, color, ply);

            // Null-move pruning
            if (!pvNode && !inCheck && depth >= 3 && hasNonPawnMaterial(board, color)) {
                int R = 2 + (depth >= 6 ? 1 : 0);
                Bitboard nullCopy = board.copy();
                nullCopy.makeNullMove();
                int nullScore = -alphaBeta(nullCopy, depth - 1 - R, -beta, -beta + 1,
                        1 - color, ply + 1, false);
                if (stopped) return 0;
                if (nullScore >= beta) {
                    if (nullScore > MATE_THRESHOLD) nullScore = beta;
                    return nullScore;
                }
            }

            Generator gen = new Generator(board);
            List<Integer> moves = gen.generateMoves(color);

            if (moves.isEmpty()) {
                if (inCheck) return -MATE_SCORE + ply;
                return 0;
            }

            int[] moveScores = scoreMoves(moves, board, ttHitMove, ply, color);
            sortMoves(moves, moveScores);

            int bestScoreLocal = -INFINITY;
            int bestMoveLocal = moves.getFirst();
            byte flag = TT_ALPHA;

            for (int i = 0; i < moves.size(); i++) {
                int move = moves.get(i);
                Bitboard copy = board.copy();
                copy.makeMove(move);

                boolean isCapture = isCapture(move, board);

                int score;
                int newDepth = depth - 1;

                // LMR
                boolean doLMR = !pvNode && i >= 3 && depth >= 3 && !isCapture && !inCheck;

                if (doLMR) {
                    int reduction = 1 + (i >= 6 ? 1 : 0);
                    score = -alphaBeta(copy, newDepth - reduction, -alpha - 1, -alpha,
                            1 - color, ply + 1, false);
                    if (score > alpha) {
                        score = -alphaBeta(copy, newDepth, -beta, -alpha,
                                1 - color, ply + 1, pvNode);
                    }
                } else if (!pvNode || i > 0) {
                    score = -alphaBeta(copy, newDepth, -alpha - 1, -alpha,
                            1 - color, ply + 1, false);
                    if (score > alpha && score < beta) {
                        score = -alphaBeta(copy, newDepth, -beta, -alpha,
                                1 - color, ply + 1, true);
                    }
                } else {
                    score = -alphaBeta(copy, newDepth, -beta, -alpha,
                            1 - color, ply + 1, true);
                }

                if (stopped) return 0;

                if (score > bestScoreLocal) {
                    bestScoreLocal = score;
                    bestMoveLocal = move;

                    if (score > alpha) {
                        alpha = score;
                        flag = TT_EXACT;

                        if (score >= beta) {
                            if (!isCapture) {
                                updateKillers(ply, move);
                                int from = move >>> 14;
                                int to = (move >>> 7) & 0x3F;
                                history[color][from][to] += depth * depth;
                            }
                            storeTT(hash, depth, beta, TT_BETA, bestMoveLocal);
                            return beta;
                        }
                    }
                }
            }

            storeTT(hash, depth, bestScoreLocal, flag, bestMoveLocal);
            return bestScoreLocal;
        }

        // ----- Quiescence -----

        private int quiescence(Bitboard board, int alpha, int beta, int color, int ply) {
            if ((nodesSearched & 4095) == 0) checkTime();
            if (stopped) return 0;

            nodesSearched++;

            int standPat = Evaluation.evaluate(board);
            if (standPat >= beta) return beta;
            if (standPat > alpha) alpha = standPat;

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

            if (!inCheck) {
                moves = moves.stream().filter(m -> isCapture(m, board)).toList();
            }

            int[] moveScores = scoreMoves(moves, board, 0, ply, color);
            var mutableMoves = new ArrayList<>(moves);
            sortMoves(mutableMoves, moveScores);

            for (int move : mutableMoves) {
                // SEE pruning: skip losing captures in quiescence (unless in check)
                if (!inCheck && isCapture(move, board) && SEE.isLosingCapture(board, move)) {
                    continue;
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

        // ----- Move Ordering -----

        private int[] scoreMoves(List<Integer> moves, Bitboard board,
                                 int ttBestMove, int ply, int color) {
            int[] scores = new int[moves.size()];
            for (int i = 0; i < moves.size(); i++) {
                int move = moves.get(i);

                if (move == ttBestMove && ttBestMove != 0) {
                    scores[i] = TT_MOVE_SCORE;
                    continue;
                }

                int to = (move >>> 7) & 0x3F;
                int captured = board.getPiece(to);

                if (captured != -1 || ((move >>> 4) & 0x7) == MoveType.EN_PASSANT.ordinal()) {
                    int seeScore = SEE.see(board, move);
                    if (seeScore >= 0) {
                        scores[i] = CAPTURE_BASE + seeScore;
                    } else {
                        scores[i] = seeScore;
                    }
                    continue;
                } else {
                    if (ply < MAX_PLY) {
                        if (move == killers[ply][0]) { scores[i] = KILLER_SCORE_0; continue; }
                        if (move == killers[ply][1]) { scores[i] = KILLER_SCORE_1; continue; }
                    }

                    int from = move >>> 14;
                    scores[i] = history[color][from][to];
                }
            }
            return scores;
        }

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

        // ----- Helpers -----

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

        private void checkTime() {
            if (System.currentTimeMillis() - startTime >= timeLimitMs) {
                stopped = true;
            }
        }
    }
}

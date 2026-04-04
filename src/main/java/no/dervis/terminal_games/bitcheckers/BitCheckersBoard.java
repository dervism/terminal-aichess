package no.dervis.terminal_games.bitcheckers;

import no.dervis.terminal_games.game_ai.AlphaBetaSearch;
import no.dervis.terminal_games.game_ai.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Bitboard-based checkers board. The full 8x8 board is represented using three
 * {@code long} bitboards: black pieces, white pieces, and kings (both colours).
 * Only dark squares are populated (where (row+col) is odd).
 *
 * <p>Square indexing follows the chess convention: a1=0, b1=1, ..., h8=63.
 * Black starts on rows 0-2 and moves up (increasing index);
 * white starts on rows 5-7 and moves down.</p>
 */
public class BitCheckersBoard implements GameState<CheckersMove> {

    public static final int BLACK = 0;
    public static final int WHITE = 1;

    static final long FILE_A = 0x0101010101010101L;
    static final long FILE_H = 0x8080808080808080L;
    static final long RANK_1 = 0x00000000000000FFL;
    static final long RANK_8 = 0xFF00000000000000L;
    static final long DARK_SQUARES = 0x55AA55AA55AA55AAL;

    private static final int[] FWD_BLACK = {7, 9};
    private static final int[] FWD_WHITE = {-7, -9};
    private static final int[] ALL_DIRS = {7, 9, -7, -9};

    private long black;
    private long white;
    private long kings;
    private int turn;

    public void initialise() {
        long rows012 = RANK_1 | (RANK_1 << 8) | (RANK_1 << 16);
        long rows567 = (RANK_1 << 40) | (RANK_1 << 48) | (RANK_1 << 56);
        black = DARK_SQUARES & rows012;
        white = DARK_SQUARES & rows567;
        kings = 0L;
        turn = BLACK;
    }

    // ---- Accessors ----

    public long black() { return black; }
    public long white() { return white; }
    public long kings() { return kings; }
    public int turn() { return turn; }
    public long currentPlayer() { return turn == BLACK ? black : white; }
    public long opponent() { return turn == BLACK ? white : black; }

    public int blackMenCount()   { return Long.bitCount(black & ~kings); }
    public int blackKingCount()  { return Long.bitCount(black & kings); }
    public int whiteMenCount()   { return Long.bitCount(white & ~kings); }
    public int whiteKingCount()  { return Long.bitCount(white & kings); }
    public int blackPieceCount() { return Long.bitCount(black); }
    public int whitePieceCount() { return Long.bitCount(white); }

    // ---- Mutators for test setup ----

    public void setPiece(int color, boolean isKing, int square) {
        long bb = 1L << square;
        if (color == BLACK) black |= bb; else white |= bb;
        if (isKing) kings |= bb;
    }

    public void setTurn(int color) { this.turn = color; }

    public BitCheckersBoard copy() {
        BitCheckersBoard c = new BitCheckersBoard();
        c.black = black; c.white = white; c.kings = kings; c.turn = turn;
        return c;
    }

    // ---- GameState implementation ----

    @Override
    public List<CheckersMove> generateMoves() {
        List<CheckersMove> jumps = generateJumps();
        if (!jumps.isEmpty()) return jumps;
        return generateSimpleMoves();
    }

    @Override
    public void makeMove(CheckersMove move) {
        long fromBB = 1L << move.from();
        long toBB   = 1L << move.to();

        if (turn == BLACK) {
            black = (black & ~fromBB) | toBB;
            white &= ~move.captured();
        } else {
            white = (white & ~fromBB) | toBB;
            black &= ~move.captured();
        }

        kings &= ~move.captured();
        if ((kings & fromBB) != 0) {
            kings = (kings & ~fromBB) | toBB;
        }

        // Promotion
        if ((kings & toBB) == 0) {
            if ((turn == BLACK && (toBB & RANK_8) != 0) ||
                (turn == WHITE && (toBB & RANK_1) != 0)) {
                kings |= toBB;
            }
        }

        turn = 1 - turn;
    }

    @Override
    public void unmakeMove(CheckersMove move) {
        turn = 1 - turn;
        long fromBB = 1L << move.from();
        long toBB   = 1L << move.to();

        if (turn == BLACK) {
            black = (black & ~toBB) | fromBB;
            white |= move.captured();
        } else {
            white = (white & ~toBB) | fromBB;
            black |= move.captured();
        }

        kings = move.prevKings();
    }

    @Override
    public boolean isTerminal() {
        if (black == 0 || white == 0) return true;
        return generateMoves().isEmpty();
    }

    @Override
    public int terminalScore() {
        if (currentPlayer() == 0) return -AlphaBetaSearch.WIN_SCORE;
        long opp = opponent();
        if (opp == 0) return AlphaBetaSearch.WIN_SCORE;
        return -AlphaBetaSearch.WIN_SCORE;
    }

    // ---- Simple (non-capture) move generation ----

    private List<CheckersMove> generateSimpleMoves() {
        List<CheckersMove> moves = new ArrayList<>();
        long myPieces = currentPlayer();
        long myMen    = myPieces & ~kings;
        long myKings  = myPieces & kings;
        long empty    = ~(black | white);

        if (turn == BLACK) {
            addShiftLeftMoves(moves, myMen,   empty, 7, FILE_H);
            addShiftLeftMoves(moves, myMen,   empty, 9, FILE_A);
            addShiftLeftMoves(moves, myKings, empty, 7, FILE_H);
            addShiftLeftMoves(moves, myKings, empty, 9, FILE_A);
            addShiftRightMoves(moves, myKings, empty, 9, FILE_H);
            addShiftRightMoves(moves, myKings, empty, 7, FILE_A);
        } else {
            addShiftRightMoves(moves, myMen,   empty, 9, FILE_H);
            addShiftRightMoves(moves, myMen,   empty, 7, FILE_A);
            addShiftRightMoves(moves, myKings, empty, 9, FILE_H);
            addShiftRightMoves(moves, myKings, empty, 7, FILE_A);
            addShiftLeftMoves(moves, myKings, empty, 7, FILE_H);
            addShiftLeftMoves(moves, myKings, empty, 9, FILE_A);
        }

        return moves;
    }

    private void addShiftLeftMoves(List<CheckersMove> moves, long pieces, long empty,
                                    int shift, long fileMask) {
        long targets = (pieces << shift) & ~fileMask & empty;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            moves.add(new CheckersMove(to - shift, to, 0L, kings));
            targets &= targets - 1;
        }
    }

    private void addShiftRightMoves(List<CheckersMove> moves, long pieces, long empty,
                                     int shift, long fileMask) {
        long targets = (pieces >>> shift) & ~fileMask & empty;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            moves.add(new CheckersMove(to + shift, to, 0L, kings));
            targets &= targets - 1;
        }
    }

    // ---- Jump (capture) move generation ----

    private List<CheckersMove> generateJumps() {
        List<CheckersMove> jumps = new ArrayList<>();
        long remaining = currentPlayer();

        while (remaining != 0) {
            int sq = Long.numberOfTrailingZeros(remaining);
            boolean isKing = (kings & (1L << sq)) != 0;
            generateJumpsFrom(sq, sq, isKing, 0L, jumps);
            remaining &= remaining - 1;
        }

        return jumps;
    }

    private void generateJumpsFrom(int startSq, int currentSq, boolean isKing,
                                    long capturedSoFar, List<CheckersMove> result) {
        int[] dirs = isKing ? ALL_DIRS : (turn == BLACK ? FWD_BLACK : FWD_WHITE);
        long myPieces  = currentPlayer();
        long oppPieces = opponent();
        boolean foundJump = false;

        for (int dir : dirs) {
            int midSq  = currentSq + dir;
            int landSq = currentSq + 2 * dir;

            if (landSq < 0 || landSq >= 64 || midSq < 0 || midSq >= 64) continue;
            if (Math.abs((currentSq % 8) - (midSq % 8)) != 1) continue;
            if (Math.abs((midSq % 8) - (landSq % 8)) != 1) continue;

            long midBB  = 1L << midSq;
            long landBB = 1L << landSq;

            if ((midBB & oppPieces & ~capturedSoFar) == 0) continue;

            long occupied = (myPieces | oppPieces) & ~capturedSoFar;
            occupied = (occupied & ~(1L << startSq)) | (1L << currentSq);
            if ((landBB & occupied) != 0) continue;

            foundJump = true;
            long newCaptured = capturedSoFar | midBB;

            // In American checkers, promotion stops the jump chain
            if (!isKing && isPromotionRank(landSq)) {
                result.add(new CheckersMove(startSq, landSq, newCaptured, kings));
            } else {
                generateJumpsFrom(startSq, landSq, isKing, newCaptured, result);
            }
        }

        if (!foundJump && capturedSoFar != 0) {
            result.add(new CheckersMove(startSq, currentSq, capturedSoFar, kings));
        }
    }

    private boolean isPromotionRank(int square) {
        int row = square / 8;
        return (turn == BLACK && row == 7) || (turn == WHITE && row == 0);
    }

    // ---- Display ----

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("    a   b   c   d   e   f   g   h\n");
        String sep = "  +---+---+---+---+---+---+---+---+\n";

        for (int row = 0; row < 8; row++) {
            sb.append(sep);
            sb.append(row + 1).append(' ');
            for (int col = 0; col < 8; col++) {
                long bb = 1L << (row * 8 + col);
                String piece;
                if ((black & bb) != 0)      piece = (kings & bb) != 0 ? "\u26C3" : "\u26C2";
                else if ((white & bb) != 0) piece = (kings & bb) != 0 ? "\u26C1" : "\u26C0";
                else                         piece = " ";
                sb.append("| ").append(piece).append(' ');
            }
            sb.append("|\n");
        }
        sb.append(sep);
        sb.append("    a   b   c   d   e   f   g   h\n");
        return sb.toString();
    }
}

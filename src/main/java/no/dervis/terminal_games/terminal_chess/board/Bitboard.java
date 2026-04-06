package no.dervis.terminal_games.terminal_chess.board;

import no.dervis.terminal_games.terminal_chess.moves.Move;

import java.util.LinkedList;
import java.util.Random;

public class Bitboard implements Board, Chess {

    // ===== Zobrist Hashing (shared by all engines) =====
    public static final long[][] ZOBRIST_PIECE = new long[12][64];
    public static final long ZOBRIST_SIDE;
    public static final long[] ZOBRIST_CASTLING = new long[16];
    public static final long[] ZOBRIST_EP_FILE = new long[8]; // en passant file

    static {
        Random rng = new Random(0xDEADBEEFL);
        for (int p = 0; p < 12; p++)
            for (int s = 0; s < 64; s++)
                ZOBRIST_PIECE[p][s] = rng.nextLong();
        ZOBRIST_SIDE = rng.nextLong();
        for (int c = 0; c < 16; c++)
            ZOBRIST_CASTLING[c] = rng.nextLong();
        for (int f = 0; f < 8; f++)
            ZOBRIST_EP_FILE[f] = rng.nextLong();
    }

    private long[] whitePieces;
    private long[] blackPieces;
    private LinkedList<Integer> history;
    private int lastMove; // cached for fast en passant detection in search copies
    private int castlingRights;
    private int colorToMove;
    private int halfMoveClock;
    private long zobristHash;

    public Bitboard() {
        whitePieces = new long[6];
        blackPieces = new long[6];
        history = new LinkedList<>();
        lastMove = 0;
        colorToMove = 0;
        castlingRights = 0b1111;
        halfMoveClock = 0;
        zobristHash = 0L;
    }

    /**
     * Fast copy for search: copies piece arrays and scalar state.
     * Shares the history list reference — only lastMove is needed in search.
     */
    public Bitboard copy() {
        Bitboard copy = new Bitboard();
        copy.whitePieces = whitePieces.clone();
        copy.blackPieces = blackPieces.clone();
        copy.history = history; // share reference — search only needs lastMove
        copy.lastMove = lastMove;
        copy.castlingRights = castlingRights;
        copy.colorToMove = colorToMove;
        copy.halfMoveClock = halfMoveClock;
        copy.zobristHash = zobristHash;
        return copy;
    }

    /**
     * Deep copy including full history. Use for game-level operations
     * where the full move history must be independent.
     */
    public Bitboard deepCopy() {
        Bitboard copy = copy();
        copy.history = new LinkedList<>(history);
        return copy;
    }

    public static Bitboard fromFEN(String fen) {
        Bitboard board = FEN.toBoard(fen);
        board.zobristHash = board.computeHashFromScratch();
        return board;
    }

    public String toFEN() {
        return FEN.fromBoard(this);
    }

    public void initialiseBoard() {
        // Initialize pawns
        whitePieces[0] = 0x000000000000FF00L;
        blackPieces[0] = 0x00FF000000000000L;

        // Initialize knights
        whitePieces[1] = 0x0000000000000042L;
        blackPieces[1] = 0x4200000000000000L;

        // Initialize bishops
        whitePieces[2] = 0x0000000000000024L;
        blackPieces[2] = 0x2400000000000000L;

        // Initialize rooks
        whitePieces[3] = 0x0000000000000081L;
        blackPieces[3] = 0x8100000000000000L;

        // Initialize queens
        whitePieces[4] = 0x0000000000000008L;
        blackPieces[4] = 0x0800000000000000L;

        // Initialize kings
        whitePieces[5] = 0x0000000000000010L;
        blackPieces[5] = 0x1000000000000000L;

        zobristHash = computeHashFromScratch();
    }

    public void setPiece(int pieceType, int color, int squareIndex) {
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        pieces[pieceType] |= bit;
    }

    public void removePiece(int pieceType, int color, int squareIndex) {
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        pieces[pieceType] &= ~bit;
    }

    public boolean hasPiece(int pieceType, int color, int squareIndex) {
        long bit = 1L << squareIndex;

        long[] pieces = color == 0 ? whitePieces : blackPieces;
        return (pieces[pieceType] & bit) != 0;
    }

    public int getPiece(int square) {
        long squareBitboard = 1L << square;

        for (int pieceType = 0; pieceType < 6; pieceType++) {
            if ((whitePieces[pieceType] & squareBitboard) != 0) {
                return pieceType; // Return piece type for white pieces (0-5)
            } else if ((blackPieces[pieceType] & squareBitboard) != 0) {
                return pieceType + 6; // Return piece type for black pieces (6-11)
            }
        }
        return -1; // No piece at the given square
    }

    public void makeMove(Tuple3 from, Tuple3 to) {
        makeMove(Move.createMove(from, to));
    }

    public void makeMove(int move) {
        history.add(move);
        lastMove = move;
        int fromSquare = move >>> 14;
        int toSquare = (move >>> 7) & 0x3F;
        int moveType = (move >>> 4) & 0x7;
        int promotionPiece = move & 0xF;

        int piece = getPiece(fromSquare);

        if (piece == -1) {
            throw new IllegalArgumentException("No piece at square " + fromSquare);
        }

        int color = piece / 6;
        int pieceType = piece % 6;

        // Detect captured piece BEFORE modifying the board
        int capturedPiece = getPiece(toSquare);
        int capturedType = capturedPiece == -1 ? -1 : capturedPiece % 6;
        int capturedColor = capturedPiece == -1 ? -1 : capturedPiece / 6;

        // Update half-move clock: reset on pawn moves or captures, increment otherwise
        boolean isCapture = capturedPiece != -1
                || moveType == MoveType.EN_PASSANT.ordinal();
        if (pieceType == pawn || isCapture) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        // Save old castling rights for hash update
        int oldCastling = castlingRights;

        long fromBitboard = 1L << fromSquare;
        long toBitboard = 1L << toSquare;

        // --- Zobrist: remove moving piece from origin ---
        zobristHash ^= ZOBRIST_PIECE[color * 6 + pieceType][fromSquare];

        if (color == white) {
            whitePieces[pieceType] ^= fromBitboard | toBitboard;
            for (int i = 0; i < 6; i++) {
                blackPieces[i] &= ~toBitboard; // Remove captured black piece, if any
            }
        } else {
            blackPieces[pieceType] ^= fromBitboard | toBitboard;
            for (int i = 0; i < 6; i++) {
                whitePieces[i] &= ~toBitboard; // Remove captured white piece, if any
            }
        }

        // --- Zobrist: remove captured piece, add moving piece at destination ---
        if (capturedPiece != -1) {
            zobristHash ^= ZOBRIST_PIECE[capturedColor * 6 + capturedType][toSquare];
        }
        zobristHash ^= ZOBRIST_PIECE[color * 6 + pieceType][toSquare];

        // Handle en passant
        if (moveType == MoveType.EN_PASSANT.ordinal()) {
            if (color == white) {
                int epSq = toSquare - 8;
                blackPieces[pawn] &= ~(1L << epSq);
                zobristHash ^= ZOBRIST_PIECE[1 * 6 + pawn][epSq]; // remove black pawn
            } else {
                int epSq = toSquare + 8;
                whitePieces[pawn] &= ~(1L << epSq);
                zobristHash ^= ZOBRIST_PIECE[0 * 6 + pawn][epSq]; // remove white pawn
            }
        }

        // Handle castling
        if (moveType == 2) { // Kingside
            if (color == white) {
                whitePieces[rook] ^= 0xA0;
                zobristHash ^= ZOBRIST_PIECE[0 * 6 + rook][7]; // h1
                zobristHash ^= ZOBRIST_PIECE[0 * 6 + rook][5]; // f1
            } else {
                blackPieces[rook] ^= 0xA000000000000000L;
                zobristHash ^= ZOBRIST_PIECE[1 * 6 + rook][63]; // h8
                zobristHash ^= ZOBRIST_PIECE[1 * 6 + rook][61]; // f8
            }
        } else if (moveType == 3) { // Queenside
            if (color == white) {
                whitePieces[rook] ^= 0x9;
                zobristHash ^= ZOBRIST_PIECE[0 * 6 + rook][0]; // a1
                zobristHash ^= ZOBRIST_PIECE[0 * 6 + rook][3]; // d1
            } else {
                blackPieces[rook] ^= 0x900000000000000L;
                zobristHash ^= ZOBRIST_PIECE[1 * 6 + rook][56]; // a8
                zobristHash ^= ZOBRIST_PIECE[1 * 6 + rook][59]; // d8
            }
        }

        // Handle promotion
        if (promotionPiece > 0) {
            if (color == white) {
                whitePieces[pawn] &= ~toBitboard;
                whitePieces[promotionPiece] |= toBitboard;
            } else {
                blackPieces[pawn] &= ~toBitboard;
                blackPieces[promotionPiece] |= toBitboard;
            }
            // Zobrist: swap pawn for promoted piece
            zobristHash ^= ZOBRIST_PIECE[color * 6 + pawn][toSquare];
            zobristHash ^= ZOBRIST_PIECE[color * 6 + promotionPiece][toSquare];
        }

        // update the castling rights
        if (color == white) {
            if (pieceType == king) {
                castlingRights &= ~0b0011;
            } else if (pieceType == rook) {
                if (fromSquare == h1.index()) {
                    castlingRights &= ~0b0001;
                } else if (fromSquare == a1.index()) {
                    castlingRights &= ~0b0010;
                }
            }
        } else {
            if (pieceType == king) {
                castlingRights &= ~0b1100;
            } else if (pieceType == rook) {
                if (fromSquare == h8.index()) {
                    castlingRights &= ~0b0100;
                } else if (fromSquare == a8.index()) {
                    castlingRights &= ~0b1000;
                }
            }
        }
        // Also update castling if a rook is captured
        if (capturedType == rook) {
            if (toSquare == h1.index()) castlingRights &= ~0b0001;
            else if (toSquare == a1.index()) castlingRights &= ~0b0010;
            else if (toSquare == h8.index()) castlingRights &= ~0b0100;
            else if (toSquare == a8.index()) castlingRights &= ~0b1000;
        }

        // Zobrist: update castling rights
        zobristHash ^= ZOBRIST_CASTLING[oldCastling];
        zobristHash ^= ZOBRIST_CASTLING[castlingRights];

        // Zobrist: flip side to move
        zobristHash ^= ZOBRIST_SIDE;

        colorToMove ^= 1;
    }

    /**
     * Returns a key that uniquely identifies the current position for
     * threefold repetition detection. Includes piece placement, side to
     * move, and castling rights (matching the FEN fields that define a position).
     */
    public String positionKey() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                int p = getPiece(rank * 8 + file);
                if (p == -1) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                int c = p / 6;
                int pt = p % 6;
                char ch = switch (pt) {
                    case pawn -> 'p'; case knight -> 'n'; case bishop -> 'b';
                    case rook -> 'r'; case queen -> 'q'; case king -> 'k';
                    default -> '?';
                };
                sb.append(c == white ? Character.toUpperCase(ch) : ch);
            }
            if (empty > 0) sb.append(empty);
            if (rank > 0) sb.append('/');
        }
        sb.append(' ').append(colorToMove == white ? 'w' : 'b');
        sb.append(' ').append(castlingRights);
        return sb.toString();
    }

    public int halfMoveClock() {
        return halfMoveClock;
    }

    public void setHalfMoveClock(int halfMoveClock) {
        this.halfMoveClock = halfMoveClock;
    }

    public long[] whitePieces() {
        return whitePieces;
    }

    public long[] blackPieces() {
        return blackPieces;
    }

    public LinkedList<Integer> history() {
        return history;
    }

    public int lastMove() {
        return lastMove;
    }

    public int castlingRights() {
        return castlingRights;
    }

    public boolean canCastle(int color) {
        return (color == white && ((castlingRights & 0b0001) != 0 || (castlingRights & 0b0010) != 0)) ||
               (color == black && ((castlingRights & 0b0100) != 0 || (castlingRights & 0b1000) != 0));
    }

    public int turn() {
        return colorToMove;
    }

    /**
     * Makes a "null move" — flips the side to move without moving any piece.
     * Used by null-move pruning in the search.
     */
    public void makeNullMove() {
        zobristHash ^= ZOBRIST_SIDE;
        lastMove = 0; // no en passant after null move
        colorToMove ^= 1;
    }

    public void setColorToMove(int color) {
        this.colorToMove = color;
    }

    public void setCastlingRights(int rights) {
        this.castlingRights = rights;
    }

    /** Returns the incrementally-updated Zobrist hash of the position. */
    public long getHash() {
        return zobristHash;
    }

    /** Recomputes the Zobrist hash from scratch (used for initialization). */
    public long computeHashFromScratch() {
        long hash = 0L;
        for (int c = 0; c < 2; c++) {
            for (int pt = 0; pt < 6; pt++) {
                long bb = (c == 0) ? whitePieces[pt] : blackPieces[pt];
                while (bb != 0) {
                    int sq = Long.numberOfTrailingZeros(bb);
                    hash ^= ZOBRIST_PIECE[c * 6 + pt][sq];
                    bb &= bb - 1;
                }
            }
        }
        if (colorToMove == black) hash ^= ZOBRIST_SIDE;
        hash ^= ZOBRIST_CASTLING[castlingRights];
        return hash;
    }

    public long opponentPieces(int color) {
        long enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        return enemyPieces;
    }

    public long allWhitePieces() {
        long whites = 0;

        for (int i = 0; i < 6; i++) {
            whites |= whitePieces[i];
        }

        return whites;
    }

    public long allBlackPieces() {
        long blacks = 0;

        for (int i = 0; i < 6; i++) {
            blacks |= blackPieces[i];
        }

        return blacks;
    }

    public long allPieces() {
        long white = 0, black = 0;

        for (int i = 0; i < 6; i++) {
            white |= whitePieces[i];
            black |= blackPieces[i];
        }

        return white | black;
    }

    public long kingPiece(int color) {
        return (color == 0 ? whitePieces[king] : blackPieces[king]);
    }

    public long getRooks(int color) {
        return (color == 0 ? whitePieces[rook] : blackPieces[rook]);
    }

    public long getBishops(int color) {
        return (color == 0 ? whitePieces[bishop] : blackPieces[bishop]);
    }

    public long getQueens(int color) {
        return (color == 0 ? whitePieces[queen] : blackPieces[queen]);
    }

    public long getKnights(int color) {
        return (color == 0 ? whitePieces[knight] : blackPieces[knight]);
    }

    public long getPawns(int color) {
        return (color == 0 ? whitePieces[pawn] : blackPieces[pawn]);
    }
}

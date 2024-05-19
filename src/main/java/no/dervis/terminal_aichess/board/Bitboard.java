package no.dervis.terminal_aichess.board;

import no.dervis.terminal_aichess.moves.Move;

import java.util.LinkedList;

public class Bitboard implements Board, Chess {

    private long[] whitePieces;
    private long[] blackPieces;
    private LinkedList<Integer> history;
    private int castlingRights;
    private int colorToMove;

    public Bitboard() {
        whitePieces = new long[6];
        blackPieces = new long[6];
        history = new LinkedList<>();
        colorToMove = 0;
        castlingRights = 0b1111;
    }

    public Bitboard copy() {
        Bitboard copy = new Bitboard();
        copy.whitePieces = whitePieces.clone();
        copy.blackPieces = blackPieces.clone();
        copy.history = new LinkedList<>(history);
        copy.castlingRights = castlingRights;
        copy.colorToMove = colorToMove;
        return copy;
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
        int fromSquare = move >>> 14;
        int toSquare = (move >>> 7) & 0x3F;
        int moveType = (move >>> 4) & 0x7;
        int promotionPiece = move & 0xF;

        int piece = getPiece(fromSquare);
        int color = piece / 6;
        int pieceType = piece % 6;

        long fromBitboard = 1L << fromSquare;
        long toBitboard = 1L << toSquare;

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

        // Handle en passant
        if (moveType == MoveType.EN_PASSANT.ordinal()) {
            if (color == white) {
                blackPieces[pawn] &= ~(1L << (toSquare - 8));
            } else {
                whitePieces[pawn] &= ~(1L << (toSquare + 8));
            }
        }

        // Handle castling
        if (moveType == 2) {
            if (color == white) {
                whitePieces[rook] ^= 0xA0; // Update rook position for white kingside castling
            } else {
                blackPieces[rook] ^= 0xA000000000000000L; // Update rook position for black kingside castling
            }
        } else if (moveType == 3) {
            if (color == white) {
                whitePieces[rook] ^= 0x9; // Update rook position for white queenside castling
            } else {
                blackPieces[rook] ^= 0x900000000000000L; // Update rook position for black queenside castling
            }
        }

        // Handle promotion
        if (moveType == 4) {
            if (color == white) {
                whitePieces[pawn] &= ~toBitboard; // Remove promoted white pawn
                whitePieces[promotionPiece] |= toBitboard; // Add promoted white piece
            } else {
                blackPieces[pawn] &= ~toBitboard; // Remove promoted black pawn
                blackPieces[promotionPiece] |= toBitboard; // Add promoted black piece
            }
        }

        // update the castling rights
        if (color == white) {
            if (pieceType == king) {
                castlingRights &= ~0b0011; // Remove white king-side and queen-side castling rights
            } else if (pieceType == rook) {
                if (fromSquare == h1.index()) {
                    castlingRights &= ~0b0001; // Remove white king-side castling rights
                } else if (fromSquare == a1.index()) {
                    castlingRights &= ~0b0010; // Remove white queen-side castling rights
                }
            }
        } else {
            if (pieceType == king) {
                castlingRights &= ~0b1100; // Remove black king-side and queen-side castling rights
            } else if (pieceType == rook) {
                if (fromSquare == h8.index()) {
                    castlingRights &= ~0b0100; // Remove black king-side castling rights
                } else if (fromSquare == a8.index()) {
                    castlingRights &= ~0b1000; // Remove black queen-side castling rights
                }
            }
        }

        colorToMove ^= 1;
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

    public long allPieces() {
        return allPieces(turn());
    }

    public long allPieces(int color) {
        long friendlyPieces = 0, enemyPieces = 0;

        for (int i = 0; i < 6; i++) {
            friendlyPieces |= (color == 0 ? whitePieces[i] : blackPieces[i]);
            enemyPieces |= (color == 0 ? blackPieces[i] : whitePieces[i]);
        }

        return friendlyPieces | enemyPieces;
    }
}


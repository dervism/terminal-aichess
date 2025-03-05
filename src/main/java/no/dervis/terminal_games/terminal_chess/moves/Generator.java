package no.dervis.terminal_games.terminal_chess.moves;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.attacks.BishopAttacks;
import no.dervis.terminal_games.terminal_chess.moves.attacks.RookAttacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Generator implements Chess {

    private final Bitboard board;

    public Generator(Bitboard board) {
        this.board = board;
    }

    public List<Integer> generateMoves(int color) {
        List<Integer> moves = new ArrayList<>();
        long enPassantTarget = getEnPassantTarget(Objects.requireNonNullElse(board.history().peekLast(), 0));

        generateKingMoves(moves, color);
        generatePawnMoves(moves, color, enPassantTarget);
        generateKnightMoves(moves, color);
        generateSlidingPieceMoves(moves, color, board.allPieces());

        return filterLegalMoves(moves, color);
    }

    public List<Integer> filterLegalMoves(List<Integer> moves, int color) {
        List<Integer> legalMoves = new ArrayList<>();
        int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(color));

        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            if (!isKingInCheck(color, kingSquare)) {
                legalMoves.add(move);
            }

        }
        return legalMoves;
    }

    private boolean isKingInCheck(int color, int kingSquare) {
        int opponent = 1 - color;
        long attacks = getAttacksForOpponent(opponent);
        return (attacks & (1L << kingSquare)) != 0;
    }

    private long getAttacksForOpponent(int opponent) {
        List<Integer> opponentMoves = this.generateMoves(opponent);
        AtomicLong attacks = new AtomicLong(0);

        opponentMoves.forEach(move -> {
            attacks.set(attacks.get() | Move.createMove(move).left());
        });

        return attacks.get();
    }

    private void generateSlidingPieceMoves(List<Integer> moves, int color, long occupied) {

        long[] allWhitePieces = board.whitePieces();
        long[] allBlackPieces = board.blackPieces();
        long ownPieces = color == white ? board.allWhitePieces() : board.allBlackPieces();

        long bishops = allWhitePieces[bishop] | allBlackPieces[bishop];
        long rooks = allWhitePieces[rook] | allBlackPieces[rook];
        long queens = allWhitePieces[queen] | allBlackPieces[queen];

        while (bishops != 0) {
            int square = Long.numberOfTrailingZeros(bishops);
            long attacks = BishopAttacks.getMagicBishopAttacks(square, occupied) & ~ownPieces;
            addMoves(moves, square, attacks);
            bishops &= bishops - 1;
        }

        while (rooks != 0) {
            int square = Long.numberOfTrailingZeros(rooks);
            long attacks = RookAttacks.getMagicRookAttacks(square, occupied) & ~ownPieces;
            addMoves(moves, square, attacks);
            rooks &= rooks - 1;
        }

        while (queens != 0) {
            int square = Long.numberOfTrailingZeros(queens);
            long attacks = (BishopAttacks.getMagicBishopAttacks(square, occupied) | RookAttacks.getMagicRookAttacks(square, occupied)) & ~ownPieces;
            addMoves(moves, square, attacks);
            queens &= queens - 1;
        }
    }

    private void addMoves(List<Integer> moves, int fromSquare, long attacks) {
        while (attacks != 0) {
            int toSquare = Long.numberOfTrailingZeros(attacks); // Get the least significant set bit
            moves.add(Move.createMove(fromSquare, toSquare, MoveType.ATTACK.ordinal())); // Convert to move representation
            attacks &= attacks - 1; // Remove the lowest set bit
        }
    }

    private long getEnPassantTarget(int lastMove) {
        int fromSquare = lastMove >>> 14;
        int toSquare = (lastMove >>> 7) & 0x3F;
        int piece = board.getPiece(toSquare);

        if (piece == (/* White pawn */ 0) && (toSquare - fromSquare) == 16) {
            return 1L << (toSquare - 8);
        } else if (piece == (/* Black pawn */ 6) && (fromSquare - toSquare) == 16) {
            return 1L << (toSquare + 8);
        } else {
            return 0L;
        }
    }

    private void generatePawnMoves(List<Integer> moves, int color, long enPassantTarget) {
        moves.addAll(new PawnMoveGenerator(board).generatePawnMoves(color, enPassantTarget));
    }

    private void generateKnightMoves(List<Integer> moves, int color) {
        moves.addAll(new KnightMoveGenerator(board).generateKnightMoves(color));
    }

    private void generateBishopMoves(List<Integer> moves, int color) {
        moves.addAll(new BishopMoveGenerator(board).generateBishopMoves(color));
    }

    private void generateRookMoves(List<Integer> moves, int color) {
        moves.addAll(new RookMoveGenerator(board).generateRookMoves(color));
    }

    private void generateQueenMoves(List<Integer> moves, int color) {
        moves.addAll(new QueenMoveGenerator(board).generateQueenMoves(color));
    }

    private void generateKingMoves(List<Integer> moves, int color) {
        moves.addAll(new KingMoveGenerator(board).generateKingMoves(color));
    }
}

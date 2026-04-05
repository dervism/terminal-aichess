package no.dervis.terminal_games.terminal_chess.board;

import static no.dervis.terminal_games.terminal_chess.board.Chess.*;

public class FEN {

    /**
     * Creates a Bitboard from a FEN string.
     * Example: {@code FEN.toBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")}
     */
    public static Bitboard toBoard(String fen) {
        Bitboard board = new Bitboard();
        String[] parts = fen.trim().split("\\s+");

        // 1. Piece placement (rank 8 to rank 1)
        String[] ranks = parts[0].split("/");
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r; // FEN starts from rank 8
            int file = 0;
            for (char c : ranks[r].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += c - '0';
                } else {
                    int square = rank * 8 + file;
                    int color = Character.isUpperCase(c) ? white : black;
                    int pieceType = switch (Character.toLowerCase(c)) {
                        case 'p' -> pawn;
                        case 'n' -> knight;
                        case 'b' -> bishop;
                        case 'r' -> rook;
                        case 'q' -> queen;
                        case 'k' -> king;
                        default -> throw new IllegalArgumentException("Unknown piece: " + c);
                    };
                    board.setPiece(pieceType, color, square);
                    file++;
                }
            }
        }

        // 2. Active color
        if (parts.length > 1) {
            board.setColorToMove(parts[1].equals("b") ? black : white);
        }

        // 3. Castling availability
        if (parts.length > 2) {
            int rights = 0;
            String castling = parts[2];
            if (castling.contains("K")) rights |= 0b0001;
            if (castling.contains("Q")) rights |= 0b0010;
            if (castling.contains("k")) rights |= 0b0100;
            if (castling.contains("q")) rights |= 0b1000;
            board.setCastlingRights(rights);
        }

        // Fields 4-6 (en passant, halfmove, fullmove) are not tracked by Bitboard

        return board;
    }

    /**
     * Returns the FEN string for the given board.
     */
    public static String fromBoard(Bitboard board) {
        StringBuilder sb = new StringBuilder();

        // 1. Piece placement
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                int piece = board.getPiece(square);
                if (piece == -1) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    int color = piece / 6;
                    int pieceType = piece % 6;
                    char c = switch (pieceType) {
                        case pawn -> 'p';
                        case knight -> 'n';
                        case bishop -> 'b';
                        case rook -> 'r';
                        case queen -> 'q';
                        case king -> 'k';
                        default -> '?';
                    };
                    sb.append(color == white ? Character.toUpperCase(c) : c);
                }
            }
            if (emptyCount > 0) sb.append(emptyCount);
            if (rank > 0) sb.append('/');
        }

        // 2. Active color
        sb.append(' ').append(board.turn() == white ? 'w' : 'b');

        // 3. Castling availability
        sb.append(' ');
        int castlingRights = board.castlingRights();
        if (castlingRights == 0) {
            sb.append('-');
        } else {
            if ((castlingRights & 0b0001) != 0) sb.append('K');
            if ((castlingRights & 0b0010) != 0) sb.append('Q');
            if ((castlingRights & 0b0100) != 0) sb.append('k');
            if ((castlingRights & 0b1000) != 0) sb.append('q');
        }

        // 4-6. En passant, halfmove clock, fullmove number (not tracked)
        sb.append(" - 0 1");

        return sb.toString();
    }
}

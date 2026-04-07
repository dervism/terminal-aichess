package no.dervis.terminal_games.terminal_chess.board;

import no.dervis.terminal_games.terminal_chess.moves.Move;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface Chess {

    int pawn = 0, bpawn = 6;
    int knight = 1, bknight = 7;
    int bishop = 2, bbishop = 8;
    int rook = 3, brook = 9;
    int queen = 4, bqueen = 10;
    int king = 5, bking = 11;
    int empty = -1;

    int white = 0;
    int black = 1;

    enum MoveType {
        NORMAL,
        EN_PASSANT,
        CASTLE_KING_SIDE,
        CASTLE_QUEEN_SIDE,
        ATTACK,
        PROMOTION;
    }

    Function<Integer, String> pieceToStr = pieceType -> switch (pieceType) {
        case bpawn    -> " ♙ ";
        case bknight  -> " ♘ ";
        case bbishop  -> " ♗ ";
        case brook    -> " ♖ ";
        case bqueen   -> " ♕ ";
        case bking -> " ♔ ";

        case pawn   -> " ♟ ";
        case knight -> " ♞ ";
        case bishop -> " ♝ ";
        case rook   -> " ♜ ";
        case queen  -> " ♛ ";
        case king   -> " ♚ ";

        case empty   -> "   ";
        default -> throw new IllegalStateException("Unexpected value: " + pieceType);
    };

    BiFunction<Board.Tuple3, Board.Tuple3, Integer> moveMaker = Move::createMove;
    BiFunction<Board.Tuple3, Board.Tuple3, Function<MoveType, Integer>> moveTypeMaker
            = (from, to) -> type -> Move.createMove(from.index(), to.index(), type.ordinal());

    BiFunction<Bitboard, Boolean, StringBuilder> boardToStr = (board, reverse) -> {
        // 24-bit color board: tan / brown squares, white / black pieces
        String lightSq = "\033[48;2;209;176;131m";  // tan
        String darkSq  = "\033[48;2;160;120;80m";   // brown
        String whiteFgLight = "\033[38;2;255;255;240m"; // ivory — visible on tan
        String whiteFgDark  = "\033[38;2;255;255;255m"; // white — visible on brown
        String blackFg = "\033[38;5;16m";
        String reset   = "\033[0m";

        StringBuilder builder = new StringBuilder();
        IntStream.range(0, 8)
                .map(i -> reverse ? (8 - 1 - i) : i)
                .forEach(row -> {
                    String line = IntStream.range(0, 8)
                            .map(c -> reverse ? c : (7 - c))
                            .mapToObj(col -> {
                                int piece = board.getPiece(BoardPrinter.indexFn.apply(row, col));
                                String symbol = pieceToStr.apply(piece);
                                boolean isLightSq = (row + col) % 2 != 0;
                                String bg = isLightSq ? lightSq : darkSq;
                                String fg;
                                if (piece >= 6) fg = blackFg;
                                else if (piece >= 0) fg = isLightSq ? whiteFgLight : whiteFgDark;
                                else fg = "";
                                return bg + fg + symbol + reset;
                            })
                            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                            .toString();
                    builder.append(line).append(System.lineSeparator());
                });
        return builder;
    };

    Function<Bitboard, StringBuilder> printBoard = board -> boardToStr.apply(board, true);
}

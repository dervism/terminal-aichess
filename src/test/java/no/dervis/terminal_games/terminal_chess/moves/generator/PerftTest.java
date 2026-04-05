package no.dervis.terminal_games.terminal_chess.moves.generator;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Perft (performance test) validates move generation correctness by counting
 * the number of leaf nodes at each depth and comparing against well-known
 * reference values.
 *
 * <p>Reference values from the Chess Programming Wiki:
 * <a href="https://www.chessprogramming.org/Perft_Results">Perft Results</a></p>
 */
class PerftTest implements Chess {

    private long perft(Bitboard board, int depth) {
        if (depth == 0) return 1;

        Generator generator = new Generator(board);
        List<Integer> moves = generator.generateMoves(board.turn());

        if (depth == 1) return moves.size();

        long nodes = 0;
        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            nodes += perft(copy, depth - 1);
        }
        return nodes;
    }

    /**
     * Prints a divide (split perft) for debugging: shows the node count
     * under each root move.
     */
    private void divide(Bitboard board, int depth) {
        Generator generator = new Generator(board);
        List<Integer> moves = generator.generateMoves(board.turn());
        long total = 0;

        for (int move : moves) {
            Bitboard copy = board.copy();
            copy.makeMove(move);
            long nodes = perft(copy, depth - 1);
            total += nodes;

            int from = move >>> 14;
            int to = (move >>> 7) & 0x3F;
            System.out.printf("%s%s: %d%n",
                    Tuple3.of(from).square().toLowerCase(),
                    Tuple3.of(to).square().toLowerCase(),
                    nodes);
        }
        System.out.println("Total: " + total);
    }

    // ---------------------------------------------------------------
    // Position 1: Initial position
    // ---------------------------------------------------------------

    private Bitboard initialPosition() {
        return Bitboard.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    @Test
    void initialPosition_depth1() {
        assertEquals(20, perft(initialPosition(), 1));
    }

    @Test
    void initialPosition_depth2() {
        assertEquals(400, perft(initialPosition(), 2));
    }

    @Test
    void initialPosition_depth3() {
        assertEquals(8902, perft(initialPosition(), 3));
    }

    @Test
    void initialPosition_depth4() {
        assertEquals(197_281, perft(initialPosition(), 4));
    }

    @Test
    void initialPosition_depth5() {
        // This takes a few seconds with copy-make
        assertEquals(4_865_609, perft(initialPosition(), 5));
    }

    // ---------------------------------------------------------------
    // Position 2: "Kiwipete" — exercises castling, en passant,
    // captures, and promotions.
    // ---------------------------------------------------------------

    private Bitboard kiwipete() {
        return Bitboard.fromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
    }

    @Test
    void kiwipete_depth1() {
        assertEquals(48, perft(kiwipete(), 1));
    }

    @Test
    void kiwipete_depth2() {
        assertEquals(2039, perft(kiwipete(), 2));
    }

    @Test
    void kiwipete_depth3() {
        assertEquals(97_862, perft(kiwipete(), 3));
    }

    @Test
    void kiwipete_depth4() {
        assertEquals(4_085_603, perft(kiwipete(), 4));
    }

    // ---------------------------------------------------------------
    // Position 3: Endgame — en passant edge cases.
    // ---------------------------------------------------------------

    private Bitboard position3() {
        return Bitboard.fromFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
    }

    @Test
    void position3_depth1() {
        assertEquals(14, perft(position3(), 1));
    }

    @Test
    void position3_depth2() {
        assertEquals(191, perft(position3(), 2));
    }

    @Test
    void position3_depth3() {
        assertEquals(2812, perft(position3(), 3));
    }

    @Test
    void position3_depth4() {
        assertEquals(43_238, perft(position3(), 4));
    }

    @Test
    void position3_depth5() {
        assertEquals(674_624, perft(position3(), 5));
    }

    // ---------------------------------------------------------------
    // Position 4: Promotions, captures, and castling rights.
    // ---------------------------------------------------------------

    private Bitboard position4() {
        return Bitboard.fromFEN("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -");
    }

    @Test
    void position4_depth1() {
        assertEquals(6, perft(position4(), 1));
    }

    @Test
    void position4_depth2() {
        assertEquals(264, perft(position4(), 2));
    }

    @Test
    void position4_depth3() {
        assertEquals(9467, perft(position4(), 3));
    }

    @Test
    void position4_depth4() {
        assertEquals(422_333, perft(position4(), 4));
    }

    // ---------------------------------------------------------------
    // Position 5: Promotion with capture and discovered checks.
    // ---------------------------------------------------------------

    private Bitboard position5() {
        return Bitboard.fromFEN("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -");
    }

    @Test
    void position5_depth1() {
        assertEquals(44, perft(position5(), 1));
    }

    @Test
    void position5_depth2() {
        assertEquals(1486, perft(position5(), 2));
    }

    @Test
    void position5_depth3() {
        assertEquals(62_379, perft(position5(), 3));
    }

    @Test
    void position5_depth4() {
        assertEquals(2_103_487, perft(position5(), 4));
    }
}

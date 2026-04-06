package no.dervis.terminal_games.terminal_chess.ai.alphabeta;

import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.openingbook.OpeningBook;

/**
 * Decorator that wraps any {@link Engine} with opening book support.
 *
 * <p>On every call to {@link #findBestMove}, the book is consulted first.
 * If the current position appears in the book, a book move is returned
 * instantly. Otherwise the delegate engine searches normally. Because the
 * lookup is position-based (a fast HashMap probe), transpositions are
 * handled naturally — even if the opponent plays an unusual move order,
 * the book will kick back in once a known position is reached.
 *
 * <p>Each {@code BookEngine} instance has its own book and random seed,
 * so two engines in a tournament independently select from the book.
 *
 * <pre>{@code
 * Engine raw = new ChessAI();
 * BookEngine engine = new BookEngine(raw);
 *
 * int move = engine.findBestMove(board, 3000);
 * if (engine.lastMoveFromBook()) {
 *     System.out.println("Book move: " + ...);
 * } else {
 *     System.out.println("Computer move: " + ...);
 * }
 * }</pre>
 */
public class BookEngine implements Engine {

    private final Engine delegate;
    private final OpeningBook book;

    private boolean lastMoveFromBook;
    private boolean justLeftBook;

    public BookEngine(Engine delegate) {
        this.delegate = delegate;
        this.book = new OpeningBook();
    }

    public BookEngine(Engine delegate, long seed) {
        this.delegate = delegate;
        this.book = new OpeningBook(seed);
    }

    @Override
    public int findBestMove(Bitboard board, long timeLimitMs) {
        justLeftBook = false;

        int bookMove = book.getBookMove(board);
        if (bookMove != 0) {
            lastMoveFromBook = true;
            return bookMove;
        }

        // Transition: we were playing from the book, now we're not
        if (lastMoveFromBook) {
            justLeftBook = true;
        }
        lastMoveFromBook = false;
        return delegate.findBestMove(board, timeLimitMs);
    }

    @Override
    public void reset() {
        lastMoveFromBook = false;
        justLeftBook = false;
    }

    /** True if the last call to {@link #findBestMove} returned a book move. */
    public boolean lastMoveFromBook() {
        return lastMoveFromBook;
    }

    /** True if the engine just transitioned out of the book on the last call. */
    public boolean justLeftBook() {
        return justLeftBook;
    }

    /**
     * Returns the name of the opening played, based on the deepest book
     * position that was matched. Call after the book phase ends for
     * the most specific name.
     */
    public String openingName() {
        return book.lastOpeningName();
    }
}

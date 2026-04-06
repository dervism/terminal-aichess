package no.dervis.terminal_games.terminal_chess.ai.tournament;

import java.util.List;

/**
 * The outcome of a single game in a tournament.
 */
public record GameResult(
        int gameNumber,
        String whiteName,
        String blackName,
        String opening,
        String result,
        String drawReason,
        String finalFEN,
        int totalMoves,
        List<String> moveHistory
) {

    public boolean isWhiteWin()  { return "1-0".equals(result); }
    public boolean isBlackWin()  { return "0-1".equals(result); }
    public boolean isDraw()      { return "1/2-1/2".equals(result); }
}

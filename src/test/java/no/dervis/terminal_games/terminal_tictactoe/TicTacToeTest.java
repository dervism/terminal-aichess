package no.dervis.terminal_games.terminal_tictactoe;

import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.PlayerSymbol;
import no.dervis.terminal_games.terminal_tictactoe.TicTacToe.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TicTacToeTest {

    @Test
    public void testWinDiagonal3x3Board() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.X
        ));
        Assertions.assertEquals(State.PlayerWon, game.checkGameState());
    }

    // test win second diagonal 3x3 board
    @Test
    public void testWinSndDiagonal3x3Board() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.X,
                PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.E,
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertEquals(State.PlayerWon, game.checkGameState());
    }

    @Test
    public void testCheckGameState_PlayerWon() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertEquals(State.PlayerWon, game.checkGameState());
    }

    @Test
    public void testCheckGameState_ComputerWon() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertEquals(State.ComputerWon, game.checkGameState());
    }

    @Test
    public void testCheckGameState_Draw() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.X,
                PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.O,
                PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.O
        ));
        Assertions.assertEquals(State.Draw, game.checkGameState());
    }

    @Test
    public void testCheckGameState_InProgress() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.O, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertEquals(State.InProgress, game.checkGameState());
    }

    @Test
    public void testRowsWinWhenPlayerWins() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertTrue(game.rowsWin(PlayerSymbol.X, game.board()));
    }

    /**
     Generate a test for this 6x6 board:

       | 1 | 2 | 3 | 4 | 5 | 6 |
     1 | X |   |   |   |   |   |
     2 |   | X |   |   |   | O |
     3 |   |   | X |   |   |   |
     4 |   |   | O | X |   | O |
     5 |   |   |   |   | O |   |
     6 |   |   |   |   |   |   |
     */

    @Test
    public void testDiagonalPlayerWins4InARowOnA6x6Board() {
        TicTacToe game = new TicTacToe(4, 6*6);
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));

        Assertions.assertEquals(State.PlayerWon, game.checkGameState());
    }

    /**
     Generate a test to check 4-in-a-row on columns
       | 1 | 2 | 3 | 4 | 5 | 6 |
     1 |   | O |   |   |   |   |
     2 |   |   |   | O | X | O |
     3 |   |   |   |   | X |   |
     4 | O |   |   |   | X |   |
     5 |   |   |   |   | X |   |
     6 |   |   |   |   | O |   |

     */

    @Test
    public void testWins4InAColumnOnA6x6Board() {
        TicTacToe game = new TicTacToe(4, 6*6);
        game.board().setCells(List.of(
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.O, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));

        Assertions.assertEquals(State.ComputerWon, game.checkGameState());
    }

    /**
     Make a test for this board to check win on the last row on this 6x6 board:

     | 1 | 2 | 3 | 4 | 5 | 6 |
     1 |   |   |   | O |   |   |
     2 |   |   |   |   | O |   |
     3 | O |   |   |   |   |   |
     4 |   |   |   |   |   |   |
     5 |   |   |   |   |   |   |
     6 |   |   | X | X | X | X |

     */
    @Test
    public void testWins4InARowOnA6x6Board() {
        TicTacToe game = new TicTacToe(4, 6*6);
        game.board().setCells(List.of(
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.X
        ));

        Assertions.assertEquals(State.PlayerWon, game.checkGameState());
    }

    @Test
    public void testNoWins4InARowOnA6x6Board() {
        TicTacToe game = new TicTacToe(4, 6*6);
        game.board().setCells(List.of(
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.X, PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.X
        ));

        Assertions.assertEquals(State.InProgress, game.checkGameState());
    }


    @Test
    public void testRowsWinWhenComputerWins() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.O, PlayerSymbol.O, PlayerSymbol.O,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertTrue(game.rowsWin(PlayerSymbol.O, game.board()));
    }
    @Test
    public void testRowsWinWhenGameIsNotOver() {
        TicTacToe game = new TicTacToe();
        game.board().setCells(List.of(
                PlayerSymbol.X, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.O, PlayerSymbol.E, PlayerSymbol.E,
                PlayerSymbol.E, PlayerSymbol.E, PlayerSymbol.E
        ));
        Assertions.assertFalse(game.rowsWin(PlayerSymbol.X, game.board()));
        Assertions.assertFalse(game.rowsWin(PlayerSymbol.O, game.board()));
    }
}

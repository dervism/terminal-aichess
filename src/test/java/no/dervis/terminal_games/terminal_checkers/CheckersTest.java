
package no.dervis.terminal_games.terminal_checkers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CheckersTest {

   @Test
   void testInitializeBoard() {
       Checkers checkers = new Checkers();
       Checkers.Board board = checkers.initializeBoard();

       assertNotNull(board);
       assertEquals(64, board.cells().size());

       int expectedWhiteCellCount = 12;
       int expectedBlackCellCount = 12;
       int expectedEmptyCellCount = 40;

       int whiteCellCount = 0;
       int blackCellCount = 0;
       int emptyCellCount = 0;

       for (Checkers.Color cellColor : board.cells().values()) {
           switch(cellColor) {
               case WHITE, WHITE_KING -> whiteCellCount++;
               case BLACK, BLACK_KING -> blackCellCount++;
               case EMPTY -> emptyCellCount++;
           }
       }

       assertEquals(expectedWhiteCellCount, whiteCellCount);


   // More test methods will be added here...
    }

   @Test
   void testGenerateSimpleMovesForBlack() {
       Checkers checkers = new Checkers();
       Checkers.Board board = checkers.initializeBoard();
       Checkers.Player playerBlack = new Checkers.Player("Player Black", Checkers.Color.BLACK);

       Checkers.Cell blackCellInitial = new Checkers.Cell(9, new Checkers.Position(1,0));
       List<Checkers.Move> validBlackMovesInitial = checkers.generateSimpleMoves(playerBlack, blackCellInitial, Checkers.Color.BLACK, board);

       // There should be no valid moves for the black player from this cell at the start of the game
       assertEquals(0, validBlackMovesInitial.size());

       Checkers.Cell blackCell = new Checkers.Cell(30, new Checkers.Position(3,6));
       List<Checkers.Move> validBlackMoves = checkers.generateSimpleMoves(playerBlack, blackCell, Checkers.Color.BLACK, board);

       // There should be a single valid move for the black player from this cell at the start of the game
       assertEquals(1, validBlackMoves.size());

       Checkers.Move blackMove = validBlackMoves.get(0);

       assertEquals(blackCell, blackMove.from());
       assertEquals(new Checkers.Cell(25, new Checkers.Position(3,1)), blackMove.to());
   }

   @Test
   void testGenerateSimpleMovesForWhite() {
       Checkers checkers = new Checkers();
       Checkers.Board board = checkers.initializeBoard();
       Checkers.Player playerWhite = new Checkers.Player("Player White", Checkers.Color.WHITE);

       Checkers.Cell whiteCellInitial = new Checkers.Cell(56, new Checkers.Position(7,0));
       List<Checkers.Move> validWhiteMovesInitial = checkers.generateSimpleMoves(playerWhite, whiteCellInitial, Checkers.Color.WHITE, board);

       // There should be no valid moves for the white player from this cell at the start of the game
       assertEquals(0, validWhiteMovesInitial.size());

       Checkers.Cell whiteCell = new Checkers.Cell(41, new Checkers.Position(5,1));
       List<Checkers.Move> validWhiteMoves = checkers.generateSimpleMoves(playerWhite, whiteCell, Checkers.Color.WHITE, board);

       // There should be a single valid move for the white player from this cell at the start of the game
       assertEquals(1, validWhiteMoves.size());

       Checkers.Move whiteMove = validWhiteMoves.get(0);

       assertEquals(whiteCell, whiteMove.from());
       assertEquals(new Checkers.Cell(34, new Checkers.Position(4, 2)), whiteMove.to());
   }

   @Test
   void testGenerateAllValidMoves() {
       Checkers checkers = new Checkers();
       Checkers.Board board = checkers.initializeBoard();
       Checkers.Player playerWhite = new Checkers.Player("Player 1", Checkers.Color.WHITE);

       // Check for valid moves for white player at start of the game.
       List<Checkers.Move> validMoves = checkers.generateAllValidMoves(playerWhite, board);
       List<Integer> validFromCells = validMoves.stream()
           .map(move -> move.from().cellNr())
           .collect(Collectors.toList());

       // Only the bottom row of white checkers can move at the start of the game.
       Integer[] expectedFromCellsArray = new Integer[] {41, 45, 45, 43, 43, 47, 47};
       List<Integer> expectedFromCells = Arrays.asList(expectedFromCellsArray);

       assertEquals(expectedFromCells, validFromCells);
   }

   @Test
   void testGenerateSimpleMoves() {
       Checkers checkers = new Checkers();
       Checkers.Board board = checkers.initializeBoard();
       Checkers.Player playerWhite = new Checkers.Player("Player 1", Checkers.Color.WHITE);
       Checkers.Player playerBlack = new Checkers.Player("Player 2", Checkers.Color.BLACK);

       Checkers.Cell whiteCell = new Checkers.Cell(41, new Checkers.Position(5,1));
       Checkers.Cell blackCell = new Checkers.Cell(24, new Checkers.Position(3,0));

       List<Checkers.Move> validWhiteMoves = checkers.generateSimpleMoves(playerWhite, whiteCell, Checkers.Color.WHITE, board);
       List<Checkers.Move> validBlackMoves = checkers.generateSimpleMoves(playerBlack, blackCell, Checkers.Color.BLACK, board);

       assertEquals(1, validWhiteMoves.size());
       assertEquals(1, validBlackMoves.size());

       Checkers.Move whiteMove = validWhiteMoves.get(0);
       Checkers.Move blackMove = validBlackMoves.get(0);

       assertEquals(whiteCell, whiteMove.from());
       assertEquals(blackCell, blackMove.from());

       assertEquals(new Checkers.Cell(34, new Checkers.Position(4, 2)), whiteMove.to());
       assertEquals(new Checkers.Cell(31, new Checkers.Position(3, 7)), blackMove.to());
   }
}

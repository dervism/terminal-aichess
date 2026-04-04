
package no.dervis.terminal_games.terminal_checkers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.IO.println;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CheckersTest {

   @Test
   void testInitializeBoard() {
       Checkers checkers = new Checkers();
       Board board = checkers.initializeBoard();

       assertNotNull(board);
       assertEquals(64, board.cells().size());

       int expectedWhiteCellCount = 12;
       int expectedBlackCellCount = 12;
       int expectedEmptyCellCount = 40;

       int whiteCellCount = 0;
       int blackCellCount = 0;
       int emptyCellCount = 0;

       for (Color cellColor : board.cells().values()) {
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
       Board board = checkers.initializeBoard();
       Player playerBlack = new Player("Player Black", Color.BLACK);

       Cell blackCellInitial = new Cell(9, new Position(1,0));
       List<Move> validBlackMovesInitial = checkers.generateSimpleMoves(playerBlack, blackCellInitial, Color.BLACK, board);

       // There should be no valid moves for the black player from this cell at the start of the game
       assertEquals(0, validBlackMovesInitial.size());

       Cell blackCell = new Cell(30, new Position(3,6));
       List<Move> validBlackMoves = checkers.generateSimpleMoves(playerBlack, blackCell, Color.BLACK, board);

       checkers.prettyPrintBoard();

       assertEquals(2, validBlackMoves.size());

       Move blackMove = validBlackMoves.get(0);
       assertEquals(blackCell, blackMove.from());
       assertEquals(new Position(4,7).toCell(), blackMove.to());

       Move blackMove2 = validBlackMoves.get(1);
       assertEquals(blackCell, blackMove2.from());
       assertEquals(new Position(4,5).toCell(), blackMove2.to());
   }

   @Test
   void testGenerateSimpleMovesForWhite() {
       Checkers checkers = new Checkers();
       Board board = checkers.initializeBoard();
       Player playerWhite = new Player("Player White", Color.WHITE);

       Cell whiteCellInitial = new Cell(56, new Position(7,0));
       List<Move> validWhiteMovesInitial = checkers.generateSimpleMoves(playerWhite, whiteCellInitial, Color.WHITE, board);

       // There should be no valid moves for the white player from this cell at the start of the game
       assertEquals(0, validWhiteMovesInitial.size());

       Cell whiteCell = new Position(6,0).toCell();
       List<Move> validWhiteMoves = checkers.generateSimpleMoves(playerWhite, whiteCell, Color.WHITE, board);

       checkers.prettyPrintBoard();
       println(validWhiteMoves);

       // There should be a single valid move for the white player from this cell at the start of the game
       assertEquals(1, validWhiteMoves.size());

       Move whiteMove = validWhiteMoves.get(0);
       assertEquals(whiteCell, whiteMove.from());
       assertEquals(new Position(5, 1).toCell(), whiteMove.to());
   }

   @Test
   void testGenerateAllValidMoves() {
       Checkers checkers = new Checkers();
       Board board = checkers.initializeBoard();
       Player playerWhite = new Player("Player 1", Color.WHITE);

       // Check for valid moves for white player at start of the game.
       List<Move> validMoves = checkers.generateAllValidMoves(playerWhite, board);
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
       Board board = checkers.initializeBoard();
       Player playerWhite = new Player("Player 1", Color.WHITE);
       Player playerBlack = new Player("Player 2", Color.BLACK);

       Cell whiteCell = new Position(5,0).toCell();
       Cell blackCell = new Position(3,0).toCell();

       List<Move> validWhiteMoves = checkers.generateSimpleMoves(playerWhite, whiteCell, Color.WHITE, board);
       List<Move> validBlackMoves = checkers.generateSimpleMoves(playerBlack, blackCell, Color.BLACK, board);

       checkers.prettyPrintBoard();

       assertEquals(1, validWhiteMoves.size());
       assertEquals(1, validBlackMoves.size());

       Move whiteMove = validWhiteMoves.get(0);
       Move blackMove = validBlackMoves.get(0);

       assertEquals(whiteCell, whiteMove.from());
       assertEquals(blackCell, blackMove.from());

       assertEquals(new Position(4, 1).toCell(), whiteMove.to());
       assertEquals(new Position(4, 1).toCell(), blackMove.to());
   }
}

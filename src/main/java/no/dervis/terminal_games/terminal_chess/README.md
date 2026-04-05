# Terminal Chess

A terminal-based chess game with an AI opponent powered by iterative-deepening alpha-beta search with a transposition table.

## Running the game

```bash
mvn compile exec:java
```

## Setup

On startup you are prompted to:

1. **Choose color** -- `w` for white, `b` for black.
2. **Choose difficulty** -- controls how long the AI thinks per move:

| Level | Think time |
|-------|-----------|
| 1. Easy | 1 second |
| 2. Medium | 3 seconds |
| 3. Hard | 5 seconds |
| 4. Expert | 10 seconds |
| 5. Extra hard | 50 seconds |

3. **Choose engine**:
   - **Single-threaded** -- standard iterative-deepening alpha-beta search.
   - **Parallel (Lazy SMP)** -- multiple threads share a transposition table for stronger play.

## Entering moves

Two input formats are supported. Standard algebraic notation (SAN) is tried first, with coordinate format as a fallback.

### Standard algebraic notation

| Move type | Example | Description |
|-----------|---------|-------------|
| Pawn move | `e4` | Pawn to e4 |
| Piece move | `Nf3` | Knight to f3 |
| Capture | `Bxc4` | Bishop captures on c4 |
| Pawn capture | `exd5` | e-pawn captures on d5 |
| Kingside castle | `O-O` | |
| Queenside castle | `O-O-O` | |
| Promotion | `e8=Q` | Promote pawn to queen |
| Capture + promotion | `exd8=N` | Capture and promote to knight |
| Disambiguation | `Nbd2` | b-file knight to d2 |

Piece letters: **K**ing, **Q**ueen, **R**ook, **B**ishop, **N**ight. Pawns have no letter prefix.

### Coordinate format

Specify the source and target squares separated by a dash: `e2-e4`, `g1-f3`.

## Promotion

When a pawn reaches the last rank, you are prompted to choose a promotion piece (Knight, Bishop, Rook, or Queen). If you use SAN with a promotion suffix (e.g. `e8=Q`), the piece is selected automatically.

## Commands

The following commands can be entered instead of a move during your turn:

| Command | Description |
|---------|-------------|
| `fen` | Print the current position as a FEN string |
| `r` | Resign the game |
| `q` | Quit immediately without saving |

## During the game

- The AI prints its search progress (depth, score, nodes, principal variation) while thinking.
- Check, checkmate, stalemate, and insufficient material are detected automatically.

## Game over

When a game ends (checkmate, stalemate, insufficient material, resignation, or quit) you are presented with four options:

1. **New game without saving** -- start a fresh game.
2. **Save and start new game** -- save the game to a file, then start a fresh game.
3. **Save game and quit** -- save the game to a file, then exit.
4. **Quit** -- exit without saving.

Saved games are written to the `saves/` folder. Each file contains the date and time, player assignments (player/computer), engine type, difficulty level, result, the final FEN string, and the full move history.

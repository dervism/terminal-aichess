# Terminal Games

The project contains three board games that are playable directly from the terminal. I created this repo because my previous trials to implement Java-based chess engines with x88 and traditional for-loops resulted in very slow engines that played poor chess. The goal of this project is, therefore, to learn advanced board representations and move generation algorithms with efficient bit-shifting techniques.

A simple version of the well-known minimax-based 𝛼/ϐ algorithm can generate strong moves for simple games with limited search space and simple heuristics. Chess, however, has an infinitely large search space that can quickly exhaust the memory and the cpu. To tackle that problem in chess, the 𝛼/ϐ cutoff search must be extended with optimizations, memory-efficient data-formats and a strong heuristic analysis and evaluation.

## Terminal Chess
Simple Java 25 chess app that uses bitboards. The implementation is mainly for learning the Bitboard data-structure, one of the most effective and compact data formats for chess. The implementation features a sudo-move generator split into multiple individual generators. All generated moves are encoded into a compact 32-bit data storage format based on my own work with earlier chess engines.

This engine uses Magic Bitboards, which allow the engine to rapidly generate attacks and sliding piece moves without loops. As it is severely hard to correctly implement bit-shifting and bit-masking correctly on Magic Bitboards, the inspiration for the magic numbers implementation is based on examples shared at the [Chess Programming Wiki](https://www.chessprogramming.org/Magic_Bitboards) website. Before Magic Bitboards, I used Java streams and loops to calculate attacks and sliding moves. This technique worked but was extremely slow and analyzed only up to four-ply depth. You can read about the [ideas behind it in the README file](src/main/java/no/dervis/terminal_games/terminal_chess/moves/attacks/history/README.md). With Magic Bitboards, however, we can analyze tens of millions of positions in a few seconds.

PS: The game works, and the AI plays strong games without an opening book even at level 1. This is still a work in progress.

Running in your terminal:
`java --enable-preview --source 25  src/main/java/no/dervis/terminal_games/terminal_chess/TerminalChess.java`

In your terminal, this is what the app will look like.

<img height="256" src="board.png" width="250"/>

The input format is coordinate-based, not real chess notation. To move the e2 pawn to e4, you enter e2-e4. Similarly, to move the bishop to c4, you enter f1-c4, etc.

**TODOs:**
- [x] Implement basic bitboard
- [x] Implement terminal ui
- [x] Implement basic move generation
- [x] Implement complete move generation (checks, attacks, castling, en-passant, promotions)
- [x] Implement legal moves filtering
- [ ] Implement perft move generation analysis
- [x] Implement basic AI (𝛼/ϐ search)
- [x] Implement advanced AI (𝛼/ϐ search + iterative deepening + quiescence search + Negamax + Principal Variation Search (PVS) + transposition table + check extensions + Late Move Reductions (LMR) + Killer & History heuristics + time management)
- [ ] Implement opening book
- [ ] Implement superior ai (monte-carlo simulation and neural networks)

## Terminal Tic-tac-toe
A complete Java 25 Tic-tac-toe game that can scale to custom sized nxn boards.

**TODOs:**
- [x] Implement list-based board
- [x] Implement board scaling
- [x] Implement terminal ui
- [x] Implement move generation
- [x] Implement basic ai (minimax with 𝛼/ϐ cutoff)

## Terminal Checkers
A Java 25 checkers game that uses a map-based board. It supports regular moves, jump moves and kings.

PS: This is still a work in progress. The game works but will only respond with random moves at the moment.

**TODOs:**
- [x] Implement map-based board
- [x] Implement terminal ui
- [x] Implement basic move generation
- [x] Implement complete move generation (forward and backward moves, jump moves)
- [ ] Implement basic ai (minimax with 𝛼/ϐ cutoff)

## Documentation
https://deepwiki.com/dervism/terminal-aichess

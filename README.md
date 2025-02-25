# Terminal Games

The project contains three board games that is playable directly from the terminal.
The goal of the project is to learn different board representations and move generation algorithms.
In most cases, the most effective algorithm is the 𝛼/ϐ cutoff search.

## Terminal Chess
Simple Java 23 chess app that uses bitboards.
This implementation is mainly for learning the bitboard data-structure, one of the most effective and compact chess data formats.
It's hard to implement the bit-shifting and bit-masking correctly.

PS: This is still work in progress. The game works, but will only respond with random moves at the moment.

Running in your terminal:
` java --enable-preview --source 23  src/main/java/no/dervis/terminal_games/terminal_chess/TerminalChess.java`

In your terminal, this is how the app will look like.

<img height="256" src="board.png" width="250"/>

**TODOs:**
- [x] Implement basic bitboard
- [x] Implement terminal ui
- [x] Implement basic move generation
- [x] Implement complete move generation (checks, attacks, castling, en-passant, promotions)
- [ ] Implement legal moves filtering
- [ ] Implement perft move generation analysis
- [ ] Implement basic ai (𝛼/ϐ search)
- [ ] Implement advanced ai (monte-carlo simulation & neural networks)

## Terminal Tic Tac Toe
A complete Java 23 tic-tac-toe game that can scale to custom set bigger boards.

**TODOs:**
- [x] Implement list-based board
- [x] Implement board scaling
- [x] Implement terminal ui
- [x] Implement move generation
- [x] Implement basic ai (minimax with 𝛼/ϐ cutoff)

## Terminal Checkers
A Java 23 checkers game that uses a map based board. It supports regular moves, jump moves and kings.

PS: This is still work in progress. The game works, but will only respond with random moves at the moment.

**TODOs:**
- [x] Implement map-based board
- [x] Implement terminal ui
- [x] Implement basic move generation
- [x] Implement complete move generation (forward and backward moves, jump moves)
- [ ] Implement basic ai (minimax with 𝛼/ϐ cutoff)
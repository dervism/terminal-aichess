# Terminal Games

The project contains three board games that are playable directly from the terminal. I created this repo because my previous trials to implement Java-based chess engines with x88 and traditional for-loops resulted in very slow engines that played poor chess. The goal of this project is, therefore, to learn advanced board representations and move generation algorithms with efficient bit-shifting techniques.

A simple version of the well-known minimax-based 𝛼/ϐ algorithm can generate strong moves for simple games with limited search space and simple heuristics. Chess, however, has an infinitely large search space that can quickly exhaust the memory and the cpu. To tackle that problem in chess, the 𝛼/ϐ cutoff search must be extended with optimizations, memory-efficient data-formats and a strong heuristic analysis and evaluation.

## Terminal Chess
Simple Java 25 chess app that uses bitboards. The implementation is mainly for learning the Bitboard data-structure, one of the most effective and compact data formats for chess. The implementation features a sudo-move generator split into multiple individual generators. All generated moves are encoded into a compact 32-bit data storage format based on my own work with earlier chess engines.

This engine uses Magic Bitboards, which allow the engine to rapidly generate attacks and sliding piece moves without loops. As it is severely hard to correctly implement bit-shifting and bit-masking correctly on Magic Bitboards, the inspiration for the magic numbers implementation is based on examples shared at the [Chess Programming Wiki](https://www.chessprogramming.org/Magic_Bitboards) website. Before Magic Bitboards, I used Java streams and loops to calculate attacks and sliding moves. This technique worked but was extremely slow and analyzed only up to four-ply depth. You can read about the [ideas behind it in the README file](src/main/java/no/dervis/terminal_games/terminal_chess/moves/attacks/history/README.md). With Magic Bitboards, however, we can analyze tens of millions of positions in a few seconds.

The engine features two ai players – a single-threaded ai, and a multithreaded parallel ai that processes multiple millions of more nodes than the single-threaded version.

PS: The AI plays strong games without an opening book, even at level 1. This is still a work in progress.

Running in your terminal:
`java --enable-preview --source 25  src/main/java/no/dervis/terminal_games/terminal_chess/TerminalChess.java`

In your terminal, this is what the app will look like.

<img height="250" src="board.png" width="250"/>

See the [README for more information](src/main/java/no/dervis/terminal_games/terminal_chess/README.md) about how to play.

**TODOs:**
- [x] Implement basic bitboard
- [x] Implement terminal ui
- [x] Implement SAN parsing for better input handling
- [x] Implement basic move generation
- [x] Implement complete move generation (checks, attacks, castling, en-passant, promotions)
- [x] Implement legal moves filtering
- [x] Implement FEN string generation
- [x] Implement perft move generation analysis
- [x] Implement basic AI (𝛼/ϐ search)
- [x] Implement advanced AI (𝛼/ϐ search + iterative deepening + quiescence search + Negamax + Principal Variation Search (PVS) + transposition table + check extensions + Late Move Reductions (LMR) + Killer & History heuristics + time management)
- [x] Implement multithreaded parallel processing ai
- [ ] Implement opening book
- [ ] Implement superior ai (monte-carlo simulation and neural networks)

### Move generation correctness

The move generator is validated with [perft](https://www.chessprogramming.org/Perft_Results) -- a standard test that counts leaf nodes at each depth and compares against known-correct values from the [Chess Programming Wiki](https://www.chessprogramming.org/Perft_Results).

Unit tests cover depths 1--5 for all six positions. A standalone deep runner pushes further:

| Position | Max depth | Nodes at max depth |
|----------|----------:|-----------:|
| [Initial position](https://www.chessprogramming.org/Perft_Results#Initial_Position) | 6 | 119,060,324 |
| [Kiwipete](https://www.chessprogramming.org/Perft_Results#Position_2) | 6 | 8,031,647,685 |
| [Position 3](https://www.chessprogramming.org/Perft_Results#Position_3) (endgame) | 7 | 178,633,661 |
| [Position 4](https://www.chessprogramming.org/Perft_Results#Position_4) (promotions) | 6 | 706,045,033 |
| [Position 5](https://www.chessprogramming.org/Perft_Results#Position_5) (discovered checks) | 5 | 89,941,194 |
| [Position 6](https://www.chessprogramming.org/Perft_Results#Position_6) (mirrored) | 6 | 6,923,051,137 |

All results match at every depth. The `--parallel` flag distributes root moves across all cores (copy-make means each thread gets its own board with zero contention):

| Position | Single-threaded | Parallel (14 cores) | Speedup |
|----------|----------------:|---------:|--------:|
| 1 (initial) depth 6 | 6,803ms | 1,314ms | **5.2x** |
| 2 (Kiwipete) depth 6 | 471,712ms | 101,960ms | **4.6x** |
| 3 (endgame) depth 6 | 759ms | 102ms | **7.4x** |
| 4 (promotions) depth 6 | 42,112ms | 9,320ms | **4.5x** |
| 5 (discovered checks) depth 5 | 5,216ms | 626ms | **8.3x** |
| 6 (mirrored) depth 6 | 401,771ms | 54,662ms | **7.4x** |

To run the deep tests:

```bash
# single-threaded
java -cp target/classes no.dervis.terminal_games.terminal_chess.PerftDeep --depth 7

# parallel — root moves split across all available cores
java -cp target/classes no.dervis.terminal_games.terminal_chess.PerftDeep --depth 7 --parallel
```


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




About multithreading:

This is actually expected behavior, and there are several reasons:                                                                                                                 
                                                                                                                                                                                     
  1. Lazy SMP has diminishing returns                                                                                                                                                
                                                            
  Lazy SMP's theoretical speedup is far less than linear. With 14 threads, you don't get 14x the effective search — typically you gain ~1-2 extra plies of depth. Most of those extra
   millions of nodes are redundant work: multiple threads searching the same positions independently, with only the TT sharing discoveries between them.
                                                                                                                                                                                     
  2. Depth gains at this level matter less                                                                                                                                           
   
  At the depths your engines reach in 1-1.5 seconds (likely depth 10-15), going one ply deeper provides a much smaller improvement in playing strength than going from depth 5 to 6  
  would. The evaluation and move ordering are identical between both engines — deeper search just refines the same judgments.
                                                                                                                                                                                     
  3. Chess between similar-strength engines draws a lot                                                                                                                              
   
  This is the biggest factor. In professional engine tournaments (TCEC, etc.), top engines draw 70-90% of games even with larger Elo gaps than yours. A ~50-70 Elo advantage (typical
   Lazy SMP gain) translates to roughly a 55-60% expected score — meaning you'd need hundreds of games to reliably see the difference. In a 4-game match, 3 draws and 1 win is
  entirely consistent with a meaningful parallel advantage.                                                                                                                          
                                                            
  4. Code-specific observations

  Looking at your ParallelChessAI, there are a couple of factors limiting its edge:                                                                                                  
   
  - Minimal depth diversity (line 279): thread 0 starts at depth 1, all others at depth 2. With 14 threads, 13 of them start at the same depth, reducing diversity.                  
  - Thread 0 controls time for everyone (line 307): when thread 0 decides time is up, all threads stop — even if a helper thread was about to complete a deeper search that would
  have changed the best move.                                                                                                                                                        
  - No move shuffling: all threads search root moves in the same order, increasing redundancy.
                                                                                                                                                                                     
  Bottom line: your results are normal. To measure the real gap, you'd want to run 100+ games. And even then, expect the parallel engine to win maybe 55-65% of decisive games — a   
  clear but not dominant advantage.   
  







Game analysis

Here are the opening moves of all 16 games, grouped by which engine played White:                                  

When Single (ChessAI) plays White — 8 games:                                                                                                                                       
                                                                                                                                                                                     
  ┌──────┬───────────────────────────────────────────────────────────────┐                                                                                                           
  │ Game │                         Opening moves                         │
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T1G1 │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3 Nc6 5. Bd3 Bd6            │                                                                                                         
  ├──────┼───────────────────────────────────────────────────────────────┤
  │ T1G3 │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3 Nc6 5. Bd3 Bd6 6. O-O     │                                                                                                           
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T2G1 │ 1. Nc3 d5 2. d4 Nc6 3. Nf3 e6 4. e4 Nf6                       │                                                                                                           
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T2G3 │ 1. e4 Nf6 2. e5 Nd5 3. c4 (outlier)                           │                                                                                                         
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T3G1 │ 1. e4 Nc6 2. d4 e6 3. Nc3 d5 (outlier)                        │                                                                                                         
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T3G3 │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3 Nc6 5. Bd3 Bb4            │                                                                                                         
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T4G1 │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3 Bd6 5. Bd3 Nc6 6. Nb5 Be7 │                                                                                                         
  ├──────┼───────────────────────────────────────────────────────────────┤                                                                                                           
  │ T4G3 │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3 Bd6 5. Nb5 Be7            │                                                                                                         
  └──────┴───────────────────────────────────────────────────────────────┘                                                                                                           

  6 out of 8 games open with the identical sequence 1. Nc3 d5 2. d4 Nf6 3. Nf3 e6 4. e3. The first 4 white moves are literally the same. T1G1 and T1G3 are identical through move 5  
  on both sides.

  When Parallel engines play White — 8 games:                                                                                                                                        
   
  ┌──────┬──────────┬──────────────────────────────────────────────────────┐                                                                                                         
  │ Game │  Engine  │                    Opening moves                     │                                                                                                       
  ├──────┼──────────┼──────────────────────────────────────────────────────┤
  │ T1G2 │ Parallel │ 1. Nf3 Nf6 2. Nc3 Nc6 3. d4 e6 4. e3 d5 5. Bd3 Bd6   │
  ├──────┼──────────┼──────────────────────────────────────────────────────┤
  │ T1G4 │ Parallel │ 1. e4 e5 2. Nc3 Nc6 3. Nf3 Nf6 4. Bb5 (Four Knights) │                                                                                                         
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T2G2 │ Parallel │ 1. Nc3 d5 2. d4 Nf6 3. e3 e6 4. Nf3 Bd6 5. Nb5 Be7   │                                                                                                         
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T2G4 │ Parallel │ 1. e4 d5 2. exd5 Nf6 3. c4 c6 4. Qa4 Qd7             │                                                                                                       
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T3G2 │ Improved │ 1. e4 d5 2. exd5 Nf6 3. c4 c6 4. Qa4 Qd7             │                                                                                                       
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T3G4 │ Improved │ 1. d4 Nf6 2. Nc3 d5 3. Nf3 e6 4. e3 Bd6              │                                                                                                       
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T4G2 │ Improved │ 1. Nc3 d5 2. d4 Nf6 3. Nf3 Nc6 4. Bf4 Bf5 5. e3 e6   │                                                                                                       
  ├──────┼──────────┼──────────────────────────────────────────────────────┤                                                                                                         
  │ T4G4 │ Improved │ 1. Nf3 Nf6 2. d4 d5 3. Nc3 e6 4. Qd3                 │                                                                                                       
  └──────┴──────────┴──────────────────────────────────────────────────────┘                                                                                                         
                                                                                                                                                                                   
  More variety than Single, but T2G4 and T3G2 are identical through move 5 (Scandinavian with 4. Qa4 Qd7) — played by Parallel and ImprovedParallel respectively.                    
                                                                                                                                                                                   
  Black responses are equally formulaic:                                                                                                                                             
                                                                                                                                                                                   
  Against 1. Nc3, black plays d5 in every single case (8/8).                                                                                                                         
  Against 1. d4 or 1. Nf3, black plays Nf6 every time.
  The black setup is almost always ...d5, ...Nf6, ...e6 in some order.                                                                                                               
                                                                                                                                                                                     
  The core problem: the engines are fully deterministic.                                                                                                                             
                                                                                                                                                                                     
  Given the same position, the same engine at the same time control will always play the same move. There is no randomness in the search — no opening book, no evaluation noise, no  
  random move selection. So:                                                                                                                                                       
                                                                                                                                                                                     
  - Single always evaluates 1. Nc3 as best from the starting position at all tested time controls                                                                                    
  - Parallel sometimes finds 1. e4 or 1. Nf3 because thread timing introduces slight non-determinism in which TT entries get written first — but it's limited
  - ImprovedParallel has the most variety thanks to root move shuffling, which creates more non-determinism in TT population                                                         
                                                                                                                                                                                     
  This means most games are replays of essentially the same game with minor deviations. The threefold repetition draws make even more sense now — the engines keep finding the same  
  "safe" shuffling patterns because they evaluate positions identically.                                                                                                             
                                                                                                                                                                                     
  To get meaningful tournament results, you'd need one or more of:                                                                                                                   
   
  1. An opening book — force different starting positions by playing the first N moves from a database of known openings                                                             
  2. Starting from preset FEN positions — the tournament picks a random middlegame position for each game                                                                          
  3. Small random noise in evaluation — add a tiny random bonus (e.g. +/- 5 centipawns) to break ties differently each game                                                          
  4. Chess960 (Fischer Random) — randomized back-rank positions                                                                                                                      
                                                                                                                                                                                     
  Want me to implement any of these?  
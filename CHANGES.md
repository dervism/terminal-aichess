# Changelog

## Search Performance Overhaul (2026-04-07)

Major performance rewrite across `Bitboard`, `ChessAI`, and `ParallelChessAI` to eliminate tactical blunders in sharp middlegame positions by reaching deeper search depths.

### Incremental Zobrist Hashing (Bitboard)
- Replaced per-node O(pieces) hash computation with O(1) incremental updates in `makeMove()`
- XOR-based: `ZOBRIST_PIECE[12][64]`, `ZOBRIST_SIDE`, `ZOBRIST_CASTLING[16]`, `ZOBRIST_EP_FILE[8]`
- Hash updated incrementally for: piece movement, captures, en passant, castling rook moves, promotions, castling rights changes, side flip
- `computeHashFromScratch()` retained for initialization and verification
- `initialiseBoard()` and `fromFEN()` compute initial hash at setup

### Board Copy Optimization (Bitboard)
- `copy()` shares history list reference (search only needs `lastMove` field)
- `deepCopy()` added for contexts that need full history (e.g., game loops)
- Eliminated LinkedList allocation per search node

### Rook-Capture Castling Rights Fix (Bitboard)
- Fixed bug: capturing a rook on its home square now removes the corresponding castling right
- Previously only handled rook movement, not rook capture

### Transposition Table Enlargement
- TT size increased from 1M to 4M entries (`1 << 22`)
- Reduces re-search of previously evaluated positions

### Logarithmic LMR Table
- Pre-computed `LMR_TABLE[64][64]` using `reduction = 0.77 + ln(depth) * ln(moveNumber) / 2.36`
- Replaces ad-hoc linear formula for smoother, more accurate reductions

### Promotion-Aware Move Ordering
- Queen promotions scored at 5M priority (between TT move at 10M and captures at 1M)
- Ensures promotions are searched early in the move list

### SEE Pruning in Main Search
- At depth <= 2: skip losing captures entirely (SEE < 0)
- At depth 3-6: let losing captures through to LMR (reduced search)
- Promotions excluded from SEE pruning

### Lazy Move Picking
- `pickMove()` selects the best remaining move one at a time (selection sort)
- Avoids full O(n log n) sort when beta cutoff happens early (most nodes)

### TT Probe in Quiescence Search
- Quiescence now probes the transposition table before evaluating captures
- Reduces redundant quiescence exploration for positions already evaluated

### Quiescence Cleanup
- Replaced `stream().filter().toList()` with simple loop for capture/promotion generation
- Quiescence now considers promotions in addition to captures

### Promotion-Safe Pruning
- Promotions excluded from futility pruning and LMP to prevent missing queen promotions

## Search Improvements (2026-04-06)

### Deleted
- **ParallelChessAI** (old) — removed the weaker parallel engine; renamed `ImprovedParallelChessAI` to `ParallelChessAI`

### New Features: Search Enhancements

Applied identically to both `ChessAI` (single-threaded) and `ParallelChessAI` (Lazy SMP).

#### Null-Move Pruning
- Skip the side-to-move's turn; if the opponent still can't beat beta, prune the subtree
- Adaptive reduction: R=2 normally, R=3 at depth >= 6
- Skipped when in check, at PV nodes, or in pawn-only endgames (zugzwang risk)

#### Static Exchange Evaluation (SEE)
- New `SEE.java` — simulates the full capture exchange sequence on a square
- Used for move ordering (replaces MVV-LVA for captures) and quiescence pruning of losing captures
- Added `Bitboard.makeNullMove()` for null-move pruning support

#### Aspiration Windows
- Root search uses a narrow window (25 cp) around the previous iteration's score
- On fail-low/fail-high: doubles the window delta and re-searches
- Falls back to full window if delta exceeds 1000 cp
- First 3 depths use full window (insufficient info for aspiration)

#### Reverse Futility Pruning (RFP)
- At non-PV nodes with depth <= 6, if `staticEval - 80*depth >= beta`, return staticEval
- Avoids expensive search in positions that are already clearly winning

#### Futility Pruning
- At depth <= 2, if `staticEval + margin < alpha`, skip quiet (non-capture) moves
- Margins: depth 1 = 200 cp, depth 2 = 350 cp

#### Razoring
- At depth <= 2, if `staticEval + margin < alpha`, verify with quiescence search
- If quiescence confirms the position is below alpha, return immediately
- Margins: depth 1 = 300 cp, depth 2 = 500 cp

#### Late Move Pruning (LMP)
- At non-PV nodes with depth <= 3, skip late quiet moves entirely
- Thresholds: depth 1 = 5 moves, depth 2 = 8, depth 3 = 12

#### Internal Iterative Deepening (IID)
- At PV nodes with no TT move and depth >= 4, do a shallow search (depth - 2) first
- Re-probes the TT to get a move for ordering

#### Singular Extensions
- At depth >= 8, if the TT move is significantly better than all alternatives, extend it by 1 ply
- Uses a reduced search excluding the TT move to verify singularity
- Singular beta = `ttScore - 2 * depth`

#### Countermove Heuristic
- New `countermoves[2][64][64]` table tracks which move refuted the previous move
- Scored at 700K in move ordering (between killer moves at 800K and history heuristic)
- Updated on beta cutoffs for quiet moves

#### History-Informed LMR
- LMR reduction adjusted by history score: bad history (< -1000) increases reduction, good history (> 4000) decreases it
- History malus: on beta cutoff, all previously searched quiet moves are penalized (`-depth*depth`)
- History values clamped to [-16384, 16384] to prevent overflow

#### Delta Pruning (Quiescence)
- In quiescence search, skips captures where `standPat + capturedValue + 200 < alpha`
- Avoids searching captures that cannot possibly raise the score above alpha

### Move Ordering (updated)

Priority order: TT move (10M) > winning/equal SEE captures (1M+) > killer 0 (900K) > killer 1 (800K) > countermove (700K) > history heuristic > losing captures (negative SEE)

## Evaluation Improvements (2026-04-06)

Applied to `Evaluation.java` — used by both engines.

### New Infrastructure
- Rank masks: `RANK_1` through `RANK_8`, `RANKS[]` array
- Center files mask: `CENTER_FILES` (C-F files)
- `chebyshevDistance(sq1, sq2)` helper for king-pawn proximity
- Imports: `PawnAttacks`, `KingAttacks`

### New Features

#### Connected Rooks (MG+EG)
Two rooks seeing each other along a rank or file (no pieces between), checked via `MagicBitboard.rookAttacks()`.
- Bonus: +15 MG, +10 EG

#### Knight Outposts (MG+EG)
Knight in enemy half with no enemy pawn on adjacent files at same or more advanced ranks. Extra bonus if defended by own pawn and if on center files (C-F).
- Unsupported: +20 MG (+3 center), +10 EG
- Supported: +30 MG (+5 center), +15 EG

#### King Danger Zone (MG only)
Count enemy pieces attacking the king zone (king square + 8 adjacent squares). Each piece type has an attack weight (N=2, B=2, R=3, Q=5). Quadratic penalty: `weight^2 / 4`, capped at 500. Requires >= 2 attackers.
- Penalty: 0 to -500 cp

#### Space Advantage (MG only)
Count safe squares in center files (C-F) on own side of board (ranks 2-4 for white, 5-7 for black), excluding squares attacked by enemy pawns. Optimized bitwise pawn attack computation.
- Bonus: +2 cp per safe square

#### Pawn Storms (MG only)
When kings are castled on opposite sides (>= 4 files apart), bonus for own pawns advanced on the 3 files around the enemy king.
- Bonus by rank advancement: 0/0/0/+10/+25/+50/0

#### Rook on 7th Rank (MG+EG)
Rook on the penultimate rank when enemy king is on back rank or enemy pawns are on that rank.
- Bonus: +25 MG, +35 EG (per rook)

#### Backward Pawns (MG+EG)
Added inside `evaluatePawnStructure`. A pawn whose stop square is attacked by an enemy pawn and no friendly pawn on adjacent files at same or lower rank can support it.
- Penalty: -10 MG, -15 EG

#### King-Pawn Proximity (EG only)
For each passed pawn, bonus for own king proximity minus penalty for enemy king proximity. Uses Chebyshev distance.
- Bonus: +5 EG per rank of closeness (own king) / -5 EG per rank (enemy king)

#### Minor Piece Imbalance (MG+EG)
Knights benefit from more pawns (closed positions), bishops from fewer pawns (open). Adjustment per piece based on total pawn count minus 8.
- Knight: +3 MG / +2 EG per pawn above 8 per knight
- Bishop: -3 MG / -2 EG per pawn above 8 per bishop

## Passed Pawn Evaluation Improvements (2026-04-06)

Dedicated `evaluatePassedPawns` method replaces the simple bonus previously in `evaluatePawnStructure`. Addresses the key weakness of severely undervaluing far-advanced passed pawns.

### Base Bonus Increase
- 6th rank: 60→80 MG, 120→180 EG
- 7th rank: 100→150 MG, 200→350 EG

### Unblockaded Passer (MG+EG)
If the stop square (square ahead) is empty, bonus scales with advancement rank.
- +5 MG, +15 EG per advancement rank

### Free Path to Promotion (MG+EG)
If all squares from the pawn to the promotion square are empty.
- +15 MG, +50 EG

### Rook Behind Passed Pawn (MG+EG)
Friendly rook on the same file behind the pawn.
- +20 MG, +30 EG

### Protected Passed Pawn (MG+EG)
Passed pawn defended by another friendly pawn.
- +15 MG, +25 EG

### Unstoppable Passed Pawn (EG only)
Enemy king is outside the "square of the pawn" (Chebyshev distance to promotion square exceeds pawn's moves to promote) AND the path is clear.
- +400 EG (combined with 350 base for 7th-rank = 750+ total, approaching queen value)

### Evaluation Order in evaluate()
1. Material + PST
2. Bishop pair
3. Minor piece imbalance
4. Pawn structure (doubled, isolated, backward)
4b. Passed pawns (base + unblocked + free path + rook support + protected + unstoppable)
5. Rook on open/semi-open files
6. Connected rooks
7. Rook on 7th rank
8. Knight outposts
9. King safety (pawn shield, MG)
10. King danger zone (MG)
11. Pawn storms (MG)
12. Mobility
13. Space advantage (MG)
14. King-pawn proximity (EG)
15. MG/EG interpolation + tempo

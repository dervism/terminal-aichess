# Changelog

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

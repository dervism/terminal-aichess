# History - Old Attack Detection

These classes are the original approach to detecting checks, attacks, and blockers
before Magic Bitboards were introduced. They are kept here for reference.

## How it worked

### Step 1: Precompute empty-board attack masks

`RookAttacks`, `BishopAttacks` and `QueenAttacks` each hold a `long[64]` table
built at startup. For every square, each class walks the piece's rays to the
board edge and sets every reachable bit. These masks answer the question
*"which squares can this piece reach on a completely empty board?"* but say
nothing about blockers.

### Step 2: Walk each ray at query time

`CheckHelper.getDirectionalSlidingAttacks()` is where the real work happened.
Given a square, a direction, and the set of occupied squares (blockers), it
iterates step-by-step along the ray using Java Streams:

```java
IntStream.iterate(
        getNextSquareInDirection(square, direction),
        s -> s != -1 && ((1L << s) & attackMask) != 0,
        s -> getNextSquareInDirection(s, direction))
    .mapToLong(s -> 1L << s)
    .takeWhile(bitboard -> (bitboard & blockers) == 0)
    ...
```

It walks until it hits a blocker or runs off the board, then picks up the first
blocker square (so the piece attacks *through to* the blocker but not beyond).
This is called once per direction - eight times per square for queens, four for
rooks and bishops.

### Why it was slow

Every call to `isSquareAttackedBySlidingPiece()` iterated up to eight rays, each
stepping square-by-square. In a search that evaluates millions of positions per
second, this added up fast.

## What replaced it

`MagicBitboard` (in `board/MagicBitboard.java`) replaces all of the above with a
single **multiply-shift-lookup** per piece type:

```java
long attacks = ROOK_TABLE[square][(occupancy & mask) * magic >>> shift];
```

The "magic number" is chosen so that multiplying the relevant occupancy bits and
shifting right produces a unique table index for each blocker configuration.
The table is built once at startup; after that, every attack query is O(1) --
no loops, no streams, no per-direction iteration.

## Files

| File | Purpose |
|------|---------|
| `CheckHelper.java` | Check/attack detection using ray iteration |
| `RookAttacks.java` | Empty-board rook attack masks |
| `BishopAttacks.java` | Empty-board bishop attack masks |
| `QueenAttacks.java` | Empty-board queen attack masks (rook + bishop union) |

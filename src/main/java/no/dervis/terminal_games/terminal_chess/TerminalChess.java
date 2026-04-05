package no.dervis.terminal_games.terminal_chess;

import no.dervis.terminal_games.terminal_chess.ai.ChessAI;
import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple2;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.BoardPrinter;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

import static no.dervis.terminal_games.terminal_chess.board.Chess.black;
import static no.dervis.terminal_games.terminal_chess.board.Chess.white;

public class TerminalChess implements BoardPrinter {

    final static Scanner scanner = new Scanner(System.in);

    final static Function<Tuple2<String, Integer>, Tuple3> t2ToT3 = value ->
            new Tuple3(
                    /*row*/    value.right(),
                    /*column*/ columnToIndex.apply(value.left()),
                    /*index*/  indexFn.apply(value.right(), columnToIndex.apply(value.left())));

    public static void main(String[] args) {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);
        ChessAI ai = new ChessAI();

        System.out.println(Chess.boardToStr.apply(board, true));
        boolean status = true;

        System.out.print("Play white or black? (w/b) ");
        boolean userColor = scanner.nextLine().equalsIgnoreCase("w");
        int userTurn = userColor ? white : black;

        long thinkTimeMs = chooseDifficulty();

        String userInput = "";

        while (status) {

            if (userTurn == board.turn()) {
                userInput = getMoveFromUser();

                if (userInput.equals("q")) {
                    scanner.close();
                    System.out.println("Quitting.");
                    break;
                }

                var userMoves = generator.generateMoves(board.turn());

                // Try SAN first, then fall back to coordinate format
                List<Tuple2<Integer, Move>> matching = matchSAN(userInput, userMoves, board);

                if (matching.isEmpty()) {
                    var parsedInput = parseMove(userInput);
                    if (parsedInput.isPresent()) {
                        var parsedMove = parsedInput.get();
                        matching = userMoves.stream()
                                .map(move -> new Tuple2<>(move, Move.createMove(move, board)))
                                .filter(t2 -> t2.right().fromSquare() == parsedMove.left().index())
                                .filter(t2 -> t2.right().toSquare() == parsedMove.right().index())
                                .toList();
                    }
                }

                if (matching.isEmpty()) {
                    System.out.println("Please enter a legal move.");
                } else if (matching.size() == 1) {
                    board.makeMove(matching.getFirst().left());
                } else {
                    int chosen = askPromotionPiece();
                    matching.stream()
                            .filter(t2 -> t2.right().promotionPiece() == chosen)
                            .findFirst()
                            .ifPresentOrElse(
                                    t2 -> board.makeMove(t2.left()),
                                    () -> System.out.println("Invalid promotion choice."));
                }
            } else {
                getMoveFromComputer(ai, board, thinkTimeMs);
            }

            clearTerminal();
            System.out.println(Chess.boardToStr.apply(board, userColor));

            Generator.GameState gameState = generator.getGameState(board.turn());
            switch (gameState) {
                case CHECKMATE -> {
                    int loser = board.turn();
                    System.out.println("Checkmate! " + (loser == white ? "Black" : "White") + " wins!");
                    status = false;
                }
                case STALEMATE -> {
                    System.out.println("Draw by stalemate!");
                    status = false;
                }
                case INSUFFICIENT_MATERIAL -> {
                    System.out.println("Draw by insufficient material!");
                    status = false;
                }
                case ONGOING -> {
                    int kingSquare = Long.numberOfTrailingZeros(board.kingPiece(board.turn()));
                    if (Generator.isKingInCheck(board, board.turn(), kingSquare)) {
                        System.out.println("Check!");
                    }
                }
            }
        }
    }

    private static void getMoveFromComputer(ChessAI ai, Bitboard board, long thinkTimeMs) {
        System.out.println("Thinking...");
        int move = ai.findBestMove(board, thinkTimeMs);
        if (move == 0) {
            System.out.println("No legal moves available.");
            return;
        }
        Move computerMove = Move.createMove(move, board);
        System.out.println("Computer move: " + computerMove.toStringShort());
        board.makeMove(move);
    }

    private static Optional<Tuple2<Tuple3, Tuple3>> parseMove(String move) {
        try {
            String[] split = move.split("-");
            String[] from = split[0].split("");
            String[] to = split[1].split("");

            Tuple3 moveFrom = t2ToT3.apply(new Tuple2<>(from[0].toUpperCase(), Integer.parseInt(from[1])-1));
            Tuple3 moveTo = t2ToT3.apply(new Tuple2<>(to[0].toUpperCase(), Integer.parseInt(to[1])-1));

            return Optional.of(Tuple2.of(moveFrom, moveTo));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static List<Tuple2<Integer, Move>> matchSAN(String input, List<Integer> legalMoves, Bitboard board) {
        try {
            String san = input.trim().replaceAll("[+#]", "");
            if (san.isEmpty()) return List.of();

            // Castling
            if (san.equals("O-O") || san.equals("0-0")) {
                return legalMoves.stream()
                        .map(m -> new Tuple2<>(m, Move.createMove(m, board)))
                        .filter(t -> t.right().moveType() == Chess.MoveType.CASTLE_KING_SIDE.ordinal())
                        .toList();
            }
            if (san.equals("O-O-O") || san.equals("0-0-0")) {
                return legalMoves.stream()
                        .map(m -> new Tuple2<>(m, Move.createMove(m, board)))
                        .filter(t -> t.right().moveType() == Chess.MoveType.CASTLE_QUEEN_SIDE.ordinal())
                        .toList();
            }

            // Parse promotion suffix (e.g. e8=Q)
            int promoPiece = 0;
            if (san.contains("=")) {
                int eqIdx = san.indexOf('=');
                if (eqIdx + 1 < san.length()) {
                    promoPiece = switch (san.charAt(eqIdx + 1)) {
                        case 'N' -> Chess.knight;
                        case 'B' -> Chess.bishop;
                        case 'R' -> Chess.rook;
                        case 'Q' -> Chess.queen;
                        default -> 0;
                    };
                }
                san = san.substring(0, eqIdx);
            }

            // Determine piece type from first character
            int pieceType;
            int startIdx;
            char first = san.charAt(0);
            if (first >= 'A' && first <= 'Z') {
                pieceType = switch (first) {
                    case 'N' -> Chess.knight;
                    case 'B' -> Chess.bishop;
                    case 'R' -> Chess.rook;
                    case 'Q' -> Chess.queen;
                    case 'K' -> Chess.king;
                    default -> -1;
                };
                if (pieceType == -1) return List.of();
                startIdx = 1;
            } else {
                pieceType = Chess.pawn;
                startIdx = 0;
            }

            // Strip piece letter and capture marker
            String rest = san.substring(startIdx).replace("x", "");
            if (rest.length() < 2) return List.of();

            // Target square is the last two characters
            char fileChar = rest.charAt(rest.length() - 2);
            char rankChar = rest.charAt(rest.length() - 1);
            int targetFile = fileChar - 'a';
            int targetRank = rankChar - '1';
            if (targetFile < 0 || targetFile > 7 || targetRank < 0 || targetRank > 7) return List.of();
            int targetSquare = targetRank * 8 + targetFile;

            // Disambiguation characters (e.g. Nbd2 -> "b", R1d1 -> "1", Qh4e1 -> "h4")
            String disambig = rest.substring(0, rest.length() - 2);
            int disambigFile = -1;
            int disambigRank = -1;
            for (char c : disambig.toCharArray()) {
                if (c >= 'a' && c <= 'h') disambigFile = c - 'a';
                else if (c >= '1' && c <= '8') disambigRank = c - '1';
            }

            final int pt = pieceType, ts = targetSquare;
            final int df = disambigFile, dr = disambigRank, pp = promoPiece;

            return legalMoves.stream()
                    .map(m -> new Tuple2<>(m, Move.createMove(m, board)))
                    .filter(t -> t.right().piece() == pt)
                    .filter(t -> t.right().toSquare() == ts)
                    .filter(t -> df == -1 || (t.right().fromSquare() % 8) == df)
                    .filter(t -> dr == -1 || (t.right().fromSquare() / 8) == dr)
                    .filter(t -> pp == 0 || t.right().promotionPiece() == pp)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String getMoveFromUser() {
        System.out.print("Enter move (e.g. Nf3, e4, exd5, O-O or e2-e4): ");
        return scanner.nextLine();
    }

    private static int askPromotionPiece() {
        System.out.println("Promote pawn to:");
        System.out.println("  1. Knight (N)");
        System.out.println("  2. Bishop (B)");
        System.out.println("  3. Rook   (R)");
        System.out.println("  4. Queen  (Q)");
        System.out.print("Choice (1-4): ");
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1", "n", "N" -> Chess.knight;
            case "2", "b", "B" -> Chess.bishop;
            case "3", "r", "R" -> Chess.rook;
            case "4", "q", "Q" -> Chess.queen;
            default -> {
                System.out.println("Defaulting to Queen.");
                yield Chess.queen;
            }
        };
    }

    private static long chooseDifficulty() {
        System.out.println("Select difficulty:");
        System.out.println("  1. Easy       (1 second)");
        System.out.println("  2. Medium     (3 seconds)");
        System.out.println("  3. Hard       (5 seconds)");
        System.out.println("  4. Expert     (10 seconds)");
        System.out.println("  5. Extra hard (50 seconds)");
        System.out.print("Choice (1-5): ");
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> 1000;
            case "2" -> 3000;
            case "3" -> 5000;
            case "4" -> 10000;
            case "5" -> 50000;
            default -> {
                System.out.println("Invalid choice, defaulting to Medium.");
                yield 3000;
            }
        };
    }

    private static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        //System.out.print("\033\143");
        System.out.flush();
    }
}




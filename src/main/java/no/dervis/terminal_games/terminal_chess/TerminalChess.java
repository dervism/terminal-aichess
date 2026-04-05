package no.dervis.terminal_games.terminal_chess;

import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ChessAI;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.Engine;
import no.dervis.terminal_games.terminal_chess.ai.alphabeta.ParallelChessAI;
import no.dervis.terminal_games.terminal_chess.board.Bitboard;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple2;
import no.dervis.terminal_games.terminal_chess.board.Board.Tuple3;
import no.dervis.terminal_games.terminal_chess.board.BoardPrinter;
import no.dervis.terminal_games.terminal_chess.board.Chess;
import no.dervis.terminal_games.terminal_chess.moves.Move;
import no.dervis.terminal_games.terminal_chess.moves.generator.Generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

import static no.dervis.terminal_games.terminal_chess.board.Chess.black;
import static no.dervis.terminal_games.terminal_chess.board.Chess.white;

public class TerminalChess implements BoardPrinter {

    final static Scanner scanner = new Scanner(System.in);

    private static final Path SAVES_DIR = Path.of("saves");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    final static Function<Tuple2<String, Integer>, Tuple3> t2ToT3 = value ->
            new Tuple3(
                    /*row*/    value.right(),
                    /*column*/ columnToIndex.apply(value.left()),
                    /*index*/  indexFn.apply(value.right(), columnToIndex.apply(value.left())));

    public static void main(String[] args) {
        boolean playAgain = true;

        while (playAgain) {
            playAgain = playGame();
        }

        scanner.close();
    }

    private static boolean playGame() {
        Bitboard board = new Bitboard();
        board.initialiseBoard();
        Generator generator = new Generator(board);

        System.out.println(Chess.boardToStr.apply(board, true));

        System.out.print("Play white or black? (w/b) ");
        boolean userColor = scanner.nextLine().equalsIgnoreCase("w");
        int userTurn = userColor ? white : black;

        String strengthLabel = chooseDifficultyLabel();
        long thinkTimeMs = thinkTimeFromLabel(strengthLabel);
        String engineLabel = chooseEngineLabel();
        Engine ai = createEngine(engineLabel);

        String whitePlayer = (userTurn == white) ? "player" : "computer";
        String blackPlayer = (userTurn == black) ? "player" : "computer";

        List<String> moveHistory = new ArrayList<>();
        boolean status = true;
        String result = "";

        while (status) {

            if (userTurn == board.turn()) {
                String userInput = getMoveFromUser();

                if (userInput.equals("q")) {
                    result = "abandoned";
                    break;
                }

                if (userInput.equals("r")) {
                    String winner = (userTurn == white) ? "Black" : "White";
                    System.out.println(winner + " wins by resignation.");
                    result = (userTurn == white) ? "0-1" : "1-0";
                    status = false;
                    break;
                }

                if (userInput.equalsIgnoreCase("fen")) {
                    System.out.println("FEN: " + board.toFEN());
                    continue;
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
                    continue;
                } else if (matching.size() == 1) {
                    moveHistory.add(matching.getFirst().right().toAlgebraic());
                    board.makeMove(matching.getFirst().left());
                } else {
                    // Multiple matches: check if it's a promotion (same from-square,
                    // different promotion pieces) or an ambiguous move (different from-squares)
                    boolean isPromotion = matching.stream()
                            .map(t2 -> t2.right().fromSquare())
                            .distinct()
                            .count() == 1;

                    if (isPromotion) {
                        int chosen = askPromotionPiece();
                        var selected = matching.stream()
                                .filter(t2 -> t2.right().promotionPiece() == chosen)
                                .findFirst();
                        if (selected.isPresent()) {
                            moveHistory.add(selected.get().right().toAlgebraic());
                            board.makeMove(selected.get().left());
                        } else {
                            System.out.println("Invalid promotion choice.");
                            continue;
                        }
                    } else {
                        System.out.println("Ambiguous move. Please select one of the pieces (e.g. Reg3, Rgg3).");
                        continue;
                    }
                }
            } else {
                int move = getMoveFromComputer(ai, board, thinkTimeMs);
                if (move != 0) {
                    moveHistory.add(Move.createMove(move, board).toAlgebraic());
                    board.makeMove(move);
                }
            }

            clearTerminal();
            System.out.println(Chess.boardToStr.apply(board, userColor));

            Generator.GameState gameState = generator.getGameState(board.turn());
            switch (gameState) {
                case CHECKMATE -> {
                    int loser = board.turn();
                    String winner = (loser == white) ? "Black" : "White";
                    System.out.println("Checkmate! " + winner + " wins!");
                    result = (loser == white) ? "0-1" : "1-0";
                    status = false;
                }
                case STALEMATE -> {
                    System.out.println("Draw by stalemate!");
                    result = "1/2-1/2";
                    status = false;
                }
                case INSUFFICIENT_MATERIAL -> {
                    System.out.println("Draw by insufficient material!");
                    result = "1/2-1/2";
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

        return handleGameOver(board, result, whitePlayer, blackPlayer,
                engineLabel, strengthLabel, moveHistory);
    }

    private static boolean handleGameOver(Bitboard board, String result,
                                          String whitePlayer, String blackPlayer,
                                          String engineLabel, String strengthLabel,
                                          List<String> moveHistory) {
        System.out.println();
        System.out.println("Game over.");
        System.out.println("  1. New game without saving");
        System.out.println("  2. Save and start new game");
        System.out.println("  3. Save game and quit");
        System.out.println("  4. Quit");
        System.out.print("Choice (1-4): ");
        String choice = scanner.nextLine().trim();

        return switch (choice) {
            case "2" -> {
                saveGame(board, result, whitePlayer, blackPlayer,
                        engineLabel, strengthLabel, moveHistory);
                yield true;
            }
            case "3" -> {
                saveGame(board, result, whitePlayer, blackPlayer,
                        engineLabel, strengthLabel, moveHistory);
                System.out.println("Goodbye.");
                yield false;
            }
            case "4" -> {
                System.out.println("Goodbye.");
                yield false;
            }
            default -> true;
        };
    }

    private static void saveGame(Bitboard board, String result,
                                 String whitePlayer, String blackPlayer,
                                 String engineLabel, String strengthLabel,
                                 List<String> moveHistory) {
        try {
            Files.createDirectories(SAVES_DIR);

            LocalDateTime now = LocalDateTime.now();
            String filename = "game_" + now.format(FILE_FMT) + ".txt";
            Path file = SAVES_DIR.resolve(filename);

            StringBuilder sb = new StringBuilder();
            sb.append("Date:     ").append(now.format(DISPLAY_FMT)).append('\n');
            sb.append("White:    ").append(whitePlayer).append('\n');
            sb.append("Black:    ").append(blackPlayer).append('\n');
            sb.append("Engine:   ").append(engineLabel).append('\n');
            sb.append("Strength: ").append(strengthLabel).append('\n');
            sb.append("Result:   ").append(result.isEmpty() ? "unknown" : result).append('\n');
            sb.append("FEN:      ").append(board.toFEN()).append('\n');
            sb.append('\n');
            sb.append("Moves:\n");

            for (int i = 0; i < moveHistory.size(); i++) {
                if (i % 2 == 0) {
                    sb.append(String.format("%d. %s", (i / 2) + 1, moveHistory.get(i)));
                } else {
                    sb.append(" ").append(moveHistory.get(i)).append('\n');
                }
            }
            // Trailing newline if last move was white's (odd total)
            if (moveHistory.size() % 2 == 1) sb.append('\n');

            Files.writeString(file, sb.toString());
            System.out.println("Game saved to " + file);
        } catch (IOException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    private static int getMoveFromComputer(Engine ai, Bitboard board, long thinkTimeMs) {
        System.out.println("Thinking...");
        int move = ai.findBestMove(board, thinkTimeMs);
        if (move == 0) {
            System.out.println("No legal moves available.");
            return 0;
        }
        Move computerMove = Move.createMove(move, board);
        System.out.println("Computer move: " + computerMove.toStringShort());
        return move;
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

    static List<Tuple2<Integer, Move>> matchSAN(String input, List<Integer> legalMoves, Bitboard board) {
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
        System.out.print("Enter move, or command (fen=print board, r=resign): ");
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

    private static String chooseDifficultyLabel() {
        System.out.println("Select difficulty:");
        System.out.println("  1. Easy       (1 second)");
        System.out.println("  2. Medium     (3 seconds)");
        System.out.println("  3. Hard       (5 seconds)");
        System.out.println("  4. Expert     (10 seconds)");
        System.out.println("  5. Extra hard (50 seconds)");
        System.out.print("Choice (1-5): ");
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> "Easy";
            case "2" -> "Medium";
            case "3" -> "Hard";
            case "4" -> "Expert";
            case "5" -> "Extra hard";
            default -> {
                System.out.println("Invalid choice, defaulting to Medium.");
                yield "Medium";
            }
        };
    }

    private static long thinkTimeFromLabel(String label) {
        return switch (label) {
            case "Easy" -> 1000;
            case "Medium" -> 3000;
            case "Hard" -> 5000;
            case "Expert" -> 10000;
            case "Extra hard" -> 50000;
            default -> 3000;
        };
    }

    private static String chooseEngineLabel() {
        System.out.println("Choose engine:");
        System.out.println("  1. Single-threaded");
        System.out.println("  2. Parallel (Lazy SMP)");
        System.out.print("Engine (1/2): ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("2")) {
            int cores = Runtime.getRuntime().availableProcessors();
            System.out.println("Using parallel engine with " + cores + " threads.");
            return "Parallel (Lazy SMP)";
        }
        System.out.println("Using single-threaded engine.");
        return "Single-threaded";
    }

    private static Engine createEngine(String engineLabel) {
        if (engineLabel.startsWith("Parallel")) {
            return new ParallelChessAI();
        }
        return new ChessAI();
    }

    private static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        //System.out.print("\033\143");
        System.out.flush();
    }
}

import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.lang.Math;

public class Main {
    public static void main(String[] args) {
        /*
        PASTE INTO TERMINAL:
        & "C:\Users\cbh5981\Documents\jdk-27\bin\java.exe" "C:\Users\cbh5981\Documents\Chess-project-in-java\chess project.java"
        */
        try {
            run();
        } catch (NoSuchElementException n) {
            System.out.println("\nInterrupt");
        } catch (GameAlreadyOverException g) {
            String message = switch (g.getMessage()) {
                case "1" -> "Checkmate white wins";
                case "2" -> "Checkmate black wins";
                default -> "Draw";
            };
            printRed("Game already over:");
            System.out.println(message);
        } catch (InvalidFENException b) {
            printRed(b.getMessage());
        }
    }
    static long nodes = 0L;
    static int getTime() {
        LocalTime localTime = LocalTime.now();
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("HH");
        DateTimeFormatter minute = DateTimeFormatter.ofPattern("mm");
        DateTimeFormatter second = DateTimeFormatter.ofPattern("ss");
        DateTimeFormatter millisecond = DateTimeFormatter.ofPattern("SSS");
        String hours = localTime.format(hour);
        String minutes = localTime.format(minute);
        String seconds = localTime.format(second);
        String milliseconds = localTime.format(millisecond);
        return Integer.parseInt(milliseconds) + 1000 * Integer.parseInt(seconds) + 60000 * Integer.parseInt(minutes) + 3600000 * Integer.parseInt(hours);
    }
    static void printRed(String stuff) {
        System.out.print("\u001B[31m");
        System.out.print(stuff);
        System.out.print("\u001B[0m");
        System.out.print("\n");
    }
    static void run() throws GameAlreadyOverException, InvalidFENException {
        Scanner scanner = new Scanner(System.in);
        int[] setup = new int[72];
        System.out.print("FEN: ");
        String fen = scanner.nextLine();
        if (Reference.openings.containsKey(fen)) {
            System.out.println(Reference.openings.get(fen));
            return;
        }
        String[] element = fen.trim().split("\\s+");
        if (element.length != 6) {
            throw new InvalidFENException("Invalid FEN element 1: board");
        }
        String[] rows = element[0].split("/");
        if (rows.length != 8) {
            throw new InvalidFENException("Invalid FEN element 1: board");
        }
        for (int i = 0; i < 8; i++) {
            char[] row = rows[i].toCharArray();
            int current = 0;
            for (char letter : row) {
                if (Character.isDigit(letter)) {
                    int emptySquares = letter - '0';
                    if (emptySquares < 1 || emptySquares > 8 || current + emptySquares > 8) {
                        throw new InvalidFENException("Invalid FEN element 1: board");
                    }
                    current += emptySquares;
                } else {
                    int piece = getId(letter);
                    if (piece == 0 || current >= 8) {
                        throw new InvalidFENException("Invalid FEN element 1: board");
                    }
                    setup[8 * (7 - i) + current] = piece;
                    current++;
                }
            }
            if (current != 8) {
                throw new InvalidFENException("Invalid FEN element 1: board");
            }
        }
        int whiteKings = 0;
        int blackKings = 0;
        for (int i = 0; i < 64; i++) {
            if (setup[i] == 1) whiteKings++;
            if (setup[i] == 2) blackKings++;
        }
        if (whiteKings != 1 || blackKings != 1) {
            throw new InvalidFENException("Invalid FEN element 1: board must contain one king per side");
        }
        if (!"w".equals(element[1]) && !"b".equals(element[1])) {
            throw new InvalidFENException("Invalid FEN element 2: color to move");
        }
        setup[64] = ("w".equals(element[1])) ? 1 : 0;
        if ("-".equals(element[2])) {
            setup[65] = 0;
            setup[66] = 0;
            setup[67] = 0;
            setup[68] = 0;
        } else for (char letter : element[2].toCharArray()) {
            switch (letter) {
                case 'K':
                    if (setup[65] != 0) throw new InvalidFENException("Invalid FEN element 3: castling rights");
                    setup[65] = 1;
                    break;
                case 'Q':
                    if (setup[66] != 0) throw new InvalidFENException("Invalid FEN element 3: castling rights");
                    setup[66] = 1;
                    break;
                case 'k':
                    if (setup[67] != 0) throw new InvalidFENException("Invalid FEN element 3: castling rights");
                    setup[67] = 1;
                    break;
                case 'q':
                    if (setup[68] != 0) throw new InvalidFENException("Invalid FEN element 3: castling rights");
                    setup[68] = 1;
                    break;
                default:
                    throw new InvalidFENException("Invalid FEN element 3: castling rights");
            }
        }
        setup[69] = ("-".equals(element[3])) ? 0 : parseSquare(element[3]);
        if (!"-".equals(element[3]) && setup[69] == 0) {
            throw new InvalidFENException("Invalid FEN element 4: en passant square");
        }
        if (setup[69] != 0 && Main.getY(setup[69]) != (setup[64] == 1 ? 6 : 3)) {
            throw new InvalidFENException("Invalid FEN element 4: en passant square");
        }
        try {
            setup[70] = Integer.parseInt(element[4]);
            setup[71] = Integer.parseInt(element[5]);
        } catch (NumberFormatException exception) {
            throw new InvalidFENException("Invalid FEN element 5 or 6: move counters");
        }
        if (setup[70] < 0 || setup[71] < 1) {
            throw new InvalidFENException("Invalid FEN element 5 or 6: move counters");
        }
        System.out.print("Depth (number of full moves): ");
        int depth = scanner.nextInt() * 2;
        System.out.print("Time limit: ");
        int timeLimit = scanner.nextInt();
        Position pos = new Position(setup, timeLimit * 1000, depth);
        pos.display();
        //pos.history.add(condense(setup));
        try {
            pos.gameStatus();
        } catch (GameEndsException status) {
            throw new GameAlreadyOverException(status.getMessage());
        }
        int[] lastCompleted = null;
        if (depth > 0) {
            for (int i = 1; i <= depth; i++){
                System.out.println("Running at depth: " + Integer.toString(i));
                System.out.println("Calculating...");
                double start = getTime();
                nodes = 0;
                try {
                    lastCompleted = pos.evalDepth(i);
                } catch (TimeExceededException t) {
                    printRed("Search exceeded execution time at depth: " + i);
                    int raw;
                    double rawEval, evaluation;
                    if (lastCompleted != null) {
                        raw = lastCompleted[0];
                        rawEval = raw;
                        evaluation = rawEval / 10.0;
                        System.out.println("Evaluation: " + Double.toString(evaluation));
                        System.out.println("Best move: " + parseNumbers(lastCompleted[1], lastCompleted[2]));
                    } else {
                        byte[][] legalMoves = pos.sortMoves(pos.generateMoves());
                        raw = pos.currentCount(false);
                        rawEval = raw;
                        evaluation = rawEval / 10.0;
                        System.out.println("Evaluation: " + Double.toString(evaluation));
                        System.out.println("Best move: " + parseNumbers(legalMoves[0][0], legalMoves[1][0]));
                    }
                    return;
                }
                double end = getTime();
                double timeTaken = (end - start) / 1000.0;
                int raw = lastCompleted[0];
                double rawEval = raw;
                double evaluation = rawEval / 10.0; // convert decipawns (1/10 pawn) to pawns
                System.out.println("Evaluation: " + Double.toString(evaluation));
                System.out.println("Best move: " + parseNumbers(lastCompleted[1], lastCompleted[2]));
                System.out.println("Nodes per second: " + Double.toString(nodes / timeTaken));
                System.out.println("in " + Double.toString(timeTaken) + " seconds\n");
            }
        }
    }
    static int parseSquare(String square) {
        //return square id
        //a1 is 1, b1 is 2, a2 is 9, etc, h8 is 64
        if (square.length() != 2 || square.charAt(0) < 'a' || square.charAt(0) > 'h' || square.charAt(1) < '1' || square.charAt(1) > '8') {
            return 0;
        }
        int x = ((int) square.charAt(0)) - 96;
        int y = square.charAt(1) - '0';
        return x + 8 * (y - 1);
    }
    static String parseNumber(int square) {
        if (square == 0) {
            return "null";
        }
        int x = square % 8;
        if (x == 0) {
            x = 8;
        }
        int y = (square % 8 == 0) ? square / 8 : square / 8 + 1;
        return Character.toString((char) (x + 96)) + Integer.toString(y);
    }
    static String parseNumbers(int start, int end) {
        //return move from start and end squares
        if (start == 0 || end == 0) {
            return "null";
        }
        return parseNumber(start) + parseNumber(end);
    }
    static int getX(int square) {
        return (square % 8 == 0) ? 8 : square % 8;
    }
    static int getY(int square) {
        return (square % 8 == 0) ? square / 8 : square / 8 + 1;
    }
    static int translate(int square, int x, int y) {
        if (getX(square) + x > 8 || getX(square) + x < 1) {
            return 0;
        }
        if (getY(square) + y > 8 || getY(square) + y < 1) {
            return 0;
        }
        return square + x + 8 * y;
    }
    static int getId(char piece) {
        return switch (piece) {
            case 'K' -> 1;
            case 'k' -> 2;
            case 'Q' -> 3;
            case 'q' -> 4;
            case 'R' -> 5;
            case 'r' -> 6;
            case 'B' -> 7;
            case 'b' -> 8;
            case 'N' -> 9;
            case 'n' -> 10;
            case 'P' -> 11;
            case 'p' -> 12;
            default -> 0;
        };
    }
    static char getChar(int piece) {
        return switch (piece) {
            case 1 -> 'K';
            case 2 -> 'k';
            case 3 -> 'Q';
            case 4 -> 'q';
            case 5 -> 'R';
            case 6 -> 'r';
            case 7 -> 'B';
            case 8 -> 'b';
            case 9 -> 'N';
            case 10 -> 'n';
            case 11 -> 'P';
            case 12 -> 'p';
            default -> '0';
        };
    }
    static int materialWorth(int piece) {
        return switch (piece) {
            case 3 -> 90;
            case 4 -> -90;
            case 5 -> 50;
            case 6 -> -50;
            case 7, 9 -> 30;
            case 8, 10 -> -30;
            case 11 -> 10;
            case 12 -> -10;
            default -> 0;
        };
    }
    static List<Integer> cast(int[] input) {
        return Arrays.stream(input).boxed().toList();
    }
    static String condense(int[] stuff) {
        String[] output = new String[64];
        for (int i = 0; i < 64; i++) {
            output[i] = Integer.toString(stuff[i]);
        }
        return String.join(" ", output);
    }
    static boolean color(int piece) {
        return (piece % 2) == 1;
    }
}
class Position {
    int[] position; //can't use byte[] because arithmetic operations will be performed
    int limit, maxDepth;
    //ArrayList<List<Integer>> history = new ArrayList<List<Integer>>();
    ArrayList<int[]> undoStack = new ArrayList<>();
    HashMap<List<Integer>, int[]> alreadyEvaluated = new HashMap<>(); //position, {evaluation, bestStart, bestEnd, depth searched, bound: -1 = lower, 0 = exact, 1 = upper}
    ArrayList<Integer> pruneHistory = new ArrayList<>(); //moves stored as start * 100 + end
    byte[] pieceActivity = new byte[64], material = new byte[64], centralControl = new byte[64],
    kingSafety = new byte[64], space = new byte[64], rooksSeventh = new byte[64], outposts = new byte[64];
    int bishopPairBonus() {
        int whiteBishops = 0, blackBishops = 0;
        for (int i = 0; i < 64; i++) {
            if (position[i] == 7) whiteBishops++;
            if (position[i] == 8) blackBishops++;
        }
        int whiteBonus = (whiteBishops >= 2) ? 2 : 0;
        int blackBonus = (blackBishops >= 2) ? -2 : 0;
        return whiteBonus + blackBonus;
    }
    int currentCount() {
        return currentCount(true);
    }
    int currentCount(boolean checkStatus) {
        if (checkStatus) {
            try {gameStatus();} catch (GameEndsException g) {
                return switch (g.getMessage()) {
                    case "1" -> Integer.MAX_VALUE;
                    case "2" -> Integer.MIN_VALUE;
                    default -> 0;
                };
            }
        }
        int output = 0;
        for (int i = 0; i < 64; i++) {
            output += pieceActivity[i] + material[i] + centralControl[i] + kingSafety[i] + space[i]
            + rooksSeventh[i] + outposts[i]; 
        }
        return output + ((position[64] == 1) ? 1 : -1) + bishopPairBonus();
    }
    void update(int square) {
        pieceActivity[square-1] = 0;
        material[square-1] = 0;
        centralControl[square-1] = 0;
        kingSafety[square-1] = 0;
        space[square-1] = 0;
        rooksSeventh[square-1] = 0;
        outposts[square-1] = 0;
        vision(square);
        int piece = getPiece(square);
        if (piece == 0) return;
        int modifier = (Main.color(piece)) ? 1 : -1;
        int sameColorPawn = (23 - modifier) / 2;
        byte[] pieceVision = retrieve(0);
        for (byte s : pieceVision) {
            if (s == 28 || s == 29 || s == 36 || s == 37) {
                centralControl[square-1] = (byte) (centralControl[square-1] + 2 * modifier);
            }
        }
        material[square-1] = (byte) Main.materialWorth(piece);
        if (piece == 3 || piece == 4) {
            pieceActivity[square-1] = (byte) (modifier * pieceVision.length / 4);
        } else if (piece == 5 || piece == 6 || piece == 7 || piece == 8) {
            pieceActivity[square-1] = (byte) (modifier * pieceVision.length / 2);
        }  
        if (piece == 9 || piece == 10) {
            int equivalentY = 9 - Main.getY(square);
            int equivalent = Main.getX(square) + 8 * (equivalentY - 1);
            pieceActivity[square-1] = (byte) (modifier * Reference.knightBonus.get((piece == 9) ? square : equivalent).intValue());

            if (Main.getY(square) == ((piece == 9) ? 5 : 4) || Main.getY(square) == ((piece == 9) ? 6 : 3)) {
                //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                if (Main.getX(square) >= 3 && Main.getX(square) <= 6) {
                    //center files, which are more valuable
                    if (getPiece(Main.translate(square, 1, -modifier)) == sameColorPawn || getPiece(Main.translate(square, -1, -modifier)) == sameColorPawn) {
                        outposts[square-1] = (byte) (modifier * 8);
                    }
                } else {
                    // flank files
                    if (getPiece(Main.translate(square, 1, -modifier)) == sameColorPawn || getPiece(Main.translate(square, -1, -modifier)) == sameColorPawn) {
                        outposts[square-1] = (byte) (modifier * 4);
                    }
                }
            }
        }
        if (piece == 1 || piece == 2) {
            double proximityToCenter = Math.hypot(Main.getX(square) - 4.5, Main.getY(square) - 4.5);
            if (endgame()) {
                pieceActivity[square-1] = (byte) (modifier * (10 - Math.round(proximityToCenter * 1.7)));
            } else {
                kingSafety[square-1] = (byte) (modifier * (Math.round(proximityToCenter * 1.7) - 10));
                if (getPiece(Main.translate(square, 0, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 10); //pawn in front of king
                }
                if (getPiece(Main.translate(square, 1, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 5);
                }
                if (getPiece(Main.translate(square, -1, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 5);
                }
            }
        }
        if (piece == 5) {
            if (Main.getY(square) == 7) rooksSeventh[square-1] = 15;
            if (Main.getY(square) == 6 || Main.getY(square) == 8) rooksSeventh[square-1] = 5;
        }
        if (piece == 6) {
            if (Main.getY(square) == 2) rooksSeventh[square-1] = -15;
            if (Main.getY(square) == 3 || Main.getY(square) == 1) rooksSeventh[square-1] = -5;
        }
        if (piece == 7 || piece == 8) {
            int square1 = (piece == 7) ? 36 : 28;
            int square2 = (piece == 7) ? 37 : 29;
            if (square == square1 || square == square2) {
                if (getPiece(Main.translate(square, 1, -modifier)) == sameColorPawn || getPiece(Main.translate(square, -1, -modifier)) == sameColorPawn) {
                    outposts[square-1] = (byte) (modifier * 5);
                }
            }
        }
        if (piece == 11 || piece == 12) {
            int equivalent = (piece == 11) ? Main.getY(square) : 9 - Main.getY(square);
            double addition = switch (Main.getX(square)) {
                // central space is worth more
                case 3, 6 -> Math.pow(equivalent, 2) / 9.0; // c and f files
                case 4, 5 -> Math.pow(equivalent, 2) / 7.0; // center files: d and e
                default -> Math.pow(equivalent, 2) / 12.0; //flank files, a, b, g, h
            };
            space[square-1] = (byte) Math.round(modifier * addition);
        }
    }
    void updatePosition(int square) {
        update(square);

        for (int s = 1; s <= 64; s++) {
            if (s == square) continue; //already been updated
            int piece = getPiece(s);
            if (piece == 0) {
                pieceActivity[s-1] = 0;
                material[s-1] = 0;
                centralControl[s-1] = 0;
                kingSafety[s-1] = 0;
                space[s-1] = 0;
                rooksSeventh[s-1] = 0;
                outposts[s-1] = 0;
                continue;
            }
            //for rooks possibly moved by castling, override and run update() instead
            if ((piece == 5) && (s == 4 || s == 6)) {
                update(s);
                continue;
            }
            if ((piece == 6) && (s == 60 || s == 62)) {
                update(s);
                continue;
            }
            vision(s);
            int modifier = (Main.color(piece)) ? 1 : -1;
            int sameColorPawn = (23 - modifier) / 2;
            byte[] pieceVision = retrieve(0);
            centralControl[s-1] = 0;
            for (byte q: pieceVision) {
                if (q == 28 || q == 29 || q == 36 || q == 37) {
                    centralControl[s-1] = (byte) (centralControl[s-1] + 2 * modifier);
                }
            }
            // pieceActivity only calculated based on pieceVision.length for bishops/rooks/queens
            // kings and knights have their own metrics, and pawns aren't calculated
            if (piece == 3 || piece == 4) {
                pieceActivity[s-1] = (byte) (modifier * pieceVision.length / 4);
            } else if (piece == 5 || piece == 6 || piece == 7 || piece == 8) {
                pieceActivity[s-1] = (byte) (modifier * pieceVision.length / 2);
            }  
            /*
            only 3 things other than vision that are affected by other pieces:
            pawn protection for outposts,
            pawn cover for kings,
            and captures affecting whether an endgame has been reached
            */
            switch (piece) {
                //check if pawn in front of king moved
                case 1:
                case 2:
                    double proximityToCenter = Math.hypot(Main.getX(s) - 4.5, Main.getY(s) - 4.5);
                    if (endgame()) {
                        pieceActivity[s-1] = (byte) (modifier * (10 - Math.round(proximityToCenter * 1.7)));
                    } else {
                        kingSafety[s-1] = (byte) (modifier * (Math.round(proximityToCenter * 1.7) - 10));
                        if (getPiece(Main.translate(s, 0, modifier)) == sameColorPawn) {
                            kingSafety[s-1] = (byte) (kingSafety[s-1] + modifier * 10); //pawn in front of king
                        }
                        if (getPiece(Main.translate(s, 1, modifier)) == sameColorPawn) {
                            kingSafety[s-1] = (byte) (kingSafety[s-1] + modifier * 5);
                        }
                        if (getPiece(Main.translate(s, -1, modifier)) == sameColorPawn) {
                            kingSafety[s-1] = (byte) (kingSafety[s-1] + modifier * 5);
                        }
                    }
                    break;
                //check if pawn protecting an outpost has moved
                case 7:
                case 8:
                    outposts[s-1] = 0;
                    if (s == ((piece == 7) ? 36 : 28) || s == ((piece == 7) ? 37 : 29)) {
                        if (getPiece(Main.translate(s, 1, -modifier)) == sameColorPawn || getPiece(Main.translate(s, -1, -modifier)) == sameColorPawn) {
                            outposts[s-1] = (byte) (modifier * 5);
                        }
                    }
                    break;
                case 9:
                case 10:
                    int equivalentY = 9 - Main.getY(s);
                    int equivalent = Main.getX(s) + 8 * (equivalentY - 1);
                    pieceActivity[s-1] = (byte) (modifier * Reference.knightBonus.get((piece == 9) ? s : equivalent).intValue());
                    outposts[s-1] = 0;
                    if (getPiece(Main.translate(s, 1, -modifier)) == sameColorPawn || getPiece(Main.translate(s, -1, -modifier)) == sameColorPawn) {
                        if (Main.getY(s) == ((piece == 9) ? 5 : 4) || Main.getY(s) == ((piece == 9) ? 6 : 3)) {
                            //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                            if (Main.getX(s) >= 3 && Main.getX(s) <= 6) {
                                //center files, which are more valuable
                                outposts[s-1] = (byte) (modifier * 8);
                            } else {
                                // flank files
                                outposts[s-1] = (byte) (modifier * 4);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
    Position(int[] setup, int timeLimit, int depth) {
        this.position = setup;
        this.limit = timeLimit;
        this.maxDepth = depth;
        for (int i = 1; i <= 64; i++) {
            update(i);
        }
    }
    int numberOfPieces() {
        int output = 0;
        for (int i = 0; i < 64; i++) {
            if (position[i] > 0) {
                output++;
            }
        }
        return output;
    }
    int getPiece(int square) {
        if (square == 0) {
            //square does not exist
            return -1;
        }
        return position[square - 1];
    }
    int kingSquare() {
        int target = 2 - position[64];
        for (int i = 0; i < 64; i++) {
            if (position[i] == target) {
                return i+1;
            }
        }
        return 0; //HOPEFULLY this never happens
    }
    void display() {
        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                String chr = Character.toString(Main.getChar(position[i * 8 + j]));
                String output = ("0".equals(chr)) ? "  " : chr + " ";
                System.out.print(output);
            }
            System.out.println("");
        }
    }
    // Hot paths!
    byte[][] pad = new byte[10][1000]; 
    /*
    avoid creating tons of new byte[] instances
    10 pads with 1000 elements each
    */
    void clearPad(int where) {
        for (int i = 0; i < 1000; i++) {
            pad[where][i] = 0;
        }
    }
    byte[] retrieve(int index) {
        //CAUTION: be careful calling this
        return Arrays.copyOf(pad[index], length(index));
    }
    int length(int index) {
        //as long as the last element is not zero
        for (int output = 1000; output > 0; output--) {
            if (pad[index][output-1] != 0) {
                return output;
            }
        }
        return 0;
    }
    void compilePad(int stop/*inclusive*/) {
        //takes all arrays in pad and stacks them together in pad[0]
        int index = length(0);
        for (int i = 1; i <= stop; i++) {
            for (int j = 0; j < length(i); j++) {
                pad[0][index] = pad[i][j];
                index++;
            }
            clearPad(i);
        }
    }
    void squaresTo(int square, int directionX, int directionY, boolean colorStop, int where) {
        clearPad(where);
        for (int i = 1; i < 8; i++) {
            int newSquare = Main.translate(square, directionX * i, directionY * i);
            if (newSquare == 0) {
                break;
            }
            if (getPiece(newSquare) == 0) {
                pad[where][i-1] = (byte) newSquare;
            } else {
                if (Main.color(getPiece(newSquare)) != colorStop) {
                    pad[where][i-1] = (byte) newSquare;
                }
                break;
            }
        }
    }
    void vision(int square) {
        clearPad(0);
        int piece = getPiece(square);
        boolean color = Main.color(piece);
        int index = 0;
        int candidate = 0;
        switch (piece) {
            case 1:
            case 2:
                candidate = Main.translate(square, 1, 0);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 1, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 0, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, 0);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 0, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 1, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                }
                break;
            case 3:
            case 4:
                squaresTo(square, 1, 0, color, 1);
                squaresTo(square, 1, 1, color, 2);
                squaresTo(square, 0, 1, color, 3);
                squaresTo(square, -1, 1, color, 4);
                compilePad(4);
                squaresTo(square, -1, 0, color, 1);
                squaresTo(square, -1, -1, color, 2);
                squaresTo(square, 0, -1, color, 3);
                squaresTo(square, 1, -1, color, 4);
                compilePad(4);
                break;
            case 5:
            case 6:
                squaresTo(square, 1, 0, color, 1);
                squaresTo(square, 0, 1, color, 2);
                squaresTo(square, -1, 0, color, 3);
                squaresTo(square, 0, -1, color, 4);
                compilePad(4);
                break;
            case 7:
            case 8:
                squaresTo(square, 1, 1, color, 1);
                squaresTo(square, -1, 1, color, 2);
                squaresTo(square, -1, -1, color, 3);
                squaresTo(square, 1, -1, color, 4);
                compilePad(4);
                break;
            case 9:
            case 10:
                candidate = Main.translate(square, 2, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 1, 2);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, 2);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -2, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, -2);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -2, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 1, -2);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, 2, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                }
                break;
            case 11:
                candidate = Main.translate(square, 1, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, 1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                break;
            case 12:
                candidate = Main.translate(square, 1, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                candidate = Main.translate(square, -1, -1);
                if (candidate != 0 && (getPiece(candidate) == 0 || Main.color(getPiece(candidate)) != color)) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
                break;
            default:
                break;
        }
    }
    void legalMovesBasic(int square) {
        clearPad(0);
        int piece = getPiece(square);
        boolean color = Main.color(piece);
        int index = 0;
        if (piece == 11) {
            //pawn
            int candidate = Main.translate(square, 1, 1);
            if (candidate != 0 && (getPiece(candidate) != 0 && Main.color(getPiece(candidate)) != color)) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = Main.translate(square, -1, 1);
            if (candidate != 0 && (getPiece(candidate) != 0 && Main.color(getPiece(candidate)) != color)) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = Main.translate(square, 0, 1);
            if (candidate != 0 && getPiece(candidate) == 0) {
                pad[0][index] = (byte) candidate;
                index++;
                candidate = Main.translate(square, 0, 2);
                if (candidate != 0 && getPiece(candidate) == 0 && square > 8 && square <= 16) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
            }
            if (position[69] != 0 && getPiece(position[69]) == 0 &&
                    (position[69] == Main.translate(square, 1, 1) || position[69] == Main.translate(square, -1, 1))) {
                pad[0][index] = (byte) position[69];
                index++;
            }
        } else if (piece == 12) {
            //pawn
            int candidate = Main.translate(square, 1, -1);
            if (candidate != 0 && (getPiece(candidate) != 0 && Main.color(getPiece(candidate)) != color)) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = Main.translate(square, -1, -1);
            if (candidate != 0 && (getPiece(candidate) != 0 && Main.color(getPiece(candidate)) != color)) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = Main.translate(square, 0, -1);
            if (candidate != 0 && getPiece(candidate) == 0) {
                pad[0][index] = (byte) candidate;
                index++;
                candidate = Main.translate(square, 0, -2);
                if (getPiece(candidate) == 0 && square > 48 && square <= 56) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
            }
            if (position[69] != 0 && getPiece(position[69]) == 0 &&
                    (position[69] == Main.translate(square, 1, -1) || position[69] == Main.translate(square, -1, -1))) {
                pad[0][index] = (byte) position[69];
                index++;
            }
        } else {
            vision(square);
        }
    }
    void makeMove(int start, int end) {
        makeMove(start, end, true);
    }
    void makeMove(int start, int end, boolean changeEval) {
        undoStack.add(position.clone());
        int piece = getPiece(start);
        int captured = getPiece(end);
        boolean epBefore = position[69] != 0;
        if (piece != 11 && piece != 12) {
            position[69] = 0;
        }
        position[70] = position[70] + 1;
        if (captured != 0) {
            position[70] = 0;
            if (end == 1 && captured == 5) position[66] = 0;
            if (end == 8 && captured == 5) position[65] = 0;
            if (end == 57 && captured == 6) position[68] = 0;
            if (end == 64 && captured == 6) position[67] = 0;
        }
        if (position[64] == 0) {
            position[71] = position[71] + 1;
        }
        position[end-1] = piece;
        position[start-1] = 0;
        switch (piece) {
            case 1:
                position[65] = 0;
                position[66] = 0;
                if (end == 3) {
                    //white castle long
                    position[0] = 0;
                    position[3] = 5;
                }
                if (end == 7) {
                    //white castle short
                    position[7] = 0;
                    position[5] = 5;
                }
                break;
            case 2:
                position[67] = 0;
                position[68] = 0;
                if (end == 59) {
                    //black castle long
                    position[56] = 0;
                    position[59] = 6;
                }
                if (end == 63) {
                    //black castle short
                    position[63] = 0;
                    position[61] = 6;
                }
                break;
            case 5:
                if (start == 1) {
                    //a1
                    position[66] = 0;
                } else if (start == 8) {
                    //h1
                    position[65] = 0;
                }
                break;
            case 6:
                if (start == 57) {
                    //a8
                    position[68] = 0;
                } else if (start == 64) {
                    //h8
                    position[67] = 0;
                }
                break;
            case 11:
                //white pawn
                //worry about en passant and promotion, assume all promotions go to queens
                if (end == position[69]) {
                    //en passant!
                    position[Main.translate(end, 0, -1) - 1] = 0;
                }
                position[69] = 0;
                if (start > 8 && start <= 16 && end > 24 && end <= 32) {
                    //pawn move 2 squares, check for en passant
                    if (getPiece(Main.translate(end, 1, 0)) == 12 || getPiece(Main.translate(end, -1, 0)) == 12) {
                        position[69] = Main.translate(start, 0, 1);
                    }
                }
                if (start > 48 && start <= 56) {
                    //promotion
                    position[end-1] = 3;
                }
                position[70] = 0;
                break;
            case 12:
                //black pawn
                if (end == position[69]) {
                    //en passant!
                    position[Main.translate(end, 0, 1) - 1] = 0;
                }
                position[69] = 0;
                if (start > 48 && start <= 56 && end > 32 && end <= 40) {
                    //pawn move 2 squares, check for en passant
                    if (getPiece(Main.translate(end, 1, 0)) == 11 || getPiece(Main.translate(end, -1, 0)) == 11) {
                        position[69] = Main.translate(start, 0, -1);
                    }
                }
                if (start > 8 && start <= 16) {
                    //promotion
                    position[end-1] = 4;
                }
                position[70] = 0;
                break;
            default:
                break;
        }
        position[64] = 1 - position[64];
        if (changeEval) {
            updatePosition(start);
            updatePosition(end);
        }
        //history.add(Main.cast(position));
    }
    void restore() {
        restore(true);
    }
    void restore(boolean changeEval) {
        int[] previousPosition = undoStack.remove(undoStack.size() - 1);
        int[] currentPosition = position.clone();
        System.arraycopy(previousPosition, 0, position, 0, position.length);
        if (changeEval) {
            for (int i = 0; i < 64; i++) {
                if (previousPosition[i] != currentPosition[i]) {
                    updatePosition(i+1);
                }
            }
        }
        //history.remove(history.size() - 1);
    }
    Byte[] legalMoves(int square) {
        legalMovesBasic(square); // placeholder
        byte[] basicMoves = retrieve(0);
        ArrayList<Byte> output = new ArrayList<>();
        for (byte move : basicMoves) {
            makeMove(square, move, false);
            position[64] = 1 - position[64];
            if (!inCheck()) {
                output.add(move);
            }
            position[64] = 1 - position[64];
            restore(false);
        }
        if (!inCheck()) {
            if (square == 5) {
                if (position[65] == 1 && getPiece(8) == 5 && getPiece(6) == 0 && getPiece(7) == 0) {
                    boolean allowed = true;
                    position[4] = 0;
                    position[5] = 1;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[5] = 0;
                    position[4] = 1;
                    position[4] = 0;
                    position[6] = 1;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[6] = 0;
                    position[4] = 1;
                    if (allowed) {
                        byte q = 7;
                        output.add(q);
                    }    
                }
                if (position[66] == 1 && getPiece(1) == 5 && getPiece(4) == 0 && getPiece(3) == 0 && getPiece(2) == 0) {   
                    boolean allowed = true; 
                    position[4] = 0;
                    position[3] = 1;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[3] = 0;
                    position[4] = 1;
                    position[4] = 0;
                    position[2] = 1;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[2] = 0;
                    position[4] = 1;
                    if (allowed) {
                        byte q = 3;
                        output.add(q);
                    }
                }
            }
            if (square == 61) {
                if (position[67] == 1 && getPiece(64) == 6 && getPiece(62) == 0 && getPiece(63) == 0) {
                    boolean allowed = true;
                    position[60] = 0;
                    position[61] = 2;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[61] = 0;
                    position[60] = 2;
                    position[60] = 0;
                    position[62] = 2;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[62] = 0;
                    position[60] = 2;
                    if (allowed) {
                        byte q = 63;
                        output.add(q);
                    }    
                }
                if (position[68] == 1 && getPiece(57) == 6 && getPiece(60) == 0 && getPiece(59) == 0 && getPiece(58) == 0) {   
                    boolean allowed = true; 
                    position[60] = 0;
                    position[59] = 2;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[59] = 0;
                    position[60] = 2;
                    position[60] = 0;
                    position[58] = 2;
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[58] = 0;
                    position[60] = 2;
                    if (allowed) {
                        byte q = 59;
                        output.add(q);
                    }
                }
            }
        }
        return output.toArray(new Byte[0]);
    }
    boolean inCheck() {
        int king = kingSquare();
        byte modified = (byte) king;
        Byte[] squareMap = attackMap(1 - position[64]);
        for (Byte square : squareMap) {
            byte q = square;
            if (q == king) {
                return true;
            }
        }
        return false;
    }
    Byte[] attackMap(int color) {
        //1 = white, 0 = black
        ArrayList<Byte> output = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (position[i] != 0) {
                if ((color == 1 && Main.color(position[i])) || (color == 0 && !Main.color(position[i]))) {
                    vision(i+1);
                    byte[] additions = retrieve(0);
                    for (byte b : additions) {
                        output.add(b);
                    }
                }
            }
        }
        return output.toArray(new Byte[0]);
    }
    byte[][] generateMoves() {
        clearPad(5);
        clearPad(6);
        //this is the culmination of everything else above
        int i = 0;
        for (int j = 0; j < 64; j++) {
            //checks all 64 squares
            boolean color = Main.color(position[j]);
            boolean toMove = position[64] == 1;
            
            if (position[j] != 0 && color == toMove) {
                //square is not empty and is of the color to move
                Byte[] moves = legalMoves(j+1);
                if (moves.length > 0) {
                    //piece has legal moves
                    for (Byte move : moves) {
                        pad[5][i] = (byte) (j+1);
                        byte q = move;
                        pad[6][i] = q;
                        i++;
                    }
                }
            }
        }
        byte[][] out = new byte[2][length(5)];
        out[0] = retrieve(5);
        out[1] = retrieve(6);
        return out;
    }
    byte[][] sortMoves(byte[][] unsortedMoves) {
        int[][] moves = new int[unsortedMoves[0].length][3]; //from, to, score
        int[] cachedMove = alreadyEvaluated.get(Main.cast(position));
        boolean isEndgame = endgame();
        /*score based on:
        1. cached best move
        2. captures, most valuable victim, least valuable attacker
        3. history
        If not:
        - Rank forward and center-seeking moves above backward moves
        - Rank pawn moves close to promotion higher
        - Rank far advanced rook moves higher
        - if endgame() rank center-seeking king moves higher
        - if !endgame() rank moves that push pawns in front of the king lower
        */
        //output += pieceActivity[i] + material[i] + centralControl[i] + kingSafety[i] + space[i]+ rooksSeventh[i] + outposts[i];
        for (int i = 0; i < moves.length; i++) {
            int start = unsortedMoves[0][i];
            int end = unsortedMoves[1][i];
            moves[i][0] = start;
            moves[i][1] = end;
            //Prefer the best move previously found for this position.
            if (cachedMove != null && start == cachedMove[1] && end == cachedMove[2]) {
                moves[i][2] = Integer.MAX_VALUE;
                continue;
            }
            int piece = getPiece(start);
            boolean pieceColor = Main.color(piece);
            int modifier = (pieceColor) ? 1 : -1;
            int captured = getPiece(end);
            boolean capture = captured != 0 || ((piece == 11 || piece == 12) && end == position[69]);
            boolean promotion = (piece == 11 && end >= 57) || (piece == 12 && end <= 8);
            //capture/promotion
            if (capture || promotion) {
                int difference = 0;
                if (capture) {
                    //most valuable victim, least valuable attacker
                    int victimWorth = (captured == 0) ? 10 : Math.abs(Main.materialWorth(captured));
                    difference += (piece == 1 || piece == 2) ? 0 : 100 + victimWorth - Math.abs(Main.materialWorth(piece));
                }
                if (promotion) {
                    difference += 180;
                }
                //max of difference is 360, for "pawn takes queen promotes to queen"
                moves[i][2] = Integer.MAX_VALUE - 361 + difference;
                continue;
            }
            //history
            if (pruneHistory.contains(start * 100 + end)) {
                moves[i][2] = Integer.MIN_VALUE;
                continue;
            }
            //quiet moves not principal or in history, counting up from 0 as opposed to counting down from 2147483647
            int cumulative = 0;
            //reward center-seeking moves
            double startProximity = Math.hypot(Main.getX(start) - 4.5, Main.getY(start) - 4.5);
            double endProximity = Math.hypot(Main.getX(end) - 4.5, Main.getY(end) - 4.5);
            int centerGain = (int) Math.round(startProximity - endProximity);
            cumulative += (isEndgame && (piece == 1 || piece == 2)) ? 3 * centerGain : centerGain;
            //reward forward moves
            cumulative += (pieceColor) ? Main.getY(end) - Main.getY(start) : Main.getY(start) - Main.getY(end);
            if (piece == 11 || piece == 12) {
                //reward pawn moves close to promotion
                boolean farAdvanced = (pieceColor) ? Main.getY(end) >= 6 : Main.getY(end) <= 3;
                if (farAdvanced) {
                    cumulative += 20;
                }
                //punish pawn moves in front of king if !endgame()
                if (!isEndgame) {
                    int king = (pieceColor) ? 1 : 2;
                    if (getPiece(Main.translate(start, 0, -modifier)) == king) {
                        cumulative -= 15;
                    }
                }
            }
            if (piece == 5 || piece == 6) {
                //reward rook moves onto the 7th
                if (Main.getY(end) == ((pieceColor) ? 7 : 2)) {
                    cumulative += 25;
                }
                if (Main.getY(end) == ((pieceColor) ? 8 : 1) || Main.getY(end) == ((pieceColor) ? 6 : 3)) {
                    cumulative += 10;
                }
            }
            moves[i][2] = cumulative;
        }
        //now sort
        Arrays.sort(moves, (a, b) -> Integer.compare(b[2], a[2])); //sort by score in descending order
        //convert int to byte, leave out evaluations

        byte[] conversions1 = new byte[moves.length];
        byte[] conversions2 = new byte[moves.length];
        //brute force conversion from int[][] to byte[][]
        for (int i = 0; i < conversions1.length; i++) {
            conversions1[i] = (byte) moves[i][0];
            conversions2[i] = (byte) moves[i][1];
            //shed the scores
        }
        return new byte[][]{conversions1, conversions2};
    }
    boolean isTacticalMove(int start, int end) {
        int piece = getPiece(start);
        return getPiece(end) != 0
            || ((piece == 11 || piece == 12) && end == position[69])
            || (piece == 11 && end >= 57)
            || (piece == 12 && end <= 8);
    }
    void gameStatus() throws GameEndsException {
        gameStatus(generateMoves());
    }
    void gameStatus(byte[][] moves) throws GameEndsException {
        if (moves[0].length == 0) {
            if (inCheck()) {
                int out = (position[64] == 1) ? 2 : 1; //checkmate
                throw new GameEndsException(out);
            }
            throw new GameEndsException(0); //stalemate
        }
        /*int[] max = {0};
        boolean[] three = {false};
        history.forEach(s -> {
            max[0] = Collections.frequency(history, s);
            if (max[0] >= 3) {
                //threefold repetition
                three[0] = true;
            }
        });*/
        if (!sufficientMaterial() || position[71] >= 100 /*|| three[0]*/) {
            throw new GameEndsException(0);
        }
    }
    boolean sufficientMaterial() {
        int whiteBishops = 0, blackBishops = 0, whiteKnights = 0, blackKnights = 0, whitePieces = 0, blackPieces = 0;
        for (int i = 0; i < 64; i++) {
            switch (position[i]) {
                case 0:
                case 1:
                case 2:
                    break;
                case 7:
                    whiteBishops++;
                    whitePieces++;
                    break;
                case 8:
                    blackBishops++;
                    blackPieces++;
                    break;
                case 9:
                    whiteKnights++;
                    whitePieces++;
                    break;
                case 10:
                    blackKnights++;
                    blackPieces++;
                    break;
                default:
                    return true;
            }
        }
        if (whitePieces == 2 && whiteKnights < 2) {
            return true;
        }
        if (blackPieces == 2 && blackKnights < 2) {
            return true;
        } 
        return false;
    }
    boolean endgame() {
        //if queens are on, return true if they're the only pieces (queen endgame)
        //if queens are off, return false if both pairs of rooks and one minor piece for each side is on (queenless middlegame)
        boolean queens = false;
        for (int i = 0; i < 64; i++) {
            switch (position[i]) {
                case 3:
                case 4:
                    queens = true;
                default:
                    break;
            }
        }
        if (queens) {
            for (int i = 0; i < 64; i++) {
                switch (position[i]) {
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                        return false;
                    default:
                        break;
                }
            }
            return true;
        }
        int rooks = 0;
        boolean minorPieces = false;
        for (int i = 0; i < 64; i++) {
            switch (position[i]) {
                case 5:
                case 6:
                    rooks++;
                    break;
                case 7:
                case 8:
                case 9:
                case 10:
                    minorPieces = true;
                    break;
                default:
                    break;
            }
        }
        return !(rooks >= 3 && minorPieces);
    }
    boolean pawnEndgame() {
        //if true, zugzwang may happen at some point -> cancel the null move prune
        if (!endgame()) return false;
        for (int i = 0; i < 64; i++) {
            switch(position[i]) {
                case 1:
                case 2:
                case 11:
                case 12:
                    break;
                default:
                    return false;  
            }
        }
        return true;
    }
    int count() {
        return count(true);
    }
    int count(boolean checkStatus) {
        // leaf node!
        if (checkStatus) {
            try {gameStatus();} catch (GameEndsException g) {
                return switch (g.getMessage()) {
                    case "1" -> Integer.MAX_VALUE;
                    case "2" -> Integer.MIN_VALUE;
                    default -> 0;
                };
            }
        }
        int material = 0, pieceActivity = 0, kingSafety = 0, centralControl = 0;
        int space = 0, bishopPair = 0, rooksSeventh = 0, outposts = 0;
        int bonus = (position[64] == 1) ? 1 : -1; // side to move gets 1 decipawn (1/10 pawn)
        int whiteBishops = 0, blackBishops = 0;
        for (int i = 1; i <= 64; i++) {
            vision(i);
            byte[] pieceVision = retrieve(0);
            for (byte square : pieceVision) {
                int modifier = (Main.color(getPiece(i))) ? 2 : -2;
                if (square == 28 || square == 29 || square == 36 || square == 37) {
                    centralControl += modifier;
                }
            }
            material += Main.materialWorth(getPiece(i));
            double proximityToCenter, addition;
            switch (getPiece(i)) {
                case 1:
                    proximityToCenter = Math.hypot(Main.getX(i) - 4.5, Main.getY(i) - 4.5);
                    if (endgame()) {
                        pieceActivity += 10 - Math.round(proximityToCenter * 1.7); //should be roughly 0 if the king is on a1/h1
                    } else {
                        kingSafety += Math.round(proximityToCenter * 1.7) - 10; //do the opposite
                        if (getPiece(Main.translate(i, 0, 1)) == 11) {
                            kingSafety += 10; //pawn in front of king
                        }
                        if (getPiece(Main.translate(i, 1, 1)) == 11) {
                            kingSafety += 5;
                        }
                        if (getPiece(Main.translate(i, -1, 1)) == 11) {
                            kingSafety += 5;
                        }
                    }
                    break;
                case 2:
                    proximityToCenter = Math.hypot(Main.getX(i) - 4.5, Main.getY(i) - 4.5);
                    if (endgame()) {
                        pieceActivity -= 10 - Math.round(proximityToCenter * 1.7); //should be roughly 0 if the king is on a8/h8
                    } else {
                        kingSafety -= Math.round(proximityToCenter * 1.7) - 10; //do the opposite
                        if (getPiece(Main.translate(i, 0, -1)) == 12) {
                            kingSafety -= 10; //pawn in front of king
                        }
                        if (getPiece(Main.translate(i, 1, -1)) == 12) {
                            kingSafety -= 5;
                        }
                        if (getPiece(Main.translate(i, -1, -1)) == 12) {
                            kingSafety -= 5;
                        }
                    }
                    break;
                case 3:
                    //piece activity is weighed less for queens, normally it's half a decipawn for each square controlled
                    pieceActivity += pieceVision.length / 4;
                    break;
                case 4:
                    pieceActivity -= pieceVision.length / 4;
                    break;
                case 5:
                    pieceActivity += pieceVision.length / 2;
                    if (Main.getY(i) == 7) {
                        rooksSeventh += 15;
                    }
                    if (Main.getY(i) == 6 || Main.getY(i) == 8) {
                        rooksSeventh += 5;
                    }
                    break;
                case 6:
                    pieceActivity -= pieceVision.length / 2;
                    if (Main.getY(i) == 2) {
                        rooksSeventh -= 15;
                    }
                    if (Main.getY(i) == 1 || Main.getY(i) == 3) {
                        rooksSeventh -= 5;
                    }
                    break;
                case 7:
                    pieceActivity += pieceVision.length / 2;
                    // bishop outposts, typically called "wooden shields", really only apply on d5 and e5 (white), d4 and e4 (black), typically
                    // knight outposts are even better though
                    if (i == 36 || i == 37 /*e5 and d5*/) {
                        //must be protected by a pawn
                        if (getPiece(Main.translate(i, 1, -1)) == 11 || getPiece(Main.translate(i, -1, -1)) == 11) {
                            outposts += 5;
                        }
                    }
                    whiteBishops++;
                    break;
                case 8:
                    pieceActivity -= pieceVision.length / 2;
                    if (i == 28 || i == 29 /*e4 and d4*/) {
                        //must be protected by a pawn
                        if (getPiece(Main.translate(i, 1, 1)) == 12 || getPiece(Main.translate(i, -1, 1)) == 12) {
                            outposts -= 5;
                        }
                    }
                    blackBishops++;
                    break;
                case 9:
                    if (Main.getY(i) == 5 || Main.getY(i) == 6) {
                        //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                        if (Main.getX(i) >= 3 && Main.getX(i) <= 6) {
                            //center files, which are more valuable
                            if (getPiece(Main.translate(i, 1, -1)) == 11 || getPiece(Main.translate(i, -1, -1)) == 11) {
                                outposts += 8;
                            }
                        } else {
                            // flank files
                            if (getPiece(Main.translate(i, 1, -1)) == 11 || getPiece(Main.translate(i, -1, -1)) == 11) {
                                outposts += 4;
                            }
                        }
                    }
                    pieceActivity += Reference.knightBonus.get(i);
                    break;
                case 10:
                    if (Main.getY(i) == 4 || Main.getY(i) == 3) {
                        if (Main.getX(i) >= 3 && Main.getX(i) <= 6) {
                            if (getPiece(Main.translate(i, 1, 1)) == 12 || getPiece(Main.translate(i, -1, 1)) == 12) {
                                outposts -= 8;
                            }
                        } else {
                            if (getPiece(Main.translate(i, 1, 1)) == 12 || getPiece(Main.translate(i, -1, 1)) == 12) {
                                outposts -= 4;
                            }
                        }
                    }
                    int equivalentY = 9 - Main.getY(i);
                    int equivalent = Main.getX(i) + 8 * (equivalentY - 1);
                    pieceActivity -= Reference.knightBonus.get(equivalent);
                    break;
                case 11:
                    //no pieceactivity metric used for pawns
                    // space, also kind of helps with giving a bonus for pawns really close to promotion
                    addition = switch (Main.getX(i)) {
                        // central space is worth more
                        case 3, 6 -> Math.pow(Main.getY(i), 2) / 9.0; // c and f files
                        case 4, 5 -> Math.pow(Main.getY(i), 2) / 7.0; // center files: d and e
                        default -> Math.pow(Main.getY(i), 2) / 12.0; //flank files, a, b, g, h
                    };
                    space += Math.round(addition);
                    break;
                case 12:
                    addition = switch (Main.getX(i)) {
                        case 3, 6 -> Math.pow(9 - Main.getY(i), 2) / 9.0; // c and f files
                        case 4, 5 -> Math.pow(9 - Main.getY(i), 2) / 7.0; // center files: d and e
                        default -> Math.pow(9 - Main.getY(i), 2) / 12.0; //flank files, a, b, g, h
                    };
                    space -= Math.round(addition);
                    break;
                default:
                    break;
            }
        }
        //side with bishop pair gets 2 decipawns
        if (whiteBishops >= 2) {
            bishopPair += 2; //disregard bishop underpromotion
        }
        if (blackBishops >= 2) {
            bishopPair -= 2;
        }
        return material + kingSafety + pieceActivity + centralControl + space + bishopPair + rooksSeventh + outposts + bonus;
    }
    int[] evalDepth(int depth) throws TimeExceededException {
        return evalDepth(depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    int evalQ(byte[][] sortedMoves) {
        return evalQ(sortedMoves, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    int evalQ(byte[][] sortedMoves, int alpha, int beta) {
        Main.nodes++;
        byte[][] moves = sortedMoves;
        boolean maximizing = position[64] == 1;
        boolean checked = inCheck();
        int best;
        if (checked) {
            best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        } else {
            best = currentCount(false) /*count(false)*/;
            if (maximizing) {
                if (best >= beta) return best;
                alpha = Math.max(alpha, best);
            } else {
                if (best <= alpha) return best;
                beta = Math.min(beta, best);
            }
        }
        for (int i = 0; i < moves[0].length; i++) {
            if (!checked && !isTacticalMove(moves[0][i], moves[1][i])) continue;
            makeMove(moves[0][i], moves[1][i]);
            int potential;
            potential = evalQ(sortMoves(generateMoves()), alpha, beta);
            restore();
            if (maximizing) {
                best = Math.max(best, potential);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, potential);
                beta = Math.min(beta, best);
            }
            if (alpha >= beta) break;
        }
        return best;
    }
    int[] evalDepth(int depth, int alpha, int beta) throws TimeExceededException {
        Main.nodes++;
        int alphaOriginal = alpha;
        int betaOriginal = beta;
        int bound = 0;
        if (alreadyEvaluated.keySet().size() > 0) {
            int[] out = alreadyEvaluated.get(Main.cast(position));
            if(out != null && out[3] >= depth) {
                if (out[4] == 0) {
                    return new int[]{out[0], out[1], out[2]};
                }
                if (out[4] == -1) {
                    if (out[0] >= beta) return new int[]{out[0], out[1], out[2]};
                    alpha = Math.max(alpha, out[0]); 
                }
                if (out[4] == 1) {
                    if (out[0] <= alpha) return new int[]{out[0], out[1], out[2]};
                    beta = Math.min(beta, out[0]);
                }
                if (alpha >= beta) {
                    return new int[]{out[0], out[1], out[2]};
                }
            }
        }
        int start = 0;
        if (depth == maxDepth) {
            start = Main.getTime();
        }
        boolean maximizing = position[64] == 1;
        //null move pruning
        boolean hasNullMoveBound = maximizing ? beta < Integer.MAX_VALUE : alpha > Integer.MIN_VALUE;
        if (depth > 2 && hasNullMoveBound && !pawnEndgame() && !inCheck()) {
            int[] evalPass;
            int enPassantSquare = position[69];
            position[64] = 1 - position[64]; //make a null move
            position[69] = 0;
            try {
                int nullDepth = depth - 3;
                evalPass = maximizing ? evalDepth(nullDepth, beta - 1, beta) : evalDepth(nullDepth, alpha, alpha + 1);
            } finally {
                position[64] = 1 - position[64]; //restore original position
                position[69] = enPassantSquare;
            }
            if (maximizing ? evalPass[0] >= beta : evalPass[0] <= alpha) {
                return new int[]{maximizing ? beta : alpha, 0, 0};
            }
        }
        byte[][] unorderedMoves = generateMoves();
        try {gameStatus(unorderedMoves);} catch (GameEndsException g) {
            return switch (g.getMessage()) {
                case "1" -> new int[]{Integer.MAX_VALUE, 0, 0};
                case "2" -> new int[]{Integer.MIN_VALUE, 0, 0};
                default -> new int[3];
            };
        }
        byte[][] moves = sortMoves(unorderedMoves);
        if (depth == 0) {
            return new int[]{evalQ(moves, alpha, beta), 0, 0};
        }
        int evaluation = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestStart = 0;
        int bestEnd = 0;
        int evalStatic = currentCount(false)/*count(false)*/;
        for (int i = 0; i < moves[0].length; i++) {
            if (depth == 1 && !isTacticalMove(moves[0][i], moves[1][i]) && !inCheck()) {
                //futility pruning
                if ((maximizing && evalStatic + 50 < alpha) || (!maximizing && evalStatic - 50 > beta)) {
                    pruneHistory.add(moves[0][i] * 100 + moves[1][i]);
                    break;
                }
                //reverse futility pruning
                if ((maximizing && evalStatic - 50 > beta) || (!maximizing && evalStatic + 50 < alpha)) {
                    pruneHistory.add(moves[0][i] * 100 + moves[1][i]);
                    return new int[]{(maximizing) ? beta : alpha, moves[0][0], moves[1][0]};
                }
            }
            boolean nodeInCheck = inCheck();
            boolean reduce = depth > 2 && i >= 3 && !isTacticalMove(moves[0][i], moves[1][i]) && !inCheck(); 
            makeMove(moves[0][i], moves[1][i]); //color to move is now flipped
            boolean givesCheck = inCheck();
            int potential;
            try {
                if (reduce && !givesCheck) {
                    potential = evalDepth(depth - 2, alpha, beta)[0];
                    if (maximizing ? potential > alpha : potential < beta) {
                        potential = evalDepth(depth - 1, alpha, beta)[0];
                    }
                } else {
                    potential = evalDepth(depth - 1, alpha, beta)[0];
                }
            } finally {
                restore();
            }
            alreadyEvaluated.put(Main.cast(position), new int[]{evaluation, bestStart, bestEnd, depth, bound});
            bound = evaluation <= alphaOriginal ? 1 : evaluation >= betaOriginal ? -1 : 0;
            if (maximizing) {
                if (potential > evaluation) {
                    evaluation = potential;
                    bestStart = moves[0][i];
                    bestEnd = moves[1][i];
                }
                alpha = Math.max(alpha, evaluation);
            } else {
                if (potential < evaluation) {
                    evaluation = potential;
                    bestStart = moves[0][i];
                    bestEnd = moves[1][i];
                }
                beta = Math.min(beta, evaluation);
            }
            if (alpha >= beta) {
                pruneHistory.add(moves[0][i] * 100 + moves[1][i]);
                break;
            }
        }
        int timeTaken = 0;
        if (depth == maxDepth) {
            timeTaken = Main.getTime() - start;
            if (timeTaken > limit) {
                throw new TimeExceededException();
            }
        }
        return new int[]{evaluation, bestStart, bestEnd};
    }
}
class Reference {
    static final Map<Integer, Integer> knightBonus = Map.ofEntries(
        Map.entry(1, -25), Map.entry(2, -20), Map.entry(3, -15), Map.entry(4, -15), Map.entry(5, -15), Map.entry(6, -15), Map.entry(7, -20), Map.entry(8, -25),
        Map.entry(9, -20), Map.entry(10, -20), Map.entry(11, -15), Map.entry(12, -10), Map.entry(13, -10), Map.entry(14, -15), Map.entry(15, -20), Map.entry(16, -20),
        Map.entry(17, -10), Map.entry(18, 0), Map.entry(19, 10), Map.entry(20, 10), Map.entry(21, 10), Map.entry(22, 10), Map.entry(23, 0), Map.entry(24, -10),
        Map.entry(25, -5), Map.entry(26, 0), Map.entry(27, 10), Map.entry(28, 10), Map.entry(29, 10), Map.entry(30, 10), Map.entry(31, 0), Map.entry(32, -5),
        Map.entry(33, 0), Map.entry(34, 5), Map.entry(35, 15), Map.entry(36, 20), Map.entry(37, 20), Map.entry(38, 20), Map.entry(39, 5), Map.entry(40, 0),
        Map.entry(41, -10), Map.entry(42, 0), Map.entry(43, 10), Map.entry(44, 15), Map.entry(45, 15), Map.entry(46, 10), Map.entry(47, 0), Map.entry(48, -10),
        Map.entry(49, -15), Map.entry(50, -15), Map.entry(51, -10), Map.entry(52, -10), Map.entry(53, -10), Map.entry(54, -10), Map.entry(55, -15), Map.entry(56, -15),
        Map.entry(57, -25), Map.entry(58, -25), Map.entry(59, -20), Map.entry(60, -20), Map.entry(61, -20), Map.entry(62, -20), Map.entry(63, -25), Map.entry(64, -25) 
    );
    static final Map<String, String> openings = Map.ofEntries(
        //0ply
        Map.entry("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "Depth 2147483647\nEvaluation: 0.2\nBest move: e2e4"), // starting position
        //1ply
        Map.entry("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", "Depth 2147483647\nEvaluation: 0.2\nBest move: e7e5"), // e4
        Map.entry("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1", "Depth 2147483647\nEvaluation: 0.2\nBest move: g8f6"), // d4
        Map.entry("rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1", "Depth 2147483647\nEvaluation: 0.1\nBest move: e7e5"), // c4, cut
        Map.entry("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQKQ - 1 1", "Depth 2147483647\nEvaluation: 0.1\nBest move: g8f6"), // Nf3, cut
        //2ply, e4
        Map.entry("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: g1f3"), // e4 e5
        Map.entry("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: g1f3"), // e4 c5
        Map.entry("rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: d2d4"), // e4 e6
        Map.entry("rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: d2d4"), // e4 c6
        //2ply, d4
        Map.entry("rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: c2c4"), // d4 Nf6
        Map.entry("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.3\nBest move: c2c4"), // d4 d5
        //3ply, e4 e5
        Map.entry("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: b8c6"), // e4 e5 Nf3
        Map.entry("rnbqkbnr/pppp1ppp/8/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.1\nBest move: g8f6"), // e4 e5 Nc3, cut
        Map.entry("rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RN1QKBNR b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.1\nBest move: g8f6"), // e4 e5 Bc4, cut
        //3ply, e4 c5
        Map.entry("rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: d7d6"), // e4 c5 Nf3
        Map.entry("rnbqkbnr/pp1ppppp/8/2p5/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: b8c6"), // e4 c5 Nc3, cut
        Map.entry("rnbqkbnr/pp1ppppp/8/2p5/4P3/2P5/PP1P1PPP/RNBQKBNR b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: g8f6"), // e4 c5 c3, cut
        //3ply, e4 e6
        Map.entry("rnbqkbnr/pppp1ppp/4p3/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: d7d5"), // e4 e6 d4
        //3ply, e4 c6
        Map.entry("rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: d7d5"), // e4 c6 d4
        //3ply, d4 Nf6
        Map.entry("r1bqkbnr/pppppppp/5n2/8/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: e7e6"), // d4 Nf6 c4
        Map.entry("r1bqkbnr/pppppppp/5n2/8/3P1B2/8/PPP1PPPP/RN1QKBNR b KQkq - 2 2", "Depth 2147483647\nEvaluation: 0.0\nBest move: d7d5"), // d4 Nf6 Bf4, cut
        //3ply, d4 d5
        Map.entry("rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2", "Depth 2147483647\nEvaluation: 0.2\nBest move: c7c6"), // d4 d5 c4
        Map.entry("rnbqkbnr/ppp1pppp/8/3p4/3P1B2/8/PPP1PPPP/RN1QKBNR b KQkq - 1 2", "Depth 2147483647\nEvaluation: 0.0\nBest move: g8f6"), // d4 d5 Bf4, cut
        //4ply, e4 e5 Nf3
        Map.entry("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: f1b5"), // e4 e5 Nf3 Nc6
        Map.entry("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: f3e5"), // e4 e5 Nf3 Nf6, cut
        //4ply, e4 c5 Nf3
        Map.entry("rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: d2d4"), // e4 c5 Nf3 d6
        Map.entry("r1bqkbnr/pp1ppppp/2n5/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: f1b5"), // e4 c5 Nf3 Nc6
        Map.entry("rnbqkbnr/pp1p1ppp/4p3/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: d2d4"), // e4 c5 Nf3 e6
        //4ply, e4 e6 d4
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: b1c3"), // e4 e6 d4 d5
        //4ply, e4 c6 d4
        Map.entry("rnbqkbnr/pp2pppp/2p5/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: e4e5"), // e4 c6 d4 d5
        //4ply, d4 Nf6 c4
        Map.entry("r1bqkbnr/pppp1ppp/4pn2/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: b1c3"), // d4 Nf6 c4 e6
        Map.entry("r1bqkbnr/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.4\nBest move: b1c3"), // d4 Nf6 c4 g6
        //4ply, d4 d5 c4
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: b1c3"), // d4 d5 c4 e6
        Map.entry("rnbqkbnr/pp2pppp/2p5/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: g1f3"), // d4 d5 c4 c6
        //5ply, e4 e5 Nf3 Nc6
        Map.entry("r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: a7a6"), // e4 e5 Nf3 Nc6 Bb5
        Map.entry("r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: g8f6"), // e4 e5 Nf3 Nc6 Bc4
        Map.entry("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: e5d4"), // e4 e5 Nf3 Nc6 d4
        Map.entry("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R b KQkq - 3 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: g8f6"), // e4 e5 Nf3 Nc6 Nc3
        //5ply, e4 c5 Nf3 d6
        Map.entry("rnbqkbnr/pp2pppp/3p4/2p5/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: c5d4"), // e4 c5 Nf3 d6 d4
        Map.entry("rnbqkbnr/pp2pppp/3p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: c8d7"), // e4 c5 Nf3 d6 Bb5+
        //5ply, e4 c5 Nf3 Nc6
        Map.entry("r1bqkbnr/pp1ppppp/2n5/2p5/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: c5d4"), // e4 c5 Nf3 Nc6 d4
        Map.entry("r1bqkbnr/pp1ppppp/2n5/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: g7g6"), // e4 c5 Nf3 Nc6 Bb5
        //5ply, e4 c5 Nf3 e6
        Map.entry("rnbqkbnr/pp1p1ppp/4p3/2p5/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: c5d4"), // e4 c5 Nf3 e6 d4
        //5ply, e4 e6 d4 d5
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.3\nBest move: f8b4"), // e4 e6 d4 d5 Nc3
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPPN1PPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: c7c5"), // e4 e6 d4 d5 Nd2
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3pP3/3P4/8/PPP2PPP/RNBQKBNR b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.1\nBest move: c7c5"), // e4 e6 d4 d5 e5
        //5ply, e4 c6 d4 d5
        Map.entry("rnbqkbnr/pp2pppp/2p5/3pP3/3P4/8/PPP2PPP/RNBQKBNR b KQkq - 0 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: c8f5"), // e4 c6 d4 d5 e5
        Map.entry("rnbqkbnr/pp2pppp/2p5/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: d5e4"), // e4 c6 d4 d5 Nc3
        //5ply, d4 Nf6 c4 e6
        Map.entry("r1bqkbnr/pppp1ppp/4pn2/8/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: f8b4"), // d4 Nf6 c4 e6 Nc3
        //5ply, d4 Nf6 c4 g6
        Map.entry("r1bqkbnr/pppppp1p/5np1/8/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.4\nBest move: d7d5"), // d4 Nf6 c4 g6 Nc3
        //Engines hate the kings indian, so the grunfeld is the next best option
        //5ply, d4 d5 c4 e6 
        Map.entry("rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3", "Depth 2147483647\nEvaluation: 0.2\nBest move: g8f6") // d4 d5 c4 e6 Nc3
    );
}
class TimeExceededException extends Exception {
    TimeExceededException() {
        super("");
    }
}
class GameAlreadyOverException extends Exception {
    GameAlreadyOverException(String message) {
        super(message);
    }
}
class InvalidFENException extends Exception {
    InvalidFENException(String message) {
        super(message);
    }
}
class GameEndsException extends Exception {
    GameEndsException(int status) {
        super(Integer.toString(status));
    }
}
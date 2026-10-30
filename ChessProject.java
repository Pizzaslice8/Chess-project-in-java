import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.Comparator;
import java.lang.Math;

public class ChessProject {
    public static void main(String[] args) {
        /*
        PASTE INTO TERMINAL:
        & "C:\Users\cbh5981\Documents\jdk-27\bin\java.exe" "C:\Users\cbh5981\Documents\Chess-project-in-java\ChessProject.java"
        */
        try {
            run();
        } catch (NoSuchElementException n) {
            System.out.println("\nInterrupt");
        } catch (GameAlreadyOverException g) {
            String message = "Draw";
            switch (g.getMessage()) {
                case "1":
                    message = "Checkmate white wins";
                    break;
                case "2":
                    message = "Checkmate black wins";
                    break;
                default:
                    break;
            }
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
        System.out.print("Initializing... ");
        Reference.initialize();
        Scanner scanner = new Scanner(System.in);
        int[] setup = new int[72];
        System.out.println("done");
        try {Thread.sleep(500);} catch (InterruptedException ie) {}
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
        if (setup[69] != 0 && ChessProject.getY(setup[69]) != (setup[64] == 1 ? 6 : 3)) {
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
        pos.displayStats();
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
                    lastCompleted = pos.evalDepth(i, lastCompleted);
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
        switch (piece) {
            case 'K': return 1;
            case 'k': return 2;
            case 'Q': return 3;
            case 'q': return 4;
            case 'R': return 5;
            case 'r': return 6;
            case 'B': return 7;
            case 'b': return 8;
            case 'N': return 9;
            case 'n': return 10;
            case 'P': return 11;
            case 'p': return 12;
            default: return 0;
        }
    }
    static char getChar(int piece) {
        switch (piece) {
            case 1: return 'K';
            case 2: return 'k';
            case 3: return 'Q';
            case 4: return 'q';
            case 5: return 'R';
            case 6: return 'r';
            case 7: return 'B';
            case 8: return 'b';
            case 9: return 'N';
            case 10: return 'n';
            case 11: return 'P';
            case 12: return 'p';
            default: return '0';
        }
    }
    static int materialWorth(int piece) {
        switch (piece) {
            case 3: return 90;
            case 4: return -90;
            case 5: return 50;
            case 6: return -50;
            case 7: case 9: return 30;
            case 8: case 10: return -30;
            case 11: return 10;
            case 12: return -10;
            default: return 0;
        }
    }
    static boolean color(int piece) {
        return (piece % 2) == 1;
    }
}
class Position {
    int[] position; //can't use byte[] because arithmetic operations will be performed
    long currentZobrist;
    long[] pieceBoard = new long[12];
    int limit, maxDepth;
    int[][] undoStack = new int[50][72];
    int slot = 0;
    int undoDeep = 0;
    //killer moves
    int[] killerMoves = new int[50];
    //history
    int[][] history = new int[500][10];
    int historyStored = 0;
    void sortHistory() {
        Arrays.sort(history, 0, historyStored, Comparator.comparingInt((int[] row) -> Reference.countNonzeros(row)).reversed());
    }
    int[] getHistory() {
        if (historyStored == 0) return new int[0];
        int[] out = new int[historyStored];
        for (int i = 0; i < historyStored; i++) {
            out[i] = history[i][0];
        }
        return out;
    }
    void storeQuietMove(int piece, int start, int end, int depth) {
        int compressed = compressMove(piece, start, end);
        //store killer move
        if (killerMoves[0] != compressed && killerMoves[1] != compressed && killerMoves[2] != compressed) {
            if (killerMoves[0] == 0) killerMoves[0] = compressed;
            else if (killerMoves[1] == 0) killerMoves[1] = compressed;
            else if (killerMoves[2] == 0) killerMoves[2] = compressed;
        }
        //store history
        int index = -1;
        for (int i = 0; i < historyStored; i++) {
            if (history[i][0] == compressed) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            history[historyStored][0] = compressed;
            historyStored++;
        } else if (Reference.countNonzeros(history[index]) != 10) {
            history[index][Reference.countNonzeros(history[index])] = compressed;
        }
    }
    //transposition table
    long[] zobristStorage = new long[Reference.length];
    int[] evals = new int[Reference.length]; //in decipawns
    int[][] otherInfo = new int[Reference.length][4]; //best move from, best move to, depth searched, bound
    void store(int[] pos, int evaluation, int bestStart, int bestEnd, int depth, int bound) {
        try {
            zobristStorage[slot] = Reference.getZobrist(position);
            evals[slot] = evaluation;
            otherInfo[slot][0] = bestStart;
            otherInfo[slot][1] = bestEnd;
            otherInfo[slot][2] = depth;
            otherInfo[slot][3] = bound;
            slot++;
        } catch (ArrayIndexOutOfBoundsException a) {
            throw new OutOfSpaceException();
        }
    }
    int[] getInfo() {
        for (int i = 0; i < slot; i++) {
            if (currentZobrist == zobristStorage[i]) {
                //found a match!
                return new int[]{evals[i], otherInfo[i][0], otherInfo[i][1], otherInfo[i][2], otherInfo[i][3]};
            }
        }
        return new int[0];
    }
    int compressMove(int piece, int start, int end) {
        //piece has 4 bits, start has 6 bits, end has 6 bits
        //will never overflow to Integer.MIN_VALUE because piece doesn't go all the way up to 16
        int out = piece-1;
        out <<= 6;
        out += start-1;
        out <<= 6;
        out += end-1;
        return out;
    }
    int[] extractMove(int compressed) {
        //start, end, piece
        int[] out = new int[3];
        out[1] = compressed % 0b1000000 + 1;
        compressed >>= 6;
        out[0] = compressed % 0b1000000 + 1;
        compressed >>= 6;
        out[2] = compressed % 0b10000 + 1;
        return out;
    }
    byte[] pieceActivity = new byte[64], material = new byte[64], centralControl = new byte[64],
    kingSafety = new byte[64], space = new byte[64], rooksSeventh = new byte[64], outposts = new byte[64];
    int bishopPairBonus(boolean tactical) {
        int whiteBishops = 0, blackBishops = 0;
        for (int i = 0; i < 64; i++) {
            if (position[i] == 7) whiteBishops++;
            if (position[i] == 8) blackBishops++;
        }
        int whiteBonus = (whiteBishops >= 2 && tactical) ? 2 : (whiteBishops >= 2 && !tactical) ? 1 : 0;
        int blackBonus = (blackBishops >= 2 && tactical) ? -2 : (blackBishops >= 2 && !tactical) ? -1 : 0;
        return whiteBonus + blackBonus;
    }
    int currentCount() {
        return currentCount(true);
    }
    int currentCount(boolean checkStatus) {
        if (checkStatus) {
            try {gameStatus();} catch (GameEndsException g) {
                switch (g.getMessage()) {
                    case "1": return Integer.MAX_VALUE;
                    case "2": return Integer.MIN_VALUE;
                    default: return 0;
                }
            }
        }
        int outputTactical = 0;
        int outputNontactical = 0;
        boolean tactical = activePieces() > 4;
        for (int i = 0; i < 64; i++) {
            outputTactical += pieceActivity[i] + material[i] + centralControl[i]  + kingSafety[i] + rooksSeventh[i] + (space[i] + outposts[i]) / 3;
            outputNontactical += material[i] + centralControl[i] + space[i] + outposts[i] + (pieceActivity[i] + kingSafety[i] + rooksSeventh[i]) / 3;
        }
        return (tactical ? outputTactical : outputNontactical) + ((position[64] == 1) ? 1 : -1) + bishopPairBonus(tactical);
    }
    int activePieces() {
        //test if both sides have a lot of really active pieces
        //if true, then currentCount gives priority to piece activity, king safety, bishop pair, active rooks, if not, give priority to space, outposts
        int reallyActive = 0;
        for (int i = 0; i < 64; i++) {
            int modifier = (ChessProject.color(position[i])) ? 1 : -1;
            if (pieceActivity[i] * modifier >= 8) {
                reallyActive++;
            }
        }
        return reallyActive;
    }
    void update(int square) {
        pieceActivity[square-1] = 0;
        material[square-1] = 0;
        centralControl[square-1] = 0;
        kingSafety[square-1] = 0;
        space[square-1] = 0;
        rooksSeventh[square-1] = 0;
        outposts[square-1] = 0;
        int piece = getPiece(square);
        if (piece == 0) return;
        int modifier = (ChessProject.color(piece)) ? 1 : -1;
        int sameColorPawn = (23 - modifier) / 2;
        int[] pieceVision = Reference.unserialize(Reference.getBitboard(square-1, position, piece));
        for (int s : pieceVision) {
            if (s == 27 || s == 28 || s == 35 || s == 36) {
                centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
            }
        }
        material[square-1] = (byte) ChessProject.materialWorth(piece);
        if (piece == 3 || piece == 4) {
            pieceActivity[square-1] = (byte) (modifier * pieceVision.length / 2);
        } else if (piece == 5 || piece == 6 || piece == 7 || piece == 8) {
            pieceActivity[square-1] = (byte) (modifier * pieceVision.length);
        }  
        if (piece == 9 || piece == 10) {
            int equivalentY = 9 - ChessProject.getY(square);
            int equivalent = ChessProject.getX(square) + 8 * (equivalentY - 1);
            pieceActivity[square-1] = (byte) (modifier * Reference.knightBonus[(piece == 9) ? square-1 : equivalent-1]);

            if (ChessProject.getY(square) == ((piece == 9) ? 5 : 4) || ChessProject.getY(square) == ((piece == 9) ? 6 : 3)) {
                //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                if (ChessProject.getX(square) >= 3 && ChessProject.getX(square) <= 6) {
                    //center files, which are more valuable
                    if (getPiece(ChessProject.translate(square, 1, -modifier)) == sameColorPawn || getPiece(ChessProject.translate(square, -1, -modifier)) == sameColorPawn) {
                        outposts[square-1] = (byte) (modifier * 8);
                    }
                } else {
                    // flank files
                    if (getPiece(ChessProject.translate(square, 1, -modifier)) == sameColorPawn || getPiece(ChessProject.translate(square, -1, -modifier)) == sameColorPawn) {
                        outposts[square-1] = (byte) (modifier * 4);
                    }
                }
            }
        }
        if (piece == 1 || piece == 2) {
            double proximityToCenter = Math.hypot(ChessProject.getX(square) - 4.5, ChessProject.getY(square) - 4.5);
            if (endgame()) {
                pieceActivity[square-1] = (byte) (modifier * (10 - Math.round(proximityToCenter * 1.7)));
            } else {
                kingSafety[square-1] = (byte) (modifier * (Math.round(proximityToCenter * 1.7) - 10));
                if (getPiece(ChessProject.translate(square, 0, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 10); //pawn in front of king
                }
                if (getPiece(ChessProject.translate(square, 1, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 5);
                }
                if (getPiece(ChessProject.translate(square, -1, modifier)) == sameColorPawn) {
                    kingSafety[square-1] = (byte) (kingSafety[square-1] + modifier * 5);
                }
            }
        }
        if (piece == 5) {
            if (ChessProject.getY(square) == 7) rooksSeventh[square-1] = 15;
            if (ChessProject.getY(square) == 6 || ChessProject.getY(square) == 8) rooksSeventh[square-1] = 5;
        }
        if (piece == 6) {
            if (ChessProject.getY(square) == 2) rooksSeventh[square-1] = -15;
            if (ChessProject.getY(square) == 3 || ChessProject.getY(square) == 1) rooksSeventh[square-1] = -5;
        }
        if (piece == 7 || piece == 8) {
            int square1 = (piece == 7) ? 36 : 28;
            int square2 = (piece == 7) ? 37 : 29;
            if (square == square1 || square == square2) {
                if (getPiece(ChessProject.translate(square, 1, -modifier)) == sameColorPawn || getPiece(ChessProject.translate(square, -1, -modifier)) == sameColorPawn) {
                    outposts[square-1] = (byte) (modifier * 5);
                }
            }
        }
        if (piece == 11 || piece == 12) {
            int equivalent = (piece == 11) ? ChessProject.getY(square) : 9 - ChessProject.getY(square);
            double addition = Math.pow(equivalent, 2) / 12.0; //flank files, a, b, g, h
            switch (ChessProject.getX(square)) {
                // central space is worth more
                case 3:
                case 6:
                    addition = Math.pow(equivalent, 2) / 9.0; // c and f files
                    break;
                case 4:
                case 5:
                    addition = Math.pow(equivalent, 2) / 7.0; // center files: d and e
                    break;
                default:
                    break;
            }
            if (endgame()) {
                for (int s : passedPawns()) {
                    if (square == s) {
                        modifier *= 5;
                        break;
                    }
                }
            }
            space[square-1] = (byte) Math.round(modifier * addition);
        }
    }
    Position(int[] setup, int timeLimit, int depth) {
        this.position = setup;
        this.limit = timeLimit;
        this.maxDepth = depth;
        this.currentZobrist = Reference.getZobrist(setup);
        for (int i = 0; i < 64; i++) {
            if (setup[i] != 0) {
                pieceBoard[setup[i] - 1] |= (1L << i);
            }
            update(i+1);
        }
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
        //HOPEFULLY this never happens
        String message = "Looking for " + ((position[64] == 1) ? "white" : "black") + " king but found none\nThis is the current position:\n";
        display();
        throw new RuntimeException(message);
    }
    void display() {
        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                String chr = Character.toString(ChessProject.getChar(position[i * 8 + j]));
                String output = ("0".equals(chr)) ? "  " : chr + " ";
                System.out.print(output);
            }
            System.out.println("");
        }
    }
    void displayStats() {
        int materialCount = 0, pieceActivityCount = 0, centralControlCount = 0, kingSafetyCount = 0, spaceCount = 0, rookCount = 0, outpostsCount = 0;
        for (int i = 0; i < 64; i++) {
            materialCount += material[i];
            pieceActivityCount += pieceActivity[i];
            centralControlCount += centralControl[i];
            kingSafetyCount += kingSafety[i];
            spaceCount += space[i];
            rookCount += rooksSeventh[i];
            outpostsCount += outposts[i];
        }
        System.out.print("Current zobrist hash: ");
        System.out.println(currentZobrist);
        System.out.println("In check? " + ((inCheck()) ? "yes" : "no"));
        System.out.print("Legal moves: ");

        byte[][] getMoves = generateMoves();
        for (int i = 0; i < getMoves[0].length; i++) {
            System.out.print(ChessProject.parseNumbers(getMoves[0][i], getMoves[1][i]));
            if (i < getMoves[0].length-1) System.out.print(", ");
        }
        System.out.println("");
        System.out.print("Material count: ");
        System.out.println(materialCount);
        System.out.print("Piece activity differential: ");
        System.out.println(pieceActivityCount);
        System.out.print("Central control differential: ");
        System.out.println(centralControlCount);
        System.out.print("King safety differential: ");
        System.out.println(kingSafetyCount);
        System.out.print("Advanced rook differential: ");
        System.out.println(rookCount);
        System.out.print("Outposts: ");
        System.out.println(outpostsCount);
        System.out.print("Number of really active pieces: ");
        System.out.println(activePieces());
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
            int newSquare = ChessProject.translate(square, directionX * i, directionY * i);
            if (newSquare == 0) {
                break;
            }
            if (getPiece(newSquare) == 0) {
                pad[where][i-1] = (byte) newSquare;
            } else {
                if (ChessProject.color(getPiece(newSquare)) != colorStop) {
                    pad[where][i-1] = (byte) newSquare;
                }
                break;
            }
        }
    }
    void legalMovesBasic(int square) {
        clearPad(0);
        int piece = getPiece(square);
        boolean color = ChessProject.color(piece);
        int index = 0;
        if (piece == 11) {
            //pawn
            int candidate = ChessProject.translate(square, 1, 1);
            if (candidate != 0 && getPiece(candidate) != 0 && ChessProject.color(getPiece(candidate)) != color && getPiece(candidate) != 2) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = ChessProject.translate(square, -1, 1);
            if (candidate != 0 && getPiece(candidate) != 0 && ChessProject.color(getPiece(candidate)) != color && getPiece(candidate) != 2) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = ChessProject.translate(square, 0, 1);
            if (candidate != 0 && getPiece(candidate) == 0) {
                pad[0][index] = (byte) candidate;
                index++;
                candidate = ChessProject.translate(square, 0, 2);
                if (candidate != 0 && getPiece(candidate) == 0 && square > 8 && square <= 16) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
            }
            if (position[69] != 0 && getPiece(position[69]) == 0 &&
                    (position[69] == ChessProject.translate(square, 1, 1) || position[69] == ChessProject.translate(square, -1, 1))) {
                pad[0][index] = (byte) position[69];
                index++;
            }
        } else if (piece == 12) {
            //pawn
            int candidate = ChessProject.translate(square, 1, -1);
            if (candidate != 0 && getPiece(candidate) != 0 && ChessProject.color(getPiece(candidate)) != color && getPiece(candidate) != 1) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = ChessProject.translate(square, -1, -1);
            if (candidate != 0 && getPiece(candidate) != 0 && ChessProject.color(getPiece(candidate)) != color && getPiece(candidate) != 1) {
                pad[0][index] = (byte) candidate;
                index++;
            }
            candidate = ChessProject.translate(square, 0, -1);
            if (candidate != 0 && getPiece(candidate) == 0) {
                pad[0][index] = (byte) candidate;
                index++;
                candidate = ChessProject.translate(square, 0, -2);
                if (getPiece(candidate) == 0 && square > 48 && square <= 56) {
                    pad[0][index] = (byte) candidate;
                    index++;
                }
            }
            if (position[69] != 0 && getPiece(position[69]) == 0 &&
                    (position[69] == ChessProject.translate(square, 1, -1) || position[69] == ChessProject.translate(square, -1, -1))) {
                pad[0][index] = (byte) position[69];
                index++;
            }
        } else {
            int[] squares = Reference.unserialize(Reference.getBitboard(square-1, position, piece));
            for (int s : squares) {
                if (position[s] == 0 || (ChessProject.color(position[s]) != color && position[s] != 1 && position[s] != 2)) {
                    pad[0][index] = (byte) (s + 1);
                    index++;
                }
            }
        }
    }
    void makeMove(int start, int end) {
        currentZobrist = Reference.updateZobrist(position, currentZobrist, start, end);
        for (int i = 0; i < 72; i++) {
            undoStack[undoDeep][i] = position[i];
        }
        undoDeep++;
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
            pieceBoard[captured-1] ^= (1L << (end - 1));
        }
        if (position[64] == 0) {
            position[71] = position[71] + 1;
        }
        position[end-1] = piece;
        position[start-1] = 0;
        pieceBoard[piece-1] |= (1L << (end - 1));
        pieceBoard[piece-1] ^= (1L << (start - 1));
        switch (piece) {
            case 1:
                position[65] = 0;
                position[66] = 0;
                if (end == 3) {
                    //white castle long
                    position[0] = 0;
                    position[3] = 5;
                    pieceBoard[4] |= (1L << 3);
                    pieceBoard[4] ^= 1L;
                }
                if (end == 7) {
                    //white castle short
                    position[7] = 0;
                    position[5] = 5;
                    pieceBoard[4] |= (1L << 5);
                    pieceBoard[4] ^= (1L << 7);
                }
                break;
            case 2:
                position[67] = 0;
                position[68] = 0;
                if (end == 59) {
                    //black castle long
                    position[56] = 0;
                    position[59] = 6;
                    pieceBoard[5] |= (1L << 59);
                    pieceBoard[5] ^= (1L << 56);
                }
                if (end == 63) {
                    //black castle short
                    position[63] = 0;
                    position[61] = 6;
                    pieceBoard[5] |= (1L << 61);
                    pieceBoard[5] ^= (1L << 63);
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
                    int squareid = ChessProject.translate(end, 0, -1) - 1;
                    pieceBoard[11] ^= (1L << squareid);
                    position[squareid] = 0;
                }
                position[69] = 0;
                if (start > 8 && start <= 16 && end > 24 && end <= 32) {
                    //pawn move 2 squares, check for en passant
                    if (getPiece(ChessProject.translate(end, 1, 0)) == 12 || getPiece(ChessProject.translate(end, -1, 0)) == 12) {
                        position[69] = ChessProject.translate(start, 0, 1);
                    }
                }
                if (start > 48 && start <= 56) {
                    //promotion
                    position[end-1] = 3;
                    pieceBoard[10] ^= (1L << end-1);
                    pieceBoard[2] |= (1L << end-1);
                }
                position[70] = 0;
                break;
            case 12:
                //black pawn
                if (end == position[69]) {
                    //en passant!
                    int squareid = ChessProject.translate(end, 0, 1) - 1;
                    pieceBoard[10] ^= (1L << squareid);
                    position[squareid] = 0;
                }
                position[69] = 0;
                if (start > 48 && start <= 56 && end > 32 && end <= 40) {
                    //pawn move 2 squares, check for en passant
                    if (getPiece(ChessProject.translate(end, 1, 0)) == 11 || getPiece(ChessProject.translate(end, -1, 0)) == 11) {
                        position[69] = ChessProject.translate(start, 0, -1);
                    }
                }
                if (start > 8 && start <= 16) {
                    //promotion
                    position[end-1] = 4;
                    pieceBoard[11] ^= (1L << end-1);
                    pieceBoard[3] |= (1L << end-1);
                }
                position[70] = 0;
                break;
            default:
                break;
        }
        position[64] = 1 - position[64];
    }
    void restore() {
        undoDeep--;
        int[] previousPosition = (undoDeep >= 0) ? undoStack[undoDeep] : new int[72]; //safeguard, shouldn't ever happen
        currentZobrist = Reference.getZobrist(previousPosition);
        for (int i = 0; i < 12; i++) {
            pieceBoard[i] = 0L;
        }
        for (int i = 0; i < 72; i++) {
            position[i] = previousPosition[i];
            if (i >= 64) continue;
            if (position[i] != 0) {
                pieceBoard[position[i] - 1] |= (1L << i);
            }
        }
    }
    byte[] legalMoves(int square) {
        legalMovesBasic(square);
        byte[] basicMoves = retrieve(0);
        clearPad(8);
        int i = 0;
        for (byte move : basicMoves) {
            makeMove(square, move);
            position[64] = 1 - position[64];
            boolean checkedAfterMove = inCheck();
            if (!checkedAfterMove) {
                pad[8][i] = move;
                i++;
            }
            position[64] = 1 - position[64];
            restore();
        }
        if (!inCheck()) {
            if (square == 5) {
                if (position[65] == 1 && getPiece(8) == 5 && getPiece(6) == 0 && getPiece(7) == 0) {
                    boolean allowed = true;
                    position[4] = 0;
                    position[5] = 1;
                    pieceBoard[0] = (1L << 5);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[5] = 0;
                    position[6] = 1;
                    pieceBoard[0] = (1L << 6);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[6] = 0;
                    position[4] = 1;
                    pieceBoard[0] = (1L << 4);
                    if (allowed) {
                        pad[8][i] = 7;
                        i++;
                    }    
                }
                if (position[66] == 1 && getPiece(1) == 5 && getPiece(4) == 0 && getPiece(3) == 0 && getPiece(2) == 0) {   
                    boolean allowed = true; 
                    position[4] = 0;
                    position[3] = 1;
                    pieceBoard[0] = (1L << 3);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[3] = 0;
                    position[2] = 1;
                    pieceBoard[0] = (1L << 2);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[2] = 0;
                    position[4] = 1;
                    pieceBoard[0] = (1L << 4);
                    if (allowed) {
                        pad[8][i] = 3;
                        i++;
                    }
                }
            }
            if (square == 61) {
                if (position[67] == 1 && getPiece(64) == 6 && getPiece(62) == 0 && getPiece(63) == 0) {
                    boolean allowed = true;
                    position[60] = 0;
                    position[61] = 2;
                    pieceBoard[1] = (1L << 61);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[61] = 0;
                    position[62] = 2;
                    pieceBoard[1] = (1L << 62);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[62] = 0;
                    position[60] = 2;
                    pieceBoard[1] = (1L << 60);
                    if (allowed) {
                        pad[8][i] = 63;
                        i++;
                    }    
                }
                if (position[68] == 1 && getPiece(57) == 6 && getPiece(60) == 0 && getPiece(59) == 0 && getPiece(58) == 0) {   
                    boolean allowed = true; 
                    position[60] = 0;
                    position[59] = 2;
                    pieceBoard[1] = (1L << 59);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[59] = 0;
                    position[58] = 2;
                    pieceBoard[1] = (1L << 58);
                    if (inCheck()) {
                        allowed = false;
                    }
                    position[58] = 0;
                    position[60] = 2;
                    pieceBoard[1] = (1L << 60);
                    if (allowed) {
                        pad[8][i] = 59;
                        i++;
                    }
                }
            }
        }
        return retrieve(8);
    }
    boolean inCheck() {
        int king = kingSquare() - 1;
        return (position[64] == 1) ? (
            //test black pieces
            (Reference.getBitboard(king, position, 2) & pieceBoard[1]) != 0 ||
            (Reference.getBitboard(king, position, 6) & (pieceBoard[5] | pieceBoard[3])) != 0 ||
            (Reference.getBitboard(king, position, 8) & (pieceBoard[7] | pieceBoard[3])) != 0 ||
            (Reference.getBitboard(king, position, 10) & pieceBoard[9]) != 0 ||
            (Reference.getBitboard(king, position, 11) & pieceBoard[11]) != 0
        ) : (
            //test white pieces
            (Reference.getBitboard(king, position, 1) & pieceBoard[0]) != 0 ||
            (Reference.getBitboard(king, position, 5) & (pieceBoard[4] | pieceBoard[2])) != 0 ||
            (Reference.getBitboard(king, position, 7) & (pieceBoard[6] | pieceBoard[2])) != 0 ||
            (Reference.getBitboard(king, position, 9) & pieceBoard[8]) != 0 ||
            (Reference.getBitboard(king, position, 12) & pieceBoard[10]) != 0
        );
    }
    byte[][] generateMoves() {
        clearPad(5);
        clearPad(6);
        //this is the culmination of everything else above
        int i = 0;
        for (int j = 0; j < 64; j++) {
            //checks all 64 squares
            boolean color = ChessProject.color(position[j]);
            boolean toMove = position[64] == 1;
            
            if (position[j] != 0 && color == toMove) {
                //square is not empty and is of the color to move
                byte[] moves = legalMoves(j+1);
                if (moves.length > 0) {
                    //piece has legal moves
                    for (byte move : moves) {
                        pad[5][i] = (byte) (j+1);
                        pad[6][i] = move;
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
        sortHistory();
        int[][] moves = new int[unsortedMoves[0].length][3]; //from, to, score
        boolean isEndgame = endgame();
        int[] data = getInfo();
        /*score based on:
        1. cached best move
        2. captures, most valuable victim, least valuable attacker
        3. killer moves
        4. history
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
            if (data.length != 0) {
                if (start == data[1] && end == data[2]) {
                    moves[i][2] = Integer.MAX_VALUE;
                    continue;
                }
            }
            int piece = getPiece(start);
            boolean pieceColor = ChessProject.color(piece);
            int modifier = (pieceColor) ? 1 : -1;
            int captured = getPiece(end);
            boolean capture = captured != 0 || ((piece == 11 || piece == 12) && end == position[69]);
            boolean promotion = (piece == 11 && end >= 57) || (piece == 12 && end <= 8);
            //capture/promotion
            if (capture || promotion) {
                int difference = 0;
                if (capture) {
                    //most valuable victim, least valuable attacker
                    int victimWorth = (captured == 0) ? 10 : Math.abs(ChessProject.materialWorth(captured));
                    difference += (piece == 1 || piece == 2) ? 0 : 100 + victimWorth - Math.abs(ChessProject.materialWorth(piece));
                }
                if (promotion) {
                    difference += 180;
                }
                //max of difference is 360, for "pawn takes queen promotes to queen"
                moves[i][2] = Integer.MAX_VALUE - 361 + difference;
                continue;
            }
            //killer moves heuristic
            boolean foundMatch = false;
            for (int killerMove : killerMoves) {
                int[] extracted = extractMove(killerMove);
                if (start == extracted[0] && end == extracted[1]) {
                    //found match
                    moves[i][2] = Integer.MAX_VALUE - 362;
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch) continue;
            //history heuristic
            int[] historySorted = getHistory();
            for (int j = 0; j < historySorted.length; j++) {
                int[] extracted = extractMove(historySorted[j]);
                if (start == extracted[0] && end == extracted[1]) {
                    //found match
                    moves[i][2] = Integer.MAX_VALUE - 363 - j;
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch) continue;
            //quiet moves not principal or in killer moves or in history, counting up from 0 as opposed to counting down from 2147483647
            int cumulative = 0;
            //reward center-seeking moves
            double startProximity = Math.hypot(ChessProject.getX(start) - 4.5, ChessProject.getY(start) - 4.5);
            double endProximity = Math.hypot(ChessProject.getX(end) - 4.5, ChessProject.getY(end) - 4.5);
            int centerGain = (int) Math.round(startProximity - endProximity);
            cumulative += (isEndgame && (piece == 1 || piece == 2)) ? 3 * centerGain : centerGain;
            //reward forward moves
            cumulative += (pieceColor) ? ChessProject.getY(end) - ChessProject.getY(start) : ChessProject.getY(start) - ChessProject.getY(end);
            if (piece == 11 || piece == 12) {
                //reward pawn moves close to promotion
                boolean farAdvanced = (pieceColor) ? ChessProject.getY(end) >= 6 : ChessProject.getY(end) <= 3;
                if (farAdvanced) {
                    cumulative += 20;
                }
                //punish pawn moves in front of king if !endgame()
                if (!isEndgame) {
                    int king = (pieceColor) ? 1 : 2;
                    if (getPiece(ChessProject.translate(start, 0, -modifier)) == king) {
                        cumulative -= 15;
                    }
                }
            }
            if (piece == 5 || piece == 6) {
                //reward rook moves onto the 7th
                if (ChessProject.getY(end) == ((pieceColor) ? 7 : 2)) {
                    cumulative += 25;
                }
                if (ChessProject.getY(end) == ((pieceColor) ? 8 : 1) || ChessProject.getY(end) == ((pieceColor) ? 6 : 3)) {
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
        if (!sufficientMaterial() || position[71] >= 100) {
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
                    break;
                default:
                    break;
            }
            if (queens == true) break;
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
            switch (position[i]) {
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
    int[] passedPawns() {
        boolean[] whitePawnFiles = {false, false, false, false, false, false, false, false}, blackPawnFiles = {false, false, false, false, false, false, false, false};
        for (int i = 0; i < 64; i++) {
            if (position[i] == 11) whitePawnFiles[i] = true;
            if (position[i] == 12) blackPawnFiles[i] = true;
        }
        boolean[] whitePassedFiles = {false, false, false, false, false, false, false, false}, blackPassedFiles = {false, false, false, false, false, false, false, false};
        boolean passed;
        for (int i = 0; i < 8; i++) {
            passed = whitePawnFiles[i] && !blackPawnFiles[i];
            try {passed &= !blackPawnFiles[i-1];} catch (ArrayIndexOutOfBoundsException a) {}
            try {passed &= !blackPawnFiles[i+1];} catch (ArrayIndexOutOfBoundsException a) {}
            whitePassedFiles[i] = passed;
            passed = blackPawnFiles[i] && !whitePawnFiles[i];
            try {passed &= !whitePawnFiles[i-1];} catch (ArrayIndexOutOfBoundsException a) {}
            try {passed &= !whitePawnFiles[i+1];} catch (ArrayIndexOutOfBoundsException a) {}
            blackPawnFiles[i] = passed;
        }
        int[] out = new int[16];
        int i = 0;
        for (int j = 0; j < 64; j++) {
            int file = j % 8;
            if (position[j] == 11 && whitePassedFiles[file]) out[i++] = j;
            if (position[j] == 12 && blackPassedFiles[file]) out[i++] = j;
        }
        return Arrays.copyOf(out, Reference.countNonzeros(out));
    }
    int simpleCheckmate() {
        //0 = no, 1 = ladder - white, 2 = white with queen, 3 = white with rook, -1 = ladder - black, -2 = black with queen, -3 = black with rook
        int whiteQueens = 0, whiteRooks = 0, whiteHeavyPieces = 0, blackQueens = 0, blackRooks = 0, blackHeavyPieces = 0;
        for (int i = 0; i < 64; i++) {
            switch (position[i]) {
                case 1: case 2: break;
                case 3:
                    whiteQueens++;
                    whiteHeavyPieces++;
                    break;
                case 4:
                    blackQueens++;
                    blackHeavyPieces++;
                    break;
                case 5:
                    whiteRooks++;
                    whiteHeavyPieces++;
                    break;
                case 6:
                    blackRooks++;
                    blackHeavyPieces++;
                    break;
                case 7: return 0;
                case 8: return 0;
                case 9: return 0;
                case 10: return 0;
                case 11: return 0;
                case 12: return 0;
            }
        }
        if (whiteHeavyPieces > 0 && blackHeavyPieces > 0) return 0;
        if (whiteHeavyPieces > 1) return 1;
        if (blackHeavyPieces > 1) return -1;
        if (whiteQueens == 1) return 2;
        if (whiteRooks == 1) return 3;
        if (blackQueens == 1) return -2;
        if (blackRooks == 1) return -3;
        return 0;
    }
    int[] evalDepth(int depth) throws TimeExceededException {
        return evalDepth(depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    int[] evalDepth(int depth, int[] previousResult) throws TimeExceededException {
        //aspiration window
        if (previousResult == null) return evalDepth(depth);
        int lower = (Integer.MIN_VALUE + 50 < previousResult[0]) ? previousResult[0] - 50 : Integer.MIN_VALUE;
        //in case previousResult[0] is less than 50 away from Integer.MIN_VALUE
        int higher = (Integer.MAX_VALUE - 50 > previousResult[0]) ? previousResult[0] + 50 : Integer.MAX_VALUE;
        //in case previousResult[0] is less than 50 away from Integer.MAX_VALUE
        int[] shallowSearch = evalDepth(depth, lower, higher);
        if (shallowSearch[0] == higher || shallowSearch[0] == lower)
        //the shallow search returned a bound, need to do a full search
        return evalDepth(depth);
        else return shallowSearch;
    }
    int evalQ(byte[][] sortedMoves, int alpha, int beta, boolean changeEval) {
        ChessProject.nodes++;
        byte[][] moves = sortedMoves;
        boolean maximizing = position[64] == 1;
        boolean checked = inCheck();
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        if (!checked) {
            //update everything temporarily
            if (changeEval) {
                for (int i = 1; i <= 64; i++) {
                    update(i);
                }
                best = currentCount(false);
            }
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
            potential = evalQ(sortMoves(generateMoves()), alpha, beta, true);
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
        ChessProject.nodes++;
        if (depth == maxDepth) {
            killerMoves[0] = 0;
            killerMoves[1] = 0;
            killerMoves[2] = 0;
        }
        int alphaOriginal = alpha;
        int betaOriginal = beta;
        int bound = 0;
        int[] data = getInfo();
        if (data.length != 0) {
            if (data[3] >= depth) {
                if (data[4] == 0) {
                    return new int[]{data[0], data[1], data[2]};
                }
                if (data[4] == -1) {
                    if (data[0] >= beta) return new int[]{data[0], data[1], data[2]};
                    alpha = Math.max(alpha, data[0]);
                }
                if (data[4] == 1) {
                    if (data[0] <= alpha) return new int[]{data[0], data[1], data[2]};
                    beta = Math.min(beta, data[0]);
                }
                if (alpha >= beta) {
                    if (!isTacticalMove(data[1], data[2])) storeQuietMove(getPiece(data[1]), data[1], data[2], depth);
                    return new int[]{data[0], data[1], data[2]};
                }
            }
        }
        int start = 0;
        if (depth == maxDepth) {
            start = ChessProject.getTime();
        }
        boolean maximizing = position[64] == 1;
        //null move pruning
        boolean hasNullMoveBound = maximizing ? beta < Integer.MAX_VALUE : alpha > Integer.MIN_VALUE;
        if (depth > 2 && hasNullMoveBound && !pawnEndgame() && !inCheck() && depth < maxDepth) { //don't want to do this at the root node
            int[] evalPass;
            int enPassantSquare = position[69];
            position[64] = 1 - position[64]; //make a null move
            position[69] = 0;
            currentZobrist = Reference.passMoveZobrist(position, currentZobrist);
            try {
                int nullDepth = depth - 3;
                evalPass = maximizing ? evalDepth(nullDepth, beta - 1, beta) : evalDepth(nullDepth, alpha, alpha + 1);
            } finally {
                position[64] = 1 - position[64]; //restore original position
                position[69] = enPassantSquare;
                currentZobrist = Reference.restoreFromPassZobrist(currentZobrist, enPassantSquare);
            }
            if (maximizing ? evalPass[0] >= beta : evalPass[0] <= alpha) {
                return new int[]{maximizing ? beta : alpha, 0, 0};
            }
        }
        byte[][] unorderedMoves = generateMoves();
        try {gameStatus(unorderedMoves);} catch (GameEndsException g) {
            switch (g.getMessage()) {
                case "1": return new int[]{Integer.MAX_VALUE, 0, 0};
                case "2": return new int[]{Integer.MIN_VALUE, 0, 0};
                default: return new int[3];
            }
        }
        byte[][] moves = sortMoves(unorderedMoves);
        if (depth == 0) {
            return new int[]{evalQ(moves, alpha, beta, true), 0, 0};
        }
        int evaluation = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestStart = 0;
        int bestEnd = 0;
        //int evalStatic = currentCount(false);
        for (int i = 0; i < moves[0].length; i++) {
            boolean nodeInCheck = inCheck();
            boolean reduce = depth > 2 && i >= 3 && !isTacticalMove(moves[0][i], moves[1][i]) && !inCheck(); 
            makeMove(moves[0][i], moves[1][i]); //color to move is now flipped
            boolean givesCheck = inCheck();
            int potential;
            try {
                //principal variation search
                //search the first move fully, then search subsequent moves with zero window and do a full search if it performs well
                if (i == 0) {
                    potential = evalDepth(depth - 1, alpha, beta)[0];
                } else {
                    if (maximizing) {
                        int trial = evalDepth(depth - 1, alpha, alpha + 1)[0];
                        potential = (trial > alpha) ? evalDepth(depth - 1, alpha, beta)[0] : trial;
                    } else {
                        int trial = evalDepth(depth - 1, beta - 1, beta)[0];
                        potential = (trial < beta) ? evalDepth(depth - 1, alpha, beta)[0] : trial;
                    }
                }
            } finally {
                restore();
            }
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
                if (!isTacticalMove(moves[0][i], moves[1][i])) storeQuietMove(getPiece(moves[0][i]), moves[0][i], moves[1][i], depth);
                break;
            }
        }
        int timeTaken = 0;
        if (depth == maxDepth) {
            timeTaken = ChessProject.getTime() - start;
            if (timeTaken > limit) {
                throw new TimeExceededException();
            }
        }
        bound = evaluation <= alphaOriginal ? 1 : evaluation >= betaOriginal ? -1 : 0;
        store(position, evaluation, bestStart, bestEnd, depth, bound);
        return new int[]{evaluation, bestStart, bestEnd};
    }
}
class Reference {
    static final int length = 8_388_608; //2 ^ 23
    static int countNonzeros(int[] inputArray) {
        int count = 0;
        for (int element : inputArray) {
            if (element != 0) count++;
        }
        return count;
    }
    private static Random generator = new Random(-0x382E4DFBA50BC32FL);
    private static final long empty = generator.nextLong();
    private static final long[][] zobrist = new long[64][13]; //13th slot is for if the square is available for en passant
    private static final long whiteToMove = generator.nextLong();
    private static final long blackToMove = generator.nextLong();
    private static final long zobristWCS = generator.nextLong(); //white castle short
    private static final long zobristWCL = generator.nextLong(); //white castle long
    private static final long zobristBCS = generator.nextLong(); //black castle short
    private static final long zobristBCL = generator.nextLong(); //black castle long
    static long getZobrist(int[] position) {
        long out = empty;
        for (int i = 0; i < 64; i++) {
            if (position[i] != 0) {
                out ^= zobrist[i][position[i]-1];
            }
        }
        out ^= ((position[64] == 1) ? whiteToMove : blackToMove);
        if (position[65] == 1) out ^= zobristWCS;
        if (position[66] == 1) out ^= zobristWCL;
        if (position[67] == 1) out ^= zobristBCS;
        if (position[68] == 1) out ^= zobristBCL;
        if (position[69] != 0) out ^= zobrist[position[69]-1][12];
        return out;
    }
    static long passMoveZobrist(int[] posBefore, long zobristBefore) {
        zobristBefore ^= whiteToMove;
        zobristBefore ^= blackToMove;
        if (posBefore[69] != 0) zobristBefore ^= zobrist[posBefore[69]-1][12];
        return zobristBefore;
    }
    static long restoreFromPassZobrist(long zobristModified, int originalEP) {
        zobristModified ^= whiteToMove;
        zobristModified ^= blackToMove;
        if (originalEP != 0) zobristModified ^= zobrist[originalEP-1][12];
        return zobristModified;
    }
    static long updateZobrist(int[] posBefore, long zobristBefore, int start, int end) {
        int piece = posBefore[start-1];
        int captured = posBefore[end-1];
        zobristBefore ^= zobrist[start-1][piece-1];
        zobristBefore ^= zobrist[end-1][piece-1];
        if (posBefore[69] != 0) zobristBefore ^= zobrist[posBefore[69]-1][12];
        if (end == posBefore[69]) {
            if (piece == 11) {
                zobristBefore ^= zobrist[(end - 8) - 1][11];
            } else if (piece == 12) {
                zobristBefore ^= zobrist[(end + 8) - 1][10];
            } else if (captured != 0) {
                zobristBefore ^= zobrist[end-1][captured-1];
            }
        } else if (captured != 0) zobristBefore ^= zobrist[end-1][captured-1];
        if (captured == 5) {
            if (end == 8 && posBefore[65] != 0) zobristBefore ^= zobristWCS;
            if (end == 1 && posBefore[66] != 0) zobristBefore ^= zobristWCL;
        }
        if (captured == 6) {
            if (end == 64 && posBefore[67] != 0) zobristBefore ^= zobristBCS;
            if (end == 57 && posBefore[68] != 0) zobristBefore ^= zobristBCL;
        }
        switch (piece) {
            case 1:
                if (start == 5) {
                    if (posBefore[65] != 0) zobristBefore ^= zobristWCS;
                    if (posBefore[66] != 0) zobristBefore ^= zobristWCL;
                    if (end == 7) {
                        //move rook
                        zobristBefore ^= zobrist[7][4];
                        zobristBefore ^= zobrist[5][4];
                    }
                    if (end == 3) {
                        //move rook
                        zobristBefore ^= zobrist[0][4];
                        zobristBefore ^= zobrist[3][4];
                    }
                }
                break;
            case 2:
                if (start == 61) {
                    if (posBefore[67] != 0) zobristBefore ^= zobristBCS;
                    if (posBefore[68] != 0) zobristBefore ^= zobristBCL;
                    if (end == 63) {
                        //move rook
                        zobristBefore ^= zobrist[63][5];
                        zobristBefore ^= zobrist[61][5];
                    }
                    if (end == 59) {
                        //move rook
                        zobristBefore ^= zobrist[56][5];
                        zobristBefore ^= zobrist[59][5];
                    }
                }
                break;
            case 5:
                if (start == 8 && posBefore[65] != 0) zobristBefore ^= zobristWCS;
                if (start == 1 && posBefore[66] != 0) zobristBefore ^= zobristWCL;
                break;
            case 6:
                if (start == 64 && posBefore[67] != 0) zobristBefore ^= zobristBCS;
                if (start == 57 && posBefore[68] != 0) zobristBefore ^= zobristBCL;
                break;
            case 11:
                if (end - start == 16) {
                    boolean pawnToLeft = end % 8 != 1 /*not on the a-file*/ && posBefore[(end - 1) - 1] == 12;
                    boolean pawnToRight = end % 8 != 0 /*not on the h-file*/ && posBefore[(end + 1) - 1] == 12;
                    if (pawnToLeft || pawnToRight) zobristBefore ^= zobrist[(end - 8) - 1][12];
                }
                if (end > 56) {
                    //promotion
                    zobristBefore ^= zobrist[end-1][10];
                    zobristBefore ^= zobrist[end-1][2];
                }
                break;
            case 12:
                if (end - start == -16) {
                    boolean pawnToLeft = end % 8 != 1 /*not on the a-file*/ && posBefore[(end - 1) - 1] == 11;
                    boolean pawnToRight = end % 8 != 0 /*not on the h-file*/ && posBefore[(end + 1) - 1] == 11;
                    if (pawnToLeft || pawnToRight) zobristBefore ^= zobrist[(end + 8) - 1][12];
                }
                if (end <= 8) {
                    //promotion
                    zobristBefore ^= zobrist[end-1][11];
                    zobristBefore ^= zobrist[end-1][3];
                }
                break;
            default:
                break;
        }
        zobristBefore ^= whiteToMove; //do these in any order
        zobristBefore ^= blackToMove;
        return zobristBefore;
    }
    //piece-to-square activity for knights
    static final byte[] knightBonus = {
        -12, -10, -8,  -8, -8, -8, -10, -12,
        -10, -10, -8,  -5, -5, -8, -10, -10,
        -5,   0,   5,   5,  5,  5,  0,  -5,
        -2,   0,   5,   5,  5,  5,  0,  -2,
        0,    2,   8,  10, 10, 10,  2,   0,
        -5,   0,   5,   8,  8,  5,  0,  -5,
        -8,  -8,  -5,  -5, -5, -5, -8,  -8,
        -12, -5,  -5,  -5, -5, -5, -5,  -12
    };
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
    private static double distance(int square1, int square2) {
        int x1 = square1 % 8;
        int y1 = (square1 - x1) / 8;
        int x2 = square2 % 8;
        int y2 = (square2 - x2) / 8;
        return Math.hypot(x2 - x1, y2 - y1);
    }

    private static long[] bitSplit(long bits) {
        long[] output = new long[Long.bitCount(bits)];
        int i = 0;
        while (bits != 0) {
            output[i] = Long.lowestOneBit(bits);
            bits ^= output[i];
            i++;
        }
        return output;
    }
    private static Set<Long> combineAll(long bits1, long bits2, long bits3, long bits4) {
        Set<Long> output = new HashSet<>();
        Set<Long> one = pickAll(bits1);
        Set<Long> two = pickAll(bits2);
        Set<Long> three = pickAll(bits3);
        Set<Long> four = pickAll(bits4);
        for (Long item1 : one) {
            for (Long item2 : two) {
                for (Long item3 : three) {
                    for (Long item4 : four) {
                        output.add(item1.longValue() | item2.longValue() | item3.longValue() | item4.longValue());
                    }
                }
            }
        }
        output.remove(0L);
        return output;
    }
    private static Set<Long> pickAll(long allBits) {
        long[] bits = bitSplit(allBits);
        Set<Long> everything = new HashSet<>();
        Set<Long> additions = new HashSet<>();
        everything.add(0L);
        for (long bit : bits) {
            for (Long item : everything) additions.add(item | bit);
            everything.addAll(additions);
            additions.clear();
        }
        return everything;
    }
    //bitboards
    private static final int[] bishopDx = {1, 1, -1, -1};
    private static final int[] bishopDy = {-1, 1, 1, -1};
    private static final int[] rookDx = {1, 0, -1, 0};
    private static final int[] rookDy = {0, 1, 0, -1};
    private static final long[] whitePawnVision = new long[64], blackPawnVision = new long[64], knightVision = new long[64], kingVision = new long[64];
    private static final long[] bishopVision = {
        0b1000000001000000001000000001000000001000000001000000001000000000L,
        0b0000000010000000010000000010000000010000000010000000010100000000L,
        0b0000000000000000100000000100000000100000000100010000101000000000L,
        0b0000000000000000000000001000000001000001001000100001010000000000L,
        0b0000000000000000000000000000000110000010010001000010100000000000L,
        0b0000000000000000000000010000001000000100100010000101000000000000L,
        0b0000000000000001000000100000010000001000000100001010000000000000L,
        0b0000000100000010000001000000100000010000001000000100000000000000L,
        0b0100000000100000000100000000100000000100000000100000000000000010L,
        0b1000000001000000001000000001000000001000000001010000000000000101L,
        0b0000000010000000010000000010000000010001000010100000000000001010L,
        0b0000000000000000100000000100000100100010000101000000000000010100L,
        0b0000000000000000000000011000001001000100001010000000000000101000L,
        0b0000000000000001000000100000010010001000010100000000000001010000L,
        0b0000000100000010000001000000100000010000101000000000000010100000L,
        0b0000001000000100000010000001000000100000010000000000000001000000L,
        0b0010000000010000000010000000010000000010000000000000001000000100L,
        0b0100000000100000000100000000100000000101000000000000010100001000L,
        0b1000000001000000001000000001000100001010000000000000101000010001L,
        0b0000000010000000010000010010001000010100000000000001010000100010L,
        0b0000000000000001100000100100010000101000000000000010100001000100L,
        0b0000000100000010000001001000100001010000000000000101000010001000L,
        0b0000001000000100000010000001000010100000000000001010000000010000L,
        0b0000010000001000000100000010000001000000000000000100000000100000L,
        0b0001000000001000000001000000001000000000000000100000010000001000L,
        0b0010000000010000000010000000010100000000000001010000100000010000L,
        0b0100000000100000000100010000101000000000000010100001000100100000L,
        0b1000000001000001001000100001010000000000000101000010001001000001L,
        0b0000000110000010010001000010100000000000001010000100010010000010L,
        0b0000001000000100100010000101000000000000010100001000100000000100L,
        0b0000010000001000000100001010000000000000101000000001000000001000L,
        0b0000100000010000001000000100000000000000010000000010000000010000L,
        0b0000100000000100000000100000000000000010000001000000100000010000L,
        0b0001000000001000000001010000000000000101000010000001000000100000L,
        0b0010000000010001000010100000000000001010000100010010000001000000L,
        0b0100000100100010000101000000000000010100001000100100000110000000L,
        0b1000001001000100001010000000000000101000010001001000001000000001L,
        0b0000010010001000010100000000000001010000100010000000010000000010L,
        0b0000100000010000101000000000000010100000000100000000100000000100L,
        0b0001000000100000010000000000000001000000001000000001000000001000L,
        0b0000010000000010000000000000001000000100000010000001000000100000L,
        0b0000100000000101000000000000010100001000000100000010000001000000L,
        0b0001000100001010000000000000101000010001001000000100000010000000L,
        0b0010001000010100000000000001010000100010010000011000000000000000L,
        0b0100010000101000000000000010100001000100100000100000000100000000L,
        0b1000100001010000000000000101000010001000000001000000001000000001L,
        0b0001000010100000000000001010000000010000000010000000010000000010L,
        0b0010000001000000000000000100000000100000000100000000100000000100L,
        0b0000001000000000000000100000010000001000000100000010000001000000L,
        0b0000010100000000000001010000100000010000001000000100000010000000L,
        0b0000101000000000000010100001000100100000010000001000000000000000L,
        0b0001010000000000000101000010001001000001100000000000000000000000L,
        0b0010100000000000001010000100010010000010000000010000000000000000L,
        0b0101000000000000010100001000100000000100000000100000000100000000L,
        0b1010000000000000101000000001000000001000000001000000001000000001L,
        0b0100000000000000010000000010000000010000000010000000010000000010L,
        0b0000000000000010000001000000100000010000001000000100000010000000L,
        0b0000000000000101000010000001000000100000010000001000000000000000L,
        0b0000000000001010000100010010000001000000100000000000000000000000L,
        0b0000000000010100001000100100000110000000000000000000000000000000L,
        0b0000000000101000010001001000001000000001000000000000000000000000L,
        0b0000000001010000100010000000010000000010000000010000000000000000L,
        0b0000000010100000000100000000100000000100000000100000000100000000L,
        0b0000000001000000001000000001000000001000000001000000001000000001L
    };
    private static final long[] rookVision = {
        0b0000000100000001000000010000000100000001000000010000000111111110L,
        0b0000001000000010000000100000001000000010000000100000001011111101L,
        0b0000010000000100000001000000010000000100000001000000010011111011L,
        0b0000100000001000000010000000100000001000000010000000100011110111L,
        0b0001000000010000000100000001000000010000000100000001000011101111L,
        0b0010000000100000001000000010000000100000001000000010000011011111L,
        0b0100000001000000010000000100000001000000010000000100000010111111L,
        0b1000000010000000100000001000000010000000100000001000000001111111L,
        0b0000000100000001000000010000000100000001000000011111111000000001L,
        0b0000001000000010000000100000001000000010000000101111110100000010L,
        0b0000010000000100000001000000010000000100000001001111101100000100L,
        0b0000100000001000000010000000100000001000000010001111011100001000L,
        0b0001000000010000000100000001000000010000000100001110111100010000L,
        0b0010000000100000001000000010000000100000001000001101111100100000L,
        0b0100000001000000010000000100000001000000010000001011111101000000L,
        0b1000000010000000100000001000000010000000100000000111111110000000L,
        0b0000000100000001000000010000000100000001111111100000000100000001L,
        0b0000001000000010000000100000001000000010111111010000001000000010L,
        0b0000010000000100000001000000010000000100111110110000010000000100L,
        0b0000100000001000000010000000100000001000111101110000100000001000L,
        0b0001000000010000000100000001000000010000111011110001000000010000L,
        0b0010000000100000001000000010000000100000110111110010000000100000L,
        0b0100000001000000010000000100000001000000101111110100000001000000L,
        0b1000000010000000100000001000000010000000011111111000000010000000L,
        0b0000000100000001000000010000000111111110000000010000000100000001L,
        0b0000001000000010000000100000001011111101000000100000001000000010L,
        0b0000010000000100000001000000010011111011000001000000010000000100L,
        0b0000100000001000000010000000100011110111000010000000100000001000L,
        0b0001000000010000000100000001000011101111000100000001000000010000L,
        0b0010000000100000001000000010000011011111001000000010000000100000L,
        0b0100000001000000010000000100000010111111010000000100000001000000L,
        0b1000000010000000100000001000000001111111100000001000000010000000L,
        0b0000000100000001000000011111111000000001000000010000000100000001L,
        0b0000001000000010000000101111110100000010000000100000001000000010L,
        0b0000010000000100000001001111101100000100000001000000010000000100L,
        0b0000100000001000000010001111011100001000000010000000100000001000L,
        0b0001000000010000000100001110111100010000000100000001000000010000L,
        0b0010000000100000001000001101111100100000001000000010000000100000L,
        0b0100000001000000010000001011111101000000010000000100000001000000L,
        0b1000000010000000100000000111111110000000100000001000000010000000L,
        0b0000000100000001111111100000000100000001000000010000000100000001L,
        0b0000001000000010111111010000001000000010000000100000001000000010L,
        0b0000010000000100111110110000010000000100000001000000010000000100L,
        0b0000100000001000111101110000100000001000000010000000100000001000L,
        0b0001000000010000111011110001000000010000000100000001000000010000L,
        0b0010000000100000110111110010000000100000001000000010000000100000L,
        0b0100000001000000101111110100000001000000010000000100000001000000L,
        0b1000000010000000011111111000000010000000100000001000000010000000L,
        0b0000000111111110000000010000000100000001000000010000000100000001L,
        0b0000001011111101000000100000001000000010000000100000001000000010L,
        0b0000010011111011000001000000010000000100000001000000010000000100L,
        0b0000100011110111000010000000100000001000000010000000100000001000L,
        0b0001000011101111000100000001000000010000000100000001000000010000L,
        0b0010000011011111001000000010000000100000001000000010000000100000L,
        0b0100000010111111010000000100000001000000010000000100000001000000L,
        0b1000000001111111100000001000000010000000100000001000000010000000L,
        0b1111111000000001000000010000000100000001000000010000000100000001L,
        0b1111110100000010000000100000001000000010000000100000001000000010L,
        0b1111101100000100000001000000010000000100000001000000010000000100L,
        0b1111011100001000000010000000100000001000000010000000100000001000L,
        0b1110111100010000000100000001000000010000000100000001000000010000L,
        0b1101111100100000001000000010000000100000001000000010000000100000L,
        0b1011111101000000010000000100000001000000010000000100000001000000L,
        0b0111111110000000100000001000000010000000100000001000000010000000L
    };
    //static Map<Integer, Map<Long, Long>> bishopBitboards = new HashMap<>();
    //static Map<Integer, Map<Long, Long>> rookBitboards = new HashMap<>();
    private static Map<Long, Long>[] bishopBitboards = new Map[64];
    private static Map<Long, Long>[] rookBitboards = new Map[64];
    static long serialize(int[] board) {
        long output = 0L;
        for (int square = 0; square < 64; square++) {
            if (board[square] != 0) output |= (1L << square);
        }
        return output;
    }
    static int[] unserialize(long bitboard) {
        int[] output = new int[Long.bitCount(bitboard)];
        int i = 0;
        while (bitboard != 0) {
            output[i] = Long.numberOfTrailingZeros(bitboard);
            i++;
            bitboard &= (bitboard-1);
        }
        return output;
    }
    private static void addConditional(long[] where, int square, int addX, int addY) {
        //tried to add square to where, if successful, return true, if unsuccessful, return false
        int squareX = square % 8;
        int squareY = (square - squareX) / 8;
        squareX += addX;
        squareY += addY;
        if (squareX >= 0 && squareX < 8 && squareY >= 0 && squareY < 8) {
            where[square] |= (1L << (squareY * 8 + squareX));
        }
    }
    private static int distanceToEdge(int squareX, int squareY, int dx, int dy) {
        int distanceX = 0, distanceY = 0;
        switch (dx) {
            case 1:
                distanceX = 7 - squareX;
                break;
            case -1:
                distanceX = squareX;
                break;
            default:
                throw new IllegalArgumentException("from Reference static method distanceToEdge");
        }
        switch (dy) {
            case 1:
                distanceY = 7 - squareY;
                break;
            case -1:
                distanceY = squareY;
                break;
            default:
                throw new IllegalArgumentException("from Reference static method distanceToEdge");
        }
        return Math.min(distanceX, distanceY);
    }
    private static long fill(int square, long pieces, int[] dx, int[] dy) {
        long output = 1L << square;
        for (int i = 0; i < dx.length; i++) {
            int currentX = square % 8 + dx[i];
            int currentY = square / 8 + dy[i];
            while (currentX >= 0 && currentX < 8 && currentY >= 0 && currentY < 8) {
                long addition = 1L << (currentX + 8 * currentY);
                output |= addition;
                if ((addition & pieces) != 0) break;
                currentX += dx[i];
                currentY += dy[i];
            }
        }
        return output;
    }
    private static long[] allPossible(int square, int[] dx, int[] dy) {
        Set<Long> everything = combineAll(
            fill(square, 0L, new int[]{dx[0]}, new int[]{dy[0]}),
            fill(square, 0L, new int[]{dx[1]}, new int[]{dy[1]}),
            fill(square, 0L, new int[]{dx[2]}, new int[]{dy[2]}),
            fill(square, 0L, new int[]{dx[3]}, new int[]{dy[3]})
        );
        long[] out = everything.stream().mapToLong(Long::longValue).toArray();
        return out;
    }
    static void initialize() {
        for (int i = 0; i < 64; i++) {
            //pawns
            addConditional(whitePawnVision, i, 1, 1);
            addConditional(whitePawnVision, i, -1, 1);
            addConditional(blackPawnVision, i, 1, -1);
            addConditional(blackPawnVision, i, -1, -1);
            //knights
            addConditional(knightVision, i, 1, 2);
            addConditional(knightVision, i, -1, 2);
            addConditional(knightVision, i, 1, -2);
            addConditional(knightVision, i, -1, -2);
            addConditional(knightVision, i, 2, 1);
            addConditional(knightVision, i, -2, 1);
            addConditional(knightVision, i, 2, -1);
            addConditional(knightVision, i, -2, -1);
            //kings
            addConditional(kingVision, i, 1, 0);
            addConditional(kingVision, i, 0, 1);
            addConditional(kingVision, i, 1, 1);
            addConditional(kingVision, i, -1, 0);
            addConditional(kingVision, i, 0, -1);
            addConditional(kingVision, i, 1, -1);
            addConditional(kingVision, i, -1, 1);
            addConditional(kingVision, i, -1, -1);
            //rooks
            Map<Long, Long> rookMoves = new HashMap<>();
            for (long p : allPossible(i, rookDx, rookDy)) rookMoves.put(p, fill(i, p, rookDx, rookDy));
            rookBitboards[i] = rookMoves; //rookBitboards.put(i, rookMoves);
            //bishops
            Map<Long, Long> bishopMoves = new HashMap<>();
            for (long p : allPossible(i, bishopDx, bishopDy)) bishopMoves.put(p, fill(i, p, bishopDx, bishopDy));
            bishopBitboards[i] = bishopMoves; //bishopBitboards.put(i, bishopMoves);
            //zobrist
            for (int j = 0; j < 13; j++) {
                zobrist[i][j] = generator.nextLong();
            }
        }
    }
    //attack map magic bitboards
    static long getBitboard(int square, int[] board, int piece) {
        switch (piece) {
            case 1: case 2: return kingVision[square];
            case 3: return getBitboard(square, board, 5) | getBitboard(square, board, 7);
            case 4: return getBitboard(square, board, 6) | getBitboard(square, board, 8);
            case 5: case 6: return rookBitboards[square].get((serialize(board) & rookVision[square]) | (1L << square));
            //return rookBitboards.get(square).get((serialize(board) & rookVision[square]) | (1L << square));
            case 7: case 8: return bishopBitboards[square].get((serialize(board) & bishopVision[square]) | (1L << square));
            //return bishopBitboards.get(square).get((serialize(board) & bishopVision[square]) | (1L << square));
            case 9: case 10: return knightVision[square];
            case 11: return whitePawnVision[square];
            case 12: return blackPawnVision[square];
            default: return 0L; //placeholder
        }
    }
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
class OutOfSpaceException extends RuntimeException {
    OutOfSpaceException() {
        super("Oops, memory threshold exceeded...");
    }
}
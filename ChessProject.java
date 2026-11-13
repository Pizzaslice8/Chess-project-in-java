import java.util.Scanner;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;
import java.lang.Math;

public class ChessProject {
    public static void main(String[] args) {
        /*
        PASTE INTO TERMINAL:
        & "C:\Users\cbh5981\Documents\jdk-27\bin\java.exe" "C:\Users\cbh5981\Documents\Chess-project-in-java\ChessProject.java"
        TEST POSITION:
        r1bq1rk1/1pp2pp1/2np1n1p/p1b1p3/2B1P3/2PP1N1P/PP3PP1/RNBQR1K1 w - - 0 9
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
        //String pgn = "";
        if (fen.equals("TEST POSITION")) {
            fen = "r1bq1rk1/1pp2pp1/2np1n1p/p1b1p3/2B1P3/2PP1N1P/PP3PP1/RNBQR1K1 w - - 0 9";
        } else if (fen.equals("TEST POSITION REVERSED")) {
            fen = "r1bq1rk1/1pp2pp1/2np1n1p/p1b1p3/2B1P3/2PP1N1P/PP3PP1/RNBQR1K1 b - - 0 9";
        }/* else if (fen.equals("PGN")) {
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
            System.out.print("PGN: ");
            pgn = scanner.nextLine();
        }*/
        else if (Reference.openings.containsKey(fen)) {
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
        System.out.print("Depth: ");
        int depth = scanner.nextInt();
        System.out.print("Time limit: ");
        int timeLimit = scanner.nextInt();
        Position pos = new Position(setup, timeLimit * 1000, depth);
        /*if (!pgn.isEmpty()) {
            String[] parts = pgn.split("\\s+");
            boolean annotation = false;
            for (String part : parts) {
                if (part.equals("*") || (part.contains("-") && !part.matches("(^O)(O$)") && !part.matches("(^0)(0$)")) || part.startsWith("$")) continue;
                if (part.startsWith(";") || part.startsWith("{") || part.startsWith("(")) annotation = true;
                if (part.matches("^[0-9]+\\.$") || part.endsWith("}") || part.endsWith(")")) annotation = false;
                else if (!annotation) {
                    if (part.endsWith("!") || part.endsWith("?")) part = part.substring(0, part.length()-1);
                    if (part.endsWith("?") || part.endsWith("!")) part = part.substring(0, part.length()-1); //in case the original had "!?" or "?!"
                    if (part.endsWith("+") || part.endsWith("#")) part = part.substring(0, part.length()-1);
                    if (part.matches("^[0-9]+\\.+")) {
                        do {
                            part.substring(0, part.length()-1);
                        }
                        while (part.matches("^\\.+"));
                    }
                    if (part.equals("O-O") || part.equals("O_O") || part.equals("0-0") || part.equals("0_0")) {
                        if (pos.position[0] == 1) pos.makeMove(5, 7);
                        else pos.makeMove(61, 63);
                    } else if (part.equals("O-O-O") || part.equals("O_O_O") || part.equals("0-0-0") || part.equals("0_0_0")) {
                        if (pos.position[0] == 1) pos.makeMove(5, 3);
                        else pos.makeMove(61, 59);
                    } else {
                        if (part.endsWith("=N") || part.endsWith("=B") || part.endsWith("=R")) throw new RuntimeException("Underpromotion not supported :(");
                        if (part.endsWith("=Q")) part = part.substring(0, part.length()-2);
                        int length = part.length();
                        int piece, destination = parseSquare(part.substring(length-2));
                        if (part.equals(part.toLowerCase())) {
                            piece = 12 - pos.position[0];
                            if (length > 2) {
                                //only possible configuration is [file letter] x [square] (4 characters)
                                //example "exd4"
                                int startFile = (int) (part.charAt(0));
                                int endFile = (int) (part.charAt(2));
                                if (piece == 11) {
                                    if (endFile > startFile) pos.makeMove(destination-9, destination); //capture towards h
                                    else if (startFile > endFile) pos.makeMove(destination-7, destination); //capture towards a
                                }
                                if (piece == 12) {
                                    if (endFile > startFile) pos.makeMove(destination+7, destination); //capture towards h
                                    else if (startFile > endFile) pos.makeMove(destination+9, destination);
                                }
                            } else {
                                //example "e4"
                                if (piece == 11) {
                                    if (getY(destination) == 4 && !pos.piecePresent(11, destination-8)) pos.makeMove(destination-16, destination);
                                    else pos.makeMove(destination-8, destination);
                                }
                                if (piece == 12) {
                                    if (getY(destination) == 5 && !pos.piecePresent(12, destination+8)) pos.makeMove(destination+16, destination);
                                    else pos.makeMove(destination+8, destination); 
                                }
                            }
                        }
                        else {
                            //have to dodge the 'x' in captures
                            piece = getId(part.charAt(0));
                            if ((part.contains("x") && length > 4) || (!part.contains("x") && length > 3)) {
                                //example "Nbd2", "N1e2", "Qb8d6" in case one promotes 3 queens
                                if (Character.isDigit(part.charAt(1))) {
                                    //rank disambiguation
                                    int rankAsInt = part.charAt(1) - 49;
                                    int start = Long.numberOfTrailingZeros(pos.pieceBoard[piece-1] & pos.rankref[rankAsInt]) + 1;
                                    pos.makeMove(start, destination);
                                } else {
                                    if (Character.isDigit(part.charAt(2))) pos.makeMove(parseSquare(part.substring(1, 3)), destination);
                                    else {
                                        //file disambiguation
                                        int fileAsInt = part.charAt(1) - 97;
                                        int start = Long.numberOfTrailingZeros(pos.pieceBoard[piece-1] & pos.fileref[fileAsInt]) + 1;
                                        pos.makeMove(start, destination);
                                    }
                                }
                            } else {
                                //example "Nf3", "Nxe5"
                                long visionFrom = Reference.getBitboard(destination-1, pos.occupiedSquares(), piece);
                                long pieceBits = pos.pieceBoard[piece-1];
                                pos.makeMove(Long.numberOfTrailingZeros(pieceBits & visionFrom) + 1, destination);
                            }
                        }
                    }
                }
            }
        }*/
        pos.display();
        pos.displayStats();
        switch (pos.gameStatus()) {
            case 1:
                throw new GameAlreadyOverException("Checkmate white wins");
            case 2:
                throw new GameAlreadyOverException("Checkmate black wins");
            case 3:
                throw new GameAlreadyOverException("Draw");
            default:
                break;
        }
        int[] lastCompleted = null;
        if (depth >= 0) {
            for (int i = 0; i <= depth; i++){
                System.out.printf("Running at depth: %d%n", i);
                System.out.println("Calculating...");
                long start = System.nanoTime();
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
                        System.out.printf("Evaluation: %f%n", evaluation);
                        System.out.println("Best move: " + parseNumbers(lastCompleted[1], lastCompleted[2]));
                    } else {
                        byte[][] legalMoves = pos.sortMoves(pos.generateMoves(true));
                        raw = pos.currentCount(false);
                        rawEval = raw;
                        evaluation = rawEval / 10.0;
                        System.out.printf("Evaluation: %f%n", evaluation);
                        System.out.println("Best move: " + parseNumbers(legalMoves[0][0], legalMoves[1][0]));
                    }
                    return;
                }
                double timeTaken = (System.nanoTime() - start) / 1000000000.0;
                int raw = lastCompleted[0];
                double rawEval = raw;
                double evaluation = rawEval / 10.0; // convert decipawns (1/10 pawn) to pawns
                if (evaluation == 214748364.7 || evaluation == -214748364.8) {
                    System.out.print("\u001b[33m");
                    System.out.printf("Evaluation: %f", evaluation);
                    System.out.print("\u001b[0m");
                    System.out.print("\n");
                }
                else System.out.printf("Evaluation: %f%n", evaluation);
                System.out.println("Best move: " + parseNumbers(lastCompleted[1], lastCompleted[2]));
                System.out.printf("Nodes per second: %f%n", (nodes / timeTaken));
                System.out.printf("in %f seconds%n%n", timeTaken);
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
        return (square - 1) % 8 + 1;
    }
    static int getY(int square) {
        return (square - 1) / 8 + 1;
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
    static int getPiece(long[] boards, int square) {
        if (square == 0) return 0;
        for (int i = 0; i < 12; i++) {
            if ((boards[i] & (1L << (square - 1))) != 0) return (i+1);
        }
        return 0;
    }
}
class Position {
    int[] position = new int[7];
    long currentZobrist;
    long[] pieceBoard = new long[12];
    int[] mailbox = new int[64];
    int limit, maxDepth;
    long[][] undoStackPieces = new long[5000][12];
    int[][] undoStackData = new int[5000][7];
    int slot = 0;
    int undoDeep = 0;
    int[] killerMoves = new int[50];
    int[][] history = new int[5000][10];
    int historyStored = 0;
    final long[] rankref = {
        0xffL,
        0x00ffL,
        0x0000ffL,
        0x000000ffL,
        0x00000000ffL,
        0x0000000000ffL,
        0x000000000000ffL,
        0x00000000000000ffL
    };
    final long[] fileref = {
        0x1010101010100L,
        0x2020202020200L,
        0x4040404040400L,
        0x8080808080800L,
        0x10101010101000L,
        0x20202020202000L,
        0x40404040404000L,
        0x80808080808000L
    };
    Position(int[] setup, int timeLimit, int depth) {
        System.arraycopy(setup, 0, this.mailbox, 0, 64);
        System.arraycopy(setup, 64, this.position, 0, 7);
        this.limit = timeLimit;
        this.maxDepth = depth;
        for (int i = 0; i < 64; i++) {
            if (setup[i] != 0) {
                this.pieceBoard[setup[i] - 1] |= (1L << i);
            }
        }
        for (int i = 0; i < 64; i++) {
            update(i+1);
        }
        this.currentZobrist = Reference.getZobrist(pieceBoard, position);
    }
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
        for (int i = 0; i < 50; i++) {
            if (killerMoves[i] == 0) {
                killerMoves[i] = compressed;
                break;
            }
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
    void store(int evaluation, int bestStart, int bestEnd, int depth, int bound) {
        try {
            zobristStorage[slot] = currentZobrist;
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
        for (int i = slot - 1; i >= 0; i--) {
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
        int whiteBishops = Long.bitCount(pieceBoard[6]);
        int blackBishops = Long.bitCount(pieceBoard[7]);
        int whiteBonus = (whiteBishops >= 2 && tactical) ? 2 : (whiteBishops >= 2 && !tactical) ? 1 : 0;
        int blackBonus = (blackBishops >= 2 && tactical) ? -2 : (blackBishops >= 2 && !tactical) ? -1 : 0;
        return whiteBonus + blackBonus;
    }
    int currentCount() {
        return currentCount(true);
    }
    int currentCount(boolean checkStatus) {
        if (checkStatus) {
            switch (gameStatus()) {
                case 1: return Integer.MAX_VALUE;
                case 2: return Integer.MIN_VALUE;
                case 3: return 0;
            }
        }
        int outputTactical = 0;
        int outputNontactical = 0;
        int materialDifferential = 0;
        double activePiecesModifier = Math.min(activePieces(), 4) / 4.0;
        for (int i = 0; i < 64; i++) {
            outputTactical += pieceActivity[i] + centralControl[i]  + kingSafety[i] + rooksSeventh[i] + (space[i] + outposts[i]) / 3;
            outputNontactical += centralControl[i] + space[i] + outposts[i] + (pieceActivity[i] + kingSafety[i] + rooksSeventh[i]) / 3;
            materialDifferential += material[i];
        }
        int tacticalOutput = outputTactical + ((position[0] == 1) ? 1 : -1) + bishopPairBonus(true);
        int nontacticalOutput = outputNontactical + ((position[0] == 1) ? 1 : -1) + bishopPairBonus(false);
        //material matters more in the endgame
        double materialModifier = 1 + percentEndgame();
        if ((pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]) == 0) {
            if ((pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) == 0) {
                //pawn endgame, any material advantage is practically decisive
                materialModifier = 5;
            }
            //minor piece endgame, any material advantage is very important
            materialModifier = 3;
        }
        return (int) Math.round(activePiecesModifier * tacticalOutput + (1 - activePiecesModifier) * nontacticalOutput + materialDifferential * materialModifier);
    }
    int activePieces() {
        //test if both sides have a lot of really active pieces
        //if true, then currentCount gives priority to piece activity, king safety, bishop pair, active rooks, if not, give priority to space, outposts
        int reallyActive = 0;
        long whitePieces = occupiedSquares(true);
        long blackPieces = occupiedSquares(false);
        while (whitePieces != 0) {
            if (pieceActivity[Long.numberOfTrailingZeros(whitePieces)] >= 8) reallyActive++;
            whitePieces &= (whitePieces-1);
        }
        while (blackPieces != 0) {
            if (pieceActivity[Long.numberOfTrailingZeros(blackPieces)] <= -8) reallyActive++;
            blackPieces &= (blackPieces-1);
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
        int piece = mailbox[square-1];
        if (piece == 0) return;
        int modifier = (ChessProject.color(piece)) ? 1 : -1;
        int sameColorPawn = (23 - modifier) / 2;
        long pieceVision = Reference.getBitboard(square-1, occupiedSquares(), piece);
        if ((pieceVision & (1L << 27)) != 0 && piece != 1 && piece != 2) centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 28)) != 0 && piece != 1 && piece != 2) centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 35)) != 0 && piece != 1 && piece != 2) centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 36)) != 0 && piece != 1 && piece != 2) centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        material[square-1] = (byte) ChessProject.materialWorth(piece);
        if (piece == 1 || piece == 2) {
            double endgame = percentEndgame();
            double kingDistance = Math.hypot(ChessProject.getX(square) - 4.5, ChessProject.getY(square) - 4.5);
            pieceActivity[square-1] = (byte) Math.round(modifier * 2 * (10 - kingDistance * 1.7) * endgame);
            int pawnShield = 0;
            if (piecePresent(sameColorPawn, ChessProject.translate(square, 0, modifier))) pawnShield += 10;
            if (piecePresent(sameColorPawn, ChessProject.translate(square, 1, modifier))) pawnShield += 5;
            if (piecePresent(sameColorPawn, ChessProject.translate(square, -1, modifier))) pawnShield += 5;
            kingSafety[square-1] = (byte) Math.round(modifier * pawnShield * (1 - endgame));
        }
        if (piece == 3 || piece == 4) {
            pieceActivity[square-1] = (byte) (modifier * Long.bitCount(pieceVision) / 2);
        } else if (piece == 5 || piece == 6 || piece == 7 || piece == 8) {
            pieceActivity[square-1] = (byte) (modifier * Long.bitCount(pieceVision));
        }  
        if (piece == 9 || piece == 10) {
            int equivalentY = 9 - ChessProject.getY(square);
            int equivalent = ChessProject.getX(square) + 8 * (equivalentY - 1);
            pieceActivity[square-1] = (byte) (modifier * Reference.knightBonus[(piece == 9) ? square-1 : equivalent-1]);
            if (ChessProject.getY(square) == ((piece == 9) ? 5 : 4) || ChessProject.getY(square) == ((piece == 9) ? 6 : 3)) {
                //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                if (ChessProject.getX(square) >= 3 && ChessProject.getX(square) <= 6) {
                    //center files, which are more valuable
                    if (piecePresent(sameColorPawn, ChessProject.translate(square, 1, -modifier)) || piecePresent(sameColorPawn, ChessProject.translate(square, -1, -modifier))) {
                        outposts[square-1] = (byte) (modifier * 8);
                    }
                } else {
                    // flank files
                    if (piecePresent(sameColorPawn, ChessProject.translate(square, 1, -modifier)) || piecePresent(sameColorPawn, ChessProject.translate(square, -1, -modifier))) {
                        outposts[square-1] = (byte) (modifier * 4);
                    }
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
                if (piecePresent(sameColorPawn, ChessProject.translate(square, 1, -modifier)) || piecePresent(sameColorPawn, ChessProject.translate(square, -1, -modifier))) {
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
            long[] data = passedPawns();
            double specialModifier = modifier;
            if (piece == 11) {
                if ((data[0] & (1L << (square - 1))) != 0) specialModifier *= (1 + 4 * percentEndgame()); //passer
                if ((data[2] & (1L << (square - 1))) != 0) specialModifier *= (1 + 2 * percentEndgame()); //potential passer
                if ((data[4] & (1L << (square - 1))) != 0) specialModifier *= (1 + percentEndgame()); //neighbors a potential passer
            }
            if (piece == 12) {
                if ((data[1] & (1L << (square - 1))) != 0) specialModifier *= (1 + 4 * percentEndgame());
                if ((data[3] & (1L << (square - 1))) != 0) specialModifier *= (1 + 2 * percentEndgame());
                if ((data[5] & (1L << (square - 1))) != 0) specialModifier *= (1 + percentEndgame());
            }
            space[square-1] = (byte) Math.round(specialModifier * addition);
        }
    }
    long occupiedSquares() {
        return occupiedSquares(true) | occupiedSquares(false);
    }
    long occupiedSquares(boolean color) {
        return (color ? (
            pieceBoard[0] | pieceBoard[2] | pieceBoard[4] | pieceBoard[6] | pieceBoard[8] | pieceBoard[10]
        ) : (
            pieceBoard[1] | pieceBoard[3] | pieceBoard[5] | pieceBoard[7] | pieceBoard[9] | pieceBoard[11]
        ));
    }
    boolean isEmpty(int square) {
        return (occupiedSquares() & (1L << (square-1))) == 0;
    }
    boolean piecePresent(int piece, int square) {
        if (square == 0) return false;
        return (pieceBoard[piece-1] & (1L << (square-1))) != 0;
    }
    int kingSquare() {
        if (pieceBoard[1-position[0]] == 0) {
        //HOPEFULLY this never happens
            String message = "Looking for " + ((position[0] == 1) ? "white" : "black") + " king but found none\nThis is the current position:\n";
            display();
            throw new RuntimeException(message);
        }
        return Long.numberOfTrailingZeros(pieceBoard[1-position[0]]);
    }
    int sizeOfKingBox(int checkmateMode) {
        if ((checkmateMode > 0) != (position[0] == 1)) return 0;
        int enemyKing = kingSquare();
        long heavyPieces = (checkmateMode > 0) ? (pieceBoard[2] | pieceBoard[4]) : (pieceBoard[3] | pieceBoard[5]);
        long controlledBits = 0L;
        long heavyCopy = heavyPieces;
        while (heavyCopy != 0) {
            controlledBits |= Reference.getBitboard(Long.numberOfTrailingZeros(heavyCopy), occupiedSquares(), 5);
            heavyCopy &= (heavyCopy-1);
        }
        int currentX, currentY, currentSquare, sizeX = 1, sizeY = 1;
        long currentBit;
        int kingX = ChessProject.getX(enemyKing+1)-1;
        int kingY = ChessProject.getY(enemyKing+1)-1;
        for (int i = 0; i < 4; i++) {
            currentX = kingX;
            currentY = kingY;
            currentSquare = currentY * 8 + currentX;
            currentBit = 1L << currentSquare;
            while (currentX >= 0 && currentX < 8 && currentY >= 0 && currentY < 8 && (currentBit & controlledBits) == 0) {
                if (currentSquare != enemyKing) {
                    if (i % 2 == 0) sizeX++;
                    else sizeY++;
                }
                currentX += Reference.rookDx[i];
                currentY += Reference.rookDy[i];
                currentSquare = currentY * 8 + currentX;
                currentBit = 1L << currentSquare;
            }
        }
        return sizeX * sizeY;
    }
    void display() {
        char[] output = new char[64];
        for (int i = 0; i < 12; i++) {
            long bitboardCopy = pieceBoard[i];
            while (bitboardCopy != 0) {
                output[Long.numberOfTrailingZeros(bitboardCopy)] = ChessProject.getChar(i+1);
                bitboardCopy &= (bitboardCopy-1);
            }
        }
        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                System.out.print((output[i * 8 + j] == 0) ? " " : output[i * 8 + j]);
                System.out.print(" ");
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

        byte[][] getMoves = sortMoves(generateMoves(true));
        for (int i = 0; i < getMoves[0].length; i++) {
            System.out.print(ChessProject.parseNumbers(getMoves[0][i], getMoves[1][i]));
            if (i < getMoves[0].length-1) System.out.print(", ");
        }
        System.out.println("");
        System.out.println("Checkmate algorithm activated? " + ((simpleCheckmate() == 0) ? "no" : "yes"));
        System.out.print("Material count: ");
        System.out.println(materialCount);
        System.out.print("Piece activity differential: ");
        System.out.println(pieceActivityCount);
        System.out.print("Central control differential: ");
        System.out.println(centralControlCount);
        System.out.print("King safety differential: ");
        System.out.println(kingSafetyCount);
        System.out.print("Space differential: ");
        System.out.println(spaceCount);
        System.out.print("Advanced rook differential: ");
        System.out.println(rookCount);
        System.out.print("Outposts: ");
        System.out.println(outpostsCount);
        System.out.print("Number of really active pieces: ");
        System.out.println(activePieces());
        System.out.print("Endgame multiplier: ");
        System.out.println(percentEndgame());
        System.out.print("Static evaluation: ");
        System.out.println(currentCount(false));
    }
    // Hot paths!
    long legalMovesBasic(int square) {
        long output = 0L;
        int piece = mailbox[square-1];
        boolean color = ChessProject.color(piece);
        if (piece == 11) {
            //pawn
            int candidate = ChessProject.translate(square, 1, 1);
            int occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && ChessProject.color(occupied) != color && occupied != 2) {
                output |= (1L << (candidate-1));
            }
            candidate = ChessProject.translate(square, -1, 1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && ChessProject.color(occupied) != color && occupied != 2) {
                output |= (1L << (candidate-1));
            }
            candidate = ChessProject.translate(square, 0, 1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied == 0) {
                output |= (1L << (candidate-1));
                candidate = ChessProject.translate(square, 0, 2);
                occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
                if (candidate != 0 && occupied == 0 && square > 8 && square <= 16) {
                    output |= (1L << (candidate-1));
                }
            }
            if (position[5] != 0 && (occupiedSquares() & (1L << (position[5]-1))) == 0 &&
                    (position[5] == ChessProject.translate(square, 1, 1) || position[5] == ChessProject.translate(square, -1, 1))) {
                output |= (1L << (position[5]-1));
            }
        } else if (piece == 12) {
            //pawn
            int candidate = ChessProject.translate(square, 1, -1);
            int occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && ChessProject.color(occupied) != color && occupied != 1) {
                output |= (1L << (candidate-1));
            }
            candidate = ChessProject.translate(square, -1, -1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && ChessProject.color(occupied) != color && occupied != 1) {
                output |= (1L << (candidate-1));
            }
            candidate = ChessProject.translate(square, 0, -1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied == 0) {
                output |= (1L << (candidate-1));
                candidate = ChessProject.translate(square, 0, -2);
                occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
                if (occupied == 0 && square > 48 && square <= 56) {
                    output |= (1L << (candidate-1));
                }
            }
            if (position[5] != 0 && (occupiedSquares() & (1L << (position[5]-1))) == 0 &&
                    (position[5] == ChessProject.translate(square, 1, -1) || position[5] == ChessProject.translate(square, -1, -1))) {
                output |= (1L << (position[5]-1));
            }
        } else {
            long squares = Reference.getBitboard(square-1, occupiedSquares(), piece);
            while (squares != 0) {
                int p = mailbox[Long.numberOfTrailingZeros(squares)];
                if (p == 0 || (ChessProject.color(p) != color && p != 1 && p != 2)) {
                    output |= Long.lowestOneBit(squares);
                }
                squares &= (squares-1);
            }
        }
        return output;
    }
    void makeMove(int start, int end) {
        currentZobrist = Reference.updateZobrist(pieceBoard, position, currentZobrist, start, end);
        System.arraycopy(pieceBoard, 0, undoStackPieces[undoDeep], 0, 12);
        System.arraycopy(position, 0, undoStackData[undoDeep], 0, 7);
        undoDeep++;
        int piece = mailbox[start-1];
        int captured = mailbox[end-1];
        if (piece != 11 && piece != 12) {
            position[5] = 0;
        }
        position[6]++;
        if (captured != 0) {
            position[6] = 0;
            if (end == 1 && captured == 5) position[2] = 0;
            if (end == 8 && captured == 5) position[1] = 0;
            if (end == 57 && captured == 6) position[4] = 0;
            if (end == 64 && captured == 6) position[3] = 0;
            pieceBoard[captured-1] ^= (1L << (end - 1));
        }
        pieceBoard[piece-1] |= (1L << (end - 1));
        pieceBoard[piece-1] ^= (1L << (start - 1));
        mailbox[end-1] = piece;
        mailbox[start-1] = 0;
        switch (piece) {
            case 1:
                position[1] = 0;
                position[2] = 0;
                if (start == 5 && end == 3) {
                    //white castle long
                    pieceBoard[4] |= (1L << 3);
                    pieceBoard[4] ^= 1L;
                    mailbox[3] = 5;
                    mailbox[0] = 0;
                }
                if (start == 5 && end == 7) {
                    //white castle short
                    pieceBoard[4] |= (1L << 5);
                    pieceBoard[4] ^= (1L << 7);
                    mailbox[5] = 5;
                    mailbox[7] = 0;
                }
                break;
            case 2:
                position[3] = 0;
                position[4] = 0;
                if (start == 61 && end == 59) {
                    //black castle long
                    pieceBoard[5] |= (1L << 59);
                    pieceBoard[5] ^= (1L << 56);
                    mailbox[59] = 6;
                    mailbox[56] = 0;
                }
                if (start == 61 && end == 63) {
                    //black castle short
                    pieceBoard[5] |= (1L << 61);
                    pieceBoard[5] ^= (1L << 63);
                    mailbox[61] = 6;
                    mailbox[63] = 0;
                }
                break;
            case 5:
                if (start == 1) {
                    //a1
                    position[2] = 0;
                } else if (start == 8) {
                    //h1
                    position[1] = 0;
                }
                break;
            case 6:
                if (start == 57) {
                    //a8
                    position[4] = 0;
                } else if (start == 64) {
                    //h8
                    position[3] = 0;
                }
                break;
            case 11:
                //white pawn
                //worry about en passant and promotion, assume all promotions go to queens
                if (end == position[5]) {
                    //en passant!
                    int squareid = ChessProject.translate(end, 0, -1) - 1;
                    pieceBoard[11] ^= (1L << squareid);
                    mailbox[squareid] = 0;
                }
                position[5] = 0;
                if (start > 8 && start <= 16 && end > 24 && end <= 32) {
                    //pawn move 2 squares, check for en passant
                    if (piecePresent(12, ChessProject.translate(end, 1, 0)) || piecePresent(12, ChessProject.translate(end, -1, 0))) {
                        position[5] = ChessProject.translate(start, 0, 1);
                    }
                }
                if (start > 48 && start <= 56) {
                    //promotion
                    pieceBoard[10] ^= (1L << end-1);
                    pieceBoard[2] |= (1L << end-1);
                    mailbox[end-1] = 3;
                }
                position[6] = 0;
                break;
            case 12:
                //black pawn
                if (end == position[5]) {
                    //en passant!
                    int squareid = ChessProject.translate(end, 0, 1) - 1;
                    pieceBoard[10] ^= (1L << squareid);
                    mailbox[squareid] = 0;
                }
                position[5] = 0;
                if (start > 48 && start <= 56 && end > 32 && end <= 40) {
                    //pawn move 2 squares, check for en passant
                    if (piecePresent(11, ChessProject.translate(end, 1, 0)) || piecePresent(11, ChessProject.translate(end, -1, 0))) {
                        position[5] = ChessProject.translate(start, 0, -1);
                    }
                }
                if (start > 8 && start <= 16) {
                    //promotion
                    pieceBoard[11] ^= (1L << end-1);
                    pieceBoard[3] |= (1L << end-1);
                    mailbox[end-1] = 4;
                }
                position[6] = 0;
                break;
            default:
                break;
        }
        position[0] = 1 - position[0];
    }
    void restore() {
        undoDeep--;
        long[] bitboardCopy = undoStackPieces[undoDeep];
        System.arraycopy(bitboardCopy, 0, pieceBoard, 0, 12);
        System.arraycopy(undoStackData[undoDeep], 0, position, 0, 7);
        currentZobrist = Reference.getZobrist(pieceBoard, position);
        for (int i = 0; i < 64; i++) mailbox[i] = 0;
        for (int i = 0; i < 12; i++) {
            while (bitboardCopy[i] != 0) {
                mailbox[Long.numberOfTrailingZeros(bitboardCopy[i])] = i+1;
                bitboardCopy[i] &= (bitboardCopy[i]-1);
            }
        }
    }
    long legalMoves(int square) {
        long template = legalMovesBasic(square);
        long templateCopy = template;
        while (templateCopy != 0) {
            makeMove(square, Long.numberOfTrailingZeros(templateCopy) + 1);
            position[0] = 1 - position[0];
            if (inCheck()) {
                template ^= Long.lowestOneBit(templateCopy);
            }
            position[0] = 1 - position[0];
            restore();
            templateCopy &= (templateCopy-1);
        }
        if (!inCheck()) {
            if (square == 5) {
                if (position[1] == 1 && ((pieceBoard[4] & (1L << 7)) != 0) && isEmpty(6) && isEmpty(7)) {
                    boolean allowed = true;
                    pieceBoard[0] = (1L << 5);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[0] = (1L << 6);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[0] = (1L << 4);
                    if (allowed) {
                        template |= (1L << 6);
                    }    
                }
                if (position[2] == 1 && ((pieceBoard[4] & 1L) != 0) && isEmpty(4) && isEmpty(3) && isEmpty(2)) {   
                    boolean allowed = true; 
                    pieceBoard[0] = (1L << 3);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[0] = (1L << 2);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[0] = (1L << 4);
                    if (allowed) {
                        template |= (1L << 2);
                    }
                }
            }
            if (square == 61) {
                if (position[3] == 1 && ((pieceBoard[5] & (1L << 63)) != 0) && isEmpty(62) && isEmpty(63)) {
                    boolean allowed = true;
                    pieceBoard[1] = (1L << 61);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[1] = (1L << 62);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[1] = (1L << 60);
                    if (allowed) {
                        template |= (1L << 62);
                    }    
                }
                if (position[4] == 1 && ((pieceBoard[5] & (1L << 56)) != 0) && isEmpty(60) && isEmpty(59) && isEmpty(58)) {   
                    boolean allowed = true; 
                    pieceBoard[1] = (1L << 59);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[1] = (1L << 58);
                    if (inCheck()) {
                        allowed = false;
                    }
                    pieceBoard[1] = (1L << 60);
                    if (allowed) {
                        template |= (1L << 58);
                    }
                }
            }
        }
        return template;
    }
    boolean inCheck() {
        int king = kingSquare();
        long occupied = occupiedSquares();
        return (position[0] == 1) ? (
            //test black pieces
            (Reference.getBitboard(king, occupied, 2) & pieceBoard[1]) != 0 ||
            (Reference.getBitboard(king, occupied, 6) & (pieceBoard[5] | pieceBoard[3])) != 0 ||
            (Reference.getBitboard(king, occupied, 8) & (pieceBoard[7] | pieceBoard[3])) != 0 ||
            (Reference.getBitboard(king, occupied, 10) & pieceBoard[9]) != 0 ||
            (Reference.getBitboard(king, occupied, 11) & pieceBoard[11]) != 0
        ) : (
            //test white pieces
            (Reference.getBitboard(king, occupied, 1) & pieceBoard[0]) != 0 ||
            (Reference.getBitboard(king, occupied, 5) & (pieceBoard[4] | pieceBoard[2])) != 0 ||
            (Reference.getBitboard(king, occupied, 7) & (pieceBoard[6] | pieceBoard[2])) != 0 ||
            (Reference.getBitboard(king, occupied, 9) & pieceBoard[8]) != 0 ||
            (Reference.getBitboard(king, occupied, 12) & pieceBoard[10]) != 0
        );
    }
    byte[][] generateMoves(boolean includeKingMoves) {
        //this is the culmination of everything else above
        long squares = occupiedSquares(position[0] == 1);
        int quantity = 0;
        long[] moves = new long[64];
        long copyOfSquares = squares;
        while (copyOfSquares != 0) {
            int square = Long.numberOfTrailingZeros(copyOfSquares);
            long movesToConsider = legalMoves(square+1);
            moves[square] = movesToConsider;
            quantity += Long.bitCount(movesToConsider);
            copyOfSquares &= (copyOfSquares-1);
        }
        byte[][] output = new byte[2][quantity];
        int i = 0;
        copyOfSquares = squares;
        while (copyOfSquares != 0) {
            int square = Long.numberOfTrailingZeros(copyOfSquares);
            long movesToConsider = moves[square];
            while (movesToConsider != 0) {
                output[0][i] = (byte) (square+1);
                output[1][i] = (byte) (Long.numberOfTrailingZeros(movesToConsider)+1);
                i++;
                movesToConsider &= (movesToConsider-1);
            }
            copyOfSquares &= (copyOfSquares-1);
        }
        return output;
    }
    byte[][] sortMoves(byte[][] unsortedMoves) {
        sortHistory();
        int[][] moves = new int[unsortedMoves[0].length][3]; //from, to, score
        //int[] data = getInfo();
        /*score based on:
        1. cached best move
        2. captures, most valuable victim, least valuable attacker
        3. killer moves
        4. history
        If not:
        - Rank forward and center-seeking moves above backward moves
        - Rank pawn moves close to promotion higher
        - Rank far advanced rook moves higher
        - Rank king moves toward the center higher towards the endgame
        */
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
            int piece = mailbox[start-1];
            boolean pieceColor = ChessProject.color(piece);
            int modifier = (pieceColor) ? 1 : -1;
            double pieceModifier = 1.0d; //helps prioritize certain pieces over others
            switch (piece) {
                case 1: case 2:
                    pieceModifier = 0.7;
                    break;
                case 3: case 4:
                    pieceModifier = 0.8;
                    break;
                case 7: case 8: case 9: case 10:
                    pieceModifier = 1.2;
                    break;
                case 11: case 12:
                    pieceModifier = 0.9;
                    break;
                default: break;
            }
            int captured = mailbox[end-1];
            boolean capture = captured != 0 || ((piece == 11 || piece == 12) && end == position[5]);
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
            if (piece != 1 && piece != 2) cumulative += centerGain;
            
            //reward forward moves
            cumulative += (pieceColor) ? ChessProject.getY(end) - ChessProject.getY(start) : ChessProject.getY(start) - ChessProject.getY(end);
            if (piece == 11 || piece == 12) {
                //reward pawn moves close to promotion
                boolean farAdvanced = (pieceColor) ? ChessProject.getY(end) >= 6 : ChessProject.getY(end) <= 3;
                if (farAdvanced) {
                    cumulative += 20;
                }
                //punish pawn moves in front of king if it's not the endgame
                int king = (pieceColor) ? 1 : 2;
                if (piecePresent(king, ChessProject.translate(start, 0, -modifier))) {
                    cumulative -= (int) Math.round(20 * (1 - percentEndgame()));
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
            moves[i][2] = (int) (cumulative * pieceModifier);
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
        int piece = mailbox[start-1];
        return !isEmpty(end) || ((piece == 11 || piece == 12) && end == position[5]) || (piece == 11 && end >= 57) || (piece == 12 && end <= 8);
    }
    int gameStatus() {
        return gameStatus(generateMoves(true));
    }
    int gameStatus(byte[][] moves) {
        if (moves[0].length == 0) {
            if (inCheck()) {
                return (position[0] == 1) ? 2 : 1; //checkmate
            }
            return 3; //stalemate
        }
        if (!sufficientMaterial() || position[6] >= 100) {
            return 3; //draw
        }
        return 0;
    }
    boolean sufficientMaterial() {
        int whiteKnights = Long.bitCount(pieceBoard[8]), blackKnights = Long.bitCount(pieceBoard[9]),
        whitePieces = Long.bitCount(occupiedSquares(true)), blackPieces = Long.bitCount(occupiedSquares(false));
        if ((pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5] | pieceBoard[10] | pieceBoard[11]) != 0) return true;
        if (whitePieces == 2 && whiteKnights < 2) return true;
        if (blackPieces == 2 && blackKnights < 2) return true;
        return false;
    }
    boolean pawnEndgame() {
        //if true, zugzwang may happen at some point -> cancel the null move prune
        return (pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5] | pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) == 0;
    }
    double percentEndgame() {
        int endgameScore = 0; //the higher, the less of an endgame it is, above 1000 it's a middlegame
        endgameScore += Long.bitCount(pieceBoard[2] | pieceBoard[3]) * 400; //queens
        endgameScore += Long.bitCount(pieceBoard[4] | pieceBoard[5]) * 200; //rooks
        endgameScore += Long.bitCount(pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) * 50; //bishops and knights
        return 1.0 - Math.min(1000.0, endgameScore) / 1000.0;
    }
    long[] passedPawns() {
        //passed pawns white + black, passed pawn candidates white + black, passed pawn candidate helpers white + black
        boolean[] whitePawnFiles = new boolean[8],
        blackPawnFiles = new boolean[8];
        for (int i = 0; i < 8; i++) {
            whitePawnFiles[i] = (pieceBoard[10] & fileref[i]) != 0;
            blackPawnFiles[i] = (pieceBoard[11] & fileref[i]) != 0;
        }
        long[] output = new long[6];
        int helpingNeighbors = 0, opposingNeighbors = 0;
        for (int i = 0; i < 8; i++) {
            if (whitePawnFiles[i] && !blackPawnFiles[i]) {
                if (i != 0) {
                    if (whitePawnFiles[i-1]) helpingNeighbors++;
                    if (blackPawnFiles[i-1]) opposingNeighbors++;
                }
                if (i != 7) {
                    if (whitePawnFiles[i+1]) helpingNeighbors++;
                    if (blackPawnFiles[i+1]) opposingNeighbors++;
                }
                if (opposingNeighbors == 0) output[0] |= (pieceBoard[10] & fileref[i]);
                else if (helpingNeighbors > opposingNeighbors) {
                    output[2] |= (pieceBoard[10] & fileref[i]);
                    if (i != 0 && whitePawnFiles[i-1]) output[4] |= (pieceBoard[10] & fileref[i-1]);
                    if (i != 7 && whitePawnFiles[i+1]) output[4] |= (pieceBoard[10] & fileref[i+1]);
                }
            } else if (blackPawnFiles[i] && !whitePawnFiles[i]) {
                if (i != 0) {
                    if (blackPawnFiles[i-1]) helpingNeighbors++;
                    if (whitePawnFiles[i-1]) opposingNeighbors++;
                }
                if (i != 7) {
                    if (blackPawnFiles[i+1]) helpingNeighbors++;
                    if (whitePawnFiles[i+1]) opposingNeighbors++;
                }
                if (opposingNeighbors == 0) output[1] |= (pieceBoard[11] & fileref[i]);
                else if (helpingNeighbors > opposingNeighbors) {
                    output[3] |= (pieceBoard[11] & fileref[i]);
                    if (i != 0 && blackPawnFiles[i-1]) output[5] |= (pieceBoard[11] & fileref[i-1]);
                    if (i != 7 && blackPawnFiles[i+1]) output[5] |= (pieceBoard[11] & fileref[i+1]);
                }
            }
        }
        return output;
    }
    int simpleCheckmate() {
        //0 = no, 1 = ladder - white, 2 = white with queen, 3 = white with rook, -1 = ladder - black, -2 = black with queen, -3 = black with rook
        int whiteQueens = Long.bitCount(pieceBoard[2]), whiteRooks = Long.bitCount(pieceBoard[4]),
        blackQueens = Long.bitCount(pieceBoard[3]), blackRooks = Long.bitCount(pieceBoard[5]);
        int whiteHeavyPieces = whiteQueens + whiteRooks, blackHeavyPieces = blackQueens + blackRooks;
        if ((pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9] | pieceBoard[10] | pieceBoard[11]) != 0) return 0;
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
        if (shallowSearch[0] >= higher || shallowSearch[0] <= lower) return evalDepth(depth);
        return shallowSearch;
    }
    int evalQ(byte[][] sortedMoves, int alpha, int beta, boolean changeEval, boolean printData) {
        switch (gameStatus(sortedMoves)) {
            case 1: return Integer.MAX_VALUE;
            case 2: return Integer.MIN_VALUE;
            case 3: return 0;
            default: break;
        }
        int checkmateMode = simpleCheckmate();
        if (checkmateMode != 0) {
            ChessProject.nodes = Integer.MAX_VALUE / 2;
            return (checkmateMode > 0)
                ? ((Integer.MAX_VALUE / 2) - sizeOfKingBox(checkmateMode))
                : ((Integer.MIN_VALUE / 2) + sizeOfKingBox(checkmateMode));
        } //the smaller the box, the better
        ChessProject.nodes++;
        byte[][] moves = sortedMoves;
        boolean maximizing = position[0] == 1;
        boolean checked = inCheck();
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        if (!checked) {
            //update everything temporarily
            if (changeEval) {
                for (int i = 1; i <= 64; i++) {
                    update(i);
                }
                best = currentCount(false);
                if (printData) {
                    System.out.printf("To move: %s, In check: %b, Alpha: %d, Beta: %d, Best: %d%n Boardstate:%n",
                    ((position[0] == 1) ? "white" : "black"), checked, alpha, beta, best);
                    display();
                }
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
            potential = evalQ(sortMoves(generateMoves(true)), alpha, beta, true, false);
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
        byte[][] unorderedMoves = generateMoves(false);
        switch (gameStatus(unorderedMoves)) {
            case 1: return new int[]{Integer.MAX_VALUE, 0, 0};
            case 2: return new int[]{Integer.MIN_VALUE, 0, 0};
            case 3: return new int[]{0, 0, 0};
        }
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
                    if (!isTacticalMove(data[1], data[2])) storeQuietMove(mailbox[data[1]-1], data[1], data[2], depth);
                    return new int[]{data[0], data[1], data[2]};
                }
            }
        }
        long start = System.nanoTime();
        boolean maximizing = position[0] == 1;
        //null move pruning
        boolean hasNullMoveBound = maximizing ? beta < Integer.MAX_VALUE : alpha > Integer.MIN_VALUE;
        if (depth > 2 && hasNullMoveBound && !pawnEndgame() && !inCheck() && depth < maxDepth && activePieces() > 2) { //don't want to do this at the root node
            int[] evalPass;
            int enPassantSquare = position[5];
            position[0] = 1 - position[0]; //make a null move
            position[5] = 0;
            currentZobrist = Reference.passMoveZobrist(position, currentZobrist);
            try {
                int nullDepth = depth - 3;
                evalPass = maximizing ? evalDepth(nullDepth, beta - 1, beta) : evalDepth(nullDepth, alpha, alpha + 1);
            } finally {
                position[0] = 1 - position[0]; //restore original position
                position[5] = enPassantSquare;
                currentZobrist = Reference.restoreFromPassZobrist(currentZobrist, enPassantSquare);
            }
            if (maximizing ? evalPass[0] >= beta : evalPass[0] <= alpha) {
                return new int[]{maximizing ? beta : alpha, 0, 0};
            }
        }
        byte[][] moves = sortMoves(unorderedMoves);
        if (depth == 0) return new int[]{evalQ(moves, alpha, beta, true, (pieceBoard[1] == (1L << 62))), 0, 0};
        int evaluation = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestStart = 0;
        int bestEnd = 0;
        for (int i = 0; i < moves[0].length; i++) {
            makeMove(moves[0][i], moves[1][i]);
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
                potential = evalDepth(depth - 1, alpha, beta)[0];
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
                if (!isTacticalMove(moves[0][i], moves[1][i])) storeQuietMove(mailbox[moves[0][i]-1], moves[0][i], moves[1][i], depth);
                break;
            }
        }
        double timeTaken = (System.nanoTime() - start) / 1000000000.0;
        if (depth == maxDepth && timeTaken > limit) throw new TimeExceededException();
        bound = evaluation <= alphaOriginal ? 1 : evaluation >= betaOriginal ? -1 : 0;
        store(evaluation, bestStart, bestEnd, depth, bound);
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
    static int countNonzeros(byte[] inputArray) {
        int count = 0;
        for (byte element : inputArray) {
            if (element != 0) count++;
        }
        return count;
    }
    static double distance(int square1, int square2) {
        int x1 = square1 % 8;
        int y1 = square1 / 8;
        int x2 = square2 % 8;
        int y2 = square2 / 8;
        return Math.hypot(x2-x1, y2-y1);
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
    static long getZobrist(long[] boards, int[] extraData) {
        long out = empty;
        for (int i = 0; i < 12; i++) {
            long boardCopy = boards[i];
            while (boardCopy != 0) {
                out ^= zobrist[Long.numberOfTrailingZeros(boardCopy)][i];
                boardCopy &= (boardCopy-1);
            }
        }
        out ^= ((extraData[0] == 1) ? whiteToMove : blackToMove);
        if (extraData[1] == 1) out ^= zobristWCS;
        if (extraData[2] == 1) out ^= zobristWCL;
        if (extraData[3] == 1) out ^= zobristBCS;
        if (extraData[4] == 1) out ^= zobristBCL;
        if (extraData[5] != 0) out ^= zobrist[extraData[5]-1][12];
        return out;
    }
    static long passMoveZobrist(int[] dataBefore, long zobristBefore) {
        zobristBefore ^= whiteToMove;
        zobristBefore ^= blackToMove;
        if (dataBefore[5] != 0) zobristBefore ^= zobrist[dataBefore[5]-1][12];
        return zobristBefore;
    }
    static long restoreFromPassZobrist(long zobristModified, int originalEP) {
        zobristModified ^= whiteToMove;
        zobristModified ^= blackToMove;
        if (originalEP != 0) zobristModified ^= zobrist[originalEP-1][12];
        return zobristModified;
    }
    static long updateZobrist(long[] boardsBefore, int[] dataBefore, long zobristBefore, int start, int end) {
        int piece = ChessProject.getPiece(boardsBefore, start);
        int captured = ChessProject.getPiece(boardsBefore, end);
        zobristBefore ^= zobrist[start-1][piece-1];
        zobristBefore ^= zobrist[end-1][piece-1];
        if (dataBefore[5] != 0) zobristBefore ^= zobrist[dataBefore[5]-1][12];
        if (end == dataBefore[5]) {
            if (piece == 11) {
                zobristBefore ^= zobrist[(end - 8) - 1][11];
            } else if (piece == 12) {
                zobristBefore ^= zobrist[(end + 8) - 1][10];
            } else if (captured != 0) {
                zobristBefore ^= zobrist[end-1][captured-1];
            }
        } else if (captured != 0) zobristBefore ^= zobrist[end-1][captured-1];
        if (captured == 5) {
            if (end == 8 && dataBefore[1] != 0) zobristBefore ^= zobristWCS;
            if (end == 1 && dataBefore[2] != 0) zobristBefore ^= zobristWCL;
        }
        if (captured == 6) {
            if (end == 64 && dataBefore[3] != 0) zobristBefore ^= zobristBCS;
            if (end == 57 && dataBefore[4] != 0) zobristBefore ^= zobristBCL;
        }
        switch (piece) {
            case 1:
                if (start == 5) {
                    if (dataBefore[1] != 0) zobristBefore ^= zobristWCS;
                    if (dataBefore[2] != 0) zobristBefore ^= zobristWCL;
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
                    if (dataBefore[3] != 0) zobristBefore ^= zobristBCS;
                    if (dataBefore[4] != 0) zobristBefore ^= zobristBCL;
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
                if (start == 8 && dataBefore[1] != 0) zobristBefore ^= zobristWCS;
                if (start == 1 && dataBefore[2] != 0) zobristBefore ^= zobristWCL;
                break;
            case 6:
                if (start == 64 && dataBefore[3] != 0) zobristBefore ^= zobristBCS;
                if (start == 57 && dataBefore[4] != 0) zobristBefore ^= zobristBCL;
                break;
            case 11:
                if (end - start == 16) {
                    boolean pawnToLeft = end % 8 != 1 /*not on the a-file*/ && ChessProject.getPiece(boardsBefore, end - 1) == 12;
                    boolean pawnToRight = end % 8 != 0 /*not on the h-file*/ && ChessProject.getPiece(boardsBefore, end + 1) == 12;
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
                    boolean pawnToLeft = end % 8 != 1 /*not on the a-file*/ && ChessProject.getPiece(boardsBefore, end - 1) == 11;
                    boolean pawnToRight = end % 8 != 0 /*not on the h-file*/ && ChessProject.getPiece(boardsBefore, end + 1) == 11;
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
    static int closestEdge(int square) {
        //1 = 1st rank, 2 = h-file, 3 = 9th rank, 4 = a-file
        int rank = square / 8;
        int file = square % 8;
        int distances1 = rank, distances2 = 7 - file, distances3 = 7 - rank, distances4 = file;
        int minimum = Math.min(distances1, Math.min(distances2, Math.min(distances3, distances4)));
        if (minimum == distances1) return 1;
        if (minimum == distances2) return 2;
        if (minimum == distances3) return 3;
        return 4;
    }
    //bitboards
    static final int[] bishopDx = {1, 1, -1, -1};
    static final int[] bishopDy = {-1, 1, 1, -1};
    static final int[] rookDx = {1, 0, -1, 0};
    static final int[] rookDy = {0, 1, 0, -1};
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
    private static long[][] bishopAttacks = new long[64][];
    private static long[][] rookAttacks = new long[64][];
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
    private static int occupancyIndex(long occupancy, long mask) {
        int index = 0;
        int bitIndex = 0;
        while (mask != 0) {
            long bit = Long.lowestOneBit(mask);
            if ((occupancy & bit) != 0) index |= 1 << bitIndex;
            mask ^= bit;
            bitIndex++;
        }
        return index;
    }
    private static long[] createSlidingAttackTable(int square, long mask, int[] dx, int[] dy) {
        long[] attacks = new long[1 << Long.bitCount(mask)];
        for (int index = 0; index < attacks.length; index++) {
            long occupancy = 0L;
            long remainingMask = mask;
            int bitIndex = 0;
            while (remainingMask != 0) {
                long bit = Long.lowestOneBit(remainingMask);
                if ((index & (1 << bitIndex)) != 0) occupancy |= bit;
                remainingMask ^= bit;
                bitIndex++;
            }
            attacks[index] = fill(square, occupancy, dx, dy);
        }
        return attacks;
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
            rookAttacks[i] = createSlidingAttackTable(i, rookVision[i], rookDx, rookDy);
            //bishops
            bishopAttacks[i] = createSlidingAttackTable(i, bishopVision[i], bishopDx, bishopDy);
            //zobrist
            for (int j = 0; j < 13; j++) {
                zobrist[i][j] = generator.nextLong();
            }
        }
    }
    //attack maps
    static long getBitboard(int square, long allPieces, int piece) {
        switch (piece) {
            case 1: case 2: return kingVision[square];
            case 3: return getBitboard(square, allPieces, 5) | getBitboard(square, allPieces, 7);
            case 4: return getBitboard(square, allPieces, 6) | getBitboard(square, allPieces, 8);
            case 5: case 6: return rookAttacks[square][occupancyIndex(allPieces, rookVision[square])];
            case 7: case 8: return bishopAttacks[square][occupancyIndex(allPieces, bishopVision[square])];
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
class OutOfSpaceException extends RuntimeException {
    OutOfSpaceException() {
        super("Oops, memory threshold exceeded...");
    }
}
import java.util.Scanner;
import java.util.Arrays;
import java.util.Random;
import java.util.Objects;
import java.lang.Math;

public class ChessProject {
    /*

    PASTE INTO TERMINAL:
    & "C:\Users\cbh5981\Documents\jdk-27\bin\java.exe" "C:\Users\cbh5981\Documents\Chess-project-in-java\ChessProject.java"

    */
    static long empty, whiteToMove, blackToMove, zobristWCS, zobristWCL, zobristBCS, zobristBCL, nodes = 0L;
    static final long rightBitsOnly = Integer.MAX_VALUE;
    static final long leftBitsOnly = (-1L) ^ Integer.MAX_VALUE;
    static final long baseLeftBit = Long.lowestOneBit(leftBitsOnly);
    static final int length = 0x800000; //around 8 million
    static long[][] zobrist = new long[64][13]; //13th slot is for if the square is available for en passant
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
    static final float[] distanceToCenter = {
        4.94975f, 4.30116f, 3.80789f, 3.53553f, 3.53553f, 3.80789f, 4.30116f, 4.94975f, 
        4.30116f, 3.53553f, 2.91548f, 2.54951f, 2.54951f, 2.91548f, 3.53553f, 4.30116f, 
        3.80789f, 2.91548f, 2.12132f, 1.58114f, 1.58114f, 2.12132f, 2.91548f, 3.80789f, 
        3.53553f, 2.54951f, 1.58114f, 0.70711f, 0.70711f, 1.58114f, 2.54951f, 3.53553f, 
        3.53553f, 2.54951f, 1.58114f, 0.70711f, 0.70711f, 1.58114f, 2.54951f, 3.53553f, 
        3.80789f, 2.91548f, 2.12132f, 1.58114f, 1.58114f, 2.12132f, 2.91548f, 3.80789f, 
        4.30116f, 3.53553f, 2.91548f, 2.54951f, 2.54951f, 2.91548f, 3.53553f, 4.30116f, 
        4.94975f, 4.30116f, 3.80789f, 3.53553f, 3.53553f, 3.80789f, 4.30116f, 4.94975f
    };
    static final int[] bishopDx = {1, 1, -1, -1};
    static final int[] bishopDy = {-1, 1, 1, -1};
    static final int[] rookDx = {1, 0, -1, 0};
    static final int[] rookDy = {0, 1, 0, -1};
    static final long[] whitePawnVision = {
        0x200L, 0x500L, 0xa00L, 0x1400L, 0x2800L, 0x5000L, 0xa000L, 0x4000L, 0x20000L, 0x50000L, 0xa0000L, 0x140000L,
        0x280000L, 0x500000L, 0xa00000L, 0x400000L, 0x2000000L, 0x5000000L, 0xa000000L, 0x14000000L, 0x28000000L,
        0x50000000L, 0xa0000000L, 0x40000000L, 0x200000000L, 0x500000000L, 0xa00000000L, 0x1400000000L, 0x2800000000L,
        0x5000000000L, 0xa000000000L, 0x4000000000L, 0x20000000000L, 0x50000000000L, 0xa0000000000L, 0x140000000000L,
        0x280000000000L, 0x500000000000L, 0xa00000000000L, 0x400000000000L, 0x2000000000000L, 0x5000000000000L,
        0xa000000000000L, 0x14000000000000L, 0x28000000000000L, 0x50000000000000L, 0xa0000000000000L, 0x40000000000000L,
        0x200000000000000L, 0x500000000000000L, 0xa00000000000000L, 0x1400000000000000L, 0x2800000000000000L,
        0x5000000000000000L, 0xa000000000000000L, 0x4000000000000000L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L
    };
    static final long[] blackPawnVision = {
        0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0x2L, 0x5L, 0xaL, 0x14L, 0x28L, 0x50L, 0xa0L, 0x40L, 0x200L, 0x500L,
        0xa00L, 0x1400L, 0x2800L, 0x5000L, 0xa000L, 0x4000L, 0x20000L, 0x50000L, 0xa0000L, 0x140000L, 0x280000L,
        0x500000L, 0xa00000L, 0x400000L, 0x2000000L, 0x5000000L, 0xa000000L, 0x14000000L, 0x28000000L, 0x50000000L,
        0xa0000000L, 0x40000000L, 0x200000000L, 0x500000000L, 0xa00000000L, 0x1400000000L, 0x2800000000L, 0x5000000000L,
        0xa000000000L, 0x4000000000L, 0x20000000000L, 0x50000000000L, 0xa0000000000L, 0x140000000000L, 0x280000000000L,
        0x500000000000L, 0xa00000000000L, 0x400000000000L, 0x2000000000000L, 0x5000000000000L, 0xa000000000000L,
        0x14000000000000L, 0x28000000000000L, 0x50000000000000L, 0xa0000000000000L, 0x40000000000000L
    };
    static final long[] knightVision = {
        0x20400L, 0x50800L, 0xa1100L, 0x142200L, 0x284400L, 0x508800L, 0xa01000L, 0x402000L, 0x2040004L, 0x5080008L,
        0xa110011L, 0x14220022L, 0x28440044L, 0x50880088L, 0xa0100010L, 0x40200020L, 0x204000402L, 0x508000805L,
        0xa1100110aL, 0x1422002214L, 0x2844004428L, 0x5088008850L, 0xa0100010a0L, 0x4020002040L, 0x20400040200L,
        0x50800080500L, 0xa1100110a00L, 0x142200221400L, 0x284400442800L, 0x508800885000L, 0xa0100010a000L, 0x402000204000L,
        0x2040004020000L, 0x5080008050000L, 0xa1100110a0000L, 0x14220022140000L, 0x28440044280000L, 0x50880088500000L,
        0xa0100010a00000L, 0x40200020400000L, 0x204000402000000L, 0x508000805000000L, 0xa1100110a000000L, 0x1422002214000000L,
        0x2844004428000000L, 0x5088008850000000L, 0xa0100010a0000000L, 0x4020002040000000L, 0x400040200000000L,
        0x800080500000000L, 0x1100110a00000000L, 0x2200221400000000L, 0x4400442800000000L, 0x8800885000000000L,
        0x100010a000000000L, 0x2000204000000000L, 0x4020000000000L, 0x8050000000000L, 0x110a0000000000L, 0x22140000000000L,
        0x44280000000000L, 0x88500000000000L, 0x10a00000000000L, 0x20400000000000L
    };
    static final long[] kingVision = {
        0x302L, 0x705L, 0xe0aL, 0x1c14L, 0x3828L, 0x7050L, 0xe0a0L, 0xc040L, 0x30203L, 0x70507L, 0xe0a0eL, 0x1c141cL,
        0x382838L, 0x705070L, 0xe0a0e0L, 0xc040c0L, 0x3020300L, 0x7050700L, 0xe0a0e00L, 0x1c141c00L, 0x38283800L,
        0x70507000L, 0xe0a0e000L, 0xc040c000L, 0x302030000L, 0x705070000L, 0xe0a0e0000L, 0x1c141c0000L, 0x3828380000L,
        0x7050700000L, 0xe0a0e00000L, 0xc040c00000L, 0x30203000000L, 0x70507000000L, 0xe0a0e000000L, 0x1c141c000000L,
        0x382838000000L, 0x705070000000L, 0xe0a0e0000000L, 0xc040c0000000L, 0x3020300000000L, 0x7050700000000L,
        0xe0a0e00000000L, 0x1c141c00000000L, 0x38283800000000L, 0x70507000000000L, 0xe0a0e000000000L, 0xc040c000000000L,
        0x302030000000000L, 0x705070000000000L, 0xe0a0e0000000000L, 0x1c141c0000000000L, 0x3828380000000000L,
        0x7050700000000000L, 0xe0a0e00000000000L, 0xc040c00000000000L, 0x203000000000000L, 0x507000000000000L,
        0xa0e000000000000L, 0x141c000000000000L, 0x2838000000000000L, 0x5070000000000000L, 0xa0e0000000000000L,
        0x40c0000000000000L
    };
    static final long[] bishopVision = {
        0x8040201008040200L, 0x80402010080500L, 0x804020110a00L, 0x8041221400L, 0x182442800L, 0x10204885000L,
        0x102040810a000L, 0x102040810204000L, 0x4020100804020002L, 0x8040201008050005L, 0x804020110a000aL, 0x804122140014L,
        0x18244280028L, 0x1020488500050L, 0x102040810a000a0L, 0x204081020400040L, 0x2010080402000204L, 0x4020100805000508L,
        0x804020110a000a11L, 0x80412214001422L, 0x1824428002844L, 0x102048850005088L, 0x2040810a000a010L, 0x408102040004020L,
        0x1008040200020408L, 0x2010080500050810L, 0x4020110a000a1120L, 0x8041221400142241L, 0x182442800284482L,
        0x204885000508804L, 0x40810a000a01008L, 0x810204000402010L, 0x804020002040810L, 0x1008050005081020L,
        0x20110a000a112040L, 0x4122140014224180L, 0x8244280028448201L, 0x488500050880402L, 0x810a000a0100804L,
        0x1020400040201008L, 0x402000204081020L, 0x805000508102040L, 0x110a000a11204080L, 0x2214001422418000L,
        0x4428002844820100L, 0x8850005088040201L, 0x10a000a010080402L, 0x2040004020100804L, 0x200020408102040L,
        0x500050810204080L, 0xa000a1120408000L, 0x1400142241800000L, 0x2800284482010000L, 0x5000508804020100L,
        0xa000a01008040201L, 0x4000402010080402L, 0x2040810204080L, 0x5081020408000L, 0xa112040800000L, 0x14224180000000L,
        0x28448201000000L, 0x50880402010000L, 0xa0100804020100L, 0x40201008040201L
    };
    static final long[] rookVision = {
        0x1010101010101feL, 0x2020202020202fdL, 0x4040404040404fbL, 0x8080808080808f7L, 0x10101010101010efL,
        0x20202020202020dfL, 0x40404040404040bfL, 0x808080808080807fL, 0x10101010101fe01L, 0x20202020202fd02L,
        0x40404040404fb04L, 0x80808080808f708L, 0x101010101010ef10L, 0x202020202020df20L, 0x404040404040bf40L,
        0x8080808080807f80L, 0x101010101fe0101L, 0x202020202fd0202L, 0x404040404fb0404L, 0x808080808f70808L,
        0x1010101010ef1010L, 0x2020202020df2020L, 0x4040404040bf4040L, 0x80808080807f8080L, 0x1010101fe010101L,
        0x2020202fd020202L, 0x4040404fb040404L, 0x8080808f7080808L, 0x10101010ef101010L, 0x20202020df202020L,
        0x40404040bf404040L, 0x808080807f808080L, 0x10101fe01010101L, 0x20202fd02020202L, 0x40404fb04040404L,
        0x80808f708080808L, 0x101010ef10101010L, 0x202020df20202020L, 0x404040bf40404040L, 0x8080807f80808080L,
        0x101fe0101010101L, 0x202fd0202020202L, 0x404fb0404040404L, 0x808f70808080808L, 0x1010ef1010101010L,
        0x2020df2020202020L, 0x4040bf4040404040L, 0x80807f8080808080L, 0x1fe010101010101L, 0x2fd020202020202L,
        0x4fb040404040404L, 0x8f7080808080808L, 0x10ef101010101010L, 0x20df202020202020L, 0x40bf404040404040L,
        0x807f808080808080L, 0xfe01010101010101L, 0xfd02020202020202L, 0xfb04040404040404L, 0xf708080808080808L,
        0xef10101010101010L, 0xdf20202020202020L, 0xbf40404040404040L, 0x7f80808080808080L
    };
    static int[] moves = new int[512];
    static long[] moveBoards = new long[64];
    static long[] absolutePinRestrictions = new long[64];
    static boolean[] whitePawnFiles = new boolean[8];
    static boolean[] blackPawnFiles = new boolean[8];
    //bitboards
    static long[][] bishopAttacks = new long[64][];
    static long[][] rookAttacks = new long[64][];
    static int[] position = new int[7];
    static long currentZobrist;
    static long[] pieceBoard = new long[12];
    static byte[] mailbox = new byte[64];
    static int maxDepth;
    static long[][] undoStackPieces = new long[4096][12];
    static byte[][] undoStackMailboxes = new byte[4096][64];
    static int[][] undoStackData = new int[4096][7];
    static boolean[] undoStackTactical = new boolean[4096];
    static boolean[] undoStackPawnStructureChanged = new boolean[4096];
    static long[] undoStackZobrist = new long[4096];
    static int slot = 0;
    static int undoDeep = 0;
    static boolean[] killerMoves = new boolean[4096];
    static int[] history = new int[4096];
    static int historyStored = 0;
    //transposition table
    static boolean[] transpositionOccupied = new boolean[length];
    static long[] zobristStorage = new long[length];
    static short[] evals = new short[length]; //in decipawns
    static int[] bestMoves = new int[length];
    static byte[] depthsSearched = new byte[length];
    static byte[] bounds = new byte[length];
    //metrics
    static byte[] pieceActivity = new byte[64], material = new byte[64], centralControl = new byte[64],
    kingSafety = new byte[64], space = new byte[64], rooksSeventh = new byte[64], outposts = new byte[64];
    static final long[] rankref = {
        0xffL,
        0xff00L,
        0xff0000L,
        0xff000000L,
        0xff00000000L,
        0xff0000000000L,
        0xff000000000000L,
        0xff00000000000000L
    };
    static final long[] fileref = {
        0x1010101010100L,
        0x2020202020200L,
        0x4040404040400L,
        0x8080808080800L,
        0x10101010101000L,
        0x20202020202000L,
        0x40404040404000L,
        0x80808080808000L
    };
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
    static long fillWithRook(int square, long pieces) {
        long output = 1L << square;
        for (int i = 0; i < 4; i++) {
            int currentX = square % 8 + rookDx[i];
            int currentY = square / 8 + rookDy[i];
            while (currentX >= 0 && currentX < 8 && currentY >= 0 && currentY < 8) {
                long addition = 1L << (currentX + 8 * currentY);
                output |= addition;
                if ((addition & pieces) != 0) break;
                currentX += rookDx[i];
                currentY += rookDy[i];
            }
        }
        return output;
    }
    static long fillWithBishop(int square, long pieces) {
        long output = 1L << square;
        for (int i = 0; i < 4; i++) {
            int currentX = square % 8 + bishopDx[i];
            int currentY = square / 8 + bishopDy[i];
            while (currentX >= 0 && currentX < 8 && currentY >= 0 && currentY < 8) {
                long addition = 1L << (currentX + 8 * currentY);
                output |= addition;
                if ((addition & pieces) != 0) break;
                currentX += bishopDx[i];
                currentY += bishopDy[i];
            }
        }
        return output;
    }
    static long buildCheckRay(int checkOrigin, int piece) {
        int king = Long.numberOfTrailingZeros(pieceBoard[1 - position[0]]) + 1;
        int deltaX = 0, deltaY = 0;
        switch (piece) {
            case 3: case 4:
                if (getX(king) == getX(checkOrigin) || getY(king) == getY(checkOrigin))
                return buildCheckRay(checkOrigin, piece + 2); //rook
                else return buildCheckRay(checkOrigin, piece + 4); //bishop
            case 5: case 6:
                if (getX(king) == getX(checkOrigin)) {
                    if (getY(king) > getY(checkOrigin)) deltaY = -1;
                    else deltaY = 1;
                } else if (getY(king) == getY(checkOrigin)) {
                    if (getX(king) > getX(checkOrigin)) deltaX = -1;
                    else deltaX = 1;
                }
            case 7: case 8:
                if (getX(king) > getX(checkOrigin)) {
                    if (getY(king) > getY(checkOrigin)) {deltaX = -1; deltaY = -1;}
                    else {deltaX = -1; deltaY = 1;}
                } else {
                    if (getY(king) > getY(checkOrigin)) {deltaX = 1; deltaY = -1;}
                    else {deltaX = 1; deltaY = 1;}
                }
            case 9: case 10: case 11: case 12: return (1L << (checkOrigin - 1));
            default: break;
        }
        long output = 0L;
        int square = king;
        while (square != checkOrigin) {
            square += 8 * deltaY + deltaX;
            output |= (1L << (square-1));
        }
        return output;
    }
    static int occupancyIndex(long occupancy, long mask) {
        return (int) Long.compress(occupancy, mask);
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
    static long getCompositeBitboard(long squares, long allPieces) {
        long output = 0L;
        while (squares != 0) {
            int squareid = Long.numberOfTrailingZeros(squares);
            output |= getBitboard(squareid, allPieces, mailbox[squareid]);
            squares &= (squares - 1);
        }
        return output;
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
    static void storeQuietMove(int move, int depth) {
        int moveStart = extract(move, 0) - 1;
        int moveEnd = extract(move, 1) - 1;
        int requiredIndex = (moveStart << 6) | moveEnd;
        killerMoves[requiredIndex] = true;
        history[requiredIndex] = Math.min(history[requiredIndex], 500); //store history
    }
    static void store(int evaluation, int bestMove, int depth, int bound) {
        int index = (int)(currentZobrist & (length - 1));
        zobristStorage[index] = currentZobrist;
        evals[index] = (short) evaluation;
        bestMoves[index] = bestMove;
        depthsSearched[index] = (byte) depth;
        bounds[index] = (byte) bound;
        transpositionOccupied[index] = true;
    }
    static int getTranspositionIndex() {
        int index = (int)(currentZobrist & (length - 1));
        if (transpositionOccupied[index] && currentZobrist == zobristStorage[index]) return index;
        return -1;
    }
    static int compress(int start, int end, int piece) {
        return (start << 24) | (end << 16) | piece;
    }
    static int extract(int compressed, int what) {
        //0 = start, 1 = end, 2 = piece
        switch (what) {
            case 0: return (compressed & 0x7f000000) >> 24;
            case 1: return (compressed & 0xff0000) >> 16;
            case 2: return compressed & 0xffff;
            default: return 0;
        }
    }
    static int bishopPairBonus(boolean tactical) {
        int whiteBishops = Long.bitCount(pieceBoard[6]);
        int blackBishops = Long.bitCount(pieceBoard[7]);
        int whiteBonus = (whiteBishops >= 2 && tactical) ? 2 : (whiteBishops >= 2 && !tactical) ? 1 : 0;
        int blackBonus = (blackBishops >= 2 && tactical) ? -2 : (blackBishops >= 2 && !tactical) ? -1 : 0;
        return whiteBonus + blackBonus;
    }
    static int currentCount() {
        int outputTactical = 0;
        int outputNontactical = 0;
        int materialDifferential = 0;
        double activePiecesModifier = Math.min(activePieces(), 4) / 4.0;
        double endgamePercent = percentEndgame();
        for (int i = 0; i < 64; i++) {
            outputTactical += pieceActivity[i] + centralControl[i]
            + kingSafety[i] + rooksSeventh[i] + (space[i] + outposts[i]) / 3;
            outputNontactical += centralControl[i] + space[i] + outposts[i]
            + (pieceActivity[i] + kingSafety[i] + rooksSeventh[i]) / 3;
            materialDifferential += material[i];
        }
        int tacticalOutput = outputTactical + ((position[0] == 1) ? 1 : -1) + bishopPairBonus(true);
        int nontacticalOutput = outputNontactical + ((position[0] == 1) ? 1 : -1) + bishopPairBonus(false);
        //material matters more in the endgame
        double materialModifier = 1 + endgamePercent;
        if ((pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]) == 0) {
            if ((pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) == 0) {
                //pawn endgame, any material advantage is practically decisive
                materialModifier = 5;
            }
            //minor piece endgame, any material advantage is very important
            materialModifier = 3;
        }
        double subtotal = activePiecesModifier * tacticalOutput
        + (1 - activePiecesModifier) * nontacticalOutput+ materialDifferential * materialModifier;
        long whitePawns = pieceBoard[10];
        long blackPawns = pieceBoard[11];
        long whitePassers = passedPawns(1);
        long whiteCandidates = passedPawns(2);
        long whiteHelpers = passedPawns(3);
        long blackPassers = passedPawns(4);
        long blackCandidates = passedPawns(5);
        long blackHelpers = passedPawns(6);
        while (whitePassers != 0) {
            int square = Long.numberOfTrailingZeros(whitePassers) + 1;
            subtotal += 0.5 * getY(square) * getY(square) * endgamePercent;
            whitePassers &= (whitePassers - 1);
        }
        while (whiteCandidates != 0) {
            int square = Long.numberOfTrailingZeros(whiteCandidates) + 1;
            subtotal += 0.3 * getY(square) * getY(square) * endgamePercent;
            whiteCandidates &= (whiteCandidates - 1);
        }
        while (whiteHelpers != 0) {
            int square = Long.numberOfTrailingZeros(whiteHelpers) + 1;
            subtotal += 0.2 * getY(square) * getY(square) * endgamePercent;
            whiteHelpers &= (whiteHelpers - 1);
        }
        while (blackPassers != 0) {
            int square = Long.numberOfTrailingZeros(blackPassers) + 1;
            subtotal -= 0.5 * (9 - getY(square)) * (9 - getY(square)) * endgamePercent;
            blackPassers &= (blackPassers - 1);
        }
        while (blackCandidates != 0) {
            int square = Long.numberOfTrailingZeros(blackCandidates) + 1;
            subtotal -= 0.3 * (9 - getY(square)) * (9 - getY(square)) * endgamePercent;
            blackCandidates &= (blackCandidates - 1);
        }
        while (blackHelpers != 0) {
            int square = Long.numberOfTrailingZeros(blackHelpers) + 1;
            subtotal -= 0.2 * (9 - getY(square)) * (9 - getY(square)) * endgamePercent;
            blackHelpers &= (blackHelpers - 1);
        }
        return (int) Math.round(subtotal);
    }
    static int activePieces() {
        //test if both sides have a lot of really active pieces
        //if true, then currentCount gives priority to piece activity, king safety, bishop pair, active rooks,
        //if not, give priority to space, outposts
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
    static void update(int square) {
        pieceActivity[square-1] = 0;
        material[square-1] = 0;
        centralControl[square-1] = 0;
        kingSafety[square-1] = 0;
        space[square-1] = 0;
        rooksSeventh[square-1] = 0;
        outposts[square-1] = 0;
        int piece = mailbox[square-1];
        if (piece == 0) return;
        int modifier = (piece % 2 == 1) ? 1 : -1;
        int sameColorPawn = (23 - modifier) / 2;
        long pieceVision = getBitboard(square-1, occupiedSquares(true) | occupiedSquares(false), piece);
        if ((pieceVision & (1L << 27)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 28)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 35)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        if ((pieceVision & (1L << 36)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = (byte) (centralControl[square-1] + modifier);
        material[square-1] = (byte) materialWorth(piece);
        if (piece == 1 || piece == 2) {
            double endgame = percentEndgame();
            double kingDistance = distanceToCenter[square-1];
            pieceActivity[square-1] = (byte) Math.round(modifier * 2 * (10 - kingDistance * 1.7) * endgame);
            int pawnShield = 0;
            if (piecePresent(sameColorPawn, translate(square, 0, modifier))) pawnShield += 10;
            if (piecePresent(sameColorPawn, translate(square, 1, modifier))) pawnShield += 5;
            if (piecePresent(sameColorPawn, translate(square, -1, modifier))) pawnShield += 5;
            kingSafety[square-1] = (byte) Math.round(modifier * pawnShield * (1 - endgame));
        }
        if (piece == 3 || piece == 4) {
            pieceActivity[square-1] = (byte) (modifier * Long.bitCount(pieceVision) / 2);
        } else if (piece == 5 || piece == 6 || piece == 7 || piece == 8) {
            pieceActivity[square-1] = (byte) (modifier * Long.bitCount(pieceVision));
        }  
        if (piece == 9 || piece == 10) {
            int equivalentY = 9 - getY(square);
            int equivalent = getX(square) + 8 * (equivalentY - 1);
            pieceActivity[square-1] = (byte) (modifier * knightBonus[(piece == 9) ? square-1 : equivalent-1]);
            if (getY(square) == ((piece == 9) ? 5 : 4) || getY(square) == ((piece == 9) ? 6 : 3)) {
                //knight outposts typically only apply on 5th and 6th rank (white), 4th and 3rd rank (black)
                if (getX(square) >= 3 && getX(square) <= 6) {
                    //center files, which are more valuable
                    if (piecePresent(sameColorPawn, translate(square, 1, -modifier))
                    || piecePresent(sameColorPawn, translate(square, -1, -modifier))) {
                        outposts[square-1] = (byte) (modifier * 8);
                    }
                } else {
                    // flank files
                    if (piecePresent(sameColorPawn, translate(square, 1, -modifier))
                    || piecePresent(sameColorPawn, translate(square, -1, -modifier))) {
                        outposts[square-1] = (byte) (modifier * 4);
                    }
                }
            }
        }
        if (piece == 5) {
            if (getY(square) == 7) rooksSeventh[square-1] = 15;
            if (getY(square) == 6 || getY(square) == 8) rooksSeventh[square-1] = 5;
        }
        if (piece == 6) {
            if (getY(square) == 2) rooksSeventh[square-1] = -15;
            if (getY(square) == 3 || getY(square) == 1) rooksSeventh[square-1] = -5;
        }
        if (piece == 7 || piece == 8) {
            int square1 = (piece == 7) ? 36 : 28;
            int square2 = (piece == 7) ? 37 : 29;
            if (square == square1 || square == square2) {
                if (piecePresent(sameColorPawn, translate(square, 1, -modifier))
                || piecePresent(sameColorPawn, translate(square, -1, -modifier))) {
                    outposts[square-1] = (byte) (modifier * 5);
                }
            }
        }
        if (piece == 11 || piece == 12) {
            int equivalent = (piece == 11) ? getY(square) : 9 - getY(square);
            double addition = equivalent * equivalent / 12.0; //flank files, a, b, g, h
            switch (getX(square)) {
                // central space is worth more
                case 3:
                case 6:
                    addition *= 0.75; // c and f files
                    break;
                case 4:
                case 5:
                    addition *= 1.7143; // center files: d and e
                    break;
                default:
                    break;
            }
            space[square-1] = (byte) Math.round(modifier * addition);
        }
    }
    static void updateEvaluation(long squares) {
        while (squares != 0) {
            update(Long.numberOfTrailingZeros(squares) + 1);
            squares &= (squares - 1);
        }
    }
    static void updateKingPawnEvaluation() {
        updateEvaluation(pieceBoard[0] | pieceBoard[1] | pieceBoard[10] | pieceBoard[11]);
    }
    static void updatePawnStructureEvaluation() {
        updateEvaluation(pieceBoard[0] | pieceBoard[1] | pieceBoard[6] | pieceBoard[7]
        | pieceBoard[8] | pieceBoard[9] | pieceBoard[10] | pieceBoard[11]);
    }
    static long occupiedSquares(boolean color) {
        return (color ? (
            pieceBoard[0] | pieceBoard[2] | pieceBoard[4] | pieceBoard[6] | pieceBoard[8] | pieceBoard[10]
        ) : (
            pieceBoard[1] | pieceBoard[3] | pieceBoard[5] | pieceBoard[7] | pieceBoard[9] | pieceBoard[11]
        ));
    }
    static boolean isEmpty(int square) {
        return ((occupiedSquares(true) | occupiedSquares(false)) & (1L << (square-1))) == 0;
    }
    static boolean piecePresent(int piece, int square) {
        if (square == 0) return false;
        return (pieceBoard[piece-1] & (1L << (square-1))) != 0;
    }
    static int sizeOfKingBox(int checkmateMode) {
        if ((checkmateMode > 0) != (position[0] == 1)) return 0;
        int enemyKing = Long.numberOfTrailingZeros(pieceBoard[1-position[0]]);
        long heavyPieces = (checkmateMode > 0) ? (pieceBoard[2] | pieceBoard[4]) : (pieceBoard[3] | pieceBoard[5]);
        long controlledBits = 0L;
        long heavyCopy = heavyPieces;
        while (heavyCopy != 0) {
            controlledBits |= getBitboard(Long.numberOfTrailingZeros(heavyCopy),
            occupiedSquares(true) | occupiedSquares(false), 5);
            heavyCopy &= (heavyCopy-1);
        }
        int currentX, currentY, currentSquare, sizeX = 1, sizeY = 1;
        long currentBit;
        int kingX = getX(enemyKing+1)-1;
        int kingY = getY(enemyKing+1)-1;
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
                currentX += rookDx[i];
                currentY += rookDy[i];
                currentSquare = currentY * 8 + currentX;
                currentBit = 1L << currentSquare;
            }
        }
        return sizeX * sizeY;
    }
    static void display() {
        char[] output = new char[64];
        for (int i = 0; i < 12; i++) {
            long bitboardCopy = pieceBoard[i];
            while (bitboardCopy != 0) {
                output[Long.numberOfTrailingZeros(bitboardCopy)] = getChar(i+1);
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
    static void displayStats() {
        int materialCount = 0, pieceActivityCount = 0, centralControlCount = 0,
        kingSafetyCount = 0, spaceCount = 0, rookCount = 0, outpostsCount = 0;
        long occupied = occupiedSquares(true) | occupiedSquares(false);
        for (int s = 0; s < 64; s++) {
            materialCount += material[s];
            pieceActivityCount += pieceActivity[s];
            centralControlCount += centralControl[s];
            kingSafetyCount += kingSafety[s];
            spaceCount += space[s];
            rookCount += rooksSeventh[s];
            outpostsCount += outposts[s];
            occupied &= (occupied-1);
        }
        System.out.print("Current zobrist hash: ");
        System.out.println(currentZobrist);
        System.out.println("In check? " + ((inCheck()) ? "yes" : "no"));
        System.out.print("Legal moves: ");

        int[] getMoves = sortMoves();
        for (int i = 0; i < getMoves.length; i++) {
            int start = extract(getMoves[i], 0);
            int end = extract(getMoves[i], 1);
            int x1 = getX(start);
            int y1 = getY(start);
            int x2 = getX(end);
            int y2 = getY(end);
            System.out.printf("%c%d%c%d", ((char)(x1 + 96)), y1, ((char)(x2 + 96)), y2);
            if (i < getMoves.length-1) System.out.print(", ");
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
        System.out.println(currentCount());
    }
    // Hot paths!
    static long legalMovesBasic(int square) {
        long output = 0L;
        int piece = mailbox[square-1];
        boolean color = color(piece);
        if (piece == 11) {
            //pawn
            int candidate = translate(square, 1, 1);
            int occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && color(occupied) != color && occupied != 2) {
                output |= (1L << (candidate-1));
            }
            candidate = translate(square, -1, 1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && color(occupied) != color && occupied != 2) {
                output |= (1L << (candidate-1));
            }
            candidate = translate(square, 0, 1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied == 0) {
                output |= (1L << (candidate-1));
                candidate = translate(square, 0, 2);
                occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
                if (candidate != 0 && occupied == 0 && square > 8 && square <= 16) {
                    output |= (1L << (candidate-1));
                }
            }
            if (position[5] != 0 && ((occupiedSquares(true) | occupiedSquares(false)) & (1L << (position[5]-1))) == 0 &&
                    (position[5] == translate(square, 1, 1) || position[5] == translate(square, -1, 1))) {
                output |= (1L << (position[5]-1));
            }
        } else if (piece == 12) {
            //pawn
            int candidate = translate(square, 1, -1);
            int occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && color(occupied) != color && occupied != 1) {
                output |= (1L << (candidate-1));
            }
            candidate = translate(square, -1, -1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied != 0 && color(occupied) != color && occupied != 1) {
                output |= (1L << (candidate-1));
            }
            candidate = translate(square, 0, -1);
            occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
            if (candidate != 0 && occupied == 0) {
                output |= (1L << (candidate-1));
                candidate = translate(square, 0, -2);
                occupied = (candidate == 0) ? 0 : mailbox[candidate-1];
                if (occupied == 0 && square > 48 && square <= 56) {
                    output |= (1L << (candidate-1));
                }
            }
            if (position[5] != 0 && ((occupiedSquares(true) | occupiedSquares(false)) & (1L << (position[5]-1))) == 0 &&
                    (position[5] == translate(square, 1, -1) || position[5] == translate(square, -1, -1))) {
                output |= (1L << (position[5]-1));
            }
        } else {
            long squares = getBitboard(square-1, occupiedSquares(true) | occupiedSquares(false), piece);
            while (squares != 0) {
                int p = mailbox[Long.numberOfTrailingZeros(squares)];
                if (p == 0 || (color(p) != color && p != 1 && p != 2)) {
                    output |= squares & (-squares);
                }
                squares &= (squares-1);
            }
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
                        output |= (1L << 6);
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
                        output |= (1L << 2);
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
                        output |= (1L << 62);
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
                        output |= (1L << 58);
                    }
                }
            }
        }
        return output;
    }
    static void makeMove(int move) {
        int start = extract(move, 0);
        int end = extract(move, 1);
        int piece = mailbox[start-1];
        int captured = mailbox[end-1];
        int epSquare = position[5];
        boolean tacticalMove = isTacticalMove(move);
        boolean pawnStructureChanged = piece == 11 || piece == 12
        || captured == 11 || captured == 12;
        undoStackTactical[undoDeep] = tacticalMove;
        undoStackPawnStructureChanged[undoDeep] = pawnStructureChanged;
        undoStackZobrist[undoDeep] = currentZobrist;
        System.arraycopy(pieceBoard, 0, undoStackPieces[undoDeep], 0, 12);
        System.arraycopy(mailbox, 0, undoStackMailboxes[undoDeep], 0, 64);
        System.arraycopy(position, 0, undoStackData[undoDeep], 0, 7);
        undoDeep++;
        if (position[5] != 0 && piece != 11 && piece != 12) {
            currentZobrist ^= zobrist[position[5]-1][12];
            position[5] = 0;
        }
        position[6]++;
        if (captured != 0) {
            position[6] = 0;
            if (end == 1 && captured == 5 && position[2] != 0) {
                position[2] = 0;
                currentZobrist ^= zobristWCL;
            }
            if (end == 8 && captured == 5) {
                position[1] = 0;
                currentZobrist ^= zobristWCS;
            }
            if (end == 57 && captured == 6) {
                position[4] = 0;
                currentZobrist ^= zobristBCL;
            }
            if (end == 64 && captured == 6) {
                position[3] = 0;
                currentZobrist ^= zobristBCS;
            }
            pieceBoard[captured-1] ^= (1L << (end - 1));
            currentZobrist ^= zobrist[end-1][captured-1];
        }
        pieceBoard[piece-1] |= (1L << (end - 1));
        pieceBoard[piece-1] ^= (1L << (start - 1));
        mailbox[end-1] = (byte) piece;
        mailbox[start-1] = 0;
        currentZobrist ^= zobrist[start-1][piece-1];
        currentZobrist ^= zobrist[end-1][piece-1];
        switch (piece) {
            case 1:
                if (position[1] != 0) {
                    position[1] = 0;
                    currentZobrist ^= zobristWCS;
                }
                if (position[2] != 0) {
                    position[2] = 0;
                    currentZobrist ^= zobristWCL;
                }
                if (start == 5 && end == 3) {
                    //white castle long
                    pieceBoard[4] |= (1L << 3);
                    pieceBoard[4] ^= 1L;
                    mailbox[3] = 5;
                    mailbox[0] = 0;
                    currentZobrist ^= zobrist[3][4];
                    currentZobrist ^= zobrist[0][4];
                }
                if (start == 5 && end == 7) {
                    //white castle short
                    pieceBoard[4] |= (1L << 5);
                    pieceBoard[4] ^= (1L << 7);
                    mailbox[5] = 5;
                    mailbox[7] = 0;
                    currentZobrist ^= zobrist[5][4];
                    currentZobrist ^= zobrist[7][4];
                }
                break;
            case 2:
                if (position[3] != 0) {
                    position[3] = 0;
                    currentZobrist ^= zobristBCS;
                }
                if (position[4] != 0) {
                    position[4] = 0;
                    currentZobrist ^= zobristBCL;
                }
                if (start == 61 && end == 59) {
                    //black castle long
                    pieceBoard[5] |= (1L << 59);
                    pieceBoard[5] ^= (1L << 56);
                    mailbox[59] = 6;
                    mailbox[56] = 0;
                    currentZobrist ^= zobrist[59][5];
                    currentZobrist ^= zobrist[56][5];
                }
                if (start == 61 && end == 63) {
                    //black castle short
                    pieceBoard[5] |= (1L << 61);
                    pieceBoard[5] ^= (1L << 63);
                    mailbox[61] = 6;
                    mailbox[63] = 0;
                    currentZobrist ^= zobrist[61][5];
                    currentZobrist ^= zobrist[63][5];
                }
                break;
            case 5:
                if (start == 1 && position[2] != 0) {
                    //a1
                    position[2] = 0;
                    currentZobrist ^= zobristWCL;
                } else if (start == 8 && position[1] != 0) {
                    //h1
                    position[1] = 0;
                    currentZobrist ^= zobristWCS;
                }
                break;
            case 6:
                if (start == 57 && position[4] != 0) {
                    //a8
                    position[4] = 0;
                    currentZobrist ^= zobristBCL;
                } else if (start == 64 && position[3] != 0) {
                    //h8
                    position[3] = 0;
                    currentZobrist ^= zobristBCS;
                }
                break;
            case 11:
                //white pawn
                //worry about en passant and promotion, assume all promotions go to queens
                if (end == position[5]) {
                    //en passant!
                    int squareid = translate(end, 0, -1) - 1;
                    pieceBoard[11] ^= (1L << squareid);
                    mailbox[squareid] = 0;
                    currentZobrist ^= zobrist[squareid][11];
                }
                if (position[5] != 0) {
                    currentZobrist ^= zobrist[position[5]-1][12];
                    position[5] = 0;
                }
                if (start > 8 && start <= 16 && end > 24 && end <= 32) {
                    //pawn move 2 squares, check for en passant
                    if (piecePresent(12, translate(end, 1, 0)) || piecePresent(12, translate(end, -1, 0))) {
                        position[5] = translate(start, 0, 1);
                        currentZobrist ^= zobrist[position[5]-1][12];
                    }
                }
                if (start > 48 && start <= 56) {
                    //promotion
                    pieceBoard[10] ^= (1L << end-1);
                    pieceBoard[2] |= (1L << end-1);
                    mailbox[end-1] = 3;
                    currentZobrist ^= zobrist[end-1][10];
                    currentZobrist ^= zobrist[end-1][2];
                }
                position[6] = 0;
                break;
            case 12:
                //black pawn
                if (end == position[5]) {
                    //en passant!
                    int squareid = translate(end, 0, 1) - 1;
                    pieceBoard[10] ^= (1L << squareid);
                    mailbox[squareid] = 0;
                    currentZobrist ^= zobrist[squareid][10];
                }
                if (position[5] != 0) {
                    currentZobrist ^= zobrist[position[5]-1][12];
                    position[5] = 0;
                }
                if (start > 48 && start <= 56 && end > 32 && end <= 40) {
                    //pawn move 2 squares, check for en passant
                    if (piecePresent(11, translate(end, 1, 0)) || piecePresent(11, translate(end, -1, 0))) {
                        position[5] = translate(start, 0, -1);
                        currentZobrist ^= zobrist[position[5]-1][12];
                    }
                }
                if (start > 8 && start <= 16) {
                    //promotion
                    pieceBoard[11] ^= (1L << end-1);
                    pieceBoard[3] |= (1L << end-1);
                    mailbox[end-1] = 4;
                    currentZobrist ^= zobrist[end-1][11];
                    currentZobrist ^= zobrist[end-1][3];
                }
                position[6] = 0;
                break;
            default:
                break;
        }
        position[0] = 1 - position[0];
        currentZobrist ^= whiteToMove;
        currentZobrist ^= blackToMove;
        update(start);
        update(end);
        if (start == 5 && end == 3) {
            update(1);
            update(4);
        }
        if (start == 5 && end == 7) {
            update(6);
            update(8);
        }
        if (start == 61 && end == 59) {
            //black castle long
            update(57);
            update(60);
        }
        if (start == 61 && end == 63) {
            //black castle short
            update(62);
            update(64);
        }
        if (pawnStructureChanged) updatePawnStructureEvaluation();
        else if (tacticalMove) updateKingPawnEvaluation();
    }
    static void restore() {
        undoDeep--;
        boolean tacticalMove = undoStackTactical[undoDeep];
        boolean pawnStructureChanged = undoStackPawnStructureChanged[undoDeep];
        long[] bitboardCopy = undoStackPieces[undoDeep];
        long changedSquares = 0L;
        for (int i = 0; i < 12; i++) changedSquares |= (bitboardCopy[i] ^ pieceBoard[i]);
        System.arraycopy(bitboardCopy, 0, pieceBoard, 0, 12);
        System.arraycopy(undoStackMailboxes[undoDeep], 0, mailbox, 0, 64);
        System.arraycopy(undoStackData[undoDeep], 0, position, 0, 7);
        currentZobrist = undoStackZobrist[undoDeep];
        updateEvaluation(changedSquares);
        if (pawnStructureChanged) updatePawnStructureEvaluation();
        else if (tacticalMove) updateKingPawnEvaluation();
    }
    static long legalMoves(int square, long checkingPieces, long opposingAttackMap, long[] absolutePinRestrictions) {
        int piece = mailbox[square-1];
        int checkStatus = Long.bitCount(checkingPieces);
        long occupied = occupiedSquares(true) | occupiedSquares(false);
        long opposingPieces = occupiedSquares(position[0] == 0);
        if (checkStatus > 1) {
            if (piece != 1 && piece != 2) return 0L;
            return getBitboard(square-1, occupied, piece) & (~opposingAttackMap);
        }
        int origin = Long.numberOfTrailingZeros(checkingPieces);
        int checkingPiece = (checkStatus == 0) ? 0 : mailbox[origin];
        long friendlyPieces = occupiedSquares(position[0] == 1);
        long basic = legalMovesBasic(square);
        long out = basic & absolutePinRestrictions[square-1];
        if (checkStatus == 1) return out & buildCheckRay(origin + 1, mailbox[origin]);
        if (piece == 1 || piece == 2) return basic & (~opposingAttackMap);
        if (position[5] != 0) {
            //dealing with the "en passant bug"
            //for example rnb1kbnr/ppp1pppp/8/2KpPq2/8/8/PPPP1PPP/RNBQ1BNR w kq - 0 1
            if (piece == 11) {
                pieceBoard[10] ^= 1L << (square-1);
                pieceBoard[10] |= 1L << (position[5]-1);
                pieceBoard[11] ^= 1L << (position[5]-9);
                if (inCheck()) out &= (~(1L << (position[5]-1)));
                else out |= 1L << (position[5]-1);
                pieceBoard[10] |= 1L << (square-1);
                pieceBoard[10] ^= 1L << (position[5]-1);
                pieceBoard[11] |= 1L << (position[5]-9);
            }
            if (piece == 12) {
                pieceBoard[11] ^= 1L << (square-1);
                pieceBoard[11] |= 1L << (position[5]-1);
                pieceBoard[10] ^= 1L << (position[5]+7);
                if (inCheck()) out &= (~(1L << (position[5]-1)));
                else out |= 1L << (position[5]-1);
                pieceBoard[11] |= 1L << (square-1);
                pieceBoard[11] ^= 1L << (position[5]-1);
                pieceBoard[10] |= 1L << (position[5]+7);
            }
        }
        return out;
    }
    static boolean inCheck() {
        long kingBit = pieceBoard[1 - position[0]];
        long attackMap = getCompositeBitboard(occupiedSquares(position[0] == 0),
        occupiedSquares(true) | occupiedSquares(false));
        return (kingBit & attackMap) != 0;
    }
    static long checkPieces() {
        //returns checking pieces
        long occupied = occupiedSquares(true) | occupiedSquares(false);
        long output = 0L;
        long opposingPieces = occupiedSquares(position[0] == 0);
        long kingBit = pieceBoard[1 - position[0]];
        while (opposingPieces != 0) {
            int squareid = Long.numberOfTrailingZeros(opposingPieces);
            if ((getBitboard(squareid, occupied, mailbox[squareid]) & kingBit) != 0) output |= Long.lowestOneBit(opposingPieces);
            opposingPieces &= (opposingPieces - 1);
        }
        return output;
    }
    static int calculateScore(int move) {
        int start = extract(move, 0);
        int end = extract(move, 1);
        int piece = mailbox[start-1];
        int t = getTranspositionIndex();
        if (t != -1) {
            if (bestMoves[t] == move) return Integer.MAX_VALUE;
        }
        boolean pieceColor = color(piece);
        int modifier = (pieceColor) ? 1 : -1;
        double pieceModifier = 1.0d; //helps prioritize certain pieces over others
        switch (piece) {
            case 1: case 2: pieceModifier = 0.7; break;
            case 3: case 4: pieceModifier = 0.8; break;
            case 7: case 8: case 9: case 10: pieceModifier = 1.2; break;
            case 11: case 12: pieceModifier = 0.9; break;
        }
        int captured = mailbox[end-1];
        boolean capture = captured != 0 || ((piece == 11 || piece == 12) && end == position[5]);
        boolean promotion = (piece == 11 && end >= 57) || (piece == 12 && end <= 8);
        //capture/promotion
        if (capture || promotion) {
            int difference = 0;
            if (capture) {
                //most valuable victim, least valuable attacker
                int victimWorth = (captured == 0) ? 10 : Math.abs(materialWorth(captured));
                difference += (piece == 1 || piece == 2) ? 0 : 100 + victimWorth - Math.abs(materialWorth(piece));
            }
            if (promotion) {
                difference += 180;
            }
            //max of difference is 360, for "pawn takes queen promotes to queen"
            return Integer.MAX_VALUE - 361 + difference;
        }
        int neededIndex = ((start - 1) >> 64) | (end - 1);
        //killer moves heuristic
        if (killerMoves[neededIndex]) return Integer.MAX_VALUE - 362;
        //history heuristic
        int timesAppeared = history[neededIndex];
        if (timesAppeared != 0) return (Integer.MAX_VALUE - 373) + timesAppeared;
        //quiet moves not principal or in killer moves or in history,
        //counting up from 0 as opposed to counting down from 2147483647
        int cumulative = 0;
        //reward center-seeking moves
        double startProximity = distanceToCenter[start-1];
        double endProximity = distanceToCenter[end-1];
        int centerGain = (int) Math.round(startProximity - endProximity);
        if (piece != 1 && piece != 2) cumulative += centerGain;
        //reward forward moves
        cumulative += (pieceColor) ? getY(end) - getY(start) : getY(start) - getY(end);
        if (piece == 11 || piece == 12) {
            //reward pawn moves close to promotion
            boolean farAdvanced = (pieceColor) ? getY(end) >= 6 : getY(end) <= 3;
            if (farAdvanced) cumulative += 20;
            //punish pawn moves in front of king if it's not the endgame
            int king = (pieceColor) ? 1 : 2;
            if (piecePresent(king, translate(start, 0, -modifier)))
            cumulative -= (int) Math.round(20 * (1 - percentEndgame()));
        }
        if (piece == 5 || piece == 6) {
            if (getY(end) == ((pieceColor) ? 7 : 2)) cumulative += 25;
            if (getY(end) == ((pieceColor) ? 8 : 1) || getY(end) == ((pieceColor) ? 6 : 3)) cumulative += 10;
        }
        return (int) Math.round(cumulative * pieceModifier);
    }
    static int[] generateTacticalMoves() {
        long squares = occupiedSquares(position[0] == 1);
        long opposingAttackMap = getCompositeBitboard(occupiedSquares(position[0] == 0),
        occupiedSquares(true) | occupiedSquares(false));
        long check = checkPieces();
        if (Long.bitCount(check) > 1) Arrays.fill(absolutePinRestrictions, -1L);
        else {
            int checkingPiece = (check == 0) ? 0 : mailbox[Long.numberOfTrailingZeros(check)];
            long friendlyPieces = squares & (~(pieceBoard[0] | pieceBoard[1]));
            while (friendlyPieces != 0) {
                int pieceSquare = Long.numberOfTrailingZeros(friendlyPieces);
                int pieceid = mailbox[pieceSquare];
                pieceBoard[pieceid-1] ^= (1L << pieceSquare);
                if (check != 0) pieceBoard[checkingPiece-1] ^= check;
                long newCheckingPiece = checkPieces();
                if (newCheckingPiece != 0) {
                    int newSquare = Long.numberOfTrailingZeros(newCheckingPiece);
                    absolutePinRestrictions[pieceSquare] = buildCheckRay(newSquare + 1, mailbox[newSquare]);
                } else absolutePinRestrictions[pieceSquare] = -1L;
                pieceBoard[pieceid-1] |= (1L << pieceSquare);
                if (check != 0) pieceBoard[checkingPiece-1] |= check;
                friendlyPieces &= (friendlyPieces - 1);
            }
        }
        int quantity = 0;
        for (int square = 0; square < 64; square++) {
            if ((squares & (1L << square)) == 0) moveBoards[square] = 0L;
            else {
                long allMoves = legalMoves(square+1, check, opposingAttackMap, absolutePinRestrictions);
                while (allMoves != 0) {
                    int move = compress(square+1, Long.numberOfTrailingZeros(allMoves)+1, mailbox[square]);
                    if (isTacticalMove(move)) moves[quantity++] = move;
                    allMoves &= (allMoves - 1);
                }
            }
        }
        return Arrays.copyOf(moves, quantity);
    }
    static int[] sortMoves() {
        long squares = occupiedSquares(position[0] == 1);
        long opposingAttackMap = getCompositeBitboard(occupiedSquares(position[0] == 0),
        occupiedSquares(true) | occupiedSquares(false));
        long check = checkPieces();
        if (Long.bitCount(check) > 1) Arrays.fill(absolutePinRestrictions, -1L);
        else {
            int checkingPiece = (check == 0) ? 0 : mailbox[Long.numberOfTrailingZeros(check)];
            long friendlyPieces = squares & (~(pieceBoard[0] | pieceBoard[1]));
            while (friendlyPieces != 0) {
                int pieceSquare = Long.numberOfTrailingZeros(friendlyPieces);
                int pieceid = mailbox[pieceSquare];
                pieceBoard[pieceid-1] ^= (1L << pieceSquare);
                if (check != 0) pieceBoard[checkingPiece-1] ^= check;
                long newCheckingPiece = checkPieces();
                if (newCheckingPiece != 0) {
                    int newSquare = Long.numberOfTrailingZeros(newCheckingPiece);
                    absolutePinRestrictions[pieceSquare] = buildCheckRay(newSquare + 1, mailbox[newSquare]);
                } else absolutePinRestrictions[pieceSquare] = -1L;
                pieceBoard[pieceid-1] |= (1L << pieceSquare);
                if (check != 0) pieceBoard[checkingPiece-1] |= check;
                friendlyPieces &= (friendlyPieces - 1);
            }
        }
        int quantity = 0;
        for (int square = 0; square < 64; square++) {
            if ((squares & (1L << square)) == 0) moveBoards[square] = 0L;
            else {
                long allMoves = legalMoves(square+1, check, opposingAttackMap, absolutePinRestrictions);
                while (allMoves != 0) {
                    moves[quantity++] = compress(square+1, Long.numberOfTrailingZeros(allMoves)+1, mailbox[square]);
                    allMoves &= (allMoves - 1);
                }
            }
        }
        long[] scoredMoves = new long[quantity];
        for (int j = 0; j < quantity; j++) {
            int score = calculateScore(moves[j]);
            scoredMoves[j] = ((long) (~score) << 32) | (moves[j] & 0xffffffffL);
        }
        Arrays.sort(scoredMoves);
        for (int j = 0; j < quantity; j++) moves[j] = (int) scoredMoves[j];
        return Arrays.copyOf(moves, quantity);
    }
    static boolean isTacticalMove(int move) {
        int start = extract(move, 0);
        int end = extract(move, 1);
        int piece = extract(move, 2);
        return !isEmpty(end) || ((piece == 11 || piece == 12) && end == position[5])
        || (piece == 11 && end >= 57) || (piece == 12 && end <= 8);
    }
    static boolean sufficientMaterial() {
        int whiteKnights = Long.bitCount(pieceBoard[8]), blackKnights = Long.bitCount(pieceBoard[9]),
        whitePieces = Long.bitCount(occupiedSquares(true)), blackPieces = Long.bitCount(occupiedSquares(false));
        if ((pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5] | pieceBoard[10] | pieceBoard[11]) != 0) return true;
        if (whitePieces == 2 && whiteKnights < 2) return true;
        if (blackPieces == 2 && blackKnights < 2) return true;
        return false;
    }
    static boolean pawnEndgame() {
        //if true, zugzwang may happen at some point -> cancel the null move prune
        return (pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]
        | pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) == 0;
    }
    static double percentEndgame() {
        int endgameScore = 0; //the higher, the less of an endgame it is, above 1000 it's a middlegame
        endgameScore += Long.bitCount(pieceBoard[2] | pieceBoard[3]) * 400; //queens
        endgameScore += Long.bitCount(pieceBoard[4] | pieceBoard[5]) * 200; //rooks
        endgameScore += Long.bitCount(pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) * 50; //bishops and knights
        return 1.0 - Math.min(1000.0, endgameScore) / 1000.0;
    }
    static long passedPawns(int type) {
        /*
        1 = white passed pawns, 2 = black passed pawns, 3 = white passer candidates,
        4 = black passer candidates, 5 = white helpers, 6 = black helpers
        */
        for (int i = 0; i < 8; i++) {
            whitePawnFiles[i] = (pieceBoard[10] & fileref[i]) != 0;
            blackPawnFiles[i] = (pieceBoard[11] & fileref[i]) != 0;
        }
        long output = 0L;
        int helpingNeighbors = 0, opposingNeighbors = 0;
        for (int i = 0; i < 8; i++) {
            if (type % 2 == 1 && whitePawnFiles[i] && !blackPawnFiles[i]) {
                if (i != 0) {
                    if (whitePawnFiles[i-1]) helpingNeighbors++;
                    if (blackPawnFiles[i-1]) opposingNeighbors++;
                }
                if (i != 7) {
                    if (whitePawnFiles[i+1]) helpingNeighbors++;
                    if (blackPawnFiles[i+1]) opposingNeighbors++;
                }
                if (type == 1 && opposingNeighbors == 0) output |= (pieceBoard[10] & fileref[i]);
                else if ((type == 3 || type == 5) && helpingNeighbors > opposingNeighbors) {
                    if (type == 3) output |= (pieceBoard[10] & fileref[i]);
                    else if (type == 5) {
                        if (i != 0 && whitePawnFiles[i-1]) output |= (pieceBoard[10] & fileref[i-1]);
                        if (i != 7 && whitePawnFiles[i+1]) output |= (pieceBoard[10] & fileref[i+1]);
                    }
                }
            } else if (type % 2 == 0 && blackPawnFiles[i] && !whitePawnFiles[i]) {
                if (i != 0) {
                    if (blackPawnFiles[i-1]) helpingNeighbors++;
                    if (whitePawnFiles[i-1]) opposingNeighbors++;
                }
                if (i != 7) {
                    if (blackPawnFiles[i+1]) helpingNeighbors++;
                    if (whitePawnFiles[i+1]) opposingNeighbors++;
                }
                if (type == 2 && opposingNeighbors == 0) output |= (pieceBoard[11] & fileref[i]);
                else if ((type == 4 || type == 6) && helpingNeighbors > opposingNeighbors) {
                    if (type == 4) output |= (pieceBoard[11] & fileref[i]);
                    else if (type == 6) {
                        if (i != 0 && blackPawnFiles[i-1]) output |= (pieceBoard[11] & fileref[i-1]);
                        if (i != 7 && blackPawnFiles[i+1]) output |= (pieceBoard[11] & fileref[i+1]);
                    }
                }
            }
        }
        return output;
    }
    static int simpleCheckmate() {
        /*
        0 = no, 1 = ladder - white, 2 = white with queen, 3 = white with rook,
        -1 = ladder - black, -2 = black with queen, -3 = black with rook
        */
        int whiteQueens = Long.bitCount(pieceBoard[2]), whiteRooks = Long.bitCount(pieceBoard[4]),
        blackQueens = Long.bitCount(pieceBoard[3]), blackRooks = Long.bitCount(pieceBoard[5]);
        int whiteHeavyPieces = whiteQueens + whiteRooks, blackHeavyPieces = blackQueens + blackRooks;
        if ((pieceBoard[6] | pieceBoard[7] | pieceBoard[8]
        | pieceBoard[9] | pieceBoard[10] | pieceBoard[11]) != 0) return 0;
        if (whiteHeavyPieces > 0 && blackHeavyPieces > 0) return 0;
        if (whiteHeavyPieces > 1) return 1;
        if (blackHeavyPieces > 1) return -1;
        if (whiteQueens == 1) return 2;
        if (whiteRooks == 1) return 3;
        if (blackQueens == 1) return -2;
        if (blackRooks == 1) return -3;
        return 0;
    }
    static int evalQ(int alpha, int beta) {
        int checkmateMode = simpleCheckmate();
        if (checkmateMode != 0) {
            nodes = Integer.MAX_VALUE / 2;
            return (checkmateMode > 0)
                ? ((Integer.MAX_VALUE / 2) - sizeOfKingBox(checkmateMode))
                : ((Integer.MIN_VALUE / 2) + sizeOfKingBox(checkmateMode));
        } //the smaller the box, the better
        nodes++;
        boolean maximizing = position[0] == 1;
        boolean checked = inCheck();
        updateEvaluation(pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5] | pieceBoard[6] | pieceBoard[7]);
        int best = currentCount();
        if (!checked) {
            if (maximizing) {
                if (best >= beta) return best;
                alpha = Math.max(alpha, best);
            } else {
                if (best <= alpha) return best;
                beta = Math.min(beta, best);
            }
        }
        //if checked, consider all legal moves, if not, only consider tactical moves
        int[] moves = (inCheck()) ? sortMoves() : generateTacticalMoves();
        for (int i = 0; i < moves.length; i++) {
            makeMove(moves[i]);
            int potential;
            potential = evalQ(alpha, beta);
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
    static long evalDepth(int depth, int alpha, int beta, long previousResult) {
        if (previousResult != baseLeftBit) {
            int evalOnly = (int)(previousResult >> 32);
            int lower = (Integer.MIN_VALUE + 50 < evalOnly) ? evalOnly - 50 : Integer.MIN_VALUE;
            //in case previousResult[0] is less than 50 away from Integer.MIN_VALUE
            int higher = (Integer.MAX_VALUE - 50 > evalOnly) ? evalOnly + 50 : Integer.MAX_VALUE;
            //in case previousResult[0] is less than 50 away from Integer.MAX_VALUE
            long shallowSearch = evalDepth(depth, lower, higher, baseLeftBit);
            if ((shallowSearch >> 32) < higher && (shallowSearch >> 32) > lower) return shallowSearch;
        }
        nodes++;
        if (depth == maxDepth) Arrays.fill(killerMoves, false);
        int alphaOriginal = alpha;
        int betaOriginal = beta;
        int bound = 0;
        int t = getTranspositionIndex();
        if (t != -1) {
            if (depthsSearched[t] >= depth) {
                if (bounds[t] == 0) return (((long)evals[t]) << 32) | (bestMoves[t] & rightBitsOnly);
                if (bounds[t] == -1) {
                    if (evals[t] >= beta) return (((long)evals[t]) << 32) | (bestMoves[t] & rightBitsOnly);
                    alpha = Math.max(alpha, evals[t]);
                }
                if (bounds[t] == 1) {
                    if (evals[t] <= alpha) return (((long)evals[t]) << 32) | (bestMoves[t] & rightBitsOnly);
                    beta = Math.min(beta, evals[t]);
                }
                if (alpha >= beta) {
                    if (!isTacticalMove(bestMoves[t])) storeQuietMove(bestMoves[t], depth);
                    return (((long)evals[t]) << 32) | (bestMoves[t] & rightBitsOnly);
                }
            }
        }
        boolean maximizing = position[0] == 1;
        //null move pruning
        boolean hasNullMoveBound = maximizing ? beta < Integer.MAX_VALUE : alpha > Integer.MIN_VALUE;
        if (depth > 2 && hasNullMoveBound && !pawnEndgame()
        && !inCheck() && depth < maxDepth && activePieces() > 2) { //don't want to do this at the root node
            long evalPass;
            int enPassantSquare = position[5];
            position[0] = 1 - position[0]; //make a null move
            position[5] = 0;
            currentZobrist ^= whiteToMove;
            currentZobrist ^= blackToMove;
            if (enPassantSquare != 0) currentZobrist ^= zobrist[enPassantSquare-1][12];
            int nullDepth = depth - 3;
            evalPass = maximizing
            ? evalDepth(nullDepth, beta - 1, beta, baseLeftBit)
            : evalDepth(nullDepth, alpha, alpha + 1, baseLeftBit);
            position[0] = 1 - position[0]; //restore original position
            position[5] = enPassantSquare;
            currentZobrist ^= whiteToMove;
            currentZobrist ^= blackToMove;
            if (enPassantSquare != 0) currentZobrist ^= zobrist[enPassantSquare-1][12];
            if (maximizing ? (evalPass >> 32) >= beta : (evalPass >> 32) <= alpha) {
                return ((long)(maximizing ? beta : alpha)) << 32;
            }
        }
        if (depth == 0) return ((long)evalQ(alpha, beta)) << 32;
        //aspiration window
        int[] moves = sortMoves();
        if (moves.length == 0) {
            if (inCheck()) return (position[0] == 1) ? Long.MIN_VALUE : leftBitsOnly;
            return 0;
        }
        int evaluation = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestMove = 0;
        for (int i = 0; i < moves.length; i++) {
            makeMove(moves[i]);
            int potential;
            //principal variation search
            //search the first move fully, then search subsequent moves with zero window and do a full search if it performs well
            if (i == 0) {
                potential = (int)(evalDepth(depth - 1, alpha, beta, baseLeftBit) >> 32);
            } else {
                if (maximizing) {
                    int trial = (int)(evalDepth(depth - 1, alpha, alpha + 1, baseLeftBit) >> 32);
                    potential = (trial > alpha) ? (int)(evalDepth(depth - 1, alpha, beta, baseLeftBit) >> 32) : trial;
                } else {
                    int trial = (int)(evalDepth(depth - 1, beta - 1, beta, baseLeftBit) >> 32);
                    potential = (trial < beta) ? (int)(evalDepth(depth - 1, alpha, beta, baseLeftBit) >> 32) : trial;
                }
            }
            restore();
            if (maximizing) {
                if (potential > evaluation) {
                    evaluation = potential;
                    bestMove = moves[i];
                }
                alpha = Math.max(alpha, evaluation);
            } else {
                if (potential < evaluation) {
                    evaluation = potential;
                    bestMove = moves[i];
                }
                beta = Math.min(beta, evaluation);
            }
            if (alpha >= beta) {
                if (!isTacticalMove(moves[i]))
                storeQuietMove(moves[i], depth);
                break;
            }
        }
        bound = evaluation <= alphaOriginal ? 1 : evaluation >= betaOriginal ? -1 : 0;
        store(evaluation, bestMove, depth, bound);
        return (((long) evaluation) << 32) | bestMove;
    }
    static int run() {
        System.out.print("Initializing... ");
        Random generator = new Random(-0x382E4DFBA50BC32FL);
        for (int i = 0; i < 64; i++) {
            long[] rookAddition = new long[1 << Long.bitCount(rookVision[i])];
            long[] bishopAddition = new long[1 << Long.bitCount(bishopVision[i])];
            for (int index = 0; index < rookAddition.length; index++) rookAddition[index] = fillWithRook(i, Long.expand(index, rookVision[i]));
            for (int index = 0; index < bishopAddition.length; index++) bishopAddition[index] = fillWithBishop(i, Long.expand(index, bishopVision[i]));
            rookAttacks[i] = rookAddition;
            bishopAttacks[i] = bishopAddition;
            //zobrist
            for (int j = 0; j < 13; j++) zobrist[i][j] = generator.nextLong();
        }
        empty = generator.nextLong();
        whiteToMove = generator.nextLong();
        blackToMove = generator.nextLong();
        zobristWCS = generator.nextLong(); //white castle short
        zobristWCL = generator.nextLong(); //white castle long
        zobristBCS = generator.nextLong(); //black castle short
        zobristBCL = generator.nextLong();
        currentZobrist = empty;
        for (int i = 0; i < 8; i++) {
            whitePawnFiles[i] = (pieceBoard[10] & fileref[i]) != 0;
            blackPawnFiles[i] = (pieceBoard[11] & fileref[i]) != 0;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("done");
        int[] setup = new int[72];
        System.out.print("FEN: ");
        char[] fen = new char[256];

        String input = scanner.nextLine(); input.getChars(0, input.length(), fen, 0);

        int i = 0, element = 1, row = 8, column = 1;
        char current = fen[i];
        while (current != '\0') {
            if (current == ' ') {
                element++;
                current = fen[++i];
                continue;
            }
            switch (element) {
                case 1:
                    if (current == '/') {
                        row--;
                        column = 1;
                    } else if (Character.isDigit(current)) {
                        column += (int)(current - '0');
                    } else {
                        int id = getId(current);
                        if (id == 0) return 1;
                        int square = (row - 1) * 8 + column;
                        mailbox[square-1] = (byte) id;
                        pieceBoard[id-1] |= (1L << (square-1));
                        currentZobrist ^= zobrist[square-1][id-1];
                        column++;
                    }
                    break;
                case 2:
                    if (current == 'w') {
                        position[0] = 1;
                        currentZobrist ^= whiteToMove;
                    } else if (current == 'b') {
                        position[0] = 0;
                        currentZobrist ^= blackToMove;
                    } else return 2;
                    break;
                case 3:
                    switch (current) {
                        case '-': break;
                        case 'K': position[1] = 1; currentZobrist ^= zobristWCS; break;
                        case 'Q': position[2] = 1; currentZobrist ^= zobristWCL; break;
                        case 'k': position[3] = 1; currentZobrist ^= zobristBCS; break;
                        case 'q': position[4] = 1; currentZobrist ^= zobristBCL; break;
                        default: return 3;
                    }
                    break;
                case 4:
                    switch (current) {
                        case '-': break;
                        case 'a': case 'b': case 'c':
                        case 'd': case 'e': case 'f':
                            position[5] += (int)(current - '`');
                            current = fen[++i];
                            if (current == '3') position[5] += 16;
                            else if (current == '6') position[5] += 40;
                            else return 4;
                            currentZobrist ^= zobrist[position[5]-1][12];
                            break;
                        default: return 4;
                    }
                    break;
                case 5:
                    if (!Character.isDigit(current)) return 5;
                    if (fen[i+1] == ' ') position[6] = (int)(current - '0');
                    else {
                        position[6] = (int)(current - '0');
                        position[6] *= 10;
                        current = fen[++i];
                        position[6] = (int)(current - '0');
                    }
                    break;
                default: break;
            }
            current = fen[++i];
        }
        updateEvaluation(occupiedSquares(true) | occupiedSquares(false));
        System.out.print("Show stats? ");
        if (scanner.nextBoolean()) {
            display();
            displayStats();    
        }
        long pieceSquares = occupiedSquares(position[0] == 1);
        if (!sufficientMaterial() || position[6] >= 100 || sortMoves().length == 0) return 6;
        System.out.print("Depth: ");
        maxDepth = scanner.nextInt();
        long lastCompleted = baseLeftBit;
        if (maxDepth >= 0) {
            for (int j = 0; j <= maxDepth; j++){
                System.out.printf("Running at depth: %d\n", j);
                System.out.println("Calculating...");
                long start = System.nanoTime();
                nodes = 0;
                lastCompleted = evalDepth(j, Integer.MIN_VALUE, Integer.MAX_VALUE, lastCompleted);
                double timeTaken = (System.nanoTime() - start) / 1000000000.0;
                int raw = (int)(lastCompleted >> 32);
                double rawEval = raw;
                double evaluation = rawEval / 10.0; // convert decipawns (1/10 pawn) to pawns
                if (evaluation == 214748364.7 || evaluation == -214748364.8) {
                    System.out.print("\u001b[33m");
                    System.out.printf("Evaluation: %f", evaluation);
                    System.out.print("\u001b[0m");
                    System.out.print("\n");
                }
                else System.out.printf("Evaluation: %f\n", evaluation);
                int bestMove = (int)(lastCompleted & 0xffffffffL);
                int moveStart = extract(bestMove, 0);
                int moveEnd = extract(bestMove, 1);
                int x1 = getX(moveStart);
                int y1 = getY(moveStart);
                int x2 = getX(moveEnd);
                int y2 = getY(moveEnd);
                System.out.printf("Best move: %c%d%c%d\n", ((char)(x1 + 96)), y1, ((char)(x2 + 96)), y2);
                System.out.printf("in %f seconds\n", timeTaken);
                System.out.printf("Total nodes searched: %d\n", nodes);
                System.out.printf("Nodes per second: %f\n\n", (nodes / timeTaken));
            }
        }
        return 0;
    }
    public static void main(String[] args) {
        int result = run();
        switch (result) {
            case 1:
                System.out.println("\u001b[31mnonexistent piece character\u001b[0m");
                break;
            case 2:
                System.out.println("\u001b[31mFEN element 2 can only be \'w\' or \'b\'\u001b[0m");
                break;
            case 3:
                System.out.println("\u001b[31mFEN element 3 can only consist of \'-\', \'K\', \'Q\', \'k\', or \'q\'\u001b[0m");
                break;
            case 4:
                System.out.println("\u001b[31mFEN element 4 can only consist of \'-\'or squares on the 3rd or 6th rank\u001b[0m");
                break;
            case 5:
                System.out.println("\u001b[31mFEN element 5 needs to be a number\u001b[0m");
                break;
            case 6:
                System.out.println("\u001b[31mGame is already over\u001b[0m");
            default: break;
        }
    }
}
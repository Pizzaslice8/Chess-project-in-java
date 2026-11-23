import java.util.Scanner;
import java.util.Arrays;
import java.util.Random;
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
    //moves
    static int[][] movesStorage = new int[100][218];
    static int[][] movesStorageQ = new int[100][218];
    static int[] legalMoveList = new int[218]; //most number of legal moves in a position is 218
    static int[] tacticalMoves = new int[218];
    static long[] movesWithScores = new long[218];
    static long[] moveBoards = new long[64];
    static long[] absolutePinRestrictions = new long[64];
    static boolean[] whitePawnFiles = new boolean[8];
    static boolean[] blackPawnFiles = new boolean[8];
    //bitboards
    static long[][] bishopAttacks = new long[64][];
    static long[][] rookAttacks = new long[64][];
    //position data
    static int[] position = new int[7];
    static long currentZobrist;
    static long[] pieceBoard = new long[12];
    static byte[] mailbox = new byte[64];
    static long whiteHoles = 0L, blackHoles = 0L;
    static long controlledByWhitePawns = 0L, controlledByBlackPawns = 0L;
    static int maxDepth;
    //move ordering heuristics
    static int slot = 0;
    static boolean[] killerMoves = new boolean[4096];
    static int[] history = new int[4096];
    static int historyStored = 0;
    //transposition table
    static boolean[] transpositionOccupied = new boolean[length];
    static long[] zobristStorage = new long[length];
    static int[] dataStorage = new int[length];
    //metrics
    static int[] pieceActivity = new int[64], material = new int[64], centralControl = new int[64],
    kingSafety = new int[64], space = new int[64], rooksSeventh = new int[64], outposts = new int[64];
    //misc.
    static long[] expandTemplate = new long[6];
    static final int[] bishopDx = {1, 1, -1, -1};
    static final int[] bishopDy = {-1, 1, 1, -1};
    static final int[] rookDx = {1, 0, -1, 0};
    static final int[] rookDy = {0, 1, 0, -1};
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
    static final byte[] quadrant = {
        3, 3, 3, 3, 4, 4, 4, 4,
        3, 3, 3, 3, 4, 4, 4, 4,
        3, 3, 3, 3, 4, 4, 4, 4,
        3, 3, 3, 3, 4, 4, 4, 4,
        2, 2, 2, 2, 1, 1, 1, 1,
        2, 2, 2, 2, 1, 1, 1, 1,
        2, 2, 2, 2, 1, 1, 1, 1,
        2, 2, 2, 2, 1, 1, 1, 1
    };
    static final long[] quadrantref = {0xf0f0f0f0L, 0xf0f0f0fL, 0xf0f0f0f00000000L, 0xf0f0f0f000000000L};
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
    //non-magic bitboards
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
        // because Long.compress and Long.expand don't exist on some mobile java apps
        occupancy &= mask;
        long mk = ~mask << 1;

        for (int i = 0; i < 6; i++) {
            long mp = mk ^ (mk << 1);
            mp ^= mp << 2;
            mp ^= mp << 4;
            mp ^= mp << 8;
            mp ^= mp << 16;
            mp ^= mp << 32;
            long mv = mp & mask;
            mask = (mask ^ mv) | (mv >>> (1 << i));
            long t = occupancy & mv;
            occupancy = (occupancy ^ t) | (t >>> (1 << i));
            mk &= ~mp;
        }
        return (int) occupancy;
    }
    static long expand(long occupancy, long mask) {
        long m0 = mask;
        long mk = ~mask << 1;
        Arrays.fill(expandTemplate, 0);

        for (int i = 0; i < 6; i++) {
            long mp = mk ^ (mk << 1);
            mp ^= mp << 2;
            mp ^= mp << 4;
            mp ^= mp << 8;
            mp ^= mp << 16;
            mp ^= mp << 32;
            long mv = mp & mask;
            expandTemplate[i] = mv;
            mask = (mask ^ mv) | (mv >>> (1 << i));
            mk &= ~mp;
        }

        for (int i = 5; i >= 0; i--) {
            long mv = expandTemplate[i];
            long t = occupancy << (1 << i);
            occupancy = (occupancy & ~mv) | (t & mv);
        }
        return occupancy & m0;
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
        int moveStart = extractStart(move) - 1;
        int moveEnd = extractEnd(move) - 1;
        int requiredIndex = (moveStart << 6) | moveEnd;
        killerMoves[requiredIndex] = true;
        history[requiredIndex] = Math.min(history[requiredIndex] + 1, 500); //store history
    }
    static void store(int evaluation, int bestMove, int depth, int bound) {
        int index = (int)(currentZobrist & (length - 1));
        zobristStorage[index] = currentZobrist;
        dataStorage[index] = compressTranspositionData(evaluation, bestMove, depth, bound);
        transpositionOccupied[index] = true;
    }
    static int getTranspositionIndex() {
        int index = (int)(currentZobrist & (length - 1));
        if (transpositionOccupied[index] && currentZobrist == zobristStorage[index]) return index;
        return -1;
    }
    static int compress(int start, int end) {
        //only takes up 12 bits
        return ((start - 1) << 6) | (end - 1);
    }
    static int extractStart(int compressed) {
        return (compressed >> 6) + 1;
    }
    static int extractEnd(int compressed) {
        return (compressed & 0x3f) + 1;
    }
    static int compressTranspositionData(int eval, int move, int depth, int bound) {
        /*
        Eval: 14 bits
        Move: 12 bits
        Depth: 4 bits (max 15)
        Bound: 2 bits
        Total: 32 bits
        */
        return (bound + 1) | ((depth & 15) << 2) | (move << 6) | ((eval + 8192) << 18);
    }
    static int extractTranspositionData(int compressedData, int what) {
        switch (what) {
            case 0: return (compressedData >>> 18) - 8192;
            case 1: return (compressedData >>> 6) & 4095;
            case 2: return (compressedData >>> 2) & 15;
            case 3: return (compressedData & 3) - 1;
            default: throw new IllegalArgumentException("Unknown transposition-data field: " + what);
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
        float activePiecesModifier = Math.min(activePieces(), 4) / 4.0f;
        float endgamePercent = percentEndgame();
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
        float materialModifier = 1 + endgamePercent;
        if ((pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]) == 0) {
            if ((pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) == 0) {
                //pawn endgame, any material advantage is practically decisive
                materialModifier = 5;
            }
            //minor piece endgame, any material advantage is very important
            materialModifier = 3;
        }
        float subtotal = activePiecesModifier * tacticalOutput
        + (1 - activePiecesModifier) * nontacticalOutput+ materialDifferential * materialModifier;
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
        /* Pawn structure considerations:
        - central isolated pawn: penalty 0.2, if the square in front is occupied by a knight, penalty 0.5
        - doubled pawns: penalty 0.3
        - tripled pawns (or more): penalty 0.7
        - central pawn duo: reward 0.6
        */
        for (int i = 0; i < 8; i++) {
            int whitePawnStack = Long.bitCount(pieceBoard[10] & fileref[i]);
            int blackPawnStack = Long.bitCount(pieceBoard[11] & fileref[i]);
            if (whitePawnStack == 2) subtotal -= 3;
            else if (whitePawnStack >= 3) subtotal -= 7;
            if (blackPawnStack == 2) subtotal += 3;
            else if (blackPawnStack >= 3) subtotal += 7;
        }
        long whitePawnsToConsider = pieceBoard[10] & 0x3c3c000000L;
        long blackPawnsToConsider = pieceBoard[11] & 0x3c3c000000L;
        while (whitePawnsToConsider != 0) {
            int square = Long.numberOfTrailingZeros(whitePawnsToConsider) + 1;
            int file = getX(square);
            if ((pieceBoard[10] & fileref[file]) == 0 && (pieceBoard[10] & fileref[file-2]) == 0) {
                if ((pieceBoard[9] & (1L << (square + 7))) == 0) subtotal -= 5;
                else subtotal -= 2;
            }
            whitePawnsToConsider &= (whitePawnsToConsider - 1);
        }
        while (blackPawnsToConsider != 0) {
            int square = Long.numberOfTrailingZeros(blackPawnsToConsider) + 1;
            int file = getX(square);
            if ((pieceBoard[11] & fileref[file]) == 0 && (pieceBoard[11] & fileref[file-2]) == 0) {
                if ((pieceBoard[8] & (1L << (square - 9))) == 0) subtotal += 5;
                else subtotal += 2;
            }
            blackPawnsToConsider &= (blackPawnsToConsider - 1);
        }
        if ((pieceBoard[10] & (1L << 27)) != 0 && (pieceBoard[10] & (1L << 28)) != 0) subtotal += 6;
        if ((pieceBoard[11] & (1L << 35)) != 0 && (pieceBoard[11] & (1L << 36)) != 0) subtotal -= 6;
        return Math.round(subtotal);
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
    static void updateWhitePawnMetrics() {
        //en passant doesn't affect holes
        whiteHoles = 0L;
        //third rank holes
        if ((pieceBoard[10] & 0x200L) == 0) whiteHoles |= 0x10000L;
        if ((pieceBoard[10] & 0x500L) == 0) whiteHoles |= 0x20000L;
        if ((pieceBoard[10] & 0xa00L) == 0) whiteHoles |= 0x40000L;
        if ((pieceBoard[10] & 0x1400L) == 0) whiteHoles |= 0x80000L;
        if ((pieceBoard[10] & 0x2800L) == 0) whiteHoles |= 0x100000L;
        if ((pieceBoard[10] & 0x5000L) == 0) whiteHoles |= 0x200000L;
        if ((pieceBoard[10] & 0xa000L) == 0) whiteHoles |= 0x400000L;
        if ((pieceBoard[10] & 0x4000L) == 0) whiteHoles |= 0x800000L;
        //fourth rank holes
        if ((pieceBoard[10] & 0x20200L) == 0) whiteHoles |= 0x1000000L;
        //pawn on b2 still has the POTENTIAL to control a4, even if it doesn't right now
        //a hole is a square that can't ever be controlled by a pawn without pawn-captures
        if ((pieceBoard[10] & 0x50500L) == 0) whiteHoles |= 0x2000000L;
        if ((pieceBoard[10] & 0xa0a00L) == 0) whiteHoles |= 0x4000000L;
        if ((pieceBoard[10] & 0x1414000L) == 0) whiteHoles |= 0x8000000L;
        if ((pieceBoard[10] & 0x2828000L) == 0) whiteHoles |= 0x10000000L;
        if ((pieceBoard[10] & 0x5050000L) == 0) whiteHoles |= 0x20000000L;
        if ((pieceBoard[10] & 0xa0a0000L) == 0) whiteHoles |= 0x40000000L;
        if ((pieceBoard[10] & 0x4040000L) == 0) whiteHoles |= 0x80000000L;
    }
    static void updateBlackPawnMetrics() {
        blackHoles = 0L;
        //third rank holes
        if ((pieceBoard[11] & 0x2000000000000L) == 0) blackHoles |= 0x10000000000L;
        if ((pieceBoard[11] & 0x5000000000000L) == 0) blackHoles |= 0x20000000000L;
        if ((pieceBoard[11] & 0xa000000000000L) == 0) blackHoles |= 0x40000000000L;
        if ((pieceBoard[11] & 0x14000000000000L) == 0) blackHoles |= 0x80000000000L;
        if ((pieceBoard[11] & 0x28000000000000L) == 0) blackHoles |= 0x100000000000L;
        if ((pieceBoard[11] & 0x50000000000000L) == 0) blackHoles |= 0x200000000000L;
        if ((pieceBoard[11] & 0xa0000000000000L) == 0) blackHoles |= 0x400000000000L;
        if ((pieceBoard[11] & 0x40000000000000L) == 0) blackHoles |= 0x800000000000L;
        //fourth rank holes
        if ((pieceBoard[11] & 0x2020000000000L) == 0) blackHoles |= 0x100000000L;
        if ((pieceBoard[11] & 0x5050000000000L) == 0) blackHoles |= 0x200000000L;
        if ((pieceBoard[11] & 0xa0a0000000000L) == 0) blackHoles |= 0x400000000L;
        if ((pieceBoard[11] & 0x141400000000000L) == 0) blackHoles |= 0x800000000L;
        if ((pieceBoard[11] & 0x282800000000000L) == 0) blackHoles |= 0x1000000000L;
        if ((pieceBoard[11] & 0x505000000000000L) == 0) blackHoles |= 0x2000000000L;
        if ((pieceBoard[11] & 0xa0a000000000000L) == 0) blackHoles |= 0x4000000000L;
        if ((pieceBoard[11] & 0x404000000000000L) == 0) blackHoles |= 0x8000000000L;
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
        int x = getX(square);
        int y = getY(square);
        long occupied = occupiedSquares(true) | occupiedSquares(false);
        long controlledByEnemyPawns = (piece % 2 == 1) ? getCompositeBitboard(pieceBoard[11], 0L) : getCompositeBitboard(pieceBoard[10], 0L);
        long pieceVision = getBitboard(square-1, occupied, piece);
        if ((pieceVision & (1L << 27)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = centralControl[square-1] + modifier;
        if ((pieceVision & (1L << 28)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = centralControl[square-1] + modifier;
        if ((pieceVision & (1L << 35)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = centralControl[square-1] + modifier;
        if ((pieceVision & (1L << 36)) != 0 && piece != 1 && piece != 2)
        centralControl[square-1] = centralControl[square-1] + modifier;
        material[square-1] = materialWorth(piece);
        float endgame;
        int addition;
        switch (piece) {
            case 1:
            case 2:
                endgame = percentEndgame();
                float kingDistance = distanceToCenter[square-1];
                pieceActivity[square-1] = Math.round(modifier * 2 * (10 - kingDistance * 1.7f) * endgame);
                float safety = 2 * (10 - kingDistance * 1.7f);
                long enemyPiecesClose = (occupiedSquares(piece % 2 == 0) & quadrantref[quadrant[square-1]-1]);
                //only include super close pieces
                while (enemyPiecesClose != 0) {
                    int squareid = Long.numberOfTrailingZeros(enemyPiecesClose);
                    int newPiece = mailbox[squareid];
                    int worth = Math.abs(materialWorth(newPiece));
                    float dangerFactor = (float) Math.hypot(getX(squareid + 1) - x, getY(squareid + 1) - y); //higher = less danger, lower = more danger
                    long thatPieceVision = getBitboard(squareid, occupied, newPiece);
                    while (thatPieceVision != 0) {
                        int squareSeen = Long.numberOfTrailingZeros(thatPieceVision) + 1;
                        //factor in how far away  squareSeen is to the king and how powerful the seeing piece is
                        dangerFactor *= Math.hypot(getX(squareSeen) - x, getY(squareSeen) - y);
                        if (dangerFactor != 0) safety -= worth / dangerFactor;
                        thatPieceVision &= (thatPieceVision - 1);
                    }
                    enemyPiecesClose &= (enemyPiecesClose - 1);
                }
                kingSafety[square-1] = Math.round(modifier * safety * (1 - endgame));
                break;
            case 3:
            case 4:
                pieceActivity[square-1] = modifier * (Long.bitCount(pieceVision & (~controlledByEnemyPawns)) / 2);
                break;
            case 5:
                pieceActivity[square-1] = modifier * Long.bitCount(pieceVision & (~controlledByEnemyPawns));
                if (getY(square) == 7) rooksSeventh[square-1] = 15;
                if (getY(square) == 6 || getY(square) == 8) rooksSeventh[square-1] = 5;
                break;
            case 6:
                pieceActivity[square-1] = modifier * Long.bitCount(pieceVision & (~controlledByEnemyPawns));
                if (getY(square) == 2) rooksSeventh[square-1] = -15;
                if (getY(square) == 3 || getY(square) == 1) rooksSeventh[square-1] = -5;
                break;
            case 7:
                pieceActivity[square-1] = modifier * Long.bitCount(pieceVision & (~controlledByEnemyPawns));
                if ((square == 36 || square == 37) && ((1L << (square - 1)) & blackHoles) != 0) {
                    if (piecePresent(11, translate(square, 1, -1))
                    || piecePresent(11, translate(square, -1, -1)))
                    outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? 4 : 2;
                }
                break;
            case 8:
                pieceActivity[square-1] = modifier * Long.bitCount(pieceVision & (~controlledByEnemyPawns));
                if ((square == 28 || square == 29) && ((1L << (square - 1)) & whiteHoles) != 0) {
                    if (piecePresent(12, translate(square, 1, 1))
                    || piecePresent(12, translate(square, -1, 1)))
                    outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? -4 : -2;
                }
                break;
            case 9:
                pieceActivity[square-1] = modifier * knightBonus[square-1];
                if (((1L << (square - 1)) & blackHoles) != 0) {
                    if (piecePresent(11, translate(square, 1, -1))
                    || piecePresent(11, translate(square, -1, -1)))
                    outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? 10 : 5;
                    else outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? 5 : 3;
                }
                break;
            case 10:
                int equivalentY = 9 - getY(square);
                int equivalent = getX(square) + 8 * (equivalentY - 1);
                pieceActivity[square-1] = modifier * knightBonus[equivalent-1];
                if (((1L << (square - 1)) & whiteHoles) != 0) {
                    if (piecePresent(12, translate(square, 1, 1))
                    || piecePresent(12, translate(square, -1, 1)))
                    outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? -10 : -5;
                    else outposts[square-1] = (getX(square) >= 3 && getX(square) <= 6) ? -5 : -3;
                }
                break;
            case 11:
                endgame = percentEndgame();
                addition = getY(square) * getY(square);
                if (endgame > 0.9) {
                    //outside passed pawns are super valuable in the endgame
                    switch (getX(square)) {
                        case 3:
                        case 6:
                            space[square-1] = Math.round(addition / 12.0f);
                            break;
                        case 4:
                        case 5:
                            space[square-1] = Math.round(addition / 7.0f);
                            break;
                        default:
                            space[square-1] = Math.round(addition / 4.0f);
                            break;
                    }
                } else {
                    //central space is more valuable in the middlegame
                    switch (getX(square)) {
                        case 3:
                        case 6:
                            space[square-1] = Math.round(addition / 4.0f);
                            break;
                        case 4:
                        case 5:
                            space[square-1] = Math.round(addition / 7.0f);
                            break;
                        default:
                            space[square-1] = Math.round(addition / 12.0f);
                            break;
                    }
                }
                break;
            case 12:
                endgame = percentEndgame();
                addition = -1 * (9 - getY(square)) * (9 - getY(square));
                if (endgame > 0.9) {
                    //outside passed pawns are super valuable in the endgame
                    switch (getX(square)) {
                        case 3:
                        case 6:
                            space[square-1] = Math.round(addition / 12.0f);
                            break;
                        case 4:
                        case 5:
                            space[square-1] = Math.round(addition / 7.0f);
                            break;
                        default:
                            space[square-1] = Math.round(addition / 4.0f);
                            break;
                    }
                } else {
                    //central space is more valuable in the middlegame
                    switch (getX(square)) {
                        case 3:
                        case 6:
                            space[square-1] = Math.round(addition / 4.0f);
                            break;
                        case 4:
                        case 5:
                            space[square-1] = Math.round(addition / 7.0f);
                            break;
                        default:
                            space[square-1] = Math.round(addition / 12.0f);
                            break;
                    }
                }
                break;
            default:
                break;
        }
    }
    static void updateEvaluation(long squares) {
        while (squares != 0) {
            update(Long.numberOfTrailingZeros(squares) + 1);
            squares &= (squares - 1);
        }
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
        long occupied = occupiedSquares(true) | occupiedSquares(false);
        while (heavyCopy != 0) {
            controlledBits |= getBitboard(Long.numberOfTrailingZeros(heavyCopy), occupied, 5);
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
        int quantity = generateMoves(getCompositeBitboard(occupiedSquares(position[0] == 0), occupied));
        for (int i = 0; i < quantity; i++) {
            int start = extractStart(legalMoveList[i]);
            int end = extractEnd(legalMoveList[i]);
            int x1 = getX(start);
            int y1 = getY(start);
            int x2 = getX(end);
            int y2 = getY(end);
            System.out.printf("%c%d%c%d", ((char)(x1 + 96)), y1, ((char)(x2 + 96)), y2);
            System.out.print(", ");
        }
        sortMoves(quantity);
        System.out.println("");
        System.out.print("After move ordering: ");
        for (int i = 0; i < quantity; i++) {
            int start = extractStart(legalMoveList[i]);
            int end = extractEnd(legalMoveList[i]);
            int x1 = getX(start);
            int y1 = getY(start);
            int x2 = getX(end);
            int y2 = getY(end);
            System.out.printf("%c%d%c%d", ((char)(x1 + 96)), y1, ((char)(x2 + 96)), y2);
            System.out.print(", ");
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
    static long legalMovesBasic(int square, long opposingAttackMap) {
        long output = 0L;
        int piece = mailbox[square-1];
        boolean color = color(piece);
        long pieces = occupiedSquares(true) | occupiedSquares(false);
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
            if (position[5] != 0 && (pieces & (1L << (position[5]-1))) == 0 &&
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
            if (position[5] != 0 && (pieces & (1L << (position[5]-1))) == 0 &&
                    (position[5] == translate(square, 1, -1) || position[5] == translate(square, -1, -1))) {
                output |= (1L << (position[5]-1));
            }
        } else {
            long squares = getBitboard(square-1, pieces, piece);
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
                    if ((opposingAttackMap & 32L) == 0 && (opposingAttackMap & 64L) == 0)
                    output |= (1L << 6);
                }
                if (position[2] == 1 && ((pieceBoard[4] & 1L) != 0) && isEmpty(4) && isEmpty(3) && isEmpty(2)) {   
                    if ((opposingAttackMap & 8L) == 0 && (opposingAttackMap & 4L) == 0)
                    output |= 4L;
                }
            }
            if (square == 61) {
                if (position[3] == 1 && ((pieceBoard[5] & (1L << 63)) != 0) && isEmpty(62) && isEmpty(63)) {
                    if ((opposingAttackMap & (1L << 61)) == 0 && (opposingAttackMap & (1L << 62)) == 0)
                    output |= (1L << 62);
                }
                if (position[4] == 1 && ((pieceBoard[5] & (1L << 56)) != 0) && isEmpty(60) && isEmpty(59) && isEmpty(58)) {
                    if ((opposingAttackMap & (1L << 59)) == 0 && (opposingAttackMap & (1L << 58)) == 0)
                    output |= (1L << 58);
                }
            }
        }
        return output;
    }
    static void makeMove(int move) {
        int start = extractStart(move);
        int end = extractEnd(move);
        int piece = mailbox[start-1];
        int captured = mailbox[end-1];
        int epSquare = position[5];
        boolean tacticalMove = isTacticalMove(move);
        boolean pawnStructureChanged = piece == 11 || piece == 12 || captured == 11 || captured == 12;
        int whiteKing = Long.numberOfTrailingZeros(pieceBoard[0]);
        int blackKing = Long.numberOfTrailingZeros(pieceBoard[1]);
        boolean kingSafetyChanged = piece == 1 || piece == 2
        || quadrant[start-1] == quadrant[whiteKing] || quadrant[end-1] == quadrant[whiteKing]
        || quadrant[start-1] == quadrant[blackKing] || quadrant[end-1] == quadrant[blackKing];
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
        long worthUpdatingHorizontal = 0L;
        long worthUpdatingDiagonal = 0L;
        long pieces = occupiedSquares(true) | occupiedSquares(false);
        if (pawnStructureChanged) {
            updateWhitePawnMetrics();
            updateBlackPawnMetrics();
            long criticalSquares = 0L;
            int startX = getX(start);
            int endX = getX(end);
            if (piece == 11) {
                if (startX != 1) criticalSquares |= 1L << (start + 6);
                if (startX != 8) criticalSquares |= 1L << (start + 8);
            }
            if (piece == 11 || captured == 11) {
                if (endX != 1) criticalSquares |= 1L << (end + 6);
                if (endX != 8) criticalSquares |= 1L << (end + 8);
            }
            if (piece == 12) {
                if (startX != 1) criticalSquares |= 1L << (start - 10);
                if (startX != 8) criticalSquares |= 1L << (start - 8);
            }
            if (piece == 12 || captured == 12) {
                if (endX != 1) criticalSquares |= 1L << (end - 10);
                if (endX != 8) criticalSquares |= 1L << (end - 8);
            }
            if (end == epSquare) {
                if (piece == 11) {
                    //knight's distance from the end square, so -15 and -17 for white, +15 and +17 for black
                    if (endX != 1) criticalSquares |= 1L << (end - 18);
                    if (endX != 8) criticalSquares |= 1L << (end - 16);
                    worthUpdatingHorizontal |= getBitboard(end - 9, pieces, 5);
                    worthUpdatingDiagonal |= getBitboard(end - 9, pieces, 7);
                }
                if (piece == 12) {
                    if (endX != 1) criticalSquares |= 1L << (end + 14);
                    if (endX != 8) criticalSquares |= 1L << (end + 16);
                    worthUpdatingHorizontal |= getBitboard(end + 7, pieces, 5);
                    worthUpdatingDiagonal |= getBitboard(end + 7, pieces, 7);
                }
            }
            updateEvaluation(criticalSquares & (pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]));
            while (criticalSquares != 0) {
                int criticalSquare = Long.numberOfTrailingZeros(criticalSquares);
                worthUpdatingHorizontal |= getBitboard(criticalSquare, pieces, 5);
                worthUpdatingDiagonal |= getBitboard(criticalSquare, pieces, 7);
                criticalSquares &= (criticalSquares - 1);
            }
        }
        update(start);
        update(end);
        if (start == 5 && end == 3) {
            update(1);
            update(4);
            worthUpdatingHorizontal |= getBitboard(0, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(3, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(0, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(3, pieces, 7);
        }
        if (start == 5 && end == 7) {
            update(6);
            update(8);
            worthUpdatingHorizontal |= getBitboard(5, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(7, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(5, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(7, pieces, 7);
        }
        if (start == 61 && end == 59) {
            //black castle long
            update(57);
            update(60);
            worthUpdatingHorizontal |= getBitboard(56, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(59, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(56, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(59, pieces, 7);
        }
        if (start == 61 && end == 63) {
            //black castle short
            update(62);
            update(64);
            worthUpdatingHorizontal |= getBitboard(61, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(63, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(61, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(63, pieces, 7);
        }
        worthUpdatingHorizontal |= getBitboard(start - 1, pieces, 5);
        worthUpdatingHorizontal |= getBitboard(end - 1, pieces, 5);
        worthUpdatingDiagonal |= getBitboard(start - 1, pieces, 7);
        worthUpdatingDiagonal |= getBitboard(end - 1, pieces, 7);
        updateEvaluation(worthUpdatingHorizontal & (pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]));
        updateEvaluation(worthUpdatingDiagonal & (pieceBoard[2] | pieceBoard[3] | pieceBoard[6] | pieceBoard[7]));
        if (tacticalMove) updateEvaluation(pieceBoard[0] | pieceBoard[1] | pieceBoard[10] | pieceBoard[11]);
        if (kingSafetyChanged) updateEvaluation(pieceBoard[0] | pieceBoard[1]);
    }
    static void undoMove(int move, int movedPiece, int captured, boolean wcsBefore, boolean wclBefore,
    boolean bcsBefore, boolean bclBefore, int enPassantSquareBefore, int halfmoveBefore, long zobristBefore) {
        int start = extractStart(move);
        int end = extractEnd(move);
        int pieceAfter = mailbox[end-1];
        boolean enPassantCapture = end == enPassantSquareBefore && (movedPiece == 11 || movedPiece == 12);
        boolean tacticalMove = captured != 0 || enPassantCapture;
        long changedSquares = (1L << (start - 1)) | (1L << (end - 1));
        if (enPassantCapture) changedSquares |= 1L << ((movedPiece == 11) ? end - 9 : end + 7);
        if (movedPiece == 1 && start == 5) {
            if (end == 3) changedSquares |= 1L | (1L << 3);
            if (end == 7) changedSquares |= (1L << 7) | (1L << 5);
        }
        if (movedPiece == 2 && start == 61) {
            if (end == 59) changedSquares |= (1L << 56) | (1L << 59);
            if (end == 63) changedSquares |= (1L << 63) | (1L << 61);
        }
        boolean pawnStructureChanged = movedPiece == 11 || movedPiece == 12 || captured == 11 || captured == 12;
        int whiteKing = Long.numberOfTrailingZeros(pieceBoard[0]);
        int blackKing = Long.numberOfTrailingZeros(pieceBoard[1]);
        boolean kingSafetyChanged = movedPiece == 1 || movedPiece == 2
        || quadrant[start-1] == quadrant[whiteKing] || quadrant[end-1] == quadrant[whiteKing]
        || quadrant[start-1] == quadrant[blackKing] || quadrant[end-1] == quadrant[blackKing];
        position[0] = 1 - position[0];
        position[5] = enPassantSquareBefore;
        position[6] = halfmoveBefore;
        if (enPassantCapture) {
            if (movedPiece == 11) {
                pieceBoard[11] |= (1L << (end - 9));
                mailbox[end-9] = 12;
            } else {
                pieceBoard[10] |= (1L << (end + 7));
                mailbox[end+7] = 11;
            }
        }
        pieceBoard[pieceAfter-1] ^= (1L << (end - 1));
        pieceBoard[movedPiece-1] |= (1L << (start - 1));
        mailbox[end-1] = 0;
        mailbox[start-1] = (byte) movedPiece;
        if (position[1] == 0 && wcsBefore) {
            position[1] = 1;
        }
        if (position[2] == 0 && wclBefore) {
            position[2] = 1;
        }
        if (position[3] == 0 && bcsBefore) {
            position[3] = 1;
        }
        if (position[4] == 0 && bclBefore) {
            position[4] = 1;
        }
        if (captured != 0) {
            pieceBoard[captured-1] |= (1L << (end - 1));
            mailbox[end-1] = (byte) captured;
        }
        if (movedPiece == 1 && start == 5) {
            if (end == 7) {
                pieceBoard[4] |= 128L;
                pieceBoard[4] ^= 32L;
                mailbox[5] = 0;
                mailbox[7] = 5;
            }
            if (end == 3) {
                pieceBoard[4] |= 1L;
                pieceBoard[4] ^= 8L;
                mailbox[3] = 0;
                mailbox[0] = 5;
            }
        }
        if (movedPiece == 2 && start == 61) {
            if (end == 63) {
                pieceBoard[5] |= (1L << 63);
                pieceBoard[5] ^= (1L << 61);
                mailbox[61] = 0;
                mailbox[63] = 6;
            }
            if (end == 59) {
                pieceBoard[5] |= (1L << 56);
                pieceBoard[5] ^= (1L << 59);
                mailbox[59] = 0;
                mailbox[56] = 6;
            }
        }
        long worthUpdatingHorizontal = 0L;
        long worthUpdatingDiagonal = 0L;
        long pieces = occupiedSquares(true) | occupiedSquares(false);
        if (pawnStructureChanged) {
            updateWhitePawnMetrics();
            updateBlackPawnMetrics();
            long criticalSquares = 0L;
            int startX = getX(start);
            int endX = getX(end);
            if (movedPiece == 11) {
                if (startX != 1) criticalSquares |= 1L << (start + 6);
                if (startX != 8) criticalSquares |= 1L << (start + 8);
            }
            if (movedPiece == 11 || captured == 11) {
                if (endX != 1) criticalSquares |= 1L << (end + 6);
                if (endX != 8) criticalSquares |= 1L << (end + 8);
            }
            if (movedPiece == 12) {
                if (startX != 1) criticalSquares |= 1L << (start - 10);
                if (startX != 8) criticalSquares |= 1L << (start - 8);
            }
            if (movedPiece == 12 || captured == 12) {
                if (endX != 1) criticalSquares |= 1L << (end - 10);
                if (endX != 8) criticalSquares |= 1L << (end - 8);
            }
            if (enPassantCapture) {
                if (movedPiece == 11) {
                    //knight's distance from the end square, so -15 and -17 for white, +15 and +17 for black
                    if (endX != 1) criticalSquares |= 1L << (end - 18);
                    if (endX != 8) criticalSquares |= 1L << (end - 16);
                    worthUpdatingHorizontal |= getBitboard(end - 9, pieces, 5);
                    worthUpdatingDiagonal |= getBitboard(end - 9, pieces, 7);
                }
                if (movedPiece == 12) {
                    if (endX != 1) criticalSquares |= 1L << (end + 14);
                    if (endX != 8) criticalSquares |= 1L << (end + 16);
                    worthUpdatingHorizontal |= getBitboard(end + 7, pieces, 5);
                    worthUpdatingDiagonal |= getBitboard(end + 7, pieces, 7);
                }
            }
            updateEvaluation(criticalSquares & (pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]));
            while (criticalSquares != 0) {
                int criticalSquare = Long.numberOfTrailingZeros(criticalSquares);
                worthUpdatingHorizontal |= getBitboard(criticalSquare, pieces, 5);
                worthUpdatingDiagonal |= getBitboard(criticalSquare, pieces, 7);
                criticalSquares &= (criticalSquares - 1);
            }
        }
        updateEvaluation(changedSquares);
        if (start == 5 && end == 3) {
            update(1);
            update(4);
            worthUpdatingHorizontal |= getBitboard(0, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(3, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(0, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(3, pieces, 7);
        }
        if (start == 5 && end == 7) {
            update(6);
            update(8);
            worthUpdatingHorizontal |= getBitboard(5, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(7, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(5, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(7, pieces, 7);
        }
        if (start == 61 && end == 59) {
            //black castle long
            update(57);
            update(60);
            worthUpdatingHorizontal |= getBitboard(56, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(59, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(56, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(59, pieces, 7);
        }
        if (start == 61 && end == 63) {
            //black castle short
            update(62);
            update(64);
            worthUpdatingHorizontal |= getBitboard(61, pieces, 5);
            worthUpdatingHorizontal |= getBitboard(63, pieces, 5);
            worthUpdatingDiagonal |= getBitboard(61, pieces, 7);
            worthUpdatingDiagonal |= getBitboard(63, pieces, 7);
        }
        worthUpdatingHorizontal |= getBitboard(start - 1, pieces, 5);
        worthUpdatingHorizontal |= getBitboard(end - 1, pieces, 5);
        worthUpdatingDiagonal |= getBitboard(start - 1, pieces, 7);
        worthUpdatingDiagonal |= getBitboard(end - 1, pieces, 7);
        updateEvaluation(worthUpdatingHorizontal & (pieceBoard[2] | pieceBoard[3] | pieceBoard[4] | pieceBoard[5]));
        updateEvaluation(worthUpdatingDiagonal & (pieceBoard[2] | pieceBoard[3] | pieceBoard[6] | pieceBoard[7]));
        if (tacticalMove) updateEvaluation(pieceBoard[0] | pieceBoard[1] | pieceBoard[10] | pieceBoard[11]);
        if (kingSafetyChanged) updateEvaluation(pieceBoard[0] | pieceBoard[1]);
        currentZobrist = zobristBefore;
    }
    static long legalMoves(int square, long checkingPieces, long opposingAttackMap) {
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
        long basic = legalMovesBasic(square, opposingAttackMap);
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
        int start = extractStart(move);
        int end = extractEnd(move);
        int piece = mailbox[start-1];
        int t = getTranspositionIndex();
        if (t != -1) {
            if (extractTranspositionData(dataStorage[t], 1) == move) return Integer.MAX_VALUE;
        }
        boolean pieceColor = color(piece);
        int modifier = (pieceColor) ? 1 : -1;
        float pieceModifier = 1.0f; //helps prioritize certain pieces over others
        switch (piece) {
            case 1: case 2: pieceModifier = 0.7f; break;
            case 3: case 4: pieceModifier = 0.8f; break;
            case 7: case 8: case 9: case 10: pieceModifier = 1.2f; break;
            case 11: case 12: pieceModifier = 0.9f; break;
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
        int neededIndex = ((start - 1) << 6) | (end - 1);
        //killer moves heuristic
        if (killerMoves[neededIndex]) return Integer.MAX_VALUE - 362;
        //history heuristic
        int timesAppeared = history[neededIndex];
        if (timesAppeared != 0) return (Integer.MAX_VALUE - 373) + timesAppeared;
        //quiet moves not principal or in killer moves or in history,
        //counting up from 0 as opposed to counting down from 2147483647
        int cumulative = 0;
        //reward center-seeking moves
        float startProximity = distanceToCenter[start-1];
        float endProximity = distanceToCenter[end-1];
        int centerGain = Math.round(startProximity - endProximity);
        if (piece != 1 && piece != 2) cumulative += centerGain;
        //reward forward moves
        cumulative += (pieceColor) ? getY(end) - getY(start) : getY(start) - getY(end);
        if (piece == 11 || piece == 12) {
            //reward pawn moves close to promotion
            boolean farAdvanced = (pieceColor) ? getY(end) >= 6 : getY(end) <= 3;
            if (farAdvanced) cumulative += 20;
        }
        if (piece == 5 || piece == 6) {
            if (getY(end) == ((pieceColor) ? 7 : 2)) cumulative += 25;
            if (getY(end) == ((pieceColor) ? 8 : 1) || getY(end) == ((pieceColor) ? 6 : 3)) cumulative += 10;
        }
        return Math.round(cumulative * pieceModifier);
    }
    static int generateTacticalMoves(long opposingAttackMap) {
        Arrays.fill(tacticalMoves, 0);
        long squares = occupiedSquares(position[0] == 1);
        long friendlyPieces = squares & (~(pieceBoard[0] | pieceBoard[1]));
        long enemyPieces = occupiedSquares(position[0] == 0);
        long check = checkPieces();
        if (check != 0) Arrays.fill(absolutePinRestrictions, -1L);
        else {
            int checkingPiece = (check == 0) ? 0 : mailbox[Long.numberOfTrailingZeros(check)];
            while (enemyPieces != 0) {
                int pieceSquare = Long.numberOfTrailingZeros(enemyPieces);
                int pieceid = mailbox[pieceSquare];
                long fullVisionPotential = getBitboard(pieceSquare, enemyPieces, pieceid);
                if (mailbox[pieceSquare] >= 3 && mailbox[pieceSquare] <= 8) {
                    long intercepting = fullVisionPotential & friendlyPieces;
                    if (Long.bitCount(intercepting) == 1)
                    absolutePinRestrictions[Long.numberOfTrailingZeros(intercepting)] = (fullVisionPotential
                    | (1L << pieceSquare)) & (~(pieceBoard[0] | pieceBoard[1]));
                }
                enemyPieces &= (enemyPieces - 1);
            }
        }
        int quantity = 0;
        for (int square = 0; square < 64; square++) {
            if ((squares & (1L << square)) == 0) moveBoards[square] = 0L;
            else {
                long allMoves = legalMoves(square+1, check, opposingAttackMap);
                while (allMoves != 0) {
                    int move = compress(square+1, Long.numberOfTrailingZeros(allMoves)+1);
                    if (isTacticalMove(move)) tacticalMoves[quantity++] = move;
                    allMoves &= (allMoves - 1);
                }
            }
        }
        return quantity;
    }
    static int generateMoves(long opposingAttackMap) {
        Arrays.fill(legalMoveList, 0);
        long squares = occupiedSquares(position[0] == 1);
        long friendlyPieces = squares & (~(pieceBoard[0] | pieceBoard[1]));
        long enemyPieces = occupiedSquares(position[0] == 0);
        long check = checkPieces();
        if (check != 0) Arrays.fill(absolutePinRestrictions, -1L);
        else {
            int checkingPiece = (check == 0) ? 0 : mailbox[Long.numberOfTrailingZeros(check)];
            while (enemyPieces != 0) {
                int pieceSquare = Long.numberOfTrailingZeros(enemyPieces);
                int pieceid = mailbox[pieceSquare];
                long fullVisionPotential = getBitboard(pieceSquare, enemyPieces, pieceid);
                if (mailbox[pieceSquare] >= 3 && mailbox[pieceSquare] <= 8) {
                    long intercepting = fullVisionPotential & friendlyPieces;
                    if (Long.bitCount(intercepting) == 1)
                    absolutePinRestrictions[Long.numberOfTrailingZeros(intercepting)] = (fullVisionPotential
                    | (1L << pieceSquare)) & (~(pieceBoard[0] | pieceBoard[1]));
                }
                enemyPieces &= (enemyPieces - 1);
            }
        }
        int quantity = 0;
        Arrays.fill(moveBoards, 0L);
        while (squares != 0) {
            int square = Long.numberOfTrailingZeros(squares);
            long allMoves = legalMoves(square+1, check, opposingAttackMap);
            while (allMoves != 0) {
                legalMoveList[quantity++] = compress(square+1, Long.numberOfTrailingZeros(allMoves)+1);
                allMoves &= (allMoves - 1);
            }
            squares &= (squares - 1);
        }
        return quantity;
    }
    static void sortMoves(int quantity) {
        float tacticalModifier = Math.min(activePieces(), 4) / 4.0f;
        for (int j = 0; j < quantity; j++) {
            int score = calculateScore(legalMoveList[j]);
            movesWithScores[j] = ((long) (~score) << 32) | (legalMoveList[j] & 0xffffffffL);
        }
        Arrays.sort(movesWithScores, 0, quantity);
        for (int j = 0; j < quantity; j++) legalMoveList[j] = (int) movesWithScores[j];
    }
    static boolean isTacticalMove(int move) {
        int start = extractStart(move);
        int end = extractEnd(move);
        int piece = mailbox[start-1];
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
    static float percentEndgame() {
        int endgameScore = 0; //the higher, the less of an endgame it is, above 1000 it's a middlegame
        endgameScore += Long.bitCount(pieceBoard[2] | pieceBoard[3]) * 400; //queens
        endgameScore += Long.bitCount(pieceBoard[4] | pieceBoard[5]) * 200; //rooks
        endgameScore += Long.bitCount(pieceBoard[6] | pieceBoard[7] | pieceBoard[8] | pieceBoard[9]) * 50; //bishops and knights
        return 1.0f - Math.min(1000.0f, endgameScore) / 1000.0f;
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
    static int evalQ(int alpha, int beta, int depth) {
        int checkmateMode = simpleCheckmate();
        if (checkmateMode != 0) {
            return (checkmateMode > 0)
                ? (7000 - sizeOfKingBox(checkmateMode))
                : (-7000 + sizeOfKingBox(checkmateMode));
        } //the smaller the box, the better
        nodes++;
        boolean maximizing = position[0] == 1;
        boolean checked = inCheck();
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
        int quantity;
        long opposingAttackMap = getCompositeBitboard(occupiedSquares(position[0] == 0), occupiedSquares(true) | occupiedSquares(false));
        if (inCheck()) {
            quantity = generateMoves(opposingAttackMap);
            sortMoves(quantity);
            System.arraycopy(legalMoveList, 0, movesStorageQ[depth], 0, quantity);
            for (int i = 0; i < quantity; i++) {
                if (movesStorageQ[depth][i] == 0) break;
                int move = movesStorageQ[depth][i];
                int movedPiece = mailbox[extractStart(move) - 1];
                int captured = mailbox[extractEnd(move) - 1];
                boolean wcsBefore = position[1] == 1;
                boolean wclBefore = position[2] == 1;
                boolean bcsBefore = position[3] == 1;
                boolean bclBefore = position[4] == 1;
                int enPassantSquareBefore = position[5];
                int halfmoveBefore = position[6];
                long zobristBefore = currentZobrist;
                makeMove(move);
                int potential;
                potential = evalQ(alpha, beta, depth + 1);
                undoMove(move, movedPiece, captured, wcsBefore, wclBefore, bcsBefore,
                bclBefore, enPassantSquareBefore, halfmoveBefore, zobristBefore);
                if (maximizing) {
                    best = Math.max(best, potential);
                    alpha = Math.max(alpha, best);
                } else {
                    best = Math.min(best, potential);
                    beta = Math.min(beta, best);
                }
                if (alpha >= beta) break;
            }
        } else {
            quantity = generateTacticalMoves(opposingAttackMap);
            System.arraycopy(tacticalMoves, 0, movesStorageQ[depth], 0, quantity);
            for (int i = 0; i < quantity; i++) {
                if (movesStorageQ[depth][i] == 0) break;
                int move = movesStorageQ[depth][i];
                int movedPiece = mailbox[extractStart(move) - 1];
                int captured = mailbox[extractEnd(move) - 1];
                boolean wcsBefore = position[1] == 1;
                boolean wclBefore = position[2] == 1;
                boolean bcsBefore = position[3] == 1;
                boolean bclBefore = position[4] == 1;
                int enPassantSquareBefore = position[5];
                int halfmoveBefore = position[6];
                long zobristBefore = currentZobrist;
                makeMove(move);
                int potential;
                potential = evalQ(alpha, beta, depth + 1);
                undoMove(move, movedPiece, captured, wcsBefore, wclBefore, bcsBefore,
                bclBefore, enPassantSquareBefore, halfmoveBefore, zobristBefore);
                if (maximizing) {
                    best = Math.max(best, potential);
                    alpha = Math.max(alpha, best);
                } else {
                    best = Math.min(best, potential);
                    beta = Math.min(beta, best);
                }
                if (alpha >= beta) break;
            }
        }
        return best;
    }
    static long evalDepth(int depth, int alpha, int beta, long previousResult) {
        if (previousResult != baseLeftBit && depth == maxDepth) {
            int evalOnly = (int)(previousResult >> 32);
            int lower = (evalOnly > -8192) ? evalOnly - 50 : -8192;
            //in case previousResult[0] is less than 50 away from -8192
            int higher = (evalOnly < 8191) ? evalOnly + 50 : 8191;
            //in case previousResult[0] is less than 50 away from 8191
            long shallowSearch = evalDepth(depth, lower, higher, baseLeftBit);
            if ((shallowSearch >> 32) < higher && (shallowSearch >> 32) > lower) return shallowSearch;
        }
        if (depth == maxDepth) Arrays.fill(killerMoves, false);
        int alphaOriginal = alpha;
        int betaOriginal = beta;
        int bound = 0;
        int t = getTranspositionIndex();
        if (t != -1) {
            if (extractTranspositionData(dataStorage[t], 2) >= depth) {
                int eval = extractTranspositionData(dataStorage[t], 0);
                int move = extractTranspositionData(dataStorage[t], 1);
                int boundFound = extractTranspositionData(dataStorage[t], 3);
                if (boundFound == 0) return (((long)eval) << 32) | (move & rightBitsOnly);
                if (boundFound == -1) {
                    if (eval >= beta) return (((long)eval) << 32) | (move & rightBitsOnly);
                    alpha = Math.max(alpha, eval);
                }
                if (boundFound == 1) {
                    if (eval <= alpha) return (((long)eval) << 32) | (move & rightBitsOnly);
                    beta = Math.min(beta, eval);
                }
                if (alpha >= beta) {
                    if (!isTacticalMove(move)) storeQuietMove(move, depth);
                    return (((long)eval) << 32) | (move & rightBitsOnly);
                }
            }
        }
        nodes++;
        boolean maximizing = position[0] == 1;
        //null move pruning
        boolean hasNullMoveBound = maximizing ? beta < 8191 : alpha > -8192;
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
        if (depth == 0) return ((long)evalQ(alpha, beta, 0)) << 32;
        long opposingAttackMap = getCompositeBitboard(occupiedSquares(position[0] == 0), occupiedSquares(true) | occupiedSquares(false));
        //aspiration window
        int quantity = generateMoves(opposingAttackMap);
        if (quantity == 0) {
            if (inCheck()) return (position[0] == 1) ? Long.MIN_VALUE : leftBitsOnly;
            return 0;
        }
        sortMoves(quantity);
        System.arraycopy(legalMoveList, 0, movesStorage[depth - 1], 0, quantity);
        int evaluation = maximizing ? -8192 : 8191;
        int bestMove = 0;
        for (int i = 0; i < quantity; i++) {
            if (movesStorage[depth - 1][i] == 0) break;
            int move = movesStorage[depth - 1][i];
            int movedPiece = mailbox[extractStart(move) - 1];
            int captured = mailbox[extractEnd(move) - 1];
            boolean wcsBefore = position[1] == 1;
            boolean wclBefore = position[2] == 1;
            boolean bcsBefore = position[3] == 1;
            boolean bclBefore = position[4] == 1;
            int enPassantSquareBefore = position[5];
            int halfmoveBefore = position[6];
            long zobristBefore = currentZobrist;
            makeMove(move);
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
            undoMove(move, movedPiece, captured, wcsBefore, wclBefore, bcsBefore,
            bclBefore, enPassantSquareBefore, halfmoveBefore, zobristBefore);
            if (maximizing) {
                if (potential > evaluation) {
                    evaluation = potential;
                    bestMove = movesStorage[depth - 1][i];
                }
                alpha = Math.max(alpha, evaluation);
            } else {
                if (potential < evaluation) {
                    evaluation = potential;
                    bestMove = movesStorage[depth - 1][i];
                }
                beta = Math.min(beta, evaluation);
            }
            if (alpha >= beta) {
                if (!isTacticalMove(movesStorage[depth - 1][i]))
                storeQuietMove(movesStorage[depth - 1][i], depth);
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
            for (int index = 0; index < rookAddition.length; index++) rookAddition[index] = fillWithRook(i, expand(index, rookVision[i]));
            for (int index = 0; index < bishopAddition.length; index++) bishopAddition[index] = fillWithBishop(i, expand(index, bishopVision[i]));
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
                        position[6] += (int)(current - '0');
                    }
                    break;
                default: break;
            }
            current = fen[++i];
        }
        updateWhitePawnMetrics();
        updateBlackPawnMetrics();
        updateEvaluation(occupiedSquares(true) | occupiedSquares(false));
        Arrays.fill(absolutePinRestrictions, -1L);
        System.out.print("Show stats? ");
        if (scanner.nextBoolean()) {
            display();
            displayStats();    
        }
        long pieceSquares = occupiedSquares(position[0] == 1);
        if (!sufficientMaterial() || position[6] >= 100) return 6;
        int quantity = generateMoves(getCompositeBitboard(occupiedSquares(position[0] == 0), occupiedSquares(true) | occupiedSquares(false)));
        if (quantity == 0) return 6;
        System.out.print("Depth: ");
        maxDepth = scanner.nextInt();
        long lastCompleted = baseLeftBit;
        if (maxDepth >= 0) {
            for (int j = 0; j <= maxDepth; j++){
                System.out.printf("Running at depth: %d\n", j);
                System.out.println("Calculating...");
                long start = System.nanoTime();
                nodes = 0;
                lastCompleted = evalDepth(j, -8192, 8191, lastCompleted);
                float timeTaken = (System.nanoTime() - start) / 1000000000.0f;
                int raw = (int)(lastCompleted >> 32);
                float rawEval = raw;
                float evaluation = rawEval / 10.0f; // convert decipawns (1/10 pawn) to pawns
                if (evaluation == 819.1 || evaluation == -819.2) {
                    System.out.print("\u001b[33m");
                    System.out.printf("Evaluation: %f", evaluation);
                    System.out.print("\u001b[0m");
                    System.out.print("\n");
                }
                else System.out.printf("Evaluation: %f\n", evaluation);
                int bestMove = (int)(lastCompleted & 0xffffffffL);
                int moveStart = extractStart(bestMove);
                int moveEnd = extractEnd(bestMove);
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

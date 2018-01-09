package com.example.tsvet.reversiai;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GameState {

    int amountOfBlackPieces;
    boolean[][] blackMovesPossible = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{8, 8});
    ArrayList<String>[][] blackPossibleDirections = (ArrayList[][]) Array.newInstance(ArrayList.class, new int[]{8, 8});
    boolean isBlackTurn;
    boolean previousBlackTurn;

    int amountOfWhitePieces;
    boolean[][] whiteMovesPossible = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{8, 8});
    ArrayList<String>[][] whitePossibleDirections = (ArrayList[][]) Array.newInstance(ArrayList.class, new int[]{8, 8});
    boolean isWhiteTurn;
    boolean previousWhiteTurn;

    int[][] gameBoard = (int[][]) Array.newInstance(Integer.TYPE, new int[]{8, 8});
    boolean gameEnd;
    int previousMoveColumn;
    int previousMoveRow;
    int aiLevel;
    int aiPlayer;
    int humanPlayer;
    int gameIndex;

    GameState() {

        int i = 0;
        while (i < 8) {
            int j = 0;
            while (j < 8) {
                this.blackPossibleDirections[i][j] = new ArrayList();
                this.whitePossibleDirections[i][j] = new ArrayList();
                j += 1;
            }
            i += 1;
        }
    }
}

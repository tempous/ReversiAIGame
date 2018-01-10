package com.example.tsvet.reversiai;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GameState {

    int countOfBlackChips;
    boolean[][] blackMovesPossible = (boolean[][]) Array.newInstance(Boolean.TYPE, 8, 8);
    ArrayList[][] blackPossibleDirections = (ArrayList[][]) Array.newInstance(ArrayList.class, 8, 8);
    boolean isBlackMove;
    boolean previousBlackMove;

    int countOfWhiteChips;
    boolean[][] whiteMovesPossible = (boolean[][]) Array.newInstance(Boolean.TYPE, 8, 8);
    ArrayList[][] whitePossibleDirections = (ArrayList[][]) Array.newInstance(ArrayList.class, 8, 8);
    boolean isWhiteMove;
    boolean previousWhiteMove;

    int[][] gameBoard = (int[][]) Array.newInstance(Integer.TYPE, 8, 8);
    boolean isEndOfGame;
    int previousMoveColumn;
    int previousMoveRow;
    int opponentLevel;
    int opponentPlayer;
    int Player;
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

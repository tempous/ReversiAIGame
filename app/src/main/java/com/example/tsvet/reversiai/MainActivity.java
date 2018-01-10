package com.example.tsvet.reversiai;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.tsvet.reversiai.R.layout.activity_main;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean isWhiteMove;
    boolean isBlackMove;

    boolean previousWhiteMove;
    boolean previousBlackMove;

    static final int BOARD_LENGTH = 8;

    int gameBoard[][];

    static final int EMPTY_CHIP = 0;
    static final int WHITE_CHIP = 1;
    static final int BLACK_CHIP = 2;

    boolean isEndOfGame;

    int countOfBlackChips = 2;
    int countOfWhiteChips = 2;

    boolean blackMovesPossible[][];
    boolean whiteMovesPossible[][];

    ArrayList[][] blackPossibleDirections;
    ArrayList[][] whitePossibleDirections;

    int previousMoveRow = 0;
    int previousMoveColumn = 0;

    TextView blackChipCount;
    TextView whiteChipCount;

    int cRow = 0;
    int cCol = 0;

    boolean proceed = true;

    int bestRow = 0;
    int bestCol = 0;

    int opponentLevel = 1;

    int opponentPlayer = WHITE_CHIP;
    int Player = BLACK_CHIP;

    GameState defaultState;

    int gameIndex = -1;

    final Handler eventHandlerHighlight = new Handler();
    final Handler eventHandlerAI = new Handler();
    final Handler eventHandlerError = new Handler();
    final Handler quitMsg = new Handler();

    Runnable animation;
    Runnable opponentMove;
    Runnable findMove;

    boolean flash = true;

    public MainActivity() {
        blackMovesPossible = new boolean[BOARD_LENGTH][BOARD_LENGTH];
        whiteMovesPossible = new boolean[BOARD_LENGTH][BOARD_LENGTH];
        blackPossibleDirections = new ArrayList[BOARD_LENGTH][BOARD_LENGTH];
        whitePossibleDirections = new ArrayList[BOARD_LENGTH][BOARD_LENGTH];
        gameBoard = new int[BOARD_LENGTH][BOARD_LENGTH];
        isWhiteMove = false;
        isBlackMove = true;
        previousWhiteMove = true;
        previousBlackMove = false;
        isEndOfGame = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        defaultState = new GameState();
        saveGameState(defaultState);

        setContentView(R.layout.game_settings);
        PlayWithOpponent(new View(this));
    }

    @Override
    public void onClick(final View view) {

        char id = Integer.toString(view.getId()).charAt(0);

        if (id == '2' && proceed) {

            proceed = false;
            handleMove(view, true);
        }
    }

    public void handleMove(final View view, final boolean updateDisplay) {

        final int row = Character.getNumericValue(Integer.toString(view.getId()).charAt(1));
        final int col = Character.getNumericValue(Integer.toString(view.getId()).charAt(2));

        if (!isEndOfGame && isWhiteMove && whiteMovesPossible[row][col]) {

            performMove(view.getId(), WHITE_CHIP, updateDisplay);

        } else if (!isEndOfGame && isBlackMove && blackMovesPossible[row][col]) {

            performMove(view.getId(), BLACK_CHIP, updateDisplay);

        } else {

            Runnable error = new Runnable() {

                @Override
                public void run() {

                    int newId = Integer.parseInt("2" + row + "" + col);
                    int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    ImageView image = (ImageView) findViewById(resourceID);

                    if (!(row == previousMoveRow && col == previousMoveColumn) && !isEndOfGame)
                        if (flash) {
                            image.setBackgroundColor(getResources().getColor(R.color.lightGray));
                            flash = false;
                            eventHandlerError.postDelayed(this, 50);

                        } else {
                            image.setBackgroundColor(Color.TRANSPARENT);
                            flash = true;
                            proceed = true;
                        }
                    else
                        proceed = true;
                }
            };

            eventHandlerError.post(error);

            if (!isEndOfGame) {

                Toast toast = Toast.makeText(getApplicationContext(), "Ход невозможен!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    public void handlePostMove() {

        updateCount();
        clearGrid();
        checkStateOfBoard();

        gameIndex++;

        if (opponentPlayer == EMPTY_CHIP && Player == EMPTY_CHIP && !isEndOfGame)
            drawPossibleMoves();

        if (opponentPlayer == WHITE_CHIP) {

            if (isBlackMove && !isEndOfGame)
                drawPossibleMoves();
            else if (isWhiteMove && !isEndOfGame) {

                opponentMove = new Runnable() {

                    @Override
                    public void run() {
                        makeAiMove();
                    }
                };

                eventHandlerAI.postDelayed(opponentMove, 50);
            }

        } else if (opponentPlayer == BLACK_CHIP) {

            if (isWhiteMove && !isEndOfGame)
                drawPossibleMoves();
            else if (isBlackMove && !isEndOfGame) {

                opponentMove = new Runnable() {

                    @Override
                    public void run() {
                        makeAiMove();
                    }
                };

                eventHandlerAI.postDelayed(opponentMove, 50);
            }
        }
    }

    public void makeAiMove() {

        GameState state = new GameState();
        saveGameState(state);

        bestRow = 0;
        bestCol = 0;

        final GameState newState = state;

        findMove = new Runnable() {
            @Override
            public void run() {
                miniMax(newState, newState, opponentLevel, true, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);

                quitMsg.post(new Runnable() {
                    @Override
                    public void run() {
                        int resID = getResources().getIdentifier("2" + bestRow + "" + bestCol, "id", getPackageName());
                        ImageView image = (ImageView) findViewById(resID);

                        onClick(image);
                    }
                });
            }
        };

        Thread thread = new Thread(findMove);
        thread.start();
    }

    public int miniMax(GameState originalState, GameState state, int depth, boolean maximizingPlayer, int alpha, int beta) {

        if (depth == 0 || isEndOfGame)
            return evaluateBoard(state, originalState);

        boolean movesPossible[][];

        if (maximizingPlayer)
            if (opponentPlayer == WHITE_CHIP)
                movesPossible = whiteMovesPossible;
            else
                movesPossible = blackMovesPossible;
        else if (opponentPlayer == WHITE_CHIP)
            movesPossible = blackMovesPossible;
        else
            movesPossible = whiteMovesPossible;

        if (maximizingPlayer) {

            int bestValue = Integer.MIN_VALUE;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (movesPossible[i][j]) {

                        if (opponentPlayer == WHITE_CHIP)
                            makeMove(Integer.parseInt("2" + i + "" + j), WHITE_CHIP, false, whitePossibleDirections, true);
                        else
                            makeMove(Integer.parseInt("2" + i + "" + j), BLACK_CHIP, false, blackPossibleDirections, true);

                        boolean whiteMoves = findMoves(opponentPlayer);
                        boolean blackMoves = findMoves(Player);

                        if (!whiteMoves && !blackMoves)
                            isEndOfGame = true;

                        GameState childState = new GameState();
                        saveGameState(childState);

                        int value;

                        if (isRepeatMove(opponentPlayer, childState))
                            value = miniMax(originalState, childState, depth - 1, true, alpha, beta);
                        else
                            value = miniMax(originalState, childState, depth - 1, false, bestValue, beta);

                        recoverState(state);

                        bestValue = getMax(bestValue, value);

                        alpha = getMax(alpha, value);

                        if (value == bestValue && depth == opponentLevel) {

                            bestRow = i;
                            bestCol = j;
                        }

                        if (beta <= alpha)
                            break;
                    }

            return bestValue;

        } else {

            int bestValue = Integer.MAX_VALUE;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (movesPossible[i][j]) {

                        if (Player == WHITE_CHIP)
                            makeMove(Integer.parseInt("2" + i + "" + j), WHITE_CHIP, false, whitePossibleDirections, true);
                        else
                            makeMove(Integer.parseInt("2" + i + "" + j), BLACK_CHIP, false, blackPossibleDirections, true);

                        boolean whiteMoves = findMoves(Player);
                        boolean blackMoves = findMoves(opponentPlayer);

                        if (!whiteMoves && !blackMoves)
                            isEndOfGame = true;

                        GameState childState = new GameState();
                        saveGameState(childState);

                        int value;

                        if (isRepeatMove(Player, childState))
                            value = miniMax(originalState, childState, depth - 1, false, beta, alpha);
                        else
                            value = miniMax(originalState, childState, depth - 1, true, alpha, bestValue);

                        recoverState(state);

                        bestValue = getMin(bestValue, value);

                        beta = getMin(beta, bestValue);

                        if (beta <= alpha)
                            break;
                    }

            return bestValue;
        }
    }

    public int getMin(int a, int b) {

        if (a < b)
            return a;
        else
            return b;
    }

    public int getMax(int a, int b) {

        if (a > b)
            return a;
        else
            return b;
    }

    public int evaluateBoard(GameState state, GameState originalState) {

        int score = 0;

        if (state.isEndOfGame) {

            int playerChip = 0;
            int opponentChip = 0;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (state.gameBoard[i][j] == opponentPlayer)
                        playerChip++;
                    else if (state.gameBoard[i][j] == Player)
                        opponentChip++;

            if (playerChip > opponentChip)
                return Integer.MAX_VALUE;
            else if (opponentChip > playerChip)
                return Integer.MIN_VALUE;
            else return 0;

        } else {

            int value[][] = {
                    {10000, -100, 50, 50, 50, 50, -100, 10000},
                    {-100, -100, 15, 15, 15, 15, -100, -100},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {-100, -100, 15, 15, 15, 15, -100, -100},
                    {10000, -100, 50, 50, 50, 50, -100, 10000}
            };

            if (originalState.gameBoard[0][0] != EMPTY_CHIP) {

                value[0][1] = 50;
                value[1][0] = 50;
                value[1][0] = 50;
            }

            if (originalState.gameBoard[7][0] != EMPTY_CHIP) {

                value[0][6] = 50;
                value[1][6] = 50;
                value[1][7] = 50;
            }

            if (originalState.gameBoard[7][0] != EMPTY_CHIP) {

                value[7][1] = 50;
                value[6][1] = 50;
                value[6][0] = 50;
            }

            if (originalState.gameBoard[7][7] != EMPTY_CHIP) {

                value[6][7] = 50;
                value[6][6] = 50;
                value[7][6] = 50;
            }

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (originalState.gameBoard[i][j] != opponentPlayer && state.gameBoard[i][j] == opponentPlayer)
                        score += value[i][j];
                    else if (originalState.gameBoard[i][j] != Player && state.gameBoard[i][j] == Player)
                        score -= value[i][j];
        }

        return score;
    }

    public boolean isRepeatMove(int chipColor, GameState gameState) {

        boolean movesPossible[][] = new boolean[BOARD_LENGTH][BOARD_LENGTH];

        if (chipColor == WHITE_CHIP)
            movesPossible = gameState.whiteMovesPossible;
        else if (chipColor == BLACK_CHIP)
            movesPossible = gameState.blackMovesPossible;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (!movesPossible[i][j])
                    return false;

        return true;
    }

    public void recoverState(GameState gameState) {

        countOfBlackChips = gameState.countOfBlackChips;
        countOfWhiteChips = gameState.countOfWhiteChips;

        isWhiteMove = gameState.isWhiteMove;
        isBlackMove = gameState.isBlackMove;

        isEndOfGame = gameState.isEndOfGame;

        previousMoveRow = gameState.previousMoveRow;
        previousMoveColumn = gameState.previousMoveColumn;

        opponentPlayer = gameState.opponentPlayer;
        Player = gameState.Player;
        opponentLevel = gameState.opponentLevel;

        gameIndex = gameState.gameIndex;

        previousBlackMove = gameState.previousBlackMove;
        previousWhiteMove = gameState.previousWhiteMove;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                blackMovesPossible[i][j] = gameState.blackMovesPossible[i][j];
                whiteMovesPossible[i][j] = gameState.whiteMovesPossible[i][j];
                gameBoard[i][j] = gameState.gameBoard[i][j];

                try {
                    blackPossibleDirections[i][j].clear();
                } catch (NullPointerException e) {
                    System.out.println();
                }

                try {
                    whitePossibleDirections[i][j].clear();
                } catch (NullPointerException e) {
                    System.out.println();
                }

                try {
                    blackPossibleDirections[i][j].addAll(gameState.blackPossibleDirections[i][j]);
                } catch (NullPointerException e) {
                    System.out.println();
                }

                try {
                    whitePossibleDirections[i][j].addAll(gameState.whitePossibleDirections[i][j]);
                } catch (NullPointerException e) {
                    System.out.println();
                }
            }
    }

    public void saveGameState(GameState gameState) {

        gameState.countOfBlackChips = countOfBlackChips;
        gameState.countOfWhiteChips = countOfWhiteChips;

        gameState.isWhiteMove = isWhiteMove;
        gameState.isBlackMove = isBlackMove;

        gameState.isEndOfGame = isEndOfGame;

        gameState.previousMoveRow = previousMoveRow;
        gameState.previousMoveColumn = previousMoveColumn;

        gameState.opponentPlayer = opponentPlayer;
        gameState.Player = Player;
        gameState.opponentLevel = opponentLevel;

        gameState.gameIndex = gameIndex;

        gameState.previousWhiteMove = previousWhiteMove;
        gameState.previousBlackMove = previousBlackMove;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                gameState.gameBoard[i][j] = gameBoard[i][j];
                gameState.blackMovesPossible[i][j] = blackMovesPossible[i][j];
                gameState.whiteMovesPossible[i][j] = whiteMovesPossible[i][j];

                try {
                    gameState.blackPossibleDirections[i][j].addAll(blackPossibleDirections[i][j]);
                } catch (NullPointerException e) {
                    System.out.println();
                }

                try {
                    gameState.whitePossibleDirections[i][j].addAll(whitePossibleDirections[i][j]);
                } catch (NullPointerException e) {
                    System.out.println();
                }
            }
    }

    @SuppressLint("SetTextI18n")
    public void updateCount() {

        countOfBlackChips = 0;
        countOfWhiteChips = 0;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (gameBoard[i][j] == WHITE_CHIP)
                    countOfWhiteChips++;
                else if (gameBoard[i][j] == BLACK_CHIP)
                    countOfBlackChips++;

        blackChipCount.setText(Integer.toString(countOfBlackChips));
        whiteChipCount.setText(Integer.toString(countOfWhiteChips));
    }

    public void clearGrid() {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                int viewID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                ImageView image = (ImageView) findViewById(viewID);

                if (isBlackMove && whiteMovesPossible[i][j] && gameBoard[i][j] == EMPTY_CHIP)
                    image.setImageDrawable(null);
                else if (isWhiteMove && blackMovesPossible[i][j] && gameBoard[i][j] == EMPTY_CHIP)
                    image.setImageDrawable(null);

                whiteMovesPossible[i][j] = false;
                blackMovesPossible[i][j] = false;
            }
    }

    public void checkStateOfBoard() {

        boolean whiteMoves = findMoves(WHITE_CHIP);
        boolean blackMoves = findMoves(BLACK_CHIP);

        if (!whiteMoves && !blackMoves) {

            isBlackMove = false;
            isWhiteMove = false;
            isEndOfGame = true;

            if (countOfBlackChips > countOfWhiteChips && opponentPlayer == BLACK_CHIP) {

                Toast toast = Toast.makeText(getApplicationContext(), "Соперник выиграл!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (countOfBlackChips < countOfWhiteChips && opponentPlayer == WHITE_CHIP) {

                Toast toast = Toast.makeText(getApplicationContext(), "Соперник выиграл!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (countOfBlackChips > countOfWhiteChips && Player == BLACK_CHIP) {

                Toast toast = Toast.makeText(getApplicationContext(), "Вы выиграли!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (countOfBlackChips < countOfWhiteChips && Player == WHITE_CHIP) {

                Toast toast = Toast.makeText(getApplicationContext(), "Вы выиграли!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            else {

                Toast toast = Toast.makeText(getApplicationContext(), "Ничья", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

        } else if (!whiteMoves && blackMoves) {

            isWhiteMove = false;
            isBlackMove = true;

        } else if (!blackMoves && whiteMoves) {

            isBlackMove = false;
            isWhiteMove = true;
        }
    }

    public void drawPossibleMoves() {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (whiteMovesPossible[i][j] && isWhiteMove) {

                    int resID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                    ImageView image = (ImageView) findViewById(resID);
                    image.setImageDrawable(getResources().getDrawable(R.drawable.circle_white_move));

                } else if (blackMovesPossible[i][j] && isBlackMove) {

                    int resID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                    ImageView image = (ImageView) findViewById(resID);
                    image.setImageDrawable(getResources().getDrawable(R.drawable.circle_black_move));
                }
    }

    public int getOppositeColor(int color) {

        if (color == WHITE_CHIP)
            return BLACK_CHIP;
        else
            return WHITE_CHIP;
    }

    public void switchMoves() {

        boolean isWhiteMoveCopy = isWhiteMove;

        isWhiteMove = isBlackMove;
        isBlackMove = isWhiteMoveCopy;
    }

    public void setupGrid() {

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.gridContainer);

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                gameBoard[i][j] = 0;

        LinearLayout.LayoutParams imageSize = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Display display = getWindowManager().getDefaultDisplay();
        imageSize.width = (display.getWidth() / (BOARD_LENGTH + 1));
        imageSize.height = imageSize.width;

        LinearLayout.LayoutParams verticalLine = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        verticalLine.width = 1;
        verticalLine.height = imageSize.height;

        LinearLayout.LayoutParams horizontalLine = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        horizontalLine.width = (imageSize.width) * BOARD_LENGTH + 9;
        horizontalLine.height = 1;

        for (int i = 0; i < BOARD_LENGTH; i++) {

            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.HORIZONTAL);
            grid.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            if (i == 0) {

                View horizontalBorder = new View(this);
                horizontalBorder.setBackgroundColor(getResources().getColor(R.color.lightGray));
                horizontalBorder.setLayoutParams(horizontalLine);

                mainLayout.addView(horizontalBorder);
            }

            View verticalBorder = new View(this);
            verticalBorder.setBackgroundColor(getResources().getColor(R.color.lightGray));
            verticalBorder.setLayoutParams(verticalLine);

            grid.addView(verticalBorder);

            for (int j = 0; j < BOARD_LENGTH; j++) {

                ImageView image = new ImageView(this);
                image.setLayoutParams(imageSize);
                image.setId(Integer.parseInt("2" + i + "" + j));
                image.setBackgroundColor(Color.parseColor("#00000000"));
                image.setOnClickListener(this);

                grid.addView(image);

                View line = new View(this);
                line.setBackgroundColor(getResources().getColor(R.color.lightGray));
                line.setLayoutParams(verticalLine);

                grid.addView(line);
            }

            View line = new View(this);
            line.setBackgroundColor(getResources().getColor(R.color.lightGray));
            line.setLayoutParams(horizontalLine);

            mainLayout.addView(grid);

            mainLayout.addView(line);
        }
    }

    public void setChips() {

        ImageView image;

        image = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(233), "id", getPackageName()));
        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));

        gameBoard[3][3] = WHITE_CHIP;

        image = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(234), "id", getPackageName()));
        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));
        gameBoard[3][4] = BLACK_CHIP;

        image = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(243), "id", getPackageName()));
        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));
        gameBoard[4][3] = BLACK_CHIP;

        image = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(244), "id", getPackageName()));
        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));
        gameBoard[4][4] = WHITE_CHIP;
    }

    public void initialSetup() {

        setupGrid();

        blackChipCount = (TextView) findViewById(R.id.blackScore);
        whiteChipCount = (TextView) findViewById(R.id.whiteScore);

        setChips();

        checkStateOfBoard();

        drawPossibleMoves();
    }

    public void PlayWithOpponent(View v) {

        gameIndex = -1;

        final SeekBar opponentDifficulty = (SeekBar) findViewById(R.id.seekBar);

        LayerDrawable layerDrawable = (LayerDrawable) opponentDifficulty.getProgressDrawable();
        Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
        progressDrawable.setColorFilter(Color.parseColor("#008000"), PorterDuff.Mode.SRC_IN);

        opponentDifficulty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (i<1) opponentDifficulty.setProgress(1);

                opponentLevel = opponentDifficulty.getProgress();

                LayerDrawable layerDrawable = (LayerDrawable) opponentDifficulty.getProgressDrawable();
                Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);

                if (opponentLevel == 1) {

                    progressDrawable.setColorFilter(Color.parseColor("#008000"), PorterDuff.Mode.SRC_IN);

                } else if (opponentLevel == 2) {

                    progressDrawable.setColorFilter(Color.parseColor("#ADFF2F"), PorterDuff.Mode.SRC_IN);

                } else if (opponentLevel == 3) {

                    progressDrawable.setColorFilter(Color.parseColor("#FFF700"), PorterDuff.Mode.SRC_IN);

                } else if (opponentLevel == 4) {

                    progressDrawable.setColorFilter(Color.parseColor("#FF4500"), PorterDuff.Mode.SRC_IN);

                } else if (opponentLevel == 5) {

                    progressDrawable.setColorFilter(Color.parseColor("#B22222"), PorterDuff.Mode.SRC_IN);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public boolean findMoves(int chipColor) {

        boolean anyMoves = false;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (gameBoard[i][j] == chipColor) {

                    boolean possible = getMoves(chipColor, i, j);

                    if (!anyMoves)
                        anyMoves = possible;
                }

        return anyMoves;
    }

    public boolean getMoves(int chipColor, int row, int column) {

        boolean anyMoves = false;

        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++)
                if (!(i == 0 && j == 0))
                    if (isLegalInDirection(chipColor, row, column, i, j)) {

                        boolean possible = checkDirections(chipColor, row, column, i, j);

                        if (!anyMoves)
                            anyMoves = possible;
                    }

        return anyMoves;
    }

    public boolean checkDirections(int chipColor, int row, int column, int deltaCol, int deltaRow) {

        boolean possibleMove = true;

        row = row + deltaRow;
        column = column + deltaCol;

        int nextPositionColour;

        try {
            nextPositionColour = gameBoard[row][column];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        if (nextPositionColour == chipColor)
            possibleMove = false;
        else if (nextPositionColour == getOppositeColor(chipColor))
            possibleMove = checkDirections(chipColor, row, column, deltaCol, deltaRow);
        else if (nextPositionColour == EMPTY_CHIP) {

            if (chipColor == WHITE_CHIP) {
                whiteMovesPossible[row][column] = true;
                whitePossibleDirections[row][column].add(-deltaCol + "." + -deltaRow);
            } else {
                blackMovesPossible[row][column] = true;
                blackPossibleDirections[row][column].add(-deltaCol + "." + -deltaRow);
            }
        }

        return possibleMove;
    }

    public boolean isLegalInDirection(int chipColor, int row, int column, int deltaColumn, int deltaRow) {

        boolean legalDirection = true;

        int newColumn = column + deltaColumn;
        int newRow = row + deltaRow;

        int nextPositionColor;

        try {
            nextPositionColor = gameBoard[newRow][newColumn];
        } catch (ArrayIndexOutOfBoundsException e) {

            return false;
        }

        if (nextPositionColor == chipColor || nextPositionColor == EMPTY_CHIP)
            legalDirection = false;

        return legalDirection;
    }

    public void makeMove(int viewID, int chipColor, boolean changeImage, ArrayList<String> possibleDirections[][], boolean callFromMiniMax) {

        final int row = Character.getNumericValue(Integer.toString(viewID).charAt(1));
        final int column = Character.getNumericValue(Integer.toString(viewID).charAt(2));

        previousMoveRow = row;
        previousMoveColumn = column;

        for (int i = 0; i < possibleDirections[row][column].size(); i++) {

            String[] delta = possibleDirections[row][column].get(i).split("\\.");

            int deltaCol = Integer.parseInt(delta[0]);
            int deltaRow = Integer.parseInt(delta[1]);

            int currentRow = row;
            int currentColumn = column;

            if (i > 0) {

                currentRow = currentRow + deltaRow;
                currentColumn = currentColumn + deltaCol;
            }

            while (gameBoard[currentRow][currentColumn] != chipColor) {

                int newId = Integer.parseInt("2" + currentRow + "" + currentColumn);
                int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());

                ImageView image = (ImageView) findViewById(resourceID);

                if (chipColor == WHITE_CHIP)
                    gameBoard[currentRow][currentColumn] = WHITE_CHIP;
                else
                    gameBoard[currentRow][currentColumn] = BLACK_CHIP;

                if (changeImage)
                    if (chipColor == WHITE_CHIP)
                        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));
                    else
                        image.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));

                currentRow = currentRow + deltaRow;
                currentColumn = currentColumn + deltaCol;
            }
        }

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {
                whitePossibleDirections[i][j].clear();
                blackPossibleDirections[i][j].clear();
            }

        if (!callFromMiniMax) {
            switchMoves();
            handlePostMove();
            proceed = true;
        }
    }

    public void performMove(final int viewID, final int chipColor, final boolean changeImage) {

        final int row = Character.getNumericValue(Integer.toString(viewID).charAt(1));
        final int column = Character.getNumericValue(Integer.toString(viewID).charAt(2));

        cRow = previousMoveRow;
        cCol = previousMoveColumn;

        int newId = Integer.parseInt("2" + previousMoveRow + "" + previousMoveColumn);
        int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
        ImageView image = (ImageView) findViewById(resourceID);

        image.setBackgroundColor(Color.TRANSPARENT);

        int rowDirectionTemp = 0;
        int columnDirectionTemp = 0;

        if (previousMoveRow > row)
            rowDirectionTemp = -1;
        else if (previousMoveRow < row)
            rowDirectionTemp = 1;

        if (previousMoveColumn > column)
            columnDirectionTemp = -1;
        else if (previousMoveColumn < column)
            columnDirectionTemp = 1;

        final int rowDirection = rowDirectionTemp;
        final int columnDirection = columnDirectionTemp;

        animation = new Runnable() {

            @Override
            public void run() {

                if (cRow != row) {

                    int newId = Integer.parseInt("2" + cRow + "" + cCol);
                    int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    ImageView image = (ImageView) findViewById(resourceID);
                    image.setBackgroundColor(Color.TRANSPARENT);

                    cRow += rowDirection;

                    newId = Integer.parseInt("2" + cRow + "" + cCol);
                    resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    image = (ImageView) findViewById(resourceID);
                    image.setBackgroundColor(getResources().getColor(R.color.lightGray));

                } else if (cCol != column) {

                    int newId;
                    newId = Integer.parseInt("2" + cRow + "" + cCol);
                    int resourceID;
                    resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    ImageView image = (ImageView) findViewById(resourceID);
                    image.setBackgroundColor(Color.TRANSPARENT);


                    cCol += columnDirection;

                    newId = Integer.parseInt("2" + cRow + "" + cCol);
                    resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    image = (ImageView) findViewById(resourceID);
                    image.setBackgroundColor(getResources().getColor(R.color.lightGray));

                } else {

                    if (chipColor == WHITE_CHIP)
                        makeMove(viewID, chipColor, changeImage, whitePossibleDirections, false);
                    else if (chipColor == BLACK_CHIP)
                        makeMove(viewID, chipColor, changeImage, blackPossibleDirections, false);

                    return;
                }

                eventHandlerHighlight.postDelayed(this, 35);
            }
        };

        if (changeImage)
            eventHandlerHighlight.post(animation);
        else if (chipColor == WHITE_CHIP)
            makeMove(viewID, chipColor, false, whitePossibleDirections, false);
        else if (chipColor == BLACK_CHIP)
            makeMove(viewID, chipColor, false, blackPossibleDirections, false);
    }

    public void selectBlack(View v) {

        Player = BLACK_CHIP;
        opponentPlayer = WHITE_CHIP;

        ImageButton img = (ImageButton) findViewById(R.id.blackPiece);
        img.setBackgroundResource(R.drawable.stroke);

        ImageButton imgOther = (ImageButton) findViewById(R.id.whitePiece);
        imgOther.setBackgroundResource(android.R.color.transparent);
    }

    public void selectWhite(View v) {

        Player = WHITE_CHIP;
        opponentPlayer = BLACK_CHIP;

        ImageButton imgWhite = (ImageButton) findViewById(R.id.whitePiece);
        imgWhite.setBackgroundResource(R.drawable.stroke);

        ImageButton imgBlack = (ImageButton) findViewById(R.id.blackPiece);
        imgBlack.setBackgroundResource(android.R.color.transparent);
    }

    public void startGame(View v) {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                whitePossibleDirections[i][j] = new ArrayList<>();
                blackPossibleDirections[i][j] = new ArrayList<>();
            }

        setContentView(activity_main);

        initialSetup();

        saveGameState(defaultState);

        if (opponentPlayer == BLACK_CHIP) {

            ((TextView) findViewById(R.id.whiteTitle)).setText("Ваш счет: ");
            ((TextView) findViewById(R.id.blackTitle)).setText("Счет соперника: ");

            if (isWhiteMove && !isEndOfGame)
                drawPossibleMoves();
            else if (isBlackMove && !isEndOfGame) {

                final Handler eventHandler = new Handler();

                eventHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        makeAiMove();
                    }
                }, 50);
            }

        } else {

            ((TextView) findViewById(R.id.whiteTitle)).setText("Счет соперника: ");
            ((TextView) findViewById(R.id.blackTitle)).setText("Ваш счет: ");
        }

        gameIndex++;
    }

    @Override
    public void onBackPressed() {

        recoverState(defaultState);
        proceed = true;

        Intent intent = new Intent(this, GameSettings.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right);
    }

    public void restart(View v) {

        recoverState(defaultState);
        proceed = true;

        startGame(v);
    }
}
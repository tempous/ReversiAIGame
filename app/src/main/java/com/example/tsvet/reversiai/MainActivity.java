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

    boolean isWhiteTurn = false;
    boolean isBlackTurn = true;

    boolean previousWhiteTurn = true;
    boolean previousBlackTurn = false;
    
    static final int BOARD_LENGTH = 8;

    int gameBoard[][] = new int[BOARD_LENGTH][BOARD_LENGTH];

    static final int EMPTY_PIECE = 0;
    static final int WHITE_PIECE = 1;
    static final int BLACK_PIECE = 2;
    
    boolean gameEnd = false;
    
    int amountOfBlackPieces = 2;
    int amountOfWhitePieces = 2;

    boolean blackMovesPossible[][] = new boolean[BOARD_LENGTH][BOARD_LENGTH];
    boolean whiteMovesPossible[][] = new boolean[BOARD_LENGTH][BOARD_LENGTH];

    ArrayList<String> blackPossibleDirections[][] = new ArrayList[BOARD_LENGTH][BOARD_LENGTH];
    ArrayList<String> whitePossibleDirections[][] = new ArrayList[BOARD_LENGTH][BOARD_LENGTH];

    int previousMoveRow = 0;
    int previousMoveColumn = 0;

    TextView blackPieceNumber;
    TextView whitePieceNumber;

    int cRow = 0;
    int cCol = 0;

    boolean proceed = true;

    int bestRow = 0;
    int bestCol = 0;

    int aiLevel = 1;

    int aiPlayer = WHITE_PIECE;
    int humanPlayer = BLACK_PIECE;

    GameState defaultState;

    int gameIndex = -1;

    final Handler handlerHighlight = new Handler();
    final Handler handlerAI = new Handler();
    final Handler handlerError = new Handler();
    final Handler quitMsg = new Handler();

    Runnable animation;
    Runnable aiMove;
    Runnable findMove;

    boolean flash = true;
    boolean entered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        defaultState = new GameState();
        saveGameState(defaultState);

        setContentView(R.layout.game_settings);
        PlayWithAI(new View(this));
    }

    @Override
    public void onClick(final View view) {

        System.out.print("");
        char identifier = Integer.toString(view.getId()).charAt(0);

        if (identifier == '2' && proceed) {

            lockInput();
            handleMove(view, true);
        }
    }

    public void handleMove(final View view, final boolean updateDisplay) {

        final int row = Character.getNumericValue(Integer.toString(view.getId()).charAt(1));
        final int col = Character.getNumericValue(Integer.toString(view.getId()).charAt(2));

        if (!gameEnd && isWhiteTurn && whiteMovesPossible[row][col]) {

            performMove(view.getId(), WHITE_PIECE, true, updateDisplay);

        } else if (!gameEnd && isBlackTurn && blackMovesPossible[row][col]) {

            performMove(view.getId(), BLACK_PIECE, true, updateDisplay);

        } else {

            Runnable error = new Runnable() {

                @Override
                public void run() {

                    int newId = Integer.parseInt("2" + row + "" + col);
                    int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    ImageView button = (ImageView) findViewById(resourceID);

                    if (!(row == previousMoveRow && col == previousMoveColumn) && !gameEnd)
                        if (flash) {
                            button.setBackgroundColor(getResources().getColor(R.color.lightGray));
                            flash = false;
                            handlerError.postDelayed(this, 50);

                        } else {
                            button.setBackgroundColor(Color.TRANSPARENT);
                            flash = true;
                            unlockInput();
                        }
                    else
                        unlockInput();
                }
            };

            handlerError.post(error);

            if (!gameEnd) {

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

        if (aiPlayer == EMPTY_PIECE && humanPlayer == EMPTY_PIECE && !gameEnd)
            paintPossibleMoves();

        if (aiPlayer == WHITE_PIECE) {

            if (isBlackTurn && !gameEnd)
                paintPossibleMoves();
            else if (isWhiteTurn && !gameEnd) {

                aiMove = new Runnable() {

                    @Override
                    public void run() {
                        makeAiMove();
                    }
                };

                handlerAI.postDelayed(aiMove, 50);
            }

        } else if (aiPlayer == BLACK_PIECE) {

            if (isWhiteTurn && !gameEnd)
                paintPossibleMoves();
            else if (isBlackTurn && !gameEnd) {

                aiMove = new Runnable() {

                    @Override
                    public void run() {
                        makeAiMove();
                    }
                };

                handlerAI.postDelayed(aiMove, 50);
            }
        }
    }

    public void makeAiMove() {

        GameState state = new GameState();
        saveGameState(state);

        bestRow = 0;
        bestCol = 0;

        System.out.println("\nНовое движение\n");

        final GameState state2 = state;

        findMove = new Runnable() {
            @Override
            public void run() {
                miniMax(state2, state2, aiLevel, true, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);

                quitMsg.post(new Runnable() {
                    @Override
                    public void run() {
                        int resID = getResources().getIdentifier("2" + bestRow + "" + bestCol, "id", getPackageName());
                        ImageView button = (ImageView) findViewById(resID);

                        onClick(button);
                    }
                });
            }
        };

        Thread thread = new Thread(findMove);
        thread.start();
    }

    public int miniMax(GameState originalState, GameState state, int depth, boolean maximizingPlayer, int alpha, int beta) {

        if (depth == 0 || gameEnd)
            return evaluateBoard(state, originalState);

        boolean movesPossible[][];

        if (maximizingPlayer)
            if (aiPlayer == WHITE_PIECE)
                movesPossible = whiteMovesPossible;
            else
                movesPossible = blackMovesPossible;
        else if (aiPlayer == WHITE_PIECE)
            movesPossible = blackMovesPossible;
        else
            movesPossible = whiteMovesPossible;

        if (maximizingPlayer) {

            int bestValue = Integer.MIN_VALUE;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (movesPossible[i][j]) {

                        if (aiPlayer == WHITE_PIECE)
                            makeMove(Integer.parseInt("2" + i + "" + j), WHITE_PIECE, true, false, whitePossibleDirections, true);
                        else
                            makeMove(Integer.parseInt("2" + i + "" + j), BLACK_PIECE, true, false, blackPossibleDirections, true);

                        clearGridAlternative();

                        boolean whiteMoves = findMoves(aiPlayer);
                        boolean blackMoves = findMoves(humanPlayer);

                        if (!whiteMoves && !blackMoves)
                            gameEnd = true;

                        GameState childState = new GameState();
                        saveGameState(childState);

                        int value;

                        System.out.println("\nНовое движение\n");

                        System.out.println("Координаты: " + i + " " + j + ". Глубина: " + depth + ". Entering depth: " + (depth - 1) + ". Альфа :" + alpha + " Бета: " + beta);

                        if (isRepeatTurn(aiPlayer, childState))
                            value = miniMax(originalState, childState, depth - 1, true, alpha, beta);
                        else
                            value = miniMax(originalState, childState, depth - 1, false, bestValue, beta);

                        recoverState(state);

                        bestValue = getMax(bestValue, value);

                        alpha = getMax(alpha, value);

                        if (value == bestValue && depth == aiLevel) {

                            bestRow = i;
                            bestCol = j;
                        }

                        System.out.println("Координаты: " + i + " " + j + ". Глубина: " + depth + ". Счет: " + value + "." + " Альфа :" + alpha + " Бета: " + beta);

                        if (beta <= alpha)
                            break;
                    }

            return bestValue;

        } else {

            int bestValue = Integer.MAX_VALUE;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (movesPossible[i][j]) {

                        if (humanPlayer == WHITE_PIECE)
                            makeMove(Integer.parseInt("2" + i + "" + j), WHITE_PIECE, true, false, whitePossibleDirections, true);
                        else
                            makeMove(Integer.parseInt("2" + i + "" + j), BLACK_PIECE, true, false, blackPossibleDirections, true);

                        clearGridAlternative();

                        boolean whiteMoves = findMoves(humanPlayer);
                        boolean blackMoves = findMoves(aiPlayer);

                        if (!whiteMoves && !blackMoves)
                            gameEnd = true;

                        GameState childState = new GameState();
                        saveGameState(childState);

                        System.out.println("Координаты: " + i + " " + j + ". Глубина: " + depth + ". Исходная глубина: " + (depth - 1) + ". Альфа :" + alpha + " Бета: " + beta);

                        int value;

                        if (isRepeatTurn(humanPlayer, childState))
                            value = miniMax(originalState, childState, depth - 1, false, beta, alpha);
                        else
                            value = miniMax(originalState, childState, depth - 1, true, alpha, bestValue);

                        recoverState(state);

                        bestValue = getMin(bestValue, value);

                        beta = getMin(beta, bestValue);

                        System.out.println("Координаты: " + i + " " + j + ". Глубина: " + depth + ". Счет: " + value + "." + " Альфа :" + alpha + " Бета: " + beta);

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

        if (state.gameEnd) {

            int playerPiece = 0;
            int enemyPiece = 0;

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (state.gameBoard[i][j] == aiPlayer)
                        playerPiece++;
                    else if (state.gameBoard[i][j] == humanPlayer)
                        enemyPiece++;

            if (playerPiece > enemyPiece)
                return Integer.MAX_VALUE;
            else if (enemyPiece > playerPiece)
                return Integer.MIN_VALUE;
            else return 0;

        } else {

            int valueOfLoc[][] = {{10000, -100, 50, 50, 50, 50, -100, 10000},
                    {-100, -100, 15, 15, 15, 15, -100, -100},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {50, 15, 15, 15, 15, 15, 15, 50},
                    {-100, -100, 15, 15, 15, 15, -100, -100},
                    {10000, -100, 50, 50, 50, 50, -100, 10000}};

            if (originalState.gameBoard[0][0] != EMPTY_PIECE) {

                valueOfLoc[0][1] = 50;
                valueOfLoc[1][0] = 50;
                valueOfLoc[1][0] = 50;
            }

            if (originalState.gameBoard[7][0] != EMPTY_PIECE) {

                valueOfLoc[0][6] = 50;
                valueOfLoc[1][6] = 50;
                valueOfLoc[1][7] = 50;
            }

            if (originalState.gameBoard[7][0] != EMPTY_PIECE) {

                valueOfLoc[7][1] = 50;
                valueOfLoc[6][1] = 50;
                valueOfLoc[6][0] = 50;
            }

            if (originalState.gameBoard[7][7] != EMPTY_PIECE) {

                valueOfLoc[6][7] = 50;
                valueOfLoc[6][6] = 50;
                valueOfLoc[7][6] = 50;
            }

            for (int i = 0; i < BOARD_LENGTH; i++)
                for (int j = 0; j < BOARD_LENGTH; j++)
                    if (originalState.gameBoard[i][j] != aiPlayer && state.gameBoard[i][j] == aiPlayer)
                        score += valueOfLoc[i][j];
                    else if (originalState.gameBoard[i][j] != humanPlayer && state.gameBoard[i][j] == humanPlayer)
                        score -= valueOfLoc[i][j];
        }

        return score;
    }

    public boolean isRepeatTurn(int pieceColor, GameState gameState) {

        boolean movesPossible[][] = new boolean[BOARD_LENGTH][BOARD_LENGTH];

        if (pieceColor == WHITE_PIECE)
            movesPossible = gameState.whiteMovesPossible;
        else if (pieceColor == BLACK_PIECE)
            movesPossible = gameState.blackMovesPossible;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (!movesPossible[i][j])
                    return false;

        return true;
    }

    public void recoverState(GameState gameState) {

        amountOfBlackPieces = gameState.amountOfBlackPieces;
        amountOfWhitePieces = gameState.amountOfWhitePieces;

        isWhiteTurn = gameState.isWhiteTurn;
        isBlackTurn = gameState.isBlackTurn;

        gameEnd = gameState.gameEnd;

        previousMoveRow = gameState.previousMoveRow;
        previousMoveColumn = gameState.previousMoveColumn;

        aiPlayer = gameState.aiPlayer;
        humanPlayer = gameState.humanPlayer;
        aiLevel = gameState.aiLevel;

        gameIndex = gameState.gameIndex;

        previousBlackTurn = gameState.previousBlackTurn;
        previousWhiteTurn = gameState.previousWhiteTurn;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                blackMovesPossible[i][j] = gameState.blackMovesPossible[i][j];
                whiteMovesPossible[i][j] = gameState.whiteMovesPossible[i][j];
                gameBoard[i][j] = gameState.gameBoard[i][j];

                try {
                    blackPossibleDirections[i][j].clear();
                } catch (NullPointerException e) {

                }

                try {
                    whitePossibleDirections[i][j].clear();
                } catch (NullPointerException e) {

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

        gameState.amountOfBlackPieces = amountOfBlackPieces;
        gameState.amountOfWhitePieces = amountOfWhitePieces;

        gameState.isWhiteTurn = isWhiteTurn;
        gameState.isBlackTurn = isBlackTurn;

        gameState.gameEnd = gameEnd;

        gameState.previousMoveRow = previousMoveRow;
        gameState.previousMoveColumn = previousMoveColumn;

        gameState.aiPlayer = aiPlayer;
        gameState.humanPlayer = humanPlayer;
        gameState.aiLevel = aiLevel;

        gameState.gameIndex = gameIndex;

        gameState.previousWhiteTurn = previousWhiteTurn;
        gameState.previousBlackTurn = previousBlackTurn;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                gameState.blackMovesPossible[i][j] = blackMovesPossible[i][j];
                gameState.whiteMovesPossible[i][j] = whiteMovesPossible[i][j];
                gameState.gameBoard[i][j] = gameBoard[i][j];


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

        amountOfBlackPieces = 0;
        amountOfWhitePieces = 0;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (gameBoard[i][j] == WHITE_PIECE)
                    amountOfWhitePieces++;
                else if (gameBoard[i][j] == BLACK_PIECE)
                    amountOfBlackPieces++;

        blackPieceNumber.setText(Integer.toString(amountOfBlackPieces));
        whitePieceNumber.setText(Integer.toString(amountOfWhitePieces));
    }

    public void clearGrid() {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                int viewID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                ImageView button = (ImageView) findViewById(viewID);

                if (isBlackTurn && whiteMovesPossible[i][j] && gameBoard[i][j] == EMPTY_PIECE)
                    button.setImageDrawable(null);
                else if (isWhiteTurn && blackMovesPossible[i][j] && gameBoard[i][j] == EMPTY_PIECE)
                    button.setImageDrawable(null);

                whiteMovesPossible[i][j] = false;
                blackMovesPossible[i][j] = false;
            }
    }

    public void clearGridAlternative() {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                whiteMovesPossible[i][j] = false;
                blackMovesPossible[i][j] = false;
            }
    }

    public void checkStateOfBoard() {

        boolean whiteMoves = findMoves(WHITE_PIECE);
        boolean blackMoves = findMoves(BLACK_PIECE);

        if (!whiteMoves && !blackMoves) {

            isBlackTurn = false;
            isWhiteTurn = false;
            gameEnd = true;

            if (amountOfBlackPieces > amountOfWhitePieces && aiPlayer == BLACK_PIECE) {

                Toast toast = Toast.makeText(getApplicationContext(), "Соперник выиграл!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (amountOfBlackPieces < amountOfWhitePieces && aiPlayer == WHITE_PIECE) {

                Toast toast = Toast.makeText(getApplicationContext(), "Соперник выиграл!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (amountOfBlackPieces > amountOfWhitePieces && humanPlayer == BLACK_PIECE) {

                Toast toast = Toast.makeText(getApplicationContext(), "Вы выиграли!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (amountOfBlackPieces < amountOfWhitePieces && humanPlayer == WHITE_PIECE) {

                Toast toast = Toast.makeText(getApplicationContext(), "Вы выиграли!", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            else {

                Toast toast = Toast.makeText(getApplicationContext(), "Ничья", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

        } else if (!whiteMoves && blackMoves) {

            isWhiteTurn = false;
            isBlackTurn = true;

        } else if (!blackMoves && whiteMoves) {

            isBlackTurn = false;
            isWhiteTurn = true;
        }
    }

    public void paintPossibleMoves() {

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (whiteMovesPossible[i][j] && isWhiteTurn) {

                    int resID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                    ImageView button = (ImageView) findViewById(resID);
                    button.setImageDrawable(getResources().getDrawable(R.drawable.circle_white_move));

                } else if (blackMovesPossible[i][j] && isBlackTurn) {

                    int resID = getResources().getIdentifier("2" + i + "" + j, "id", getPackageName());
                    ImageView button = (ImageView) findViewById(resID);
                    button.setImageDrawable(getResources().getDrawable(R.drawable.circle_black_move));
                }
    }

    public int getOppositeColor(int color) {

        if (color == WHITE_PIECE)
            return BLACK_PIECE;
        else
            return WHITE_PIECE;
    }

    public void switchTurns() {

        boolean isWhiteTurnCopy = isWhiteTurn;

        isWhiteTurn = isBlackTurn;
        isBlackTurn = isWhiteTurnCopy;
    }

    public void setupGrid() {

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.gridContainer);

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                gameBoard[i][j] = 0;

        LinearLayout.LayoutParams buttonSize = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Display display = getWindowManager().getDefaultDisplay();
        buttonSize.width = (display.getWidth() / (BOARD_LENGTH + 1));
        buttonSize.height = buttonSize.width;

        LinearLayout.LayoutParams verticalLine = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        verticalLine.width = 1;
        verticalLine.height = buttonSize.height;

        LinearLayout.LayoutParams horizontalLine = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        horizontalLine.width = (buttonSize.width) * BOARD_LENGTH + 9;
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

                ImageView button = new ImageView(this);
                button.setLayoutParams(buttonSize);
                button.setId(Integer.parseInt("2" + i + "" + j));
                button.setBackgroundColor(Color.parseColor("#00000000"));
                button.setOnClickListener(this);

                grid.addView(button);

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

    public void setInitialPieces() {

        ImageView button;

        button = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(233), "id", getPackageName()));
        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));

        gameBoard[3][3] = WHITE_PIECE;

        button = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(234), "id", getPackageName()));
        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));
        gameBoard[3][4] = BLACK_PIECE;

        button = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(243), "id", getPackageName()));
        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));
        gameBoard[4][3] = BLACK_PIECE;

        button = (ImageView) findViewById(getResources().getIdentifier(Integer.toString(244), "id", getPackageName()));
        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));
        gameBoard[4][4] = WHITE_PIECE;
    }

    public void initialSetup() {

        setupGrid();

        blackPieceNumber = (TextView) findViewById(R.id.blackScore);
        whitePieceNumber = (TextView) findViewById(R.id.whiteScore);

        setInitialPieces();

        checkStateOfBoard();

        paintPossibleMoves();
    }

    public void PlayWithAI(View v) {

        gameIndex = -1;

        final SeekBar aiDifficulty = (SeekBar) findViewById(R.id.seekBar);

        LayerDrawable layerDrawable = (LayerDrawable) aiDifficulty.getProgressDrawable();
        Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
        progressDrawable.setColorFilter(Color.parseColor("#008000"), PorterDuff.Mode.SRC_IN);

        aiDifficulty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (i<1) {

                    aiDifficulty.setProgress(1);
                }

                aiLevel = aiDifficulty.getProgress();

                LayerDrawable layerDrawable = (LayerDrawable) aiDifficulty.getProgressDrawable();
                Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);

                if (aiLevel == 1) {

                    progressDrawable.setColorFilter(Color.parseColor("#008000"), PorterDuff.Mode.SRC_IN);

                } else if (aiLevel == 2) {

                    progressDrawable.setColorFilter(Color.parseColor("#ADFF2F"), PorterDuff.Mode.SRC_IN);

                } else if (aiLevel == 3) {

                    progressDrawable.setColorFilter(Color.parseColor("#FFF700"), PorterDuff.Mode.SRC_IN);

                } else if (aiLevel == 4) {

                    progressDrawable.setColorFilter(Color.parseColor("#FF4500"), PorterDuff.Mode.SRC_IN);

                } else if (aiLevel == 5) {

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

    public boolean findMoves(int pieceColor) {

        boolean anyMoves = false;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++)
                if (gameBoard[i][j] == pieceColor) {

                    boolean possible = getMoves(pieceColor, i, j);

                    if (!anyMoves)
                        anyMoves = possible;
                }

        return anyMoves;
    }

    public boolean getMoves(int pieceColor, int row, int column) {

        boolean anyMoves = false;

        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++)
                if (!(i == 0 && j == 0))
                    if (isLegalInDirection(pieceColor, row, column, i, j)) {

                        boolean possible = checkDirections(pieceColor, row, column, i, j);

                        if (!anyMoves)
                            anyMoves = possible;
                    }

        return anyMoves;
    }

    public boolean checkDirections(int pieceColor, int row, int column, int deltaCol, int deltaRow) {

        boolean possible = true;

        row = row + deltaRow;
        column = column + deltaCol;

        int nextPositionColour;

        try {
            nextPositionColour = gameBoard[row][column];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        if (nextPositionColour == pieceColor)
            possible = false;
        else if (nextPositionColour == getOppositeColor(pieceColor))
            possible = checkDirections(pieceColor, row, column, deltaCol, deltaRow);
        else if (nextPositionColour == EMPTY_PIECE) {

            if (pieceColor == WHITE_PIECE) {
                whiteMovesPossible[row][column] = true;
                whitePossibleDirections[row][column].add(-deltaCol + "." + -deltaRow);
            } else {
                blackMovesPossible[row][column] = true;
                blackPossibleDirections[row][column].add(-deltaCol + "." + -deltaRow);
            }
        }

        return possible;
    }

    public boolean isLegalInDirection(int pieceColor, int row, int column, int deltaColumn, int deltaRow) {

        boolean legalDirection = true;

        int newColumn = column + deltaColumn;
        int newRow = row + deltaRow;

        int nextPositionColour;

        try {
            nextPositionColour = gameBoard[newRow][newColumn];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        if (nextPositionColour == pieceColor || nextPositionColour == EMPTY_PIECE)
            legalDirection = false;

        return legalDirection;
    }

    public void makeMove(int viewID, int pieceColor, boolean userTriggered, boolean changeImage, ArrayList<String> playableDirections[][], boolean callFromMiniMax) {

        final int row = Character.getNumericValue(Integer.toString(viewID).charAt(1));
        final int column = Character.getNumericValue(Integer.toString(viewID).charAt(2));

        previousMoveRow = row;
        previousMoveColumn = column;

        for (int i = 0; i < playableDirections[row][column].size(); i++) {

            String[] delta = playableDirections[row][column].get(i).split("\\.");

            int deltaCol = Integer.parseInt(delta[0]);
            int deltaRow = Integer.parseInt(delta[1]);

            int currentRow = row;
            int currentColumn = column;

            if (i > 0) {

                currentRow = currentRow + deltaRow;
                currentColumn = currentColumn + deltaCol;
            }

            while (gameBoard[currentRow][currentColumn] != pieceColor) {

                int newId = Integer.parseInt("2" + currentRow + "" + currentColumn);
                int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());

                ImageView button = (ImageView) findViewById(resourceID);

                if (pieceColor == WHITE_PIECE)
                    gameBoard[currentRow][currentColumn] = WHITE_PIECE;
                else
                    gameBoard[currentRow][currentColumn] = BLACK_PIECE;

                if (changeImage)
                    if (pieceColor == WHITE_PIECE)
                        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_white));
                    else
                        button.setImageDrawable(getResources().getDrawable(R.drawable.circle_black));

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
            switchTurns();
            handlePostMove();
            unlockInput();
        }
    }

    public void performMove(final int viewID, final int pieceColor, final boolean userTriggered, final boolean changeImage) {

        final int row = Character.getNumericValue(Integer.toString(viewID).charAt(1));
        final int column = Character.getNumericValue(Integer.toString(viewID).charAt(2));

        cRow = previousMoveRow;
        cCol = previousMoveColumn;

        int newId = Integer.parseInt("2" + previousMoveRow + "" + previousMoveColumn);
        int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
        ImageView button = (ImageView) findViewById(resourceID);

        button.setBackgroundColor(Color.TRANSPARENT);

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
                    ImageView button = (ImageView) findViewById(resourceID);
                    button.setBackgroundColor(Color.TRANSPARENT);

                    cRow = cRow + rowDirection;

                    newId = Integer.parseInt("2" + cRow + "" + cCol);
                    resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    button = (ImageView) findViewById(resourceID);
                    button.setBackgroundColor(getResources().getColor(R.color.lightGray));

                } else if (cCol != column) {

                    int newId = Integer.parseInt("2" + cRow + "" + cCol);
                    int resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    ImageView button = (ImageView) findViewById(resourceID);
                    button.setBackgroundColor(Color.TRANSPARENT);


                    cCol = cCol + columnDirection;

                    newId = Integer.parseInt("2" + cRow + "" + cCol);
                    resourceID = getResources().getIdentifier(Integer.toString(newId), "id", getPackageName());
                    button = (ImageView) findViewById(resourceID);
                    button.setBackgroundColor(getResources().getColor(R.color.lightGray));

                } else {

                    if (pieceColor == WHITE_PIECE)
                        makeMove(viewID, pieceColor, userTriggered, changeImage, whitePossibleDirections, false);
                    else if (pieceColor == BLACK_PIECE)
                        makeMove(viewID, pieceColor, userTriggered, changeImage, blackPossibleDirections, false);

                    return;
                }

                handlerHighlight.postDelayed(this, 35);
            }
        };

        if (changeImage)
            handlerHighlight.post(animation);
        else if (pieceColor == WHITE_PIECE)
            makeMove(viewID, pieceColor, userTriggered, changeImage, whitePossibleDirections, false);
        else if (pieceColor == BLACK_PIECE)
            makeMove(viewID, pieceColor, userTriggered, changeImage, blackPossibleDirections, false);
    }

    public void lockInput() {

        proceed = false;
    }

    public void unlockInput() {

        proceed = true;
    }

    public void selectBlack(View v) {

        humanPlayer = BLACK_PIECE;
        aiPlayer = WHITE_PIECE;

        ImageButton img = (ImageButton) findViewById(R.id.blackPiece);
        img.setBackgroundResource(R.drawable.stroke);

        ImageButton imgOther = (ImageButton) findViewById(R.id.whitePiece);
        imgOther.setBackgroundResource(android.R.color.transparent);
    }

    public void selectWhite(View v) {

        humanPlayer = WHITE_PIECE;
        aiPlayer = BLACK_PIECE;

        ImageButton img = (ImageButton) findViewById(R.id.whitePiece);
        img.setBackgroundResource(R.drawable.stroke);

        ImageButton imgOther = (ImageButton) findViewById(R.id.blackPiece);
        imgOther.setBackgroundResource(android.R.color.transparent);
    }

    public void startGame(View v) {

        setContentView(activity_main);

        entered = true;

        for (int i = 0; i < BOARD_LENGTH; i++)
            for (int j = 0; j < BOARD_LENGTH; j++) {

                whitePossibleDirections[i][j] = new ArrayList<String>();
                blackPossibleDirections[i][j] = new ArrayList<String>();
            }

        initialSetup();

        saveGameState(defaultState);

        if (aiPlayer == BLACK_PIECE) {

            ((TextView) findViewById(R.id.whiteTitle)).setText("Ваш счет: ");
            ((TextView) findViewById(R.id.blackTitle)).setText("Счет соперника: ");

            if (isWhiteTurn && !gameEnd)
                paintPossibleMoves();
            else if (isBlackTurn && !gameEnd) {

                final Handler handler = new Handler();

                handler.postDelayed(new Runnable() {

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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

    public void restart(View v) {

        try {
            handlerHighlight.removeCallbacks(animation);
        } catch (NullPointerException e) {

        }

        try {
            handlerAI.removeCallbacks(aiMove);
        } catch (NullPointerException e) {

        }

        try {
            quitMsg.removeCallbacks(findMove);
        } catch (NullPointerException e) {

        }

        recoverState(defaultState);
        proceed = true;

        startGame(v);
    }
}
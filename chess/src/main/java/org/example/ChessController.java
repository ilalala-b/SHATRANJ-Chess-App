package org.example;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;

public class ChessController {

    public enum BoardTheme { NEUTRAL, BROWN, GREEN }

    @FXML private StackPane rootStack;
    @FXML private GridPane boardGrid;
    @FXML private Label statusLabel;
    @FXML private Label blackClockLabel;
    @FXML private Label whiteClockLabel;
    @FXML private Button undoButton;
    @FXML private Button premoveToggle;
    @FXML private VBox movesList;
    @FXML private Button hamburgerButton;
    @FXML private Button playAsWhiteButton;
    @FXML private Button playAsBlackButton;

    @FXML private Label whiteCapturedLabel;
    @FXML private Label blackCapturedLabel;

    @FXML private StackPane startScreenOverlay;
    @FXML private Button startButton;
    @FXML private Button continueButton;
    @FXML private Button quitButton;

    @FXML private StackPane gameOverOverlay;
    @FXML private Label popupTitleLabel;
    @FXML private Label popupMessageLabel;
    @FXML private Label popupTimeLabel;
    @FXML private Button popupNewGameButton;
    @FXML private Button popupViewBoardButton;
    @FXML private Button popupQuitButton;

    @FXML private StackPane cell_0_0, cell_0_1, cell_0_2, cell_0_3, cell_0_4, cell_0_5, cell_0_6, cell_0_7;
    @FXML private StackPane cell_1_0, cell_1_1, cell_1_2, cell_1_3, cell_1_4, cell_1_5, cell_1_6, cell_1_7;
    @FXML private StackPane cell_2_0, cell_2_1, cell_2_2, cell_2_3, cell_2_4, cell_2_5, cell_2_6, cell_2_7;
    @FXML private StackPane cell_3_0, cell_3_1, cell_3_2, cell_3_3, cell_3_4, cell_3_5, cell_3_6, cell_3_7;
    @FXML private StackPane cell_4_0, cell_4_1, cell_4_2, cell_4_3, cell_4_4, cell_4_5, cell_4_6, cell_4_7;
    @FXML private StackPane cell_5_0, cell_5_1, cell_5_2, cell_5_3, cell_5_4, cell_5_5, cell_5_6, cell_5_7;
    @FXML private StackPane cell_6_0, cell_6_1, cell_6_2, cell_6_3, cell_6_4, cell_6_5, cell_6_6, cell_6_7;
    @FXML private StackPane cell_7_0, cell_7_1, cell_7_2, cell_7_3, cell_7_4, cell_7_5, cell_7_6, cell_7_7;

    private final StackPane[][] cells = new StackPane[8][8];
    private final Text[][] pieceTexts = new Text[8][8];
    private final Rectangle[][] highlights = new Rectangle[8][8];
    private final Circle[][] dots = new Circle[8][8];
    private final Rectangle[][] lastMoveBorders = new Rectangle[8][8];

    private final GameSession session = new GameSession();

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean botThinking = false;
    private boolean paused = false;
    private boolean popupShown = false;
    private boolean showingStartScreen = true;

    private boolean premoveEnabled = false;
    private int premoveFromRow = -1;
    private int premoveFromCol = -1;
    private int premoveToRow = -1;
    private int premoveToCol = -1;

    private int lastMovedPieceRow = -1;
    private int lastMovedPieceCol = -1;

    private BoardTheme theme = BoardTheme.NEUTRAL;

    private Timeline clock;
    private StackPane drawerOverlay;
    private boolean boardFlipped = false;

    private boolean modeExpanded = false;
    private boolean gameExpanded = false;
    private boolean difficultyExpanded = false;
    private boolean themeExpanded = false;
    private boolean timeExpanded = false;

    private String lightColor() {
        switch (theme) {
            case BROWN: return "#F0D9B5";
            case GREEN: return "#EEEED2";
            default:    return "#EBECF0";
        }
    }

    private String darkColor() {
        switch (theme) {
            case BROWN: return "#B58863";
            case GREEN: return "#769656";
            default:    return "#A8ACB5";
        }
    }

    private static final String SELECTED_COLOR = "#F7D774";
    private static final String PIECE_FONT = "Segoe UI Symbol";

    @FXML
    public void initialize() {
        mapCells();
        setupVisualsInCells();

        undoButton.setOnAction(e -> undoLastTwoMoves());
        hamburgerButton.setOnAction(e -> openDrawer());
        playAsWhiteButton.setOnAction(e -> startNewGame(Piece.Color.WHITE));
        playAsBlackButton.setOnAction(e -> startNewGame(Piece.Color.BLACK));

        premoveToggle.setOnAction(e -> {
            if (!showingStartScreen && !session.getMoveNotation().isEmpty()) return;
            premoveEnabled = !premoveEnabled;
            updatePremoveToggleStyle();
        });

        startButton.setOnAction(e -> {
            startScreenOverlay.setVisible(false);
            showingStartScreen = false;
            paused = false;
            updatePremoveToggleStyle();
            startNewGame(session.getHumanColor());
        });

        continueButton.setOnAction(e -> {
            if (continueButton.isDisabled()) return;
            startScreenOverlay.setVisible(false);
            showingStartScreen = false;
            paused = false;
            redrawBoard(); updateClocks(); updateSideButtons(); updatePremoveToggleStyle(); updateCapturedPieces();
        });

        quitButton.setOnAction(e -> Platform.exit());

        popupNewGameButton.setOnAction(e -> {
            gameOverOverlay.setVisible(false);
            resetGame();
        });

        popupViewBoardButton.setOnAction(e -> gameOverOverlay.setVisible(false));

        popupQuitButton.setOnAction(e -> {
            gameOverOverlay.setVisible(false);
            paused = true;
            showStartScreen();
        });

        updatePremoveToggleStyle();
        startClock();
        showStartScreen();
    }

    private void mapCells() {
        cells[0][0] = cell_0_0; cells[0][1] = cell_0_1; cells[0][2] = cell_0_2; cells[0][3] = cell_0_3;
        cells[0][4] = cell_0_4; cells[0][5] = cell_0_5; cells[0][6] = cell_0_6; cells[0][7] = cell_0_7;
        cells[1][0] = cell_1_0; cells[1][1] = cell_1_1; cells[1][2] = cell_1_2; cells[1][3] = cell_1_3;
        cells[1][4] = cell_1_4; cells[1][5] = cell_1_5; cells[1][6] = cell_1_6; cells[1][7] = cell_1_7;
        cells[2][0] = cell_2_0; cells[2][1] = cell_2_1; cells[2][2] = cell_2_2; cells[2][3] = cell_2_3;
        cells[2][4] = cell_2_4; cells[2][5] = cell_2_5; cells[2][6] = cell_2_6; cells[2][7] = cell_2_7;
        cells[3][0] = cell_3_0; cells[3][1] = cell_3_1; cells[3][2] = cell_3_2; cells[3][3] = cell_3_3;
        cells[3][4] = cell_3_4; cells[3][5] = cell_3_5; cells[3][6] = cell_3_6; cells[3][7] = cell_3_7;
        cells[4][0] = cell_4_0; cells[4][1] = cell_4_1; cells[4][2] = cell_4_2; cells[4][3] = cell_4_3;
        cells[4][4] = cell_4_4; cells[4][5] = cell_4_5; cells[4][6] = cell_4_6; cells[4][7] = cell_4_7;
        cells[5][0] = cell_5_0; cells[5][1] = cell_5_1; cells[5][2] = cell_5_2; cells[5][3] = cell_5_3;
        cells[5][4] = cell_5_4; cells[5][5] = cell_5_5; cells[5][6] = cell_5_6; cells[5][7] = cell_5_7;
        cells[6][0] = cell_6_0; cells[6][1] = cell_6_1; cells[6][2] = cell_6_2; cells[6][3] = cell_6_3;
        cells[6][4] = cell_6_4; cells[6][5] = cell_6_5; cells[6][6] = cell_6_6; cells[6][7] = cell_6_7;
        cells[7][0] = cell_7_0; cells[7][1] = cell_7_1; cells[7][2] = cell_7_2; cells[7][3] = cell_7_3;
        cells[7][4] = cell_7_4; cells[7][5] = cell_7_5; cells[7][6] = cell_7_6; cells[7][7] = cell_7_7;
    }

    private void setupVisualsInCells() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                StackPane cell = cells[r][c];
                Rectangle highlight = new Rectangle(80.0, 80.0);
                highlights[r][c] = highlight;
                Circle dot = new Circle(8);
                dot.setVisible(false);
                dot.setMouseTransparent(true);
                dots[r][c] = dot;
                Rectangle lastBorder = new Rectangle(76.0, 76.0);
                lastBorder.setFill(Color.TRANSPARENT);
                lastBorder.setStroke(Color.web("#888888"));
                lastBorder.setStrokeWidth(3);
                lastBorder.getStrokeDashArray().addAll(8.0, 6.0);
                lastBorder.setVisible(false);
                lastMoveBorders[r][c] = lastBorder;
                Text pieceText = new Text("");
                pieceText.setFont(Font.font(PIECE_FONT, 56));
                pieceTexts[r][c] = pieceText;
                cell.getChildren().addAll(highlight, dot, lastBorder, pieceText);
                final int row = r;
                final int col = c;
                cell.setOnMouseClicked(event -> onCellClicked(row, col));
            }
        }
    }

    private void startClock() {
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (paused || showingStartScreen) return;

            session.tickClock();
            updateClocks();

            if (session.isGameOver() && !popupShown) {
                popupShown = true;
                showGameOverCard();
            }
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updatePremoveToggleStyle() {
        if (premoveToggle == null) return;
        boolean locked = !showingStartScreen && !session.getMoveNotation().isEmpty();
        premoveToggle.setDisable(locked);

        premoveToggle.getStyleClass().removeAll("btn-base", "btn-active");

        if (locked) {
            premoveToggle.setText("Premove: " + (premoveEnabled ? "ON" : "OFF"));
            premoveToggle.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 8;");
        } else {
            premoveToggle.setStyle("");
            if (premoveEnabled) {
                premoveToggle.setText("Premove: ON");
                premoveToggle.getStyleClass().add("btn-active");
            } else {
                premoveToggle.setText("Premove: OFF");
                premoveToggle.getStyleClass().add("btn-base");
            }
        }
    }

    private void clearPremove() {
        premoveFromRow = -1; premoveFromCol = -1; premoveToRow = -1; premoveToCol = -1;
    }

    private void showStartScreen() {
        if (drawerOverlay != null) return;
        showingStartScreen = true;
        paused = true;
        popupShown = false;
        updatePremoveToggleStyle();

        startScreenOverlay.setVisible(true);
        continueButton.setDisable(!session.isGameOver() && session.getMoveNotation().isEmpty());
    }

    private void startNewGame(Piece.Color color) {
        gameOverOverlay.setVisible(false);
        session.startNewGame(session.getMode(), session.getDifficulty(), color, 180);
        boardFlipped = (session.getMode() == GameSession.Mode.VS_BOT) && (color == Piece.Color.BLACK);
        selectedRow = -1; selectedCol = -1;
        clearLastMovedPiece(); clearPremove();
        botThinking = false; paused = false;
        popupShown = false;
        redrawBoard(); updateStatus(); updateClocks(); updateUndoButton(); updateMovesList(); updateCapturedPieces(); updateSideButtons(); updatePremoveToggleStyle();

        if (session.getMode() == GameSession.Mode.VS_BOT && session.getBoard().getSideToMove() != color) {
            startBotTurn();
        }
    }

    private void resetGame() {
        startNewGame(session.getHumanColor());
    }

    private void undoLastTwoMoves() {
        if (botThinking || session.isGameOver() || paused) return;
        session.undoLastTwoMoves();
        selectedRow = -1; selectedCol = -1;
        clearLastMovedPiece(); clearPremove();
        redrawBoard(); updateStatus(); updateClocks(); updateUndoButton(); updateMovesList(); updateCapturedPieces();
    }

    private void clearLastMovedPiece() {
        lastMovedPieceRow = -1; lastMovedPieceCol = -1;
    }

    private void updateClocks() {
        if (whiteClockLabel != null) {
            whiteClockLabel.setText(formatTime(session.getWhiteTime()));
            whiteClockLabel.setStyle("-fx-text-fill: " + (session.getBoard().getSideToMove() == Piece.Color.WHITE && !session.isGameOver() ? "#81b64c" : "#ffffff") + "; -fx-font-size: 34px; -fx-font-weight: bold;");
        }
        if (blackClockLabel != null) {
            blackClockLabel.setText(formatTime(session.getBlackTime()));
            blackClockLabel.setStyle("-fx-text-fill: " + (session.getBoard().getSideToMove() == Piece.Color.BLACK && !session.isGameOver() ? "#81b64c" : "#ffffff") + "; -fx-font-size: 34px; -fx-font-weight: bold;");
        }
    }

    private void updateCapturedPieces() {
        if (whiteCapturedLabel == null || blackCapturedLabel == null) return;
        String whiteCaptured = session.getCapturedPieces(Piece.Color.WHITE);
        String blackCaptured = session.getCapturedPieces(Piece.Color.BLACK);
        whiteCapturedLabel.setText(whiteCaptured.isEmpty() ? "None" : whiteCaptured);
        blackCapturedLabel.setText(blackCaptured.isEmpty() ? "None" : blackCaptured);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void updateUndoButton() {
        if (undoButton == null) return;
        boolean enabled = (!botThinking && !session.isGameOver() && !paused && !showingStartScreen && session.getMoveNotation().size() >= 2);
        undoButton.setDisable(!enabled);

        undoButton.getStyleClass().removeAll("btn-base", "btn-warn");
        if (enabled) {
            undoButton.getStyleClass().add("btn-warn");
        } else {
            undoButton.getStyleClass().add("btn-base");
        }
    }

    private void updateMovesList() {
        if (movesList == null) return;
        movesList.getChildren().clear();
        List<String> notation = session.getMoveNotation();
        for (int i = 0; i < notation.size(); i += 2) {
            String line = (i / 2 + 1) + ". " + notation.get(i);
            if (i + 1 < notation.size()) line += "   " + notation.get(i + 1);
            Label l = new Label(line);
            l.getStyleClass().add("panel-label");
            movesList.getChildren().add(l);
        }
    }

    private void updateSideButtons() {
        if (playAsWhiteButton == null || playAsBlackButton == null) return;
        boolean pvp = (session.getMode() == GameSession.Mode.VS_PLAYER);
        playAsWhiteButton.setDisable(pvp); playAsBlackButton.setDisable(pvp);
    }

    private int displayRow(int boardRow) { return boardFlipped ? 7 - boardRow : boardRow; }
    private int displayCol(int boardCol) { return boardFlipped ? 7 - boardCol : boardCol; }

    private void onCellClicked(int displayRow, int displayCol) {
        if (showingStartScreen || session.isGameOver() || paused) return;
        int row = boardFlipped ? 7 - displayRow : displayRow;
        int col = boardFlipped ? 7 - displayCol : displayCol;

        boolean playerTurn = (session.getMode() == GameSession.Mode.VS_PLAYER) || (session.getBoard().getSideToMove() == session.getHumanColor());

        if (!playerTurn && !premoveEnabled) return;
        if (!playerTurn && premoveEnabled) { handlePremoveClick(row, col); return; }
        if (botThinking) return;

        Piece clicked = session.getBoard().getPiece(row, col);
        Piece.Color turn = session.getBoard().getSideToMove();

        if (selectedRow == -1) {
            if (clicked != null && clicked.getColor() == turn) {
                selectedRow = row; selectedCol = col; redrawBoard();
            }
            return;
        }

        Move chosen = findMove(selectedRow, selectedCol, row, col);
        if (chosen != null) {
            session.makeMove(chosen);
            lastMovedPieceRow = chosen.getToRow();
            lastMovedPieceCol = chosen.getToCol();
            selectedRow = -1; selectedCol = -1;
            redrawBoard(); updateStatus(); updateClocks(); updateUndoButton(); updateMovesList(); updateCapturedPieces(); updatePremoveToggleStyle();
            if (session.isGameOver()) { showGameOverCard(); return; }
            if (session.getMode() == GameSession.Mode.VS_BOT) startBotTurn();
        } else {
            if (clicked != null && clicked.getColor() == turn) {
                selectedRow = row; selectedCol = col;
            } else {
                selectedRow = -1; selectedCol = -1;
            }
            redrawBoard();
        }
    }

    private void handlePremoveClick(int row, int col) {
        Piece clicked = session.getBoard().getPiece(row, col);
        if (premoveFromRow == -1) {
            if (clicked != null && clicked.getColor() == session.getHumanColor()) {
                premoveFromRow = row; premoveFromCol = col; redrawBoard();
            }
            return;
        }
        if (clicked != null && clicked.getColor() == session.getHumanColor() && !(row == premoveFromRow && col == premoveFromCol)) {
            premoveFromRow = row; premoveFromCol = col; premoveToRow = -1; premoveToCol = -1; redrawBoard();
            return;
        }
        premoveToRow = row; premoveToCol = col; redrawBoard();
    }

    private void startBotTurn() {
        botThinking = true;
        updateStatus();
        updateUndoButton();

        Task<Move> botTask = new Task<>() {
            @Override
            protected Move call() throws Exception {
                Thread.sleep(1200);
                return session.getBotMove();
            }
        };

        botTask.setOnSucceeded(event -> {
            Move botMove = botTask.getValue();
            if (botMove != null) {
                session.makeMove(botMove);
                lastMovedPieceRow = botMove.getToRow();
                lastMovedPieceCol = botMove.getToCol();
            }
            botThinking = false;
            redrawBoard(); updateStatus(); updateClocks(); updateUndoButton(); updatePremoveToggleStyle(); updateMovesList(); updateCapturedPieces();
            if (session.isGameOver()) { showGameOverCard(); return; }

            if (session.getMode() == GameSession.Mode.VS_BOT && session.getBoard().getSideToMove() == session.getHumanColor() && premoveFromRow != -1) {
                executePremove();
            }
        });

        botTask.setOnFailed(event -> {
            botThinking = false;
            redrawBoard(); updateStatus(); updateUndoButton();
        });

        Thread thread = new Thread(botTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void executePremove() {
        if (premoveFromRow == -1 || premoveToRow == -1) return;
        Move chosen = findMove(premoveFromRow, premoveFromCol, premoveToRow, premoveToCol);
        clearPremove();
        if (chosen != null) {
            session.makeMove(chosen);
            lastMovedPieceRow = chosen.getToRow();
            lastMovedPieceCol = chosen.getToCol();
            redrawBoard(); updateStatus(); updateClocks(); updateUndoButton(); updateMovesList(); updateCapturedPieces(); updatePremoveToggleStyle();
            if (session.isGameOver()) { showGameOverCard(); return; }
            if (session.getMode() == GameSession.Mode.VS_BOT) startBotTurn();
        } else {
            redrawBoard();
        }
    }

    private Move findMove(int fromRow, int fromCol, int toRow, int toCol) {
        List<Move> current = MoveGenerator.generateLegalMoves(session.getBoard());
        Move fallback = null;
        for (Move m : current) {
            if (m.getFromRow() == fromRow && m.getFromCol() == fromCol && m.getToRow() == toRow && m.getToCol() == toCol) {
                if (m.getFlag() == Move.Flag.PROMOTION_QUEEN) return m;
                if (fallback == null) fallback = m;
            }
        }
        return fallback;
    }

    private void showGameOverCard() {
        if (session.getMoveNotation().isEmpty()) return;

        String title, message;
        if (session.getWhiteTime() == 0) { title = "TIME OUT"; message = "Black wins on time!"; }
        else if (session.getBlackTime() == 0) { title = "TIME OUT"; message = "White wins on time!"; }
        else if (session.getBoard().isInCheck(session.getBoard().getSideToMove())) {
            Piece.Color winner = (session.getBoard().getSideToMove() == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;
            title = "CHECKMATE"; message = (winner == Piece.Color.WHITE ? "White" : "Black") + " wins!";
        } else {
            title = "STALEMATE"; message = "It's a draw.";
        }

        popupTitleLabel.setText(title);
        popupMessageLabel.setText(message);
        popupTimeLabel.setText("Time used — White: " + formatTime(session.getStartingTime() - session.getWhiteTime()) + "   Black: " + formatTime(session.getStartingTime() - session.getBlackTime()));

        gameOverOverlay.setVisible(true);
    }

    private void openDrawer() {
        if (drawerOverlay != null || showingStartScreen) return;
        drawerOverlay = new StackPane();
        drawerOverlay.getStyleClass().add("drawer-overlay");
        drawerOverlay.setOnMouseClicked(e -> { if (e.getTarget() == drawerOverlay) closeDrawer(); });

        VBox drawer = new VBox(10);
        drawer.setAlignment(Pos.TOP_LEFT);
        drawer.setPrefWidth(320); drawer.setMaxWidth(320);
        drawer.getStyleClass().add("drawer");
        StackPane.setAlignment(drawer, Pos.CENTER_LEFT);

        VBox modeSection = collapsibleSection("GAME MODE", modeExpanded, () -> modeExpanded = !modeExpanded);
        if (modeExpanded) {
            Button botBtn = drawerButton("Player vs Bot", session.getMode() == GameSession.Mode.VS_BOT);
            botBtn.setOnAction(e -> { session.startNewGame(GameSession.Mode.VS_BOT, session.getDifficulty(), session.getHumanColor(), 180); difficultyExpanded = true; closeDrawer(); openDrawer(); });
            modeSection.getChildren().add(botBtn);

            if (session.getMode() == GameSession.Mode.VS_BOT && difficultyExpanded) {
                VBox diffBlock = new VBox(6);
                diffBlock.setPadding(new Insets(0, 0, 0, 12));
                HBox diffRow = new HBox(6);
                Button easyBtn = difficultyButton("Easy", GameSession.Difficulty.EASY);
                Button mediumBtn = difficultyButton("Medium", GameSession.Difficulty.MEDIUM);
                Button hardBtn = difficultyButton("Hard", GameSession.Difficulty.HARD);
                HBox.setHgrow(easyBtn, Priority.ALWAYS); HBox.setHgrow(mediumBtn, Priority.ALWAYS); HBox.setHgrow(hardBtn, Priority.ALWAYS);
                diffRow.getChildren().addAll(easyBtn, mediumBtn, hardBtn);
                diffBlock.getChildren().add(diffRow);
                modeSection.getChildren().add(diffBlock);
            }

            Button pvpBtn = drawerButton("Player vs Player", session.getMode() == GameSession.Mode.VS_PLAYER);
            pvpBtn.setOnAction(e -> { session.startNewGame(GameSession.Mode.VS_PLAYER, session.getDifficulty(), session.getHumanColor(), 180); difficultyExpanded = false; closeDrawer(); resetGame(); });
            modeSection.getChildren().add(pvpBtn);
        }

        VBox gameSection = collapsibleSection("GAME", gameExpanded, () -> gameExpanded = !gameExpanded);
        if (gameExpanded) {
            Button newGameBtn = drawerButton("New Game", false);
            newGameBtn.setOnAction(e -> { closeDrawer(); resetGame(); });
            Button pauseBtn = drawerButton(paused ? "Resume" : "Pause", false);
            pauseBtn.setOnAction(e -> { paused = !paused; closeDrawer(); updateStatus(); updateUndoButton(); });
            Button quitBtn = new Button("Quit to Menu");
            quitBtn.setMaxWidth(Double.MAX_VALUE);
            quitBtn.getStyleClass().add("btn-danger");
            quitBtn.setOnAction(e -> { closeDrawer(); paused = true; showStartScreen(); });
            gameSection.getChildren().addAll(newGameBtn, pauseBtn, quitBtn);
        }

        VBox timeSection = collapsibleSection("TIME CONTROL", timeExpanded, () -> timeExpanded = !timeExpanded);
        if (timeExpanded) {
            HBox row1 = new HBox(6);
            Button bulletBtn = timeButton("Bullet", 60); Button blitzBtn = timeButton("Blitz", 180);
            HBox.setHgrow(bulletBtn, Priority.ALWAYS); HBox.setHgrow(blitzBtn, Priority.ALWAYS);
            row1.getChildren().addAll(bulletBtn, blitzBtn);
            HBox row2 = new HBox(6);
            Button rapidBtn = timeButton("Rapid", 600); Button classicBtn = timeButton("Classic", 1800);
            HBox.setHgrow(rapidBtn, Priority.ALWAYS); HBox.setHgrow(classicBtn, Priority.ALWAYS);
            row2.getChildren().addAll(rapidBtn, classicBtn);
            timeSection.getChildren().addAll(row1, row2);
        }

        VBox themeSection = collapsibleSection("THEME", themeExpanded, () -> themeExpanded = !themeExpanded);
        if (themeExpanded) {
            HBox themeRow = new HBox(6);
            Button neutralBtn = themeButton("Neutral", BoardTheme.NEUTRAL);
            Button brownBtn = themeButton("Brown", BoardTheme.BROWN);
            Button greenBtn = themeButton("Green", BoardTheme.GREEN);
            HBox.setHgrow(neutralBtn, Priority.ALWAYS); HBox.setHgrow(brownBtn, Priority.ALWAYS); HBox.setHgrow(greenBtn, Priority.ALWAYS);
            themeRow.getChildren().addAll(neutralBtn, brownBtn, greenBtn);
            themeSection.getChildren().add(themeRow);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.getStyleClass().add("btn-base");
        closeBtn.setOnAction(e -> closeDrawer());

        drawer.getChildren().addAll(modeSection, gameSection, timeSection, themeSection, closeBtn);
        drawerOverlay.getChildren().add(drawer);
        rootStack.getChildren().add(drawerOverlay);
        drawerOverlay.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(200), drawerOverlay);
        fade.setFromValue(0); fade.setToValue(1); fade.play();
    }

    private VBox collapsibleSection(String title, boolean expanded, Runnable onToggle) {
        VBox section = new VBox(6);
        Button header = new Button((expanded ? "▼  " : "▶  ") + title);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("section-header");
        header.setOnAction(e -> { onToggle.run(); closeDrawer(); openDrawer(); });
        section.getChildren().add(header);
        return section;
    }

    private Button drawerButton(String text, boolean active) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add(active ? "btn-active" : "btn-base");
        return b;
    }

    private Button difficultyButton(String text, GameSession.Difficulty d) {
        Button b = new Button(text);
        boolean active = (session.getDifficulty() == d);
        b.getStyleClass().add(active ? "btn-small-active" : "btn-small");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> { session.startNewGame(session.getMode(), d, session.getHumanColor(), 180); closeDrawer(); resetGame(); });
        return b;
    }

    private Button themeButton(String text, BoardTheme t) {
        Button b = new Button(text);
        boolean active = (theme == t);
        b.getStyleClass().add(active ? "btn-small-active" : "btn-small");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> { theme = t; closeDrawer(); redrawBoard(); });
        return b;
    }

    private Button timeButton(String text, int seconds) {
        Button b = new Button(text);
        boolean active = (session.getStartingTime() == seconds);
        b.getStyleClass().add(active ? "btn-small-active" : "btn-small");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> { session.startNewGame(session.getMode(), session.getDifficulty(), session.getHumanColor(), seconds); closeDrawer(); resetGame(); });
        return b;
    }

    private void closeDrawer() {
        if (drawerOverlay == null) return;
        rootStack.getChildren().remove(drawerOverlay);
        drawerOverlay = null;
    }

    private void redrawBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int dr = displayRow(r); int dc = displayCol(c);
                Color bg = Color.web(((r + c) % 2 == 0) ? lightColor() : darkColor());

                if (r == selectedRow && c == selectedCol) bg = Color.web(SELECTED_COLOR);
                else if (r == premoveFromRow && c == premoveFromCol) bg = Color.web("#C4B5E0");
                else if (r == premoveToRow && c == premoveToCol) bg = Color.web("#C4B5E0");

                highlights[dr][dc].setFill(bg);

                boolean isLegalDest = (selectedRow != -1) && isLegalDestination(r, c);
                Piece target = session.getBoard().getPiece(r, c);
                Circle dot = dots[dr][dc];

                if (isLegalDest && target == null) {
                    dot.setRadius(8); dot.setFill(Color.web("#3a3a3a", 0.55)); dot.setStroke(Color.TRANSPARENT); dot.setVisible(true);
                } else if (isLegalDest && target != null) {
                    dot.setRadius(30); dot.setFill(Color.TRANSPARENT); dot.setStroke(Color.web("#3a3a3a", 0.6)); dot.setStrokeWidth(4); dot.setVisible(true);
                } else { dot.setVisible(false); }

                lastMoveBorders[dr][dc].setVisible(r == lastMovedPieceRow && c == lastMovedPieceCol);

                Piece piece = session.getBoard().getPiece(r, c);
                pieceTexts[dr][dc].setText(piece == null ? "" : pieceSymbol(piece));
            }
        }
    }

    private boolean isLegalDestination(int row, int col) {
        List<Move> legal = MoveGenerator.generateLegalMoves(session.getBoard());
        for (Move m : legal) {
            if (m.getFromRow() == selectedRow && m.getFromCol() == selectedCol && m.getToRow() == row && m.getToCol() == col) return true;
        }
        return false;
    }

    private String pieceSymbol(Piece p) {
        boolean white = p.getColor() == Piece.Color.WHITE;
        switch (p.getType()) {
            case KING:   return white ? "\u2654" : "\u265A";
            case QUEEN:  return white ? "\u2655" : "\u265B";
            case ROOK:   return white ? "\u2656" : "\u265C";
            case BISHOP: return white ? "\u2657" : "\u265D";
            case KNIGHT: return white ? "\u2658" : "\u265E";
            case PAWN:   return white ? "\u2659" : "\u265F";
            default:     return "";
        }
    }

    private void updateStatus() {
        if (session.isGameOver()) return;
        if (paused) { statusLabel.setText("Paused"); return; }
        if (botThinking) {
            statusLabel.setText(premoveFromRow != -1 && premoveToRow != -1 ? "Bot is thinking... (Premove queued)" : "Bot is thinking...");
        } else {
            Piece.Color turn = session.getBoard().getSideToMove();
            String check = session.getBoard().isInCheck(turn) ? " (CHECK!)" : "";
            statusLabel.setText((turn == Piece.Color.WHITE ? "White" : "Black") + " to move" + check);
        }
    }
}
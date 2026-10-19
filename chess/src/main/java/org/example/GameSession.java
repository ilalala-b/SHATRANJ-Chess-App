package org.example;

import java.util.ArrayList;
import java.util.List;

public class GameSession {

    public enum Mode { VS_BOT, VS_PLAYER }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private Board board;
    private List<Board.MoveInfo> history;
    private List<String> moveNotation;
    private final Engine engine = new Engine();

    private Mode mode;
    private Difficulty difficulty;
    private Piece.Color humanColor;

    private int whiteTime;
    private int blackTime;
    private int startingTime;
    private boolean clockRunning;
    private boolean gameOver;

    public GameSession() {
        this.board = new Board();
        this.history = new ArrayList<>();
        this.moveNotation = new ArrayList<>();
        this.mode = Mode.VS_BOT;
        this.difficulty = Difficulty.MEDIUM;
        this.humanColor = Piece.Color.WHITE;
    }

    public void startNewGame(Mode newMode, Difficulty newDifficulty, Piece.Color color, int timeInSeconds) {
        this.mode = newMode;
        this.difficulty = newDifficulty;
        this.humanColor = color;
        this.startingTime = timeInSeconds;

        this.board = new Board();
        this.history.clear();
        this.moveNotation.clear();
        this.whiteTime = timeInSeconds;
        this.blackTime = timeInSeconds;
        this.clockRunning = false;
        this.gameOver = false;
    }

    public boolean makeMove(Move move) {
        if (gameOver) return false;

        Piece movingPiece = board.getPiece(move.getFromRow(), move.getFromCol());
        Piece capturedPiece = board.getPiece(move.getToRow(), move.getToCol());
        if (move.getFlag() == Move.Flag.EN_PASSANT) {
            capturedPiece = board.getPiece(move.getFromRow(), move.getToCol());
        }

        String notation = moveToNotation(move, movingPiece, capturedPiece);
        Board.MoveInfo info = board.makeMove(move);
        history.add(info);
        moveNotation.add(notation);
        clockRunning = true;

        return checkGameOver();
    }

    public Move getBotMove() {
        int depth = getSearchDepth();
        return engine.findBestMove(board, depth);
    }

    public void undoLastTwoMoves() {
        if (history.size() < 2) return;

        int count = 0;
        while (count < 2 && !history.isEmpty()) {
            Board.MoveInfo info = history.remove(history.size() - 1);
            board.unmakeMove(info);
            if (!moveNotation.isEmpty()) {
                moveNotation.remove(moveNotation.size() - 1);
            }
            count++;
        }
        gameOver = false;
    }

    public void tickClock() {
        if (!clockRunning || gameOver) return;

        if (board.getSideToMove() == Piece.Color.WHITE) {
            whiteTime--;
            if (whiteTime <= 0) {
                whiteTime = 0;
                gameOver = true;
                clockRunning = false;
            }
        } else {
            blackTime--;
            if (blackTime <= 0) {
                blackTime = 0;
                gameOver = true;
                clockRunning = false;
            }
        }
    }

    private boolean checkGameOver() {
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        if (moves.isEmpty()) {
            gameOver = true;
            clockRunning = false;

            if (board.isInCheck(board.getSideToMove())) {
                moveNotation.set(moveNotation.size() - 1, moveNotation.get(moveNotation.size() - 1) + "#");
            }
            return true;
        }

        if (board.isInCheck(board.getSideToMove())) {
            moveNotation.set(moveNotation.size() - 1, moveNotation.get(moveNotation.size() - 1) + "+");
        }
        return false;
    }

    private int getSearchDepth() {
        switch (difficulty) {
            case EASY:   return 1;
            case HARD:   return 3;
            default:     return 2;
        }
    }

    private String moveToNotation(Move move, Piece movingPiece, Piece capturedPiece) {
        StringBuilder sb = new StringBuilder();
        if (move.isCastle()) {
            sb.append(move.getFlag() == Move.Flag.CASTLE_KINGSIDE ? "O-O" : "O-O-O");
        } else {
            char file = (char)('a' + move.getToCol());
            char rank = (char)('1' + (7 - move.getToRow()));
            String dest = "" + file + rank;
            switch (movingPiece.getType()) {
                case KING:   sb.append("K"); break;
                case QUEEN:  sb.append("Q"); break;
                case ROOK:   sb.append("R"); break;
                case BISHOP: sb.append("B"); break;
                case KNIGHT: sb.append("N"); break;
                case PAWN:   break;
            }
            if (movingPiece.getType() == Piece.Type.PAWN && capturedPiece != null) {
                char fromFile = (char)('a' + move.getFromCol());
                sb.append(fromFile);
            }
            if (capturedPiece != null) sb.append("x");
            sb.append(dest);
            if (move.isPromotion()) {
                switch (move.getFlag()) {
                    case PROMOTION_QUEEN:  sb.append("=Q"); break;
                    case PROMOTION_ROOK:   sb.append("=R"); break;
                    case PROMOTION_BISHOP: sb.append("=B"); break;
                    case PROMOTION_KNIGHT: sb.append("=N"); break;
                    default: break;
                }
            }
        }
        return sb.toString();
    }

    public String getCapturedPieces(Piece.Color color) {
        int pawns = 0, knights = 0, bishops = 0, rooks = 0, queens = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getColor() == color) {
                    switch (p.getType()) {
                        case PAWN:   pawns++; break;
                        case KNIGHT: knights++; break;
                        case BISHOP: bishops++; break;
                        case ROOK:   rooks++; break;
                        case QUEEN:  queens++; break;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        appendMissing(sb, pawns, 8, "\u265F");
        appendMissing(sb, knights, 2, "\u265E");
        appendMissing(sb, bishops, 2, "\u265D");
        appendMissing(sb, rooks, 2, "\u265C");
        appendMissing(sb, queens, 1, "\u265B");
        return sb.toString().trim();
    }

    private void appendMissing(StringBuilder sb, int current, int max, String symbol) {
        for (int i = current; i < max; i++) {
            sb.append(symbol).append(" ");
        }
    }

    // --- Getters ---
    public Board getBoard() { return board; }
    public Mode getMode() { return mode; }
    public Difficulty getDifficulty() { return difficulty; }
    public Piece.Color getHumanColor() { return humanColor; }
    public int getWhiteTime() { return whiteTime; }
    public int getBlackTime() { return blackTime; }
    public int getStartingTime() { return startingTime; }
    public boolean isClockRunning() { return clockRunning; }
    public boolean isGameOver() { return gameOver; }
    public List<String> getMoveNotation() { return moveNotation; }
}
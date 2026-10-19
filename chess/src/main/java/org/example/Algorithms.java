package org.example;

import java.util.List;

public class Algorithms {

    private static final int MATE_SCORE = 100000;

    private static int nodesSearched = 0;

    public static int getNodesSearched() {
        return nodesSearched;
    }

    public static Move findBestMove(Board board, int depth) {
        nodesSearched = 0;

        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        orderMoves(board, moves);

        Move bestMove = null;
        int bestScore = -MATE_SCORE * 2;
        int alpha = -MATE_SCORE * 2;
        int beta = MATE_SCORE * 2;

        for (Move move : moves) {
            Board.MoveInfo info = board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha);
            board.unmakeMove(info);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return bestMove;
    }

    private static int negamax(Board board, int depth, int alpha, int beta) {
        nodesSearched++;

        List<Move> moves = MoveGenerator.generateLegalMoves(board);

        if (moves.isEmpty()) {
            if (board.isInCheck(board.getSideToMove())) {
                return -MATE_SCORE + (10 - depth);
            } else {
                return 0;
            }
        }

        if (depth == 0) {
            int eval = evaluate(board);
            return (board.getSideToMove() == Piece.Color.WHITE) ? eval : -eval;
        }

        orderMoves(board, moves);

        int bestScore = -MATE_SCORE * 2;
        for (Move move : moves) {
            Board.MoveInfo info = board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha);
            board.unmakeMove(info);

            if (score > bestScore) {
                bestScore = score;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                break;
            }
        }

        return bestScore;
    }

    private static void orderMoves(Board board, List<Move> moves) {
        moves.sort((a, b) -> scoreMove(board, b) - scoreMove(board, a));
    }

    private static int scoreMove(Board board, Move move) {
        int score = 0;

        Piece victim = board.getPiece(move.getToRow(), move.getToCol());
        Piece attacker = board.getPiece(move.getFromRow(), move.getFromCol());

        if (victim != null && attacker != null) {
            score += 10 * pieceValue(victim.getType()) - pieceValue(attacker.getType());
        }

        if (move.isPromotion()) {
            score += 800;
        }

        if (move.isCastle()) {
            score += 50;
        }

        return score;
    }

    public static int evaluate(Board board) {
        int whiteScore = 0;
        int blackScore = 0;
        int whiteMaterial = 0;
        int blackMaterial = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                int value = pieceValue(p.getType());
                if (p.getColor() == Piece.Color.WHITE) {
                    whiteMaterial += value;
                } else {
                    blackMaterial += value;
                }
            }
        }

        boolean endgame = (whiteMaterial + blackMaterial) < 23400;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;

                int material = pieceValue(p.getType());
                int positionBonus;

                if (p.getColor() == Piece.Color.WHITE) {
                    positionBonus = pstValue(p.getType(), r, c, true, endgame);
                    whiteScore += material + positionBonus;
                } else {
                    positionBonus = pstValue(p.getType(), r, c, false, endgame);
                    blackScore += material + positionBonus;
                }
            }
        }

        return whiteScore - blackScore;
    }

    private static int pieceValue(Piece.Type type) {
        switch (type) {
            case PAWN:   return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK:   return 500;
            case QUEEN:  return 900;
            case KING:   return 20000;
            default:     return 0;
        }
    }

    private static int pstValue(Piece.Type type, int row, int col, boolean isWhite, boolean endgame) {
        int r = isWhite ? row : (7 - row);
        int c = isWhite ? col : (7 - col);

        switch (type) {
            case PAWN:   return PAWN_PST[r][c];
            case KNIGHT: return KNIGHT_PST[r][c];
            case BISHOP: return BISHOP_PST[r][c];
            case ROOK:   return ROOK_PST[r][c];
            case QUEEN:  return QUEEN_PST[r][c];
            case KING:   return endgame ? KING_END_PST[r][c] : KING_MIDDLE_PST[r][c];
            default:     return 0;
        }
    }

    public static int perft(Board board, int depth) {
        if (depth == 0) return 1;
        List<Move> moves = MoveGenerator.generateLegalMoves(board);
        int nodes = 0;
        for (Move move : moves) {
            Board.MoveInfo info = board.makeMove(move);
            nodes += perft(board, depth - 1);
            board.unmakeMove(info);
        }
        return nodes;
    }

    private static final int[][] PAWN_PST = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            { 50, 50, 50, 50, 50, 50, 50, 50},
            { 10, 10, 20, 30, 30, 20, 10, 10},
            {  5,  5, 10, 25, 25, 10,  5,  5},
            {  0,  0,  0, 20, 20,  0,  0,  0},
            {  5, -5,-10,  0,  0,-10, -5,  5},
            {  5, 10, 10,-20,-20, 10, 10,  5},
            {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_PST = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_PST = {
            {-20,-10,-10,-10,-10,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5, 10, 10,  5,  0,-10},
            {-10,  5,  5, 10, 10,  5,  5,-10},
            {-10,  0, 10, 10, 10, 10,  0,-10},
            {-10, 10, 10, 10, 10, 10, 10,-10},
            {-10,  5,  0,  0,  0,  0,  5,-10},
            {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_PST = {
            {  0,  0,  0,  0,  0,  0,  0,  0},
            {  5, 10, 10, 10, 10, 10, 10,  5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            { -5,  0,  0,  0,  0,  0,  0, -5},
            {  0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_PST = {
            {-20,-10,-10, -5, -5,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5,  5,  5,  5,  0,-10},
            { -5,  0,  5,  5,  5,  5,  0, -5},
            {  0,  0,  5,  5,  5,  5,  0, -5},
            {-10,  5,  5,  5,  5,  5,  0,-10},
            {-10,  0,  5,  0,  0,  0,  0,-10},
            {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_MIDDLE_PST = {
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-20,-30,-30,-40,-40,-30,-30,-20},
            {-10,-20,-20,-20,-20,-20,-20,-10},
            { 20, 20,  0,  0,  0,  0, 20, 20},
            { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    private static final int[][] KING_END_PST = {
            {-50,-40,-30,-20,-20,-30,-40,-50},
            {-30,-20,-10,  0,  0,-10,-20,-30},
            {-30,-10, 20, 30, 30, 20,-10,-30},
            {-30,-10, 30, 40, 40, 30,-10,-30},
            {-30,-10, 30, 40, 40, 30,-10,-30},
            {-30,-10, 20, 30, 30, 20,-10,-30},
            {-30,-30,  0,  0,  0,  0,-30,-30},
            {-50,-30,-30,-30,-30,-30,-30,-50}
    };
}
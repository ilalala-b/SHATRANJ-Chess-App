package org.example;

public class Engine {

    private static final int DEFAULT_DEPTH = 3;

    public Move findBestMove(Board board) {
        return Algorithms.findBestMove(board, DEFAULT_DEPTH);
    }

    public Move findBestMove(Board board, int depth) {
        return Algorithms.findBestMove(board, depth);
    }

    public int getNodesSearched() {
        return Algorithms.getNodesSearched();
    }
}
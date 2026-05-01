package com.example.tictactoe.model;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private final String sessionId;
    private final char[] board;
    private final List<AgentExperience> experiences;
    private GameStatus status;
    private String message;

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
        this.board = "---------".toCharArray();
        this.experiences = new ArrayList<>();
        this.status = GameStatus.IN_PROGRESS;
        this.message = "Your turn";
    }

    public String getSessionId() {
        return sessionId;
    }

    public char[] getBoard() {
        return board;
    }

    public boolean isCellEmpty(int position) {
        return board[position] == '-';
    }

    public void placeMark(int position, char playerMark) {
        board[position] = playerMark;
    }

    public List<AgentExperience> getExperiences() {
        return experiences;
    }

    public void recordExperience(AgentExperience experience) {
        experiences.add(experience);
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFinished() {
        return status != GameStatus.IN_PROGRESS;
    }

    public String boardAsString() {
        return new String(board);
    }

    public List<String> boardAsList() {
        List<String> cells = new ArrayList<>(board.length);
        for (char cell : board) {
            cells.add(String.valueOf(cell == '-' ? ' ' : cell));
        }
        return cells;
    }
}

package com.example.tictactoe.service;

import com.example.tictactoe.model.GameSession;
import com.example.tictactoe.model.GameStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TicTacToeRules {
    public static final char HUMAN_MARK = 'X';
    public static final char AI_MARK = 'O';
    public static final char EMPTY_MARK = '-';

    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    public void validateHumanMove(GameSession session, int position) {
        if (!session.isCellEmpty(position)) {
            throw new IllegalArgumentException("That square is already taken.");
        }
    }

    public List<Integer> getAvailableMoves(char[] board) {
        List<Integer> moves = new ArrayList<>();
        for (int index = 0; index < board.length; index++) {
            if (board[index] == EMPTY_MARK) {
                moves.add(index);
            }
        }
        return moves;
    }

    public boolean hasWinner(char[] board, char playerMark) {
        for (int[] line : WINNING_LINES) {
            if (board[line[0]] == playerMark && board[line[1]] == playerMark && board[line[2]] == playerMark) {
                return true;
            }
        }
        return false;
    }

    public void updateGameStatus(GameSession session, char playerMark) {
        if (hasWinner(session.getBoard(), playerMark)) {
            if (playerMark == HUMAN_MARK) {
                session.setStatus(GameStatus.HUMAN_WON);
                session.setMessage("You won. The AI will learn from this game.");
            } else {
                session.setStatus(GameStatus.AI_WON);
                session.setMessage("AI won. It will reinforce these moves.");
            }
            return;
        }

        if (getAvailableMoves(session.getBoard()).isEmpty()) {
            session.setStatus(GameStatus.DRAW);
            session.setMessage("Draw. The AI will still learn from the sequence.");
        }
    }
}


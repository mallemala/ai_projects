package com.example.tictactoe.service;

import com.example.tictactoe.model.AgentExperience;
import com.example.tictactoe.model.GameResponse;
import com.example.tictactoe.model.GameSession;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final GameSessionStore gameSessionStore;
    private final TicTacToeRules ticTacToeRules;
    private final LearningAgent learningAgent;

    public GameService(GameSessionStore gameSessionStore, TicTacToeRules ticTacToeRules, LearningAgent learningAgent) {
        this.gameSessionStore = gameSessionStore;
        this.ticTacToeRules = ticTacToeRules;
        this.learningAgent = learningAgent;
    }

    public GameResponse startGame() {
        GameSessionStore.CreatedSession createdSession = gameSessionStore.createSession();
        GameSession session = createdSession.session();

        if (!createdSession.humanStarts()) {
            playAiOpeningTurn(session);
        }

        return toResponse(session);
    }

    public GameResponse getGame(String sessionId) {
        return toResponse(gameSessionStore.getSession(sessionId));
    }

    public GameResponse playHumanMove(String sessionId, int position) {
        GameSession session = gameSessionStore.getSession(sessionId);

        if (session.isFinished()) {
            return toResponse(session);
        }

        ticTacToeRules.validateHumanMove(session, position);
        session.placeMark(position, TicTacToeRules.HUMAN_MARK);
        ticTacToeRules.updateGameStatus(session, TicTacToeRules.HUMAN_MARK);

        if (session.isFinished()) {
            learningAgent.learnFromFinishedGame(session);
            return toResponse(session);
        }

        playAiTurn(session);
        return toResponse(session);
    }

    public com.example.tictactoe.model.AgentStats getStats() {
        return learningAgent.getStats();
    }

    private void playAiTurn(GameSession session) {
        String boardStateBeforeAiMove = session.boardAsString();
        int aiMove = learningAgent.chooseMove(session.getBoard());

        session.placeMark(aiMove, TicTacToeRules.AI_MARK);
        session.recordExperience(new AgentExperience(boardStateBeforeAiMove, aiMove));
        ticTacToeRules.updateGameStatus(session, TicTacToeRules.AI_MARK);

        if (session.isFinished()) {
            learningAgent.learnFromFinishedGame(session);
        } else {
            session.setMessage("Your turn");
        }
    }

    private void playAiOpeningTurn(GameSession session) {
        playAiTurn(session);

        if (!session.isFinished()) {
            session.setMessage("AI made the first move. Your turn.");
        }
    }

    private GameResponse toResponse(GameSession session) {
        return new GameResponse(
                session.getSessionId(),
                session.boardAsList(),
                session.getStatus(),
                session.getMessage(),
                learningAgent.getStats()
        );
    }
}

package com.example.tictactoe.service;

import com.example.tictactoe.model.GameSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionStore {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private boolean humanStartsNextGame = true;

    public synchronized CreatedSession createSession() {
        boolean humanStarts = humanStartsNextGame;
        humanStartsNextGame = !humanStartsNextGame;
        GameSession session = new GameSession(UUID.randomUUID().toString());
        sessions.put(session.getSessionId(), session);
        return new CreatedSession(session, humanStarts);
    }

    public GameSession getSession(String sessionId) {
        GameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Game session not found.");
        }
        return session;
    }

    public record CreatedSession(GameSession session, boolean humanStarts) {
    }
}

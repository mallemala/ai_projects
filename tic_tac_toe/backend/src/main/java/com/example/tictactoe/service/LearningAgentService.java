package com.example.tictactoe.service;

import com.example.tictactoe.model.AgentExperience;
import com.example.tictactoe.model.AgentStats;
import com.example.tictactoe.model.GameResponse;
import com.example.tictactoe.model.GameSession;
import com.example.tictactoe.model.GameStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LearningAgentService {
    private static final char HUMAN = 'X';
    private static final char AI = 'O';
    private static final char EMPTY = '-';
    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final ObjectMapper objectMapper;
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Double>> qValues = new ConcurrentHashMap<>();
    private final Path storePath;
    private final double epsilon;
    private final double alpha;
    private final double gamma;
    private AgentStats stats = new AgentStats(0, 0, 0, 0, 0);

    public LearningAgentService(
            ObjectMapper objectMapper,
            @Value("${learning.store.path}") String storePath,
            @Value("${learning.epsilon}") double epsilon,
            @Value("${learning.alpha}") double alpha,
            @Value("${learning.gamma}") double gamma
    ) {
        this.objectMapper = objectMapper;
        this.storePath = Path.of(storePath);
        this.epsilon = epsilon;
        this.alpha = alpha;
        this.gamma = gamma;
    }

    @PostConstruct
    public void loadKnowledge() {
        try {
            Files.createDirectories(storePath.getParent());
            if (Files.exists(storePath)) {
                KnowledgeStore store = objectMapper.readValue(storePath.toFile(), KnowledgeStore.class);
                qValues.clear();
                qValues.putAll(store.qValues());
                stats = store.stats();
            } else {
                persistKnowledge();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load learning store", exception);
        }
    }

    public GameResponse startGame() {
        GameSession session = new GameSession(UUID.randomUUID().toString());
        sessions.put(session.getSessionId(), session);
        return toResponse(session);
    }

    public GameResponse getGame(String sessionId) {
        return toResponse(getSession(sessionId));
    }

    public GameResponse playHumanMove(String sessionId, int position) {
        GameSession session = getSession(sessionId);
        if (session.isFinished()) {
            return toResponse(session);
        }

        if (session.getBoard()[position] != EMPTY) {
            throw new IllegalArgumentException("That square is already taken.");
        }

        session.getBoard()[position] = HUMAN;
        updateStatusAfterMove(session, HUMAN);
        if (session.isFinished()) {
            trainAndFinalize(session);
            return toResponse(session);
        }

        String stateBeforeAi = session.boardAsString();
        int aiMove = chooseAiMove(session.getBoard());
        session.getBoard()[aiMove] = AI;
        session.getExperiences().add(new AgentExperience(stateBeforeAi, aiMove));
        updateStatusAfterMove(session, AI);

        if (session.isFinished()) {
            trainAndFinalize(session);
        } else {
            session.setMessage("Your turn");
        }

        return toResponse(session);
    }

    public AgentStats getStats() {
        return new AgentStats(qValues.size(), stats.gamesPlayed(), stats.aiWins(), stats.humanWins(), stats.draws());
    }

    private GameSession getSession(String sessionId) {
        GameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Game session not found.");
        }
        return session;
    }

    private void updateStatusAfterMove(GameSession session, char player) {
        if (hasWon(session.getBoard(), player)) {
            if (player == HUMAN) {
                session.setStatus(GameStatus.HUMAN_WON);
                session.setMessage("You won. The AI will learn from this game.");
            } else {
                session.setStatus(GameStatus.AI_WON);
                session.setMessage("AI won. It will reinforce these moves.");
            }
            return;
        }

        if (availableMoves(session.getBoard()).isEmpty()) {
            session.setStatus(GameStatus.DRAW);
            session.setMessage("Draw. The AI will still learn from the sequence.");
        }
    }

    private int chooseAiMove(char[] board) {
        List<Integer> availableMoves = availableMoves(board);
        if (availableMoves.isEmpty()) {
            throw new IllegalStateException("No moves left for AI.");
        }

        String state = new String(board);
        Map<Integer, Double> stateValues = qValues.computeIfAbsent(state, key -> new ConcurrentHashMap<>());
        for (int move : availableMoves) {
            stateValues.putIfAbsent(move, 0.0);
        }

        if (ThreadLocalRandom.current().nextDouble() < epsilon) {
            return availableMoves.get(ThreadLocalRandom.current().nextInt(availableMoves.size()));
        }

        return availableMoves.stream()
                .max(Comparator.comparingDouble(move -> stateValues.getOrDefault(move, 0.0)))
                .orElse(availableMoves.get(0));
    }

    private void trainAndFinalize(GameSession session) {
        double finalReward = switch (session.getStatus()) {
            case AI_WON -> 1.0;
            case DRAW -> 0.35;
            case HUMAN_WON -> -1.0;
            case IN_PROGRESS -> 0.0;
        };

        List<AgentExperience> experiences = new ArrayList<>(session.getExperiences());
        double target = finalReward;
        for (int index = experiences.size() - 1; index >= 0; index--) {
            AgentExperience experience = experiences.get(index);
            Map<Integer, Double> stateValues = qValues.computeIfAbsent(experience.state(), key -> new ConcurrentHashMap<>());
            double current = stateValues.getOrDefault(experience.action(), 0.0);
            double updated = current + alpha * (target - current);
            stateValues.put(experience.action(), updated);
            target *= gamma;
        }

        stats = switch (session.getStatus()) {
            case AI_WON -> new AgentStats(qValues.size(), stats.gamesPlayed() + 1, stats.aiWins() + 1, stats.humanWins(), stats.draws());
            case HUMAN_WON -> new AgentStats(qValues.size(), stats.gamesPlayed() + 1, stats.aiWins(), stats.humanWins() + 1, stats.draws());
            case DRAW -> new AgentStats(qValues.size(), stats.gamesPlayed() + 1, stats.aiWins(), stats.humanWins(), stats.draws() + 1);
            case IN_PROGRESS -> stats;
        };

        persistKnowledge();
    }

    private void persistKnowledge() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storePath.toFile(), new KnowledgeStore(new HashMap<>(qValues), stats));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist learning store", exception);
        }
    }

    private boolean hasWon(char[] board, char player) {
        for (int[] line : WINNING_LINES) {
            if (board[line[0]] == player && board[line[1]] == player && board[line[2]] == player) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> availableMoves(char[] board) {
        List<Integer> moves = new ArrayList<>();
        for (int index = 0; index < board.length; index++) {
            if (board[index] == EMPTY) {
                moves.add(index);
            }
        }
        return moves;
    }

    private GameResponse toResponse(GameSession session) {
        return new GameResponse(
                session.getSessionId(),
                session.boardAsList(),
                session.getStatus(),
                session.getMessage(),
                getStats()
        );
    }

    private record KnowledgeStore(Map<String, Map<Integer, Double>> qValues, AgentStats stats) {
    }
}

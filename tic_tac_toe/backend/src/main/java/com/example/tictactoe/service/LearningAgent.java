package com.example.tictactoe.service;

import com.example.tictactoe.model.AgentExperience;
import com.example.tictactoe.model.AgentStats;
import com.example.tictactoe.model.GameSession;
import com.example.tictactoe.model.GameStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LearningAgent {
    private final LearningModelStore learningModelStore;
    private final TicTacToeRules ticTacToeRules;
    private final double explorationRate;
    private final double learningRate;
    private final double discountFactor;

    public LearningAgent(
            LearningModelStore learningModelStore,
            TicTacToeRules ticTacToeRules,
            @Value("${learning.epsilon}") double explorationRate,
            @Value("${learning.alpha}") double learningRate,
            @Value("${learning.gamma}") double discountFactor
    ) {
        this.learningModelStore = learningModelStore;
        this.ticTacToeRules = ticTacToeRules;
        this.explorationRate = explorationRate;
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
    }

    public int chooseMove(char[] board) {
        List<Integer> availableMoves = ticTacToeRules.getAvailableMoves(board);
        if (availableMoves.isEmpty()) {
            throw new IllegalStateException("No moves left for AI.");
        }

        String boardState = new String(board);
        Map<Integer, Double> stateValues = learningModelStore.getStateValues(boardState);
        fillMissingMoveValues(stateValues, availableMoves);

        if (shouldExplore()) {
            return pickRandomMove(availableMoves);
        }

        return pickBestMove(availableMoves, stateValues);
    }

    public void learnFromFinishedGame(GameSession session) {
        double reward = getReward(session.getStatus());
        List<AgentExperience> experiences = new ArrayList<>(session.getExperiences());
        double nextTarget = reward;

        for (int index = experiences.size() - 1; index >= 0; index--) {
            AgentExperience experience = experiences.get(index);
            Map<Integer, Double> stateValues = learningModelStore.getStateValues(experience.state());
            double currentValue = stateValues.getOrDefault(experience.action(), 0.0);
            double updatedValue = currentValue + learningRate * (nextTarget - currentValue);
            stateValues.put(experience.action(), updatedValue);
            nextTarget *= discountFactor;
        }

        AgentStats updatedStats = buildUpdatedStats(session.getStatus());
        learningModelStore.recordCompletedGame(updatedStats);
    }

    public AgentStats getStats() {
        return learningModelStore.getStats();
    }

    private void fillMissingMoveValues(Map<Integer, Double> stateValues, List<Integer> availableMoves) {
        for (int move : availableMoves) {
            stateValues.putIfAbsent(move, 0.0);
        }
    }

    private boolean shouldExplore() {
        return ThreadLocalRandom.current().nextDouble() < explorationRate;
    }

    private int pickRandomMove(List<Integer> availableMoves) {
        int randomIndex = ThreadLocalRandom.current().nextInt(availableMoves.size());
        return availableMoves.get(randomIndex);
    }

    private int pickBestMove(List<Integer> availableMoves, Map<Integer, Double> stateValues) {
        return availableMoves.stream()
                .max(Comparator.comparingDouble(move -> stateValues.getOrDefault(move, 0.0)))
                .orElse(availableMoves.get(0));
    }

    private double getReward(GameStatus status) {
        return switch (status) {
            case AI_WON -> 1.0;
            case DRAW -> 0.35;
            case HUMAN_WON -> -1.0;
            case IN_PROGRESS -> 0.0;
        };
    }

    private AgentStats buildUpdatedStats(GameStatus status) {
        AgentStats currentStats = learningModelStore.getStats();
        int learnedStates = learningModelStore.getLearnedStateCount();

        return switch (status) {
            case AI_WON -> new AgentStats(
                    learnedStates,
                    currentStats.gamesPlayed() + 1,
                    currentStats.aiWins() + 1,
                    currentStats.humanWins(),
                    currentStats.draws()
            );
            case HUMAN_WON -> new AgentStats(
                    learnedStates,
                    currentStats.gamesPlayed() + 1,
                    currentStats.aiWins(),
                    currentStats.humanWins() + 1,
                    currentStats.draws()
            );
            case DRAW -> new AgentStats(
                    learnedStates,
                    currentStats.gamesPlayed() + 1,
                    currentStats.aiWins(),
                    currentStats.humanWins(),
                    currentStats.draws() + 1
            );
            case IN_PROGRESS -> currentStats;
        };
    }
}


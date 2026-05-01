package com.example.tictactoe.service;

import com.example.tictactoe.model.AgentStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LearningModelStore {
    private final ObjectMapper objectMapper;
    private final Path storePath;

    private final Map<String, Map<Integer, Double>> qValues = new ConcurrentHashMap<>();
    private AgentStats stats = new AgentStats(0, 0, 0, 0, 0);

    public LearningModelStore(ObjectMapper objectMapper, @Value("${learning.store.path}") String storePath) {
        this.objectMapper = objectMapper;
        this.storePath = Path.of(storePath);
    }

    @PostConstruct
    public void load() {
        try {
            Files.createDirectories(storePath.getParent());

            if (Files.exists(storePath)) {
                StoredLearningData storedData = objectMapper.readValue(storePath.toFile(), StoredLearningData.class);
                qValues.clear();
                qValues.putAll(storedData.qValues());
                stats = storedData.stats();
            } else {
                save();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load learning store", exception);
        }
    }

    public Map<Integer, Double> getStateValues(String boardState) {
        return qValues.computeIfAbsent(boardState, key -> new ConcurrentHashMap<>());
    }

    public void recordCompletedGame(AgentStats updatedStats) {
        stats = updatedStats;
        save();
    }

    public AgentStats getStats() {
        return new AgentStats(qValues.size(), stats.gamesPlayed(), stats.aiWins(), stats.humanWins(), stats.draws());
    }

    public int getLearnedStateCount() {
        return qValues.size();
    }

    private void save() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storePath.toFile(), new StoredLearningData(new HashMap<>(qValues), stats));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist learning store", exception);
        }
    }

    private record StoredLearningData(Map<String, Map<Integer, Double>> qValues, AgentStats stats) {
    }
}


package com.example.tictactoe.model;

public record AgentStats(
        int learnedStates,
        int gamesPlayed,
        int aiWins,
        int humanWins,
        int draws
) {
}


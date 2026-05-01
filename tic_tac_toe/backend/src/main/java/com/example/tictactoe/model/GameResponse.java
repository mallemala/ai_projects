package com.example.tictactoe.model;

import java.util.List;

public record GameResponse(
        String sessionId,
        List<String> board,
        GameStatus status,
        String message,
        AgentStats agentStats
) {
}


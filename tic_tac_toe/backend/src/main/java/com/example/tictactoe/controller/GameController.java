package com.example.tictactoe.controller;

import com.example.tictactoe.model.AgentStats;
import com.example.tictactoe.model.GameResponse;
import com.example.tictactoe.model.MoveRequest;
import com.example.tictactoe.service.LearningAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {
    private final LearningAgentService learningAgentService;

    public GameController(LearningAgentService learningAgentService) {
        this.learningAgentService = learningAgentService;
    }

    @PostMapping("/new")
    public GameResponse startGame() {
        return learningAgentService.startGame();
    }

    @GetMapping("/{sessionId}")
    public GameResponse getGame(@PathVariable String sessionId) {
        return learningAgentService.getGame(sessionId);
    }

    @PostMapping("/{sessionId}/move")
    public GameResponse playMove(@PathVariable String sessionId, @Valid @RequestBody MoveRequest moveRequest) {
        return learningAgentService.playHumanMove(sessionId, moveRequest.position());
    }

    @GetMapping("/stats")
    public AgentStats getStats() {
        return learningAgentService.getStats();
    }
}

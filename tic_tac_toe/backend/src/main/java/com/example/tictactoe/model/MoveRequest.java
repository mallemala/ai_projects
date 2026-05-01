package com.example.tictactoe.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull
        @Min(0)
        @Max(8)
        Integer position
) {
}


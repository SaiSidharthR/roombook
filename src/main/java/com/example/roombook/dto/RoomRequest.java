package com.example.roombook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RoomRequest(
        @NotBlank String name,
        @Positive int capacity,
        boolean hasProjector,
        boolean hasWhiteboard,
        @NotBlank String location) {
}
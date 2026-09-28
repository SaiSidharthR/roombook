package com.example.roombook.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record BookingCreateRequest(
        @NotNull @Positive Long roomId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull LocalDateTime endTime) {
}
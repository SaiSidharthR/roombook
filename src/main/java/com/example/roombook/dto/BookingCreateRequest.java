package com.example.roombook.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDateTime;

public record BookingCreateRequest(
        @NotNull @Positive Long roomId,
        @NotNull @Future LocalDateTime startTime,
                @NotNull LocalDateTime endTime) {
        @AssertTrue(message = "End time must be later than start time")
        public boolean isEndTimeAfterStartTime() {
                return startTime == null || endTime == null || endTime.isAfter(startTime);
        }
}
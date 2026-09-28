package com.example.roombook.dto;

import com.example.roombook.model.Booking;
import com.example.roombook.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long roomId,
        String roomName,
        Long organizerId,
        String organizerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        boolean checkedIn,
        LocalDateTime createdAt) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getRoom().getName(),
                booking.getOrganizer().getId(), booking.getOrganizer().getName(), booking.getStartTime(),
                booking.getEndTime(), booking.getStatus(), booking.isCheckedIn(), booking.getCreatedAt());
    }
}
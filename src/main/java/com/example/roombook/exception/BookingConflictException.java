package com.example.roombook.exception;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException() {
        super("Room is already booked for the requested time range");
    }
}
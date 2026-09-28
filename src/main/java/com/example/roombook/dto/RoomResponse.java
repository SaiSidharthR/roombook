package com.example.roombook.dto;

import com.example.roombook.model.Room;

public record RoomResponse(
        Long id,
        String name,
        int capacity,
        boolean hasProjector,
        boolean hasWhiteboard,
        String location) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(),
                room.isHasProjector(), room.isHasWhiteboard(), room.getLocation());
    }
}
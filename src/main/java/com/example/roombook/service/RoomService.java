package com.example.roombook.service;

import com.example.roombook.exception.ResourceConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.model.Room;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public Page<Room> listRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    public Room createRoom(Room room) {
        room.setId(null);
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = getRoom(id);
        room.setName(updatedRoom.getName());
        room.setCapacity(updatedRoom.getCapacity());
        room.setHasProjector(updatedRoom.isHasProjector());
        room.setHasWhiteboard(updatedRoom.isHasWhiteboard());
        room.setLocation(updatedRoom.getLocation());
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = getRoom(id);
        if (bookingRepository.existsByRoom_Id(id)) {
            throw new ResourceConflictException("Room has booking history and cannot be deleted");
        }
        roomRepository.delete(room);
    }
}
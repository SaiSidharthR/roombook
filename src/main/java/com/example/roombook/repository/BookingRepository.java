package com.example.roombook.repository;

import com.example.roombook.model.Booking;
import com.example.roombook.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
            select b from Booking b
            where b.room.id = :roomId
              and b.status = :status
              and b.startTime < :requestedEnd
              and b.endTime > :requestedStart
            """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("status") BookingStatus status,
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd);

    List<Booking> findByStatusAndCheckedInFalseAndStartTimeLessThanEqual(
            BookingStatus status,
            LocalDateTime startTime);

    List<Booking> findByOrganizerId(Long organizerId);

        boolean existsByRoom_Id(Long roomId);

        boolean existsByOrganizer_Id(Long organizerId);
}
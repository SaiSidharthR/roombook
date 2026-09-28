package com.example.roombook.repository;

import com.example.roombook.model.Booking;
import com.example.roombook.model.BookingStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            select b from Booking b
            where b.room.id = :roomId
              and b.status = :status
              and b.id <> :excludedBookingId
              and b.startTime < :requestedEnd
              and b.endTime > :requestedStart
            """)
    List<Booking> findOtherOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("status") BookingStatus status,
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd,
            @Param("excludedBookingId") Long excludedBookingId);

    List<Booking> findByStatusAndCheckedInFalseAndStartTimeLessThanEqual(
            BookingStatus status,
            LocalDateTime startTime);

        @Override
        @EntityGraph(attributePaths = {"room", "organizer"})
        List<Booking> findAll(Sort sort);

        @EntityGraph(attributePaths = {"room", "organizer"})
        List<Booking> findByOrganizerId(Long organizerId);

        @EntityGraph(attributePaths = {"room", "organizer"})
        List<Booking> findByStartTimeLessThanAndEndTimeGreaterThan(
                java.time.LocalDateTime dayEnd,
                java.time.LocalDateTime dayStart,
                Sort sort);

        @EntityGraph(attributePaths = {"room", "organizer"})
        List<Booking> findByOrganizerIdAndStartTimeLessThanAndEndTimeGreaterThan(
                Long organizerId,
                java.time.LocalDateTime dayEnd,
                java.time.LocalDateTime dayStart,
                Sort sort);

        @EntityGraph(attributePaths = {"room", "organizer"})
        Optional<Booking> findWithAssociationsById(Long id);

        boolean existsByRoom_Id(Long roomId);

        boolean existsByOrganizer_Id(Long organizerId);
}
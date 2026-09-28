package com.example.roombook.service;

import com.example.roombook.exception.BookingConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.model.Booking;
import com.example.roombook.model.BookingStatus;
import com.example.roombook.model.Employee;
import com.example.roombook.model.Room;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.EmployeeRepository;
import com.example.roombook.repository.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final EmployeeRepository employeeRepository;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            EmployeeRepository employeeRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> listBookings() {
        return bookingRepository.findAll(Sort.by(Sort.Direction.ASC, "startTime"));
    }

    @Transactional(readOnly = true)
    public List<Booking> listBookingsForEmployee(Long employeeId) {
        return bookingRepository.findByOrganizerId(employeeId);
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    public Booking createBooking(Long roomId, Long organizerId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be later than start time");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking start time must be in the future");
        }

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                roomId, BookingStatus.CONFIRMED, startTime, endTime);
        if (!overlaps.isEmpty()) {
            throw new BookingConflictException();
        }

        Employee organizer = employeeRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", organizerId));
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setOrganizer(organizer);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(Long bookingId, Long actorId, boolean administrator) {
        Booking booking = getBooking(bookingId);
        verifyOwner(booking, actorId, administrator);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public Booking checkIn(Long bookingId, Long actorId, boolean administrator) {
        Booking booking = getBooking(bookingId);
        verifyOwner(booking, actorId, administrator);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be checked in");
        }
        booking.setCheckedIn(true);
        return bookingRepository.save(booking);
    }

    @Scheduled(fixedRate = 60_000)
    public void releaseNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Booking> overdue = bookingRepository
                .findByStatusAndCheckedInFalseAndStartTimeLessThanEqual(BookingStatus.CONFIRMED, cutoff);
        overdue.forEach(booking -> booking.setStatus(BookingStatus.NO_SHOW_RELEASED));
        bookingRepository.saveAll(overdue);
    }

    private void verifyOwner(Booking booking, Long actorId, boolean administrator) {
        if (!administrator && !booking.getOrganizer().getId().equals(actorId)) {
            throw new AccessDeniedException("You may only manage your own bookings");
        }
    }
}
package com.example.roombook.service;

import com.example.roombook.exception.BookingConflictException;
import com.example.roombook.model.Booking;
import com.example.roombook.model.BookingStatus;
import com.example.roombook.model.Employee;
import com.example.roombook.model.Room;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.EmployeeRepository;
import com.example.roombook.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @InjectMocks
    private BookingService bookingService;

    private Room room;
    private Employee employee;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(10L);
        employee = new Employee();
        employee.setId(20L);
        startTime = LocalDateTime.now().plusDays(1);
        endTime = startTime.plusHours(1);
    }

    @Test
    void createBookingSavesWhenNoConfirmedBookingOverlaps() {
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(
                10L, BookingStatus.CONFIRMED, startTime, endTime)).thenReturn(List.of());
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(10L, 20L, startTime, endTime);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(room, booking.getRoom());
        assertEquals(employee, booking.getOrganizer());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBookingRejectsAnOverlappingConfirmedBooking() {
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(
                10L, BookingStatus.CONFIRMED, startTime, endTime)).thenReturn(List.of(new Booking()));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(10L, 20L, startTime, endTime));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void cancelBookingChangesStatusAndAllowsTheSlotToBeReused() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithAssociationsById(30L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(bookingRepository.findOverlappingBookings(
                10L, BookingStatus.CONFIRMED, startTime, endTime)).thenReturn(List.of());

        Booking cancelled = bookingService.cancelBooking(30L, 20L, false);
        boolean available = bookingService.isRoomAvailable(10L, startTime, endTime);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertTrue(available);
    }

    @Test
    void releaseNoShowsMarksOnlyRepositoryCandidatesAsReleased() {
        Booking overdue = confirmedBooking();
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(bookingRepository.findByStatusAndCheckedInFalseAndStartTimeLessThanEqual(
                eq(BookingStatus.CONFIRMED), any(LocalDateTime.class))).thenReturn(List.of(overdue));

        bookingService.releaseNoShows();

        assertEquals(BookingStatus.NO_SHOW_RELEASED, overdue.getStatus());
        verify(bookingRepository).findByStatusAndCheckedInFalseAndStartTimeLessThanEqual(
            eq(BookingStatus.CONFIRMED), cutoffCaptor.capture());
        verify(bookingRepository).saveAll(List.of(overdue));
        assertTrue(cutoffCaptor.getValue().isAfter(LocalDateTime.now().minusMinutes(10).minusSeconds(2)));
        assertTrue(cutoffCaptor.getValue().isBefore(LocalDateTime.now().minusMinutes(10).plusSeconds(2)));
    }

    @Test
    void checkInPreventsNoShowReleaseEligibility() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithAssociationsById(30L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking checkedIn = bookingService.checkIn(30L, 20L, false);

        assertTrue(checkedIn.isCheckedIn());
        assertFalse(checkedIn.getStatus() == BookingStatus.NO_SHOW_RELEASED);
    }

    private Booking confirmedBooking() {
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setRoom(room);
        booking.setOrganizer(employee);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
package com.example.roombook.controller;

import com.example.roombook.dto.BookingCreateRequest;
import com.example.roombook.dto.BookingResponse;
import com.example.roombook.model.Booking;
import com.example.roombook.model.Employee;
import com.example.roombook.service.BookingService;
import com.example.roombook.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final EmployeeService employeeService;

    public BookingController(BookingService bookingService, EmployeeService employeeService) {
        this.bookingService = bookingService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<BookingResponse> listBookings(
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication) {
        Employee actor = currentEmployee(principal);
        List<Booking> bookings = isAdministrator(authentication)
                ? bookingService.listBookings()
                : bookingService.listBookingsForEmployee(actor.getId());
        return bookings.stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/availability")
    public boolean isRoomAvailable(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return bookingService.isRoomAvailable(roomId, startTime, endTime);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication) {
        Employee actor = currentEmployee(principal);
        return BookingResponse.from(bookingService.getBookingForActor(
                id, actor.getId(), isAdministrator(authentication)));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Employee actor = currentEmployee(principal);
        Booking saved = bookingService.createBooking(
                request.roomId(), actor.getId(), request.startTime(), request.endTime());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(BookingResponse.from(saved));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication) {
        Employee actor = currentEmployee(principal);
        return BookingResponse.from(bookingService.cancelBooking(
                id, actor.getId(), isAdministrator(authentication)));
    }

    @PostMapping("/{id}/check-in")
    public BookingResponse checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication) {
        Employee actor = currentEmployee(principal);
        return BookingResponse.from(bookingService.checkIn(
                id, actor.getId(), isAdministrator(authentication)));
    }

    private Employee currentEmployee(UserDetails principal) {
        return employeeService.getEmployeeByEmail(principal.getUsername());
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
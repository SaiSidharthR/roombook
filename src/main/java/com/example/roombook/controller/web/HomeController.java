package com.example.roombook.controller.web;

import com.example.roombook.model.BookingStatus;
import com.example.roombook.model.Employee;
import com.example.roombook.service.BookingService;
import com.example.roombook.service.EmployeeService;
import com.example.roombook.service.RoomService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {
    private final BookingService bookingService;
    private final EmployeeService employeeService;
    private final RoomService roomService;

    public HomeController(
            BookingService bookingService,
            EmployeeService employeeService,
            RoomService roomService) {
        this.bookingService = bookingService;
        this.employeeService = employeeService;
        this.roomService = roomService;
    }

    @GetMapping("/")
    public String dashboard(Authentication authentication, Model model) {
        Employee employee = employeeService.getEmployeeByEmail(authentication.getName());
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<com.example.roombook.model.Booking> bookings = administrator
                ? bookingService.listBookingsForDay(LocalDate.now())
                : bookingService.listBookingsForEmployeeOnDay(employee.getId(), LocalDate.now());

        model.addAttribute("today", LocalDate.now());
        model.addAttribute("todayBookings", bookings);
        model.addAttribute("confirmedCount", bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count());
        model.addAttribute("roomCount", roomService.listRooms(org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        model.addAttribute("bookingCount", bookings.size());
        if (administrator) {
            model.addAttribute("employeeCount", employeeService.listEmployees(
                    org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        }
        return "dashboard";
    }
}
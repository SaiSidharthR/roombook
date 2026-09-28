package com.example.roombook.controller.web;

import com.example.roombook.dto.BookingForm;
import com.example.roombook.exception.BookingConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.model.Booking;
import com.example.roombook.model.Employee;
import com.example.roombook.service.BookingService;
import com.example.roombook.service.EmployeeService;
import com.example.roombook.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingPageController {
    private final BookingService bookingService;
    private final EmployeeService employeeService;
    private final RoomService roomService;

    public BookingPageController(
            BookingService bookingService,
            EmployeeService employeeService,
            RoomService roomService) {
        this.bookingService = bookingService;
        this.employeeService = employeeService;
        this.roomService = roomService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails principal, Authentication authentication, Model model) {
        Employee employee = currentEmployee(principal);
        List<Booking> bookings = isAdministrator(authentication)
                ? bookingService.listBookings()
                : bookingService.listBookingsForEmployee(employee.getId());
        model.addAttribute("bookings", bookings);
        return "bookings/list";
    }

    @GetMapping("/new")
    public String newBooking(@RequestParam(required = false) Long roomId, Model model) {
        BookingForm form = new BookingForm();
        form.setRoomId(roomId);
        form.setStartTime(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0));
        form.setEndTime(form.getStartTime().plusHours(1));
        model.addAttribute("bookingForm", form);
        model.addAttribute("editing", false);
        addRoomOptions(model);
        return "bookings/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Employee employee = currentEmployee(principal);
        Booking booking = bookingService.getBookingForActor(id, employee.getId(), isAdministrator(authentication));
        if (booking.getStatus().name().equals("CONFIRMED") && !booking.isCheckedIn()) {
            BookingForm form = new BookingForm();
            form.setRoomId(booking.getRoom().getId());
            form.setStartTime(booking.getStartTime());
            form.setEndTime(booking.getEndTime());
            model.addAttribute("bookingForm", form);
            model.addAttribute("editing", true);
            model.addAttribute("bookingId", booking.getId());
            addRoomOptions(model);
            return "bookings/form";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Only unchecked-in confirmed bookings can be changed.");
        return "redirect:/bookings";
    }

    @GetMapping("/availability")
    @ResponseBody
    public boolean isAvailable(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return bookingService.isRoomAvailable(roomId, startTime, endTime);
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("bookingForm") BookingForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            addRoomOptions(model);
            return "bookings/form";
        }
        Employee employee = currentEmployee(principal);
        try {
            bookingService.createBooking(
                    form.getRoomId(), employee.getId(), form.getStartTime(), form.getEndTime());
            redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed.");
            return "redirect:/bookings";
        } catch (BookingConflictException | IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("editing", false);
            addRoomOptions(model);
            return "bookings/form";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("bookingForm") BookingForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("editing", true);
        model.addAttribute("bookingId", id);
        if (bindingResult.hasErrors()) {
            addRoomOptions(model);
            return "bookings/form";
        }
        Employee employee = currentEmployee(principal);
        try {
            bookingService.updateBooking(id, form.getRoomId(), employee.getId(),
                    isAdministrator(authentication), form.getStartTime(), form.getEndTime());
            redirectAttributes.addFlashAttribute("successMessage", "Booking updated.");
            return "redirect:/bookings";
        } catch (BookingConflictException | IllegalArgumentException | IllegalStateException
                 | AccessDeniedException | ResourceNotFoundException exception) {
            model.addAttribute("formError", exception.getMessage());
            addRoomOptions(model);
            return "bookings/form";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Employee employee = currentEmployee(principal);
        try {
            bookingService.cancelBooking(id, employee.getId(), isAdministrator(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled.");
        } catch (AccessDeniedException | IllegalStateException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Employee employee = currentEmployee(principal);
        try {
            bookingService.checkIn(id, employee.getId(), isAdministrator(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Check-in recorded.");
        } catch (AccessDeniedException | IllegalStateException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/bookings";
    }

    private Employee currentEmployee(UserDetails principal) {
        return employeeService.getEmployeeByEmail(principal.getUsername());
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private void addRoomOptions(Model model) {
        model.addAttribute("rooms", roomService.listRooms(
                PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name"))).getContent());
    }
}
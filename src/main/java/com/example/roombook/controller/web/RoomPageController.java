package com.example.roombook.controller.web;

import com.example.roombook.exception.ResourceConflictException;
import com.example.roombook.model.Room;
import com.example.roombook.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomPageController {
    private final RoomService roomService;

    public RoomPageController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Pageable pageable, Model model) {
        Pageable request = PageRequest.of(pageable.getPageNumber(), 12, Sort.by("name").ascending());
        Page<Room> rooms = roomService.listRooms(request);
        model.addAttribute("rooms", rooms);
        return "rooms/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newRoom(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("pageTitle", "Add a room");
        return "rooms/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(
            @Valid @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Add a room");
            return "rooms/form";
        }
        roomService.createRoom(room);
        redirectAttributes.addFlashAttribute("successMessage", "Room added.");
        return "redirect:/rooms";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("room", roomService.getRoom(id));
        model.addAttribute("pageTitle", "Edit room");
        return "rooms/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            room.setId(id);
            model.addAttribute("pageTitle", "Edit room");
            return "rooms/form";
        }
        roomService.updateRoom(id, room);
        redirectAttributes.addFlashAttribute("successMessage", "Room updated.");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted.");
        } catch (ResourceConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/rooms";
    }
}
package com.example.roombook.controller.web;

import com.example.roombook.dto.EmployeeForm;
import com.example.roombook.exception.ResourceConflictException;
import com.example.roombook.model.Employee;
import com.example.roombook.model.EmployeeRole;
import com.example.roombook.service.EmployeeService;
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
@RequestMapping("/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeePageController {
    private final EmployeeService employeeService;

    public EmployeePageController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(Pageable pageable, Model model) {
        Pageable request = PageRequest.of(pageable.getPageNumber(), 15, Sort.by("name").ascending());
        Page<Employee> employees = employeeService.listEmployees(request);
        model.addAttribute("employees", employees);
        return "employees/list";
    }

    @GetMapping("/new")
    public String newEmployee(Model model) {
        model.addAttribute("employeeForm", new EmployeeForm());
        model.addAttribute("pageTitle", "Add an employee");
        model.addAttribute("creating", true);
        model.addAttribute("roles", EmployeeRole.values());
        return "employees/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "required", "Password is required for a new employee.");
        }
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Add an employee", true);
            return "employees/form";
        }

        try {
            employeeService.createEmployee(toEmployee(form));
            redirectAttributes.addFlashAttribute("successMessage", "Employee added.");
            return "redirect:/employees";
        } catch (ResourceConflictException exception) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
            prepareForm(model, "Add an employee", true);
            return "employees/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployee(id);
        EmployeeForm form = new EmployeeForm();
        form.setId(employee.getId());
        form.setName(employee.getName());
        form.setEmail(employee.getEmail());
        form.setDepartment(employee.getDepartment());
        form.setRole(employee.getRole());
        model.addAttribute("employeeForm", form);
        prepareForm(model, "Edit employee", false);
        return "employees/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Edit employee", false);
            return "employees/form";
        }
        try {
            employeeService.updateEmployee(id, toEmployee(form));
            redirectAttributes.addFlashAttribute("successMessage", "Employee updated.");
            return "redirect:/employees";
        } catch (ResourceConflictException exception) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
            prepareForm(model, "Edit employee", false);
            return "employees/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Employee deleted.");
        } catch (ResourceConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/employees";
    }

    private void prepareForm(Model model, String pageTitle, boolean creating) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("creating", creating);
        model.addAttribute("roles", EmployeeRole.values());
    }

    private Employee toEmployee(EmployeeForm form) {
        Employee employee = new Employee();
        employee.setName(form.getName());
        employee.setEmail(form.getEmail());
        employee.setDepartment(form.getDepartment());
        employee.setRole(form.getRole());
        employee.setPassword(form.getPassword());
        return employee;
    }
}
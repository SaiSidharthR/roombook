package com.example.roombook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPagesTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageRendersForUnauthenticatedVisitors() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back.")));
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void dashboardAndMainDirectoryPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Today's bookings")));

        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/list"));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/list"));
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void creationFormsRender() throws Exception {
        mockMvc.perform(get("/rooms/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/form"));

        mockMvc.perform(get("/employees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/form"));

        mockMvc.perform(get("/bookings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"));
    }

    @Test
    @WithMockUser(username = "employee@example.test", roles = "EMPLOYEE")
    void employeeCannotOpenEmployeeAdministration() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isForbidden());
    }
}
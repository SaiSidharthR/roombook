package com.example.roombook.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class WebModelAdvice {
    @ModelAttribute
    public void addNavigationState(Model model, Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        String currentEmail = "";
        boolean administrator = false;
        if (authenticated) {
            currentEmail = authentication.getName();
            administrator = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        model.addAttribute("currentEmail", currentEmail);
        model.addAttribute("isAdmin", administrator);
    }
}
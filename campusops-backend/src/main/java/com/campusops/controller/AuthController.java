package com.campusops.controller;

import com.campusops.entity.User;
import com.campusops.entity.enums.Role;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * Serves Thymeleaf-based authentication pages.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Renders the login page.
     */
    @GetMapping("/login")
    public String loginPage(java.security.Principal principal) {
        if (principal != null) return "redirect:/dashboard";
        return "auth/login";
    }

    /**
     * Renders the registration page.
     */
    @GetMapping("/register")
    public String registerPage(java.security.Principal principal) {
        if (principal != null) return "redirect:/dashboard";
        return "auth/register";
    }

    /**
     * Processes the registration form.
     */
    @PostMapping("/register")
    public String processRegistration(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
            
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "An account with that email already exists.");
            return "auth/register";
        }
        
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.MANAGER); // Default role for new signups
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        return "redirect:/login?registered=true";
    }
}

package com.campusops.controller.web;

import com.campusops.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final UserService userService;

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getCounselors());
        return "admin/users";
    }

    @GetMapping("/integrations")
    public String listIntegrations(Model model) {
        model.addAttribute("integrations", Collections.emptyList());
        return "admin/integrations";
    }

    @org.springframework.web.bind.annotation.PostMapping("/users")
    public String createUser(
            @org.springframework.web.bind.annotation.RequestParam String fullName,
            @org.springframework.web.bind.annotation.RequestParam String email,
            @org.springframework.web.bind.annotation.RequestParam String password,
            @org.springframework.web.bind.annotation.RequestParam String role,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.campusops.security.CustomUserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        try {
            com.campusops.dto.UserCreateRequest request = new com.campusops.dto.UserCreateRequest();
            request.setFullName(fullName);
            request.setEmail(email);
            request.setPassword(password);
            request.setRole(role);
            
            userService.createUser(request, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Team member added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding member: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @org.springframework.web.bind.annotation.PostMapping("/users/{id}/edit")
    public String updateUser(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam String fullName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String password,
            @org.springframework.web.bind.annotation.RequestParam String role,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.campusops.security.CustomUserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        try {
            com.campusops.dto.UserCreateRequest request = new com.campusops.dto.UserCreateRequest();
            request.setFullName(fullName);
            request.setPassword(password);
            request.setRole(role);
            
            userService.updateUser(id, request, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Team member updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating member: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
}

package com.campusops.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationWebController {

    @GetMapping
    public String listNotifications(Model model) {
        model.addAttribute("notifications", Collections.emptyList());
        return "notifications/list";
    }
}

package com.campusops.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MiscWebController {

    private final com.campusops.service.ActivityLogService activityLogService;

    public MiscWebController(com.campusops.service.ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/admin/logs")
    public String logs(org.springframework.ui.Model model) {
        model.addAttribute("logs", activityLogService.getRecentLogs(org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
        return "admin/logs";
    }
}

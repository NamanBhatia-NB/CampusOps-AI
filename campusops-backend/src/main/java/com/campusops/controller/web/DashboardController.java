package com.campusops.controller.web;

import com.campusops.dto.DashboardDTO;
import com.campusops.entity.enums.Role;
import com.campusops.service.DashboardService;
import com.campusops.service.LeadService;
import com.campusops.service.TaskService;
import com.campusops.service.UserService;
import com.campusops.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final LeadService leadService;
    private final TaskService taskService;
    private final UserService userService;



    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long userId = userDetails.getUser().getId();
        Role role = userDetails.getUser().getRole();

        DashboardDTO dashboard = dashboardService.getDashboardData(userId, role);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("sourceMap", dashboardService.getLeadsBySourceMap(userId, role));
        model.addAttribute("activities", dashboardService.getRecentActivity(PageRequest.of(0, 10)).getContent());
        model.addAttribute("urgentTasks", taskService.getOverdueTasks().stream().limit(5).toList());

        return "dashboard/index";
    }
}

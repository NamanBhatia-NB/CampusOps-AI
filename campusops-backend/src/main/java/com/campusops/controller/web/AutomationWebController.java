package com.campusops.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;

@Controller
@RequestMapping("/automation")
@RequiredArgsConstructor
public class AutomationWebController {

    private final com.campusops.service.AutomationService automationService;

    @GetMapping
    public String listRules(Model model) {
        model.addAttribute("rules", automationService.getAllRules(org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
        return "automation/list";
    }

    @org.springframework.web.bind.annotation.PostMapping
    public String createRule(
            @org.springframework.web.bind.annotation.RequestParam String name,
            @org.springframework.web.bind.annotation.RequestParam String description,
            @org.springframework.web.bind.annotation.RequestParam String triggerType,
            @org.springframework.web.bind.annotation.RequestParam String triggerConfig,
            @org.springframework.web.bind.annotation.RequestParam String actionType,
            @org.springframework.web.bind.annotation.RequestParam String actionConfig,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.campusops.security.CustomUserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = userDetails.getUser().getId();
            com.campusops.entity.enums.TriggerType tType = com.campusops.entity.enums.TriggerType.valueOf(triggerType);
            com.campusops.entity.enums.ActionType aType = com.campusops.entity.enums.ActionType.valueOf(actionType);
            
            // Format config as JSON
            String tConfigJson = "{\"condition\":\"" + triggerConfig + "\"}";
            String aConfigJson = "{}";
            if (aType == com.campusops.entity.enums.ActionType.UPDATE_STATUS) {
                aConfigJson = "{\"status\":\"" + actionConfig + "\"}";
            } else if (aType == com.campusops.entity.enums.ActionType.CREATE_TASK) {
                aConfigJson = "{\"title\":\"" + actionConfig + "\"}";
            }
            
            automationService.createRule(name, description, tType, tConfigJson, aType, aConfigJson, userId);
            redirectAttributes.addFlashAttribute("success", "Automation rule created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating rule: " + e.getMessage());
        }
        
        return "redirect:/automation";
    }
}

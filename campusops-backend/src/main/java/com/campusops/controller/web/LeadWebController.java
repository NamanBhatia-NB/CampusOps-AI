package com.campusops.controller.web;

import com.campusops.dto.LeadCreateRequest;
import com.campusops.dto.LeadDTO;
import com.campusops.entity.enums.LeadStatus;
import com.campusops.security.CustomUserDetails;
import com.campusops.service.AiInsightService;
import com.campusops.service.ConversationService;
import com.campusops.service.LeadService;
import com.campusops.service.TaskService;
import com.campusops.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadWebController {

    private final LeadService leadService;
    private final UserService userService;
    private final ConversationService conversationService;
    private final TaskService taskService;
    private final AiInsightService aiInsightService;

    @GetMapping({"", "/"})
    public String listLeads(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeadDTO> leads = leadService.filterLeads(status, source, priority, ownerId, search, pageable);

        model.addAttribute("leads", leads);
        model.addAttribute("counselors", userService.getCounselors());
        
        return "leads/list";
    }

    @GetMapping("/{id}")
    public String viewLead(@PathVariable Long id, Model model) {
        LeadDTO lead = leadService.getLeadById(id);
        model.addAttribute("lead", lead);
        model.addAttribute("notes", leadService.getNotes(id));
        model.addAttribute("conversations", conversationService.getConversationsByLead(id));
        model.addAttribute("tasks", taskService.getTasksByLead(id));
        
        // Ensure AI insight is generated/updated if viewing
        // In reality, this might be triggered asynchronously
        
        return "leads/detail";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("leadRequest", new LeadCreateRequest());
        model.addAttribute("counselors", userService.getCounselors());
        return "leads/form";
    }

    @PostMapping
    public String createLead(
            @Valid @ModelAttribute("leadRequest") LeadCreateRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirectAttributes,
            Model model,
            @RequestHeader(value = "Referer", required = false) String referer) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please provide all required fields (Name and Email).");
            if (referer != null && referer.contains("/dashboard")) {
                return "redirect:/dashboard";
            }
            return "redirect:/leads/new";
        }

        try {
            LeadDTO lead = leadService.createLead(request, user.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Lead created successfully");
            return "redirect:/leads/" + lead.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating lead: " + e.getMessage());
            if (referer != null && referer.contains("/dashboard")) {
                return "redirect:/dashboard";
            }
            return "redirect:/leads/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editLeadForm(@PathVariable Long id, Model model) {
        LeadDTO lead = leadService.getLeadById(id);
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFullName(lead.getFullName());
        request.setEmail(lead.getEmail());
        request.setPhone(lead.getPhone());
        request.setSource(lead.getSource());
        request.setProgramInterest(lead.getProgramInterest());
        request.setCity(lead.getCity());
        request.setState(lead.getState());
        request.setCountry(lead.getCountry());
        request.setParentName(lead.getParentName());
        request.setParentPhone(lead.getParentPhone());
        request.setQualification(lead.getQualification());
        request.setStatus(lead.getStatus());
        request.setPriority(lead.getPriority());
        request.setOwnerId(lead.getOwnerId());
        
        if (lead.getDateOfBirth() != null) {
            request.setDateOfBirth(lead.getDateOfBirth().split("T")[0]);
        }

        model.addAttribute("leadRequest", request);
        model.addAttribute("counselors", userService.getCounselors());
        model.addAttribute("leadId", id);
        return "leads/form";
    }

    @PostMapping("/{id}/edit")
    public String updateLead(
            @PathVariable Long id,
            @Valid @ModelAttribute("leadRequest") LeadCreateRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("counselors", userService.getCounselors());
            model.addAttribute("leadId", id);
            return "leads/form";
        }

        try {
            leadService.updateLead(id, request, user.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Lead updated successfully");
            return "redirect:/leads/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating lead: " + e.getMessage());
            return "redirect:/leads/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, 
                               @AuthenticationPrincipal CustomUserDetails user,
                               RedirectAttributes redirectAttributes) {
        try {
            leadService.updateLeadStatus(id, LeadStatus.valueOf(status), user.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status");
        }
        return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String content,
                          @RequestParam(required = false) Boolean isInternal,
                          @AuthenticationPrincipal CustomUserDetails user,
                          RedirectAttributes redirectAttributes) {
        try {
            leadService.addNote(id, content, Boolean.TRUE.equals(isInternal), user.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Note added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding note");
        }
        return "redirect:/leads/" + id;
    }
}

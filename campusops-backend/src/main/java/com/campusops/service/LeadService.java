package com.campusops.service;

import com.campusops.dto.LeadCreateRequest;
import com.campusops.dto.LeadDTO;
import com.campusops.entity.*;
import com.campusops.entity.enums.*;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final AiInsightRepository aiInsightRepository;
    private final ActivityLogService activityLogService;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // ==========================================
    // CRUD Operations
    // ==========================================

    @Transactional(readOnly = true)
    public Page<LeadDTO> getAllLeads(Pageable pageable) {
        return leadRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> searchLeads(String query, Pageable pageable) {
        return leadRepository.searchByNameOrEmail(query, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeadsByStatus(LeadStatus status, Pageable pageable) {
        return leadRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeadsByOwner(Long ownerId, Pageable pageable) {
        return leadRepository.findByOwnerId(ownerId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> filterLeads(String status, String source, String priority,
                                      Long ownerId, String search, Pageable pageable) {
        LeadStatus leadStatus = status != null && !status.isEmpty() ? LeadStatus.valueOf(status) : null;
        Priority leadPriority = priority != null && !priority.isEmpty() ? Priority.valueOf(priority) : null;

        if (search != null && !search.isBlank()) {
            return leadRepository.searchByNameOrEmail(search, pageable).map(this::toDTO);
        }

        if (leadStatus != null && ownerId != null) {
            return leadRepository.findByStatusAndOwnerId(leadStatus, ownerId, pageable).map(this::toDTO);
        }

        if (leadStatus != null) {
            return leadRepository.findByStatus(leadStatus, pageable).map(this::toDTO);
        }

        if (ownerId != null) {
            return leadRepository.findByOwnerId(ownerId, pageable).map(this::toDTO);
        }

        return leadRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public LeadDTO getLeadById(Long id) {
        Lead lead = findLeadOrThrow(id);
        LeadDTO dto = toDTO(lead);

        // Attach AI insight if available
        aiInsightRepository.findByLeadId(id).ifPresent(insight -> {
            dto.setLeadScore(insight.getLeadScore());
            dto.setClassification(insight.getClassification().name());
            dto.setAiSummary(insight.getSummary());
            dto.setRecommendedAction(insight.getRecommendedAction());
        });

        return dto;
    }

    public LeadDTO createLead(LeadCreateRequest request, Long createdByUserId) {
        Lead lead = new Lead();
        mapRequestToEntity(request, lead);

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getOwnerId()));
            lead.setOwner(owner);
        }

        lead = leadRepository.save(lead);
        log.info("Lead created: id={}, name={}", lead.getId(), lead.getFullName());

        activityLogService.log("LEAD_CREATED", "LEAD", lead.getId(),
                "Lead created: " + lead.getFullName(), createdByUserId);

        return toDTO(lead);
    }

    public LeadDTO updateLead(Long id, LeadCreateRequest request, Long updatedByUserId) {
        Lead lead = findLeadOrThrow(id);
        String oldStatus = lead.getStatus().name();

        mapRequestToEntity(request, lead);

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getOwnerId()));
            lead.setOwner(owner);
        }

        lead = leadRepository.save(lead);
        log.info("Lead updated: id={}, name={}", lead.getId(), lead.getFullName());

        String newStatus = lead.getStatus().name();
        String details = oldStatus.equals(newStatus)
                ? "Lead updated: " + lead.getFullName()
                : "Status changed: " + oldStatus + " → " + newStatus;

        activityLogService.log("LEAD_UPDATED", "LEAD", lead.getId(), details, updatedByUserId);

        return toDTO(lead);
    }

    public LeadDTO updateLeadStatus(Long id, LeadStatus newStatus, Long updatedByUserId) {
        Lead lead = findLeadOrThrow(id);
        String oldStatus = lead.getStatus().name();
        lead.setStatus(newStatus);

        if (newStatus == LeadStatus.ADMITTED) {
            lead.setEnrolledAt(LocalDateTime.now());
        }

        lead = leadRepository.save(lead);

        activityLogService.log("STATUS_CHANGED", "LEAD", lead.getId(),
                "Status: " + oldStatus + " → " + newStatus.name(), updatedByUserId);

        return toDTO(lead);
    }

    public void deleteLead(Long id, Long deletedByUserId) {
        Lead lead = findLeadOrThrow(id);
        String name = lead.getFullName();
        leadRepository.delete(lead);
        log.info("Lead deleted: id={}, name={}", id, name);

        activityLogService.log("LEAD_DELETED", "LEAD", id, "Lead deleted: " + name, deletedByUserId);
    }

    public LeadDTO assignOwner(Long leadId, Long ownerId, Long assignedByUserId) {
        Lead lead = findLeadOrThrow(leadId);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        lead.setOwner(owner);
        lead = leadRepository.save(lead);

        activityLogService.log("LEAD_ASSIGNED", "LEAD", leadId,
                "Assigned to: " + owner.getFullName(), assignedByUserId);

        return toDTO(lead);
    }

    // ==========================================
    // Notes
    // ==========================================

    @Transactional(readOnly = true)
    public List<LeadNote> getNotes(Long leadId) {
        findLeadOrThrow(leadId);
        return leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
    }

    public LeadNote addNote(Long leadId, String content, boolean isInternal, Long authorId) {
        Lead lead = findLeadOrThrow(leadId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        LeadNote note = new LeadNote();
        note.setLead(lead);
        note.setAuthor(author);
        note.setContent(content);
        note.setIsInternal(isInternal);

        note = leadNoteRepository.save(note);
        log.info("Note added to lead {}: author={}", leadId, author.getFullName());

        activityLogService.log("NOTE_ADDED", "LEAD", leadId,
                "Note added by " + author.getFullName(), authorId);

        return note;
    }

    // ==========================================
    // Statistics
    // ==========================================

    @Transactional(readOnly = true)
    public long countByStatus(LeadStatus status) {
        return leadRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countTotal() {
        return leadRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Lead> getLeadsNeedingFollowUp() {
        return leadRepository.findByNextFollowUpBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Lead> getInactiveLeads(int daysInactive) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysInactive);
        return leadRepository.findByStatusAndLastContactedAtBefore(LeadStatus.CONTACTED, threshold);
    }

    // ==========================================
    // Helpers
    // ==========================================

    private Lead findLeadOrThrow(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));
    }

    private void mapRequestToEntity(LeadCreateRequest request, Lead lead) {
        lead.setFullName(request.getFullName());
        lead.setEmail(request.getEmail());
        lead.setPhone(request.getPhone());
        lead.setSource(request.getSource());
        lead.setProgramInterest(request.getProgramInterest());
        lead.setTags(request.getTags());
        lead.setCity(request.getCity());
        lead.setState(request.getState());
        lead.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        lead.setParentName(request.getParentName());
        lead.setParentPhone(request.getParentPhone());
        lead.setQualification(request.getQualification());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            lead.setStatus(LeadStatus.valueOf(request.getStatus()));
        } else if (lead.getStatus() == null) {
            lead.setStatus(LeadStatus.NEW);
        }

        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            lead.setPriority(Priority.valueOf(request.getPriority()));
        } else if (lead.getPriority() == null) {
            lead.setPriority(Priority.MEDIUM);
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            lead.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        }

        if (request.getNextFollowUp() != null && !request.getNextFollowUp().isBlank()) {
            lead.setNextFollowUp(LocalDateTime.parse(request.getNextFollowUp(), DT_FORMAT));
        }
    }

    public LeadDTO toDTO(Lead lead) {
        return LeadDTO.builder()
                .id(lead.getId())
                .fullName(lead.getFullName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .source(lead.getSource())
                .status(lead.getStatus().name())
                .priority(lead.getPriority().name())
                .programInterest(lead.getProgramInterest())
                .ownerId(lead.getOwner() != null ? lead.getOwner().getId() : null)
                .ownerName(lead.getOwner() != null ? lead.getOwner().getFullName() : "Unassigned")
                .tags(lead.getTags())
                .city(lead.getCity())
                .state(lead.getState())
                .country(lead.getCountry())
                .dateOfBirth(lead.getDateOfBirth() != null ? lead.getDateOfBirth().toString() : null)
                .parentName(lead.getParentName())
                .parentPhone(lead.getParentPhone())
                .qualification(lead.getQualification())
                .lastContactedAt(lead.getLastContactedAt() != null ? lead.getLastContactedAt().toString() : null)
                .nextFollowUp(lead.getNextFollowUp() != null ? lead.getNextFollowUp().toString() : null)
                .enrolledAt(lead.getEnrolledAt() != null ? lead.getEnrolledAt().toString() : null)
                .lostReason(lead.getLostReason())
                .createdAt(lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : null)
                .updatedAt(lead.getUpdatedAt() != null ? lead.getUpdatedAt().toString() : null)
                .build();
    }
}

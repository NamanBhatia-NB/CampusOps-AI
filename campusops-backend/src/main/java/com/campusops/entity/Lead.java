package com.campusops.entity;

import com.campusops.entity.enums.LeadStatus;
import com.campusops.entity.enums.Priority;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_leads_status", columnList = "status"),
        @Index(name = "idx_leads_owner", columnList = "owner_id"),
        @Index(name = "idx_leads_source", columnList = "source"),
        @Index(name = "idx_leads_priority", columnList = "priority"),
        @Index(name = "idx_leads_email", columnList = "email"),
        @Index(name = "idx_leads_phone", columnList = "phone"),
        @Index(name = "idx_leads_next_followup", columnList = "next_follow_up"),
        @Index(name = "idx_leads_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Size(max = 255)
    private String email;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Size(max = 100)
    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Size(max = 255)
    @Column(name = "program_interest")
    private String programInterest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Size(max = 500)
    @Column(length = 500)
    private String tags;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String state;

    @Size(max = 100)
    @Column(length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 255)
    @Column(name = "parent_name")
    private String parentName;

    @Size(max = 20)
    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Size(max = 255)
    private String qualification;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @Column(name = "next_follow_up")
    private LocalDateTime nextFollowUp;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Size(max = 500)
    @Column(name = "lost_reason", length = 500)
    private String lostReason;

    // --- Relationships ---

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LeadNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToOne(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AiInsight aiInsight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lead lead = (Lead) o;
        return getId() != null && getId().equals(lead.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Lead{" +
                "id=" + getId() +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }
}

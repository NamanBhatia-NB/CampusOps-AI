package com.campusops.entity;

import com.campusops.entity.enums.Classification;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_insights", indexes = {
        @Index(name = "idx_insight_score", columnList = "lead_score"),
        @Index(name = "idx_insight_class", columnList = "classification")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    private Lead lead;

    @Column(name = "lead_score", nullable = false)
    @Builder.Default
    private Integer leadScore = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Classification classification = Classification.WARM;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "risk_flags", columnDefinition = "json")
    private String riskFlags;

    @Size(max = 500)
    @Column(name = "recommended_action", length = 500)
    private String recommendedAction;

    @Size(max = 50)
    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiInsight that = (AiInsight) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

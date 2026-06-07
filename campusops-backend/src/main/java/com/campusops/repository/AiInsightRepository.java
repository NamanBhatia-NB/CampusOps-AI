package com.campusops.repository;

import com.campusops.entity.AiInsight;
import com.campusops.entity.enums.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {

    Optional<AiInsight> findByLeadId(Long leadId);

    List<AiInsight> findByClassification(Classification classification);

    List<AiInsight> findByLeadScoreLessThan(int score);
}

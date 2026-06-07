package com.campusops.repository;

import com.campusops.entity.RuleExecution;
import com.campusops.entity.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleExecutionRepository extends JpaRepository<RuleExecution, Long> {

    org.springframework.data.domain.Page<RuleExecution> findByRuleIdOrderByExecutedAtDesc(Long ruleId, org.springframework.data.domain.Pageable pageable);

    long countByRuleIdAndStatus(Long ruleId, ExecutionStatus status);
}

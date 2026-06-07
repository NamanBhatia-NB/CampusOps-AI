package com.campusops.repository;

import com.campusops.entity.AutomationRule;
import com.campusops.entity.enums.TriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByIsActiveTrue();

    List<AutomationRule> findByTriggerType(TriggerType triggerType);
}

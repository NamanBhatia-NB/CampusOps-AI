package com.campusops.repository;

import com.campusops.entity.Integration;
import com.campusops.entity.enums.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    List<Integration> findByType(IntegrationType type);

    List<Integration> findByIsActiveTrue();
}

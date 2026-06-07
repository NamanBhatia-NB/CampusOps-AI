package com.campusops.repository;

import com.campusops.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ActivityLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Page<ActivityLog> findByUserId(Long userId, Pageable pageable);
}

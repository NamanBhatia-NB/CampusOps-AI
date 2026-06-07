package com.campusops.repository;

import com.campusops.entity.Lead;
import com.campusops.entity.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByStatus(LeadStatus status);

    List<Lead> findByOwnerId(Long ownerId);

    Page<Lead> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE LOWER(l.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Lead> searchByNameOrEmail(@Param("keyword") String keyword, Pageable pageable);

    List<Lead> findByNextFollowUpBefore(LocalDateTime dateTime);

    @Query("SELECT l FROM Lead l WHERE l.status = :status AND l.lastContactedAt < :before")
    List<Lead> findByStatusAndLastContactedAtBefore(
            @Param("status") LeadStatus status,
            @Param("before") LocalDateTime before);

    long countByStatus(LeadStatus status);

    long countByOwnerIdAndStatus(Long ownerId, LeadStatus status);

    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);

    Page<Lead> findByStatusAndOwnerId(LeadStatus status, Long ownerId, Pageable pageable);

    long countByOwnerId(Long ownerId);
}

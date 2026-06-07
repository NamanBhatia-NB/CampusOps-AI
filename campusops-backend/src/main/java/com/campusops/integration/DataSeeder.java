package com.campusops.integration;

import com.campusops.config.AppConfig;
import com.campusops.entity.User;
import com.campusops.entity.enums.Role;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final com.campusops.repository.LeadRepository leadRepository;
    private final com.campusops.service.LeadService leadService;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig appConfig;

    @Override
    public void run(String... args) {
        if (!appConfig.getDemo().isSeedData()) {
            return;
        }

        if (userRepository.count() == 0) {
            log.info("No users found. Seeding initial admin user...");
            seedAdminUser();
        }

        if (leadRepository.count() == 0) {
            log.info("No leads found. Seeding dummy leads...");
            seedDummyLeads();
        }
    }

    private void seedDummyLeads() {
        User admin = userRepository.findByEmail(appConfig.getDemo().getAdminEmail()).orElse(null);
        Long adminId = admin != null ? admin.getId() : 1L;

        com.campusops.dto.LeadCreateRequest lead1 = new com.campusops.dto.LeadCreateRequest();
        lead1.setFullName("John Doe");
        lead1.setEmail("john.doe@example.com");
        lead1.setPhone("9876543210");
        lead1.setProgramInterest("B.Tech Computer Science");
        lead1.setSource("Website");
        lead1.setStatus("NEW");
        lead1.setPriority("HIGH");
        lead1.setOwnerId(adminId);
        leadService.createLead(lead1, adminId);

        com.campusops.dto.LeadCreateRequest lead2 = new com.campusops.dto.LeadCreateRequest();
        lead2.setFullName("Jane Smith");
        lead2.setEmail("jane.smith@example.com");
        lead2.setPhone("9876543211");
        lead2.setProgramInterest("MBA Marketing");
        lead2.setSource("Referral");
        lead2.setStatus("CONTACTED");
        lead2.setPriority("MEDIUM");
        lead2.setOwnerId(adminId);
        leadService.createLead(lead2, adminId);
        
        com.campusops.dto.LeadCreateRequest lead3 = new com.campusops.dto.LeadCreateRequest();
        lead3.setFullName("Rahul Verma");
        lead3.setEmail("rahul.verma@example.com");
        lead3.setPhone("9876543212");
        lead3.setProgramInterest("BBA");
        lead3.setSource("WhatsApp");
        lead3.setStatus("QUALIFIED");
        lead3.setPriority("URGENT");
        lead3.setOwnerId(adminId);
        leadService.createLead(lead3, adminId);
        
        log.info("Dummy leads seeded successfully.");
    }

    private void seedAdminUser() {
        String adminEmail = appConfig.getDemo().getAdminEmail();
        String adminPassword = appConfig.getDemo().getAdminPassword();

        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        admin.setDepartment("Management");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.save(admin);
        log.info("Admin user created successfully with email: {}", adminEmail);

        // Seed counselor if not exists
        if (!userRepository.existsByEmail("counselor@campusops.ai")) {
            User counselor = new User();
            counselor.setFullName("Demo Counselor");
            counselor.setEmail("counselor@campusops.ai");
            counselor.setPasswordHash(passwordEncoder.encode("password"));
            counselor.setRole(Role.COUNSELOR);
            counselor.setIsActive(true);
            counselor.setCreatedAt(LocalDateTime.now());
            counselor.setUpdatedAt(LocalDateTime.now());
            userRepository.save(counselor);
            log.info("Demo Counselor created.");
        }
    }
}

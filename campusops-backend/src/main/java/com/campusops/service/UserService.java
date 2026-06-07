package com.campusops.service;

import com.campusops.dto.UserCreateRequest;
import com.campusops.dto.UserDTO;
import com.campusops.entity.User;
import com.campusops.entity.enums.Role;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getActiveUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getCounselors() {
        return getUsersByRole(Role.COUNSELOR);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toDTO(user);
    }

    public UserDTO createUser(UserCreateRequest request, Long createdByUserId) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.valueOf(request.getRole()));
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setIsActive(true);

        user = userRepository.save(user);
        log.info("User created: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        activityLogService.log("USER_CREATED", "USER", user.getId(),
                "User created: " + user.getFullName() + " (" + user.getRole() + ")", createdByUserId);

        return toDTO(user);
    }

    public UserDTO updateUser(Long id, UserCreateRequest request, Long updatedByUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setFullName(request.getFullName());
        user.setRole(Role.valueOf(request.getRole()));
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("User updated: id={}, email={}", user.getId(), user.getEmail());

        activityLogService.log("USER_UPDATED", "USER", user.getId(),
                "User updated: " + user.getFullName(), updatedByUserId);

        return toDTO(user);
    }

    public void toggleActive(Long id, Long performedByUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        String action = user.getIsActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        activityLogService.log(action, "USER", user.getId(),
                user.getFullName() + " " + (user.getIsActive() ? "activated" : "deactivated"),
                performedByUserId);
    }

    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.findByRole(role).size();
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .active(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}

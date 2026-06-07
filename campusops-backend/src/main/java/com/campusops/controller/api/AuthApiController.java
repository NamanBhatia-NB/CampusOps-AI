package com.campusops.controller.api;

import com.campusops.dto.ApiResponse;
import com.campusops.dto.AuthRequest;
import com.campusops.dto.AuthResponse;
import com.campusops.entity.User;
import com.campusops.security.CustomUserDetails;
import com.campusops.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API endpoints for JWT-based authentication.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticates the user and returns an access token.
     *
     * @param request validated email + password
     * @return JWT token wrapped in {@link AuthResponse}
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        log.info("API login attempt for: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        AuthResponse authResponse = AuthResponse.builder()
                .token(accessToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();

        log.info("API login successful for: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param body JSON body with {@code refreshToken} field
     * @return new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired refresh token"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);

        AuthResponse authResponse = AuthResponse.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .email(username)
                .build();

        log.info("Token refreshed for: {}", username);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    /**
     * Returns the currently authenticated user's profile information.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        AuthResponse response = AuthResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

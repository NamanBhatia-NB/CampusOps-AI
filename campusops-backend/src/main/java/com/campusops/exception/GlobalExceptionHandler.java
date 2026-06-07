package com.campusops.exception;

import com.campusops.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that distinguishes between API requests
 * (returns JSON) and web/browser requests (redirects to error pages).
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Validation errors (400) ─────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed: {}", errors);

        if (isApiRequest(request)) {
            ApiResponse<Map<String, String>> body = ApiResponse.<Map<String, String>>builder()
                    .success(false)
                    .message("Validation failed")
                    .data(errors)
                    .build();
            return ResponseEntity.badRequest().body(body);
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("errors", errors);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    // ─── Resource not found (404) ────────────────────────────────────

    @ExceptionHandler({ResourceNotFoundException.class, org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public Object handleResourceNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public Object handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    // ─── Access denied (403) ─────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    // ─── Catch-all (500) ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
        }

        ModelAndView mav = new ModelAndView("error/500");
        
        // Build stack trace string
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            trace.append(element.toString()).append("\n");
        }
        
        mav.addObject("message", ex.getMessage() + "\n\nStack Trace:\n" + trace.toString());
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Heuristic to decide whether the request originates from an API client
     * (expects JSON) or a browser (expects HTML).
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api")
                || (accept != null && accept.contains("application/json"));
    }
}

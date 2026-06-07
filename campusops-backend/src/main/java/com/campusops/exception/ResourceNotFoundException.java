package com.campusops.exception;

import lombok.Getter;

/**
 * Exception thrown when a requested resource is not found in the database.
 * Carries context about which entity, field, and value caused the miss.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String entityName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", entityName, fieldName, fieldValue));
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}

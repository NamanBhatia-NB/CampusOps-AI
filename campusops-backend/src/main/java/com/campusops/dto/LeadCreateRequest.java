package com.campusops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadCreateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    private String source;
    private String status;
    private String priority;
    private String programInterest;
    private Long ownerId;
    private String tags;
    private String city;
    private String state;
    private String country;
    private String dateOfBirth;
    private String parentName;
    private String parentPhone;
    private String qualification;
    private String nextFollowUp;
}

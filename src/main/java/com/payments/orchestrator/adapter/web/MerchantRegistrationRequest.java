package com.payments.orchestrator.adapter.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for merchant registration.
 */
@Getter
@Setter
@NoArgsConstructor
public class MerchantRegistrationRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be 2-255 characters")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 2, max = 3, message = "Country must be 2-3 character code")
    private String country;

    @Size(min = 3, max = 3, message = "Currency must be 3 character ISO code")
    private String currency;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
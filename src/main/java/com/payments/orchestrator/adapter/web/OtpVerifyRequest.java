package com.payments.orchestrator.adapter.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to verify OTP — step 2 of login.
 */
@Getter
@Setter
@NoArgsConstructor
public class OtpVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6,
            message = "OTP must be exactly 6 digits")
    private String otp;
}
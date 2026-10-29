package com.payments.orchestrator.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Email service for sending OTP emails.
 * Uses Gmail SMTP with app password.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends OTP email to merchant.
     * Async — does not block the request thread.
     */
    @Async
    public void sendOtpEmail(
            String toEmail,
            String businessName,
            String otp) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    "NexusPay — Your Login OTP");

            String htmlContent = buildOtpEmail(
                    businessName, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent. email={}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email. email={}",
                    toEmail, e);
            throw new RuntimeException(
                    "Failed to send OTP email", e);
        }
    }

    /**
     * Builds professional HTML email for OTP.
     */
    private String buildOtpEmail(
            String businessName, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                        content="width=device-width">
                </head>
                <body style="margin:0;padding:0;
                    background-color:#f8fafc;
                    font-family:-apple-system,BlinkMacSystemFont,
                    'Segoe UI',sans-serif;">
                    <div style="max-width:480px;margin:40px auto;
                        background:#ffffff;border-radius:16px;
                        overflow:hidden;
                        box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                        <!-- Header -->
                        <div style="background:#1e3a5f;
                            padding:32px;text-align:center;">
                            <div style="font-size:40px;
                                margin-bottom:8px;">💳</div>
                            <h1 style="color:#ffffff;margin:0;
                                font-size:24px;font-weight:700;">
                                NexusPay
                            </h1>
                            <p style="color:#93c5fd;margin:4px 0 0;
                                font-size:14px;">
                                Payment Orchestration Platform
                            </p>
                        </div>
                        <!-- Body -->
                        <div style="padding:40px;">
                            <p style="color:#1e293b;font-size:16px;
                                margin:0 0 8px;">
                                Hello, <strong>%s</strong>
                            </p>
                            <p style="color:#64748b;font-size:14px;
                                margin:0 0 32px;">
                                Your one-time password (OTP) for
                                NexusPay login:
                            </p>
                            <!-- OTP Box -->
                            <div style="background:#eff6ff;
                                border:2px dashed #2563eb;
                                border-radius:12px;padding:24px;
                                text-align:center;
                                margin-bottom:32px;">
                                <div style="font-size:42px;
                                    font-weight:800;
                                    letter-spacing:12px;
                                    color:#1e3a5f;">
                                    %s
                                </div>
                                <p style="color:#2563eb;
                                    font-size:13px;margin:8px 0 0;">
                                    Valid for 5 minutes
                                </p>
                            </div>
                            <p style="color:#94a3b8;font-size:13px;
                                margin:0;">
                                If you did not request this OTP,
                                please ignore this email or contact
                                support immediately.
                            </p>
                        </div>
                        <!-- Footer -->
                        <div style="background:#f8fafc;
                            padding:20px;text-align:center;
                            border-top:1px solid #e2e8f0;">
                            <p style="color:#94a3b8;font-size:12px;
                                margin:0;">
                                NexusPay — Razorpay · PayU ·
                                Stripe · Checkout.com
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(businessName, otp);
    }
}
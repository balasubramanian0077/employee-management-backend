package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.OtpEntity;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.OtpRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;

    // Generate 6-digit OTP
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Step 1: Send OTP
    public void sendOtp(String email) {
        // Check if email exists in users table
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not registered"));

        // Generate OTP and expiry time
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Save or update OTP record (overwrite old one for the same email)
        OtpEntity otpEntity = otpRepository.findByEmail(email).orElse(new OtpEntity());
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(expiryTime);
        otpRepository.save(otpEntity);

        // Send OTP via email
        emailService.sendOtpEmail(email, otp);
    }

    // Step 2: Verify OTP
    public void verifyOtp(String email, String otp) {
        OtpEntity otpEntity = otpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No OTP request found for this email"));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new one.");
        }

        if (!otpEntity.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        // OTP is valid – we don't delete it yet; we keep it for password reset.
    }

    // Step 3: Reset password (requires valid, non-expired OTP)
    public void resetPassword(String email, String newPassword) {
        OtpEntity otpEntity = otpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No OTP request found. Please request OTP first."));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password (BCrypt encoded)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete OTP record after successful reset
        otpRepository.delete(otpEntity);
    }
}
package com.ayushsingh.doc_helper.features.auth.service.service_impl;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.commons.email_handling.EmailService;
import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.auth.dto.EmailVerificationRequestDto;
import com.ayushsingh.doc_helper.features.auth.dto.VerificationResponseDto;
import com.ayushsingh.doc_helper.features.auth.dto.PasswordResetRequestDto;
import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import com.ayushsingh.doc_helper.features.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom;

    public AuthServiceImpl(FirebaseAuth firebaseAuth, UserService userService, EmailService emailService,
            RedisTemplate<String, String> redisTemplate) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }

    @Override
    @Transactional
    public UserDetailsDto signUp(UserCreateDto userCreateDto) {

        if (!userService.existsByEmail(userCreateDto.getEmail())) {
            var createRequest = new UserRecord.CreateRequest()
                    .setEmail(userCreateDto.getEmail())
                    .setPassword(userCreateDto.getPassword())
                    .setDisplayName(userCreateDto.getFirstName() + " " + userCreateDto.getLastName());
            try {
                var createdUserRecord = firebaseAuth.createUser(createRequest);
                log.info("Firebase user created ...");
                return userService.createUser(userCreateDto, createdUserRecord.getUid());
            } catch (FirebaseAuthException e) {
                log.error("Error creating user in Firebase: {}", e.getMessage());
                throw new BaseException("Error creating user in Firebase: " + e.getMessage(),
                        ExceptionCodes.FIREBASE_AUTH_EXCEPTION);
            }
        }
        throw new BaseException("User already exists with email: " + userCreateDto.getEmail(),
                ExceptionCodes.DUPLICATE_USER_FOUND);
    }

    @Async
    @Override
    public void sendEmailVerificationOtp(EmailVerificationRequestDto emailDto) {
        final var email = emailDto.getEmail();
        var otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        var key = "otp:" + email;

        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);

        var subject = "Your OTP Code";
        var htmlBody = """
                    <html>
                        <body>
                            <h2>Doc Helper Email Verification Code</h2>
                            <p>Hello,</p>
                            <p>Your OTP is: <b style="font-size:18px;">%s</b></p>
                            <p>This OTP is valid for 5 minutes.</p>
                        </body>
                    </html>
                """.formatted(otp);

        emailService.sendEmail(email, subject, htmlBody, true);
    }

    @Override
    public VerificationResponseDto verifyEmailOtp(EmailVerificationRequestDto emailDto) {
        final var email = emailDto.getEmail();
        final String otp = emailDto.getOtp();
        String key = "otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);

            var isUpdated = userService.updateUserVerifiedStatus(email, true);

            return new VerificationResponseDto(isUpdated, email);
        } else {
            throw new BaseException("Invalid or expired OTP", ExceptionCodes.INVALID_OTP);
        }
    }

    @Override
    @Async
    public void sendPasswordResetOtp(EmailVerificationRequestDto emailDto) {
        final var email = emailDto.getEmail();
        var otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        var key = "otp:reset:" + email;

        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);

        var subject = "Your Password Reset OTP Code";
        var htmlBody = """
                    <html>
                        <body>
                            <h2>Doc Helper Password Reset Code</h2>
                            <p>Hello,</p>
                            <p>Your OTP is: <b style="font-size:18px;">%s</b></p>
                            <p>This OTP is valid for 5 minutes.</p>
                        </body>
                    </html>
                """.formatted(otp);

        emailService.sendEmail(email, subject, htmlBody, true);
    }

    @Override
    @Transactional
    public VerificationResponseDto resetPassword(PasswordResetRequestDto emailDto) {
        final var email = emailDto.getEmail();
        final String otp = emailDto.getOtp();
        final String newPassword = emailDto.getNewPassword();
        String key = "otp:reset:" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            try {
                var isPasswordUpdated = userService.updateUserPassword(email, newPassword);

                if (isPasswordUpdated) {
                    var userRecord = firebaseAuth.getUserByEmail(email);
                    var updateRequest = new UserRecord.UpdateRequest(userRecord.getUid())
                            .setPassword(newPassword); // raw password
                    firebaseAuth.updateUser(updateRequest);

                    redisTemplate.delete(key);

                    return new VerificationResponseDto(true, email);
                } else {
                    throw new BaseException("Failed to update password: " + email,
                            ExceptionCodes.PASSWORD_UPDATION_EXCEPTION);
                }
            } catch (FirebaseAuthException e) {
                log.error("Error resetting password in Firebase: {}", e.getMessage());
                throw new BaseException("Error resetting password in Firebase: " + e.getMessage(),
                        ExceptionCodes.FIREBASE_AUTH_EXCEPTION);
            }
        } else {
            throw new BaseException("Invalid or expired OTP", ExceptionCodes.INVALID_OTP);
        }
    }

}

package com.ayushsingh.doc_helper.features.auth.service.service_impl;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
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

    public AuthServiceImpl(FirebaseAuth firebaseAuth, UserService userService) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
    }

    @Override
    @Transactional
    public UserDetailsDto signUp(UserCreateDto userCreateDto) {

        if (userService.existsByEmail(userCreateDto.getEmail())) {
            var createRequest = new UserRecord.CreateRequest()
                    .setEmail(userCreateDto.getEmail())
                    .setPassword(userCreateDto.getPassword())
                    .setDisplayName(userCreateDto.getFirstName() + " " + userCreateDto.getLastName());
            try {
                var createdUserRecord = firebaseAuth.createUser(createRequest);
                var userDetails = userService.createUser(userCreateDto, createdUserRecord.getUid());
                return userDetails;
            } catch (FirebaseAuthException e) {
                log.error("Error creating user in Firebase: {}", e.getMessage());
                throw new BaseException("Error creating user in Firebase: " + e.getMessage(),
                        ExceptionCodes.FIREBASE_AUTH_EXCEPTION);
            }
        }
        throw new BaseException("User already exists with email: " + userCreateDto.getEmail(),
                ExceptionCodes.DUPLICATE_USER_FOUND);
    }
}
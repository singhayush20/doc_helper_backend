package com.ayushsingh.doc_helper.features.auth.service.service_impl;

import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.DuplicateUserFoundException;
import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import com.ayushsingh.doc_helper.features.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;

    public AuthServiceImpl(FirebaseAuth firebaseAuth, UserService userService) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
    }

    public UserDetailsDto signUp(UserCreateDto userCreateDto, String firebaseToken) {
        try {
            var decodedToken = firebaseAuth.verifyIdToken(firebaseToken);
            var firebaseUid = decodedToken.getUid();

            if (userService.existsByEmailOrFirebaseUid(userCreateDto.getEmail(), firebaseUid)) {
                throw new DuplicateUserFoundException("User account already exists");
            }

            return userService.createUser(userCreateDto, decodedToken);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }
}

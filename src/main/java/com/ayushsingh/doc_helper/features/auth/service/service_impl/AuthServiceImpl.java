package com.ayushsingh.doc_helper.features.auth.service.service_impl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
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

    public UserDetailsDto signUp(UserCreateDto userCreateDto,
            String firebaseToken) {
        try {
            var decodedToken = firebaseAuth.verifyIdToken(firebaseToken);
            var firebaseUid = decodedToken.getUid();
            var email = decodedToken.getEmail();

            if (!userCreateDto.getEmail().equals(email)) {
                throw new BaseException(
                        "Email in token and email in request do not " +
                        "match", ExceptionCodes.EMAIL_MISMATCH);
            }

            if (userService.existsByEmailOrFirebaseUid(userCreateDto.getEmail(),
                    firebaseUid)) {
                throw new BaseException("User account already exists",
                        ExceptionCodes.DUPLICATE_USER_FOUND);
            }

            return userService.createUser(userCreateDto, decodedToken);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }
}

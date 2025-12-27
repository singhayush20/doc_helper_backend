package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import com.ayushsingh.doc_helper.features.user.entity.User;

public interface UserService {
    User findByFirebaseUid(String firebaseUid);

    UserDetailsDto createUser(UserCreateDto userCreateDto, String firebaseUid);

    Boolean updateUserVerifiedStatus(String email, Boolean isVerified);

    Boolean existsByEmail(String email);

    User findByEmail(String email);

    Boolean updateUserPassword(String email, String newPassword);

    UserDetailsDto getUserDetails();

}

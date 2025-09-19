package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.domain.User;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;

public interface UserService {
    User findByFirebaseUid(String firebaseUid);

    UserDetailsDto createUser(UserCreateDto userCreateDto, String firebaseUid);

    Boolean existsByEmail(String email);

    Boolean updateUserPassword(String email, String newPassword);

}

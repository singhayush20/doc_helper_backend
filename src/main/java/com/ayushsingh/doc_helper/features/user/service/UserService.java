package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import com.ayushsingh.doc_helper.features.user.domain.User;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;

import java.util.Set;

public interface UserService {
    User findByFirebaseUid(String firebaseUid);

    UserDetailsDto createUser(UserCreateDto userCreateDto);

    Boolean existsByEmailOrFirebaseUid(String email, String firebaseUid);

}

package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import com.ayushsingh.doc_helper.features.user.domain.User;

public interface UserService {
    User findByFirebaseUid(String firebaseUid);

    User findByEmail(String email);

    User createUser(User user);

    void assignRoleToUser(User user, Role role);
}

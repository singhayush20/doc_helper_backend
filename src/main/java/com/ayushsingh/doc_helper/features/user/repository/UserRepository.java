package com.ayushsingh.doc_helper.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByFirebaseUid(String firebaseUid);
    Boolean existsByEmail(String email);
    Boolean existsByFirebaseUid(String firebaseUid);
    Boolean existsByEmailOrFirebaseUid(String email, String firebaseUid);
}

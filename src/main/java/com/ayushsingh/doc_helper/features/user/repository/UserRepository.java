package com.ayushsingh.doc_helper.features.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ayushsingh.doc_helper.features.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByFirebaseUid(String firebaseUid);

    Boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.isVerified = :isVerified WHERE u.email = :email")
    void updateUserVerifiedStatus(String email, Boolean isVerified);

    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.email = :email")
    void updateUserPassword(String email, String password);
}

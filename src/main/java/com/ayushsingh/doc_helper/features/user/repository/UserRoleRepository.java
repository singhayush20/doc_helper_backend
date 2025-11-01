package com.ayushsingh.doc_helper.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole,Long> {
}

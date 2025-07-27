package com.ayushsingh.doc_helper.features.user.repository;

import com.ayushsingh.doc_helper.features.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole,Long> {
}

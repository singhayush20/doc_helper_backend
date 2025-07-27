package com.ayushsingh.doc_helper.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

}

package com.ayushsingh.doc_helper.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user.entity.Role;

import java.util.Collection;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Set<Role> findAllByNameIn(Collection<String> names);
}

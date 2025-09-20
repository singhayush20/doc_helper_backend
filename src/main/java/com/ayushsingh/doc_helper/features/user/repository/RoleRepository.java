package com.ayushsingh.doc_helper.features.user.repository;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Set<Role> findAllByNameIn(Collection<String> names);
}

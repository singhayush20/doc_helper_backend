package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public interface RoleService {
    Set<Role> findAllByNameIn(Set<String> roleNames);
}

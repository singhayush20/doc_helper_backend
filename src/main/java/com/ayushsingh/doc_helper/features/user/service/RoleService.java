package com.ayushsingh.doc_helper.features.user.service;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.features.user.entity.Role;

import java.util.Set;

@Service
public interface RoleService {
    Set<Role> findAllByNameIn(Set<String> roleNames);
}

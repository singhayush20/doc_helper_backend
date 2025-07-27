package com.ayushsingh.doc_helper.features.user.service;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import org.springframework.stereotype.Service;

@Service
public interface RoleService {
    Role findByName(String name);

    Role createRole(String name, String description);
}

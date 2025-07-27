package com.ayushsingh.doc_helper.features.user.service.service_impl;

import com.ayushsingh.doc_helper.features.user.domain.Role;
import com.ayushsingh.doc_helper.features.user.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public Role findByName(String name) {
        return null;
    }

    @Override
    public Role createRole(String name, String description) {
        return null;
    }
}

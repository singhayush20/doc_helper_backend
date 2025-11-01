package com.ayushsingh.doc_helper.features.user.service.service_impl;

import com.ayushsingh.doc_helper.features.user.entity.Role;
import com.ayushsingh.doc_helper.features.user.repository.RoleRepository;
import com.ayushsingh.doc_helper.features.user.service.RoleService;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }


    @Override
    public Set<Role> findAllByNameIn(Set<String> roleNames) {
        return roleRepository.findAllByNameIn(roleNames);
    }
}

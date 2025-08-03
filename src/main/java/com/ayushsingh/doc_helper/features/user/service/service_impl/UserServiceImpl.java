package com.ayushsingh.doc_helper.features.user.service.service_impl;

import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.FirebaseAuthenticationException;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.RolesNotFoundException;
import com.ayushsingh.doc_helper.features.user.domain.Role;
import com.ayushsingh.doc_helper.features.user.domain.User;
import com.ayushsingh.doc_helper.features.user.domain.UserRole;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import com.ayushsingh.doc_helper.features.user.repository.UserRepository;
import com.ayushsingh.doc_helper.features.user.service.RoleService;
import com.ayushsingh.doc_helper.features.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleService roleService, ModelMapper modelMapper,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByFirebaseUid(String firebaseUid) {
        return null;
    }

    @Transactional
    @Override
    public UserDetailsDto createUser(UserCreateDto userCreateDto, FirebaseToken firebaseToken) {

        var exists = existsByEmailOrFirebaseUid(userCreateDto.getEmail(), firebaseToken.getUid());
        if(exists) {
            throw new RuntimeException("User already exists");
        }

        var roles = roleService.findAllByNameIn(userCreateDto.getRoles());
        if(roles.isEmpty()) {
            throw new RolesNotFoundException("Roles not found: " + userCreateDto.getRoles());
        }
        User user = new User();
        user.setEmail(userCreateDto.getEmail());
        user.setFirstName(userCreateDto.getFirstName());
        user.setLastName(userCreateDto.getLastName());
        user.setFirebaseUid(firebaseToken.getUid());
        var userRoles = new HashSet<UserRole>();
        for (Role role : roles) {
            var userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoles.add(userRole);
        }
        user.setUserRoles(userRoles);
        user.setPassword(this.passwordEncoder.encode(userCreateDto.getPassword()));
        var savedUser = this.userRepository.save(user);
        return this.modelMapper.map(savedUser, UserDetailsDto.class);
    }

    @Override
    public Boolean existsByEmailOrFirebaseUid(String email, String firebaseUid) {
        return userRepository.existsByEmailOrFirebaseUid(email, firebaseUid);
    }
}

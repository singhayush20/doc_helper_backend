package com.ayushsingh.doc_helper.features.auth.service.service_impl;

import com.ayushsingh.doc_helper.features.auth.dto.SignUpRequestDto;
import com.ayushsingh.doc_helper.features.auth.dto.SignUpResponseDto;
import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.domain.User;
import com.ayushsingh.doc_helper.features.user.dto.UserDto;
import com.ayushsingh.doc_helper.features.user.service.RoleService;
import com.ayushsingh.doc_helper.features.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public AuthServiceImpl(FirebaseAuth firebaseAuth,
                       UserService userService,
                       RoleService roleService,
                       PasswordEncoder passwordEncoder,
                       ModelMapper modelMapper) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    public SignUpResponseDto signUp(SignUpRequestDto request) {
        return null;
    }

    private UserDto convertToUserDto(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        return new UserDto(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                roles,
                user.getCreatedAt());
    }
}

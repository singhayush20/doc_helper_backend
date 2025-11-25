package com.ayushsingh.doc_helper.features.user.service.service_impl;

import java.util.HashSet;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.commons.utility.EmailUtils;
import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import com.ayushsingh.doc_helper.features.user.entity.Role;
import com.ayushsingh.doc_helper.features.user.entity.RoleTypes;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user.entity.UserRole;
import com.ayushsingh.doc_helper.features.user.repository.UserRepository;
import com.ayushsingh.doc_helper.features.user.service.RoleService;
import com.ayushsingh.doc_helper.features.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final QuotaManagementService quotaManagementService;

    @Override
    public User findByFirebaseUid(String firebaseUid) {
        return this.userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new BaseException("User not found with firebaseUid: " + firebaseUid,
                        ExceptionCodes.USER_NOT_FOUND));
    }

    @Transactional
    @Override
    public UserDetailsDto createUser(UserCreateDto userCreateDto, String firebaseUid) {

        var roles = roleService.findAllByNameIn(Set.of(RoleTypes.USER.value()));
        if (roles.isEmpty()) {
            log.error("Roles not found: ");
            throw new BaseException("Roles not found: ", ExceptionCodes.ROLES_NOT_FOUND);
        }

        User user = new User();
        user.setEmail(EmailUtils.normalizeAndValidateEmail(userCreateDto.getEmail()));
        user.setFirstName(userCreateDto.getFirstName());
        user.setLastName(userCreateDto.getLastName());
        user.setFirebaseUid(firebaseUid);
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
        this.quotaManagementService.createDefaultQuota(savedUser.getId());
        log.info("User created ...");

        return this.modelMapper.map(savedUser, UserDetailsDto.class);
    }

    @Transactional
    @Override
    public Boolean updateUserVerifiedStatus(String email, Boolean isVerified) {
        String normalizedEmail = EmailUtils.normalizeAndValidateEmail(email);
        userRepository.updateUserVerifiedStatus(normalizedEmail, isVerified);
        return true;
    }

    @Transactional
    @Override
    public Boolean updateUserPassword(String email, String newPassword) {
        String normalizedEmail = EmailUtils.normalizeAndValidateEmail(email);
        userRepository.updateUserPassword(normalizedEmail, passwordEncoder.encode(newPassword));
        return true;
    }

    @Override
    public Boolean existsByEmail(String email) {
        String normalizedEmail = EmailUtils.normalizeAndValidateEmail(email);
        return this.userRepository.existsByEmail(normalizedEmail);
    }

    @Override
    public UserDetailsDto getUserDetails() {
        var authUser = UserContext.getCurrentUser();

        return this.modelMapper.map(authUser.getUser(), UserDetailsDto.class);
    }

    @Override
    public User findByEmail(String email) {
        String normalizedEmail = EmailUtils.normalizeAndValidateEmail(email);
        return this.userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BaseException("User not found with email: " + normalizedEmail,
                        ExceptionCodes.USER_NOT_FOUND));
    }

}

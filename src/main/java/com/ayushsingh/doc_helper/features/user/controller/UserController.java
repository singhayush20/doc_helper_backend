package com.ayushsingh.doc_helper.features.user.controller;

import com.ayushsingh.doc_helper.commons.constants.AuthConstants;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.FirebaseAuthenticationException;
import com.ayushsingh.doc_helper.features.auth.service.AuthService;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDetailsDto> signupUser(UserCreateDto userCreateDto, HttpServletRequest request) {
        var authorizationHeader = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            var firebaseToken = authorizationHeader.substring(7);
            var userDetailsDto = this.authService.signUp(userCreateDto, firebaseToken);
            return ResponseEntity.ok(userDetailsDto);
        }
        else throw new FirebaseAuthenticationException("Token not found");
    }
}

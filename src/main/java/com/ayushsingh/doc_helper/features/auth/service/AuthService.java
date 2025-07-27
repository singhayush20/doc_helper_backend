package com.ayushsingh.doc_helper.features.auth.service;
import com.ayushsingh.doc_helper.features.user.dto.UserCreateDto;
import com.ayushsingh.doc_helper.features.user.dto.UserDetailsDto;

public interface AuthService {
    UserDetailsDto signUp(UserCreateDto signUpRequestDto);
}

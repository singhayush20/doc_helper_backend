package com.ayushsingh.doc_helper.features.auth.service;
import com.ayushsingh.doc_helper.features.auth.dto.SignUpRequestDto;
import com.ayushsingh.doc_helper.features.auth.dto.SignUpResponseDto;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

public interface AuthService {
    SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto);
}

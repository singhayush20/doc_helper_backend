package com.ayushsingh.doc_helper.features.user.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String publicId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<String> roles;
    private Instant createdAt;
}

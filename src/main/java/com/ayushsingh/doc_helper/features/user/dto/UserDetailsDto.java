package com.ayushsingh.doc_helper.features.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDto {
    private String publicId;
    private String firstName;
    private String lastName;
    private String email;
    private String firebaseUid;
}

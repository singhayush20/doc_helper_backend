package com.ayushsingh.doc_helper.features.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsDto that = (UserDetailsDto) o;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }
}

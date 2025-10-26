package com.ayushsingh.doc_helper.commons.utility;

import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Trim whitespace and convert to lowercase
        return email.trim().toLowerCase();
    }

    public static boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email regex pattern
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    public static String normalizeAndValidateEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        
        if (!isValidEmailFormat(normalizedEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        return normalizedEmail;
    }
}

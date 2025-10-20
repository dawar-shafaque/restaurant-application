package com.restaurant.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordEncoderUtil {

    private PasswordEncoderUtil(){}

     // Encode a plaintext password.
    public static String encodePassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

     // Match a plaintext password against an encoded password.
    public static boolean matchPassword(String plaintext, String encoded) {
        return BCrypt.checkpw(plaintext, encoded);
    }
}
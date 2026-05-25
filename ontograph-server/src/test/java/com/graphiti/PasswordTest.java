package com.ontograph;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        boolean matches = encoder.matches("admin123", hash);
        System.out.println("Password 'admin123' matches hash: " + matches);
        
        // 生成一个新的正确哈希
        String newHash = encoder.encode("admin123");
        System.out.println("New hash for 'admin123': " + newHash);
    }
}

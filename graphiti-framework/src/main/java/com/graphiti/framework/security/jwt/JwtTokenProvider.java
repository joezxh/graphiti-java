package com.graphiti.framework.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

/**
 * JWT Token 提供器
 * 生成、解析、验证 JWT Token
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    @Value("${graphiti.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${graphiti.security.jwt.expiration:86400}")
    private int jwtExpirationInSeconds;
    
    /**
     * 获取签名 Key
     * @return Key
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * 生成 JWT Token
     * @param authentication Authentication
     * @return String
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() +
                               jwtExpirationInSeconds * 1000L);

        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 从 Token 中获取用户名
     * @param token JWT Token
     * @return String
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @return boolean
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token", e);
            return false;
        }
    }
}

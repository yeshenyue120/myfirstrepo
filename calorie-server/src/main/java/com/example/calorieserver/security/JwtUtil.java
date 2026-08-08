package com.example.calorieserver.security;

import com.example.calorieserver.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 生成与解析。subject 存 userId，claim 带角色。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // 生成 token（subject=userId）
    public String generateToken(Long userId, User.Role role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role != null ? role.name() : "USER")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    // 解析 token → userId（非法/过期抛异常）
    public Long parseUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    // 解析 token → 角色
    public String parseRole(String token) {
        return parse(token).get("role", String.class);
    }

    // 校验 token 是否合法
    public boolean isValid(String token) {
        try {
            parseUserId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

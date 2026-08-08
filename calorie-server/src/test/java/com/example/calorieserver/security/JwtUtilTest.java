package com.example.calorieserver.security;

import com.example.calorieserver.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具测试：签发 / 解析 / 过期 / 非法 token。
 */
class JwtUtilTest {

    /** 测试用 secret（64 字符，满足 HS512 密钥长度要求） */
    private static final String SECRET =
            "test-secret-key-0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void generateAndParse_roundtrip() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600_000);
        String token = jwtUtil.generateToken(42L, User.Role.USER);

        assertEquals(42L, jwtUtil.parseUserId(token));
        assertEquals("USER", jwtUtil.parseRole(token));
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void generateToken_nullRole_defaultsToUser() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600_000);
        String token = jwtUtil.generateToken(7L, null);
        assertEquals("USER", jwtUtil.parseRole(token));
    }

    @Test
    void parseRole_adminRole() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600_000);
        String token = jwtUtil.generateToken(1L, User.Role.ADMIN);
        assertEquals("ADMIN", jwtUtil.parseRole(token));
    }

    @Test
    void expiredToken_isInvalid() {
        // expirationMs 为负 → 签发即过期
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1000);
        String token = jwtUtil.generateToken(1L, User.Role.USER);

        assertFalse(jwtUtil.isValid(token));
        assertThrows(Exception.class, () -> jwtUtil.parseUserId(token));
    }

    @Test
    void invalidToken_isInvalid() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600_000);
        assertFalse(jwtUtil.isValid("not-a-jwt"));
        assertThrows(Exception.class, () -> jwtUtil.parseUserId("not-a-jwt"));
    }

    @Test
    void tamperedToken_isInvalid() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600_000);
        String token = jwtUtil.generateToken(1L, User.Role.USER);
        assertFalse(jwtUtil.isValid(token + "x"));
    }
}

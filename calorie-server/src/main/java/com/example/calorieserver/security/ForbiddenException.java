package com.example.calorieserver.security;

/**
 * 403 无权限异常（归属校验失败等）。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

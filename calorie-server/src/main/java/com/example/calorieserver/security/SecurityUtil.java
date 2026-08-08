package com.example.calorieserver.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：读取当前登录用户 userId。
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    // 当前登录用户 id（未登录返回 null）
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            return null;
        }
        try {
            return Long.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 校验记录归属：不属于当前登录者则抛 403
    public static void requireOwner(Long recordOwnerId) {
        Long current = currentUserId();
        if (current == null) {
            throw new ForbiddenException("未登录或登录已过期");
        }
        if (!current.equals(recordOwnerId)) {
            throw new ForbiddenException("无权操作该记录");
        }
    }

}

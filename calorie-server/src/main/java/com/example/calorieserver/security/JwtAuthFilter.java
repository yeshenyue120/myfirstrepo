package com.example.calorieserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer，合法则写入 SecurityContext。
 * 无 token / token 无效不在此拒绝，交给 SecurityConfig 对受保护路径统一返回 401。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtUtil.parseUserId(token);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String role = jwtUtil.parseRole(token);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // token 无效：不设置认证
            }
        }
        filterChain.doFilter(request, response);
    }
}

package com.example.calorieserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * 归属校验拦截器：请求中携带的目标 userId（@PathVariable userId 或 ?userId=）必须等于当前登录者，
 * 否则 403。record 级接口（路径/参数里没有 userId，如 PUT/DELETE /records/{recordId}）不做拦截，
 * 由对应 service 层校验记录归属。
 */
@Component
public class OwnershipGuardInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long current = SecurityUtil.currentUserId();
        if (current == null) {
            return reject(response, 401, "未登录或登录已过期");
        }

        // 目标 userId：优先取 Spring MVC 解析出的路径变量 userId
        String pathUserId = null;
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars instanceof Map<?, ?> map) {
            Object v = map.get("userId");
            if (v != null) {
                pathUserId = v.toString();
            }
        }
        // 部分接口用 query 参数传 userId（收藏、自定义食物等）
        if (pathUserId == null) {
            pathUserId = request.getParameter("userId");
        }

        // 没有 userId → 非用户级接口或 record 级接口，交给 service 校验
        if (pathUserId == null) {
            return true;
        }

        try {
            Long target = Long.valueOf(pathUserId);
            if (!current.equals(target)) {
                return reject(response, 403, "无权访问该用户的数据");
            }
        } catch (NumberFormatException e) {
            return reject(response, 400, "userId 参数格式错误");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("message", message));
        return false;
    }
}

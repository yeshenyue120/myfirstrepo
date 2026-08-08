package com.example.calorieserver.config;

import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.security.ForbiddenException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 全局异常处理：
 * - 业务异常（RuntimeException）→ 400 + 中文 message
 * - 找不到资源（NoSuchElementException）→ 404
 * - 参数校验失败 → 400 + 具体字段错误
 * - 兜底 → 500（不泄露内部堆栈）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常（用户输入/业务状态导致的已知错误）→ 400 + 中文提示
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "请求失败" : e.getMessage()));
    }

    // 其他运行时异常（系统故障）→ 500，打完整日志，不向用户泄露内部消息
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        log.error("未处理的运行时异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "服务器开小差了，请稍后再试"));
    }

    // 归属校验失败 → 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "数据不存在或已被删除"));
    }

    // @RequestBody DTO 校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    // 查询参数校验失败
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    // @RequestParam/@PathVariable 校验失败（@Validated + @Positive/@Min 等）
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    // 请求体格式错误（JSON 解析失败等）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "请求数据格式错误"));
    }

    // 兜底
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "服务器开小差了，请稍后再试"));
    }
}

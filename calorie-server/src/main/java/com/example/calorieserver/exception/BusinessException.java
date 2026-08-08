package com.example.calorieserver.exception;

/**
 * 业务异常：用户输入或业务状态导致请求无法完成（HTTP 400）。
 * 与系统运行时异常区分开，避免真正的系统故障（NPE、DB 连接失败等）被误报为 400 且不落日志。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

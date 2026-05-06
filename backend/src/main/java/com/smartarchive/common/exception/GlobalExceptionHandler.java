package com.smartarchive.common.exception;

import com.smartarchive.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static String safeUserMessage(Throwable ex, String fallbackIfBlank) {
        Throwable cur = ex;
        for (int i = 0; i < 8 && cur != null; i++) {
            String m = cur.getMessage();
            if (StringUtils.hasText(m)) {
                return m.trim();
            }
            cur = cur.getCause();
        }
        if (StringUtils.hasText(fallbackIfBlank)) {
            return fallbackIfBlank.trim();
        }
        return ex.getClass().getSimpleName();
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.failure(4001, safeUserMessage(ex, "Business rule violation"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Validation failed");
        return ApiResponse.failure(4000, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableBody(HttpMessageNotReadableException ex) {
        Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String detail = safeUserMessage(root, "请求体 JSON 格式错误或字段类型不匹配");
        log.warn("Unreadable HTTP message: {}", detail);
        return ApiResponse.failure(4000, detail);
    }

    /**
     * NPE 的 getMessage() 多为 null，通用 handler 只会回传类名「NullPointerException」，无法定位。
     * 单独处理并带上首帧栈，便于对照修复。
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResponse<Void> handleNullPointer(NullPointerException ex) {
        log.error("NullPointerException", ex);
        StackTraceElement[] st = ex.getStackTrace();
        String where = (st != null && st.length > 0) ? st[0].toString() : "unknown";
        return ApiResponse.failure(5000, "空指针 @ " + where);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.failure(5000, safeUserMessage(ex, null));
    }
}

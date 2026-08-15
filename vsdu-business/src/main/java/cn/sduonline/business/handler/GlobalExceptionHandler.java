package cn.sduonline.business.handler;


import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public <T> ResponseEntity<Result<T>> handleHttpRequestNotSupportedException(
            HttpRequestMethodNotSupportedException he,
            HttpServletRequest request
    ) {
        log.warn(
                "错误的请求方法 | 路径：{} {} | ip：{}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );

        return ResponseEntity
                .status(BizCode.BAD_REQUEST.getHttpStatusCode())
                .body(
                        Result.error(
                                BizCode.BAD_REQUEST,
                                String.format("路径%s不支持请求方法 %s，支持的请求方法：%s",
                                        request.getRequestURI(),
                                        he.getMethod(),
                                        he.getSupportedHttpMethods()
                                )
                        )
                );
    }

    /**
     * 处理枚举类编号无效异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public <T> ResponseEntity<Result<T>> handle(
            IllegalArgumentException ie,
            HttpServletRequest request
    ) {
        log.warn(
                "无效的枚举类编号 | 信息：{} | 路径：{} {}",
                ie.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                ie
        );
        return ResponseEntity
                .status(BizCode.BAD_REQUEST.getHttpStatusCode())
                .body(
                        Result.error(BizCode.BAD_REQUEST, ie.getMessage())
                );
    }

    /**
     * 处理请求体 json 解析失败异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public <T> ResponseEntity<Result<T>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException he,
            HttpServletRequest request
    ) {
        log.warn(
                "请求体不可解析 | 信息：{} | 路径：{} {}",
                he.getMessage(),
                request.getMethod(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(BizCode.BAD_REQUEST.getHttpStatusCode())
                .body(
                        Result.error(BizCode.BAD_REQUEST, "请求体格式错误或不可解析")
                );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public <T> ResponseEntity<Result<T>> handleMaxUploadSizeExceededException(
            HttpServletRequest request
    ) {
        log.warn(
                "上传超限 | 路径：{} {}",
                request.getMethod(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(BizCode.UPLOAD_SIZE_TOO_LARGE.getHttpStatusCode())
                .body(
                        Result.error(BizCode.UPLOAD_SIZE_TOO_LARGE)
                );
    }

    /**
     * 请求体字段校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public  ResponseEntity<Result<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.toString(error.getDefaultMessage(), "参数校验失败"),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        log.warn(
                "请求体字段不合规范 | 信息：{} | 路径：{} {}",
                fieldErrors,
                request.getMethod(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(BizCode.BAD_REQUEST.getHttpStatusCode())
                .body(
                        Result.error(BizCode.BAD_REQUEST, fieldErrors, "请求体字段格式错误")
                );
    }

    /**
     * 方法参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> {
                            String propertyPath = violation.getPropertyPath().toString();
                            int lastDotIndex = propertyPath.lastIndexOf('.');
                            return propertyPath.substring(lastDotIndex + 1);
                        },
                        violation -> Objects.toString(violation.getMessage(), "参数校验失败"),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        log.warn(
                "请求参数不合规范 | 信息：{} | 路径：{} {}",
                fieldErrors,
                request.getMethod(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(BizCode.BAD_REQUEST.getHttpStatusCode())
                .body(
                        Result.error(BizCode.BAD_REQUEST, fieldErrors, "请求参数格式错误")
                );
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BizException.class)
    public <T> ResponseEntity<Result<T>> handleBizException(BizException be) {
        log.warn("业务异常 | 业务码：{} | 信息：{}", be.getBizCode().getCode(), be.getBizCode().getMsg());
        return ResponseEntity
                .status(be.getBizCode().getHttpStatusCode())
                .body(
                        Result.error(be.getBizCode(), be.getMessage())
                );
    }

    /**
     * 处理系统异常（500）
     */
    @ExceptionHandler(Exception.class)
    public <T> ResponseEntity<Result<T>> handleException(
            Exception e,
            HttpServletRequest request
    ) {

        log.error(
                "服务器异常 | 路径：{} {} | queryString：{}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                e
        );

        return ResponseEntity
                .status(BizCode.INTERNAL_SERVER_ERROR.getHttpStatusCode())
                .body(
                        Result.error(BizCode.INTERNAL_SERVER_ERROR)
                );
    }

}

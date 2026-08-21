package com.ljl.agent.exception;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.common.Result;
import com.ljl.agent.redis.BlacklistUnavailableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 Controller 和 Service 抛出的异常统一转换成 Result JSON。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException exception
    ) {
        LOGGER.warn(
                "business request rejected: code={}, message={}",
                exception.getCode(),
                exception.getMessage()
        );
        return ResponseEntity
                .status(resolveBusinessStatus(exception.getCode()))
                .body(Result.failure(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>>
            handleMethodArgumentNotValidException(
                    MethodArgumentNotValidException exception
            ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted((left, right) -> Integer.compare(
                        validationPriority(left),
                        validationPriority(right)
                ))
                .forEach(fieldError -> errors.putIfAbsent(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ));

        return validationFailure(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Map<String, String>>>
            handleConstraintViolationException(
                    ConstraintViolationException exception
            ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation
                : exception.getConstraintViolations()) {
            String field = lastPathSegment(
                    violation.getPropertyPath().toString()
            );
            errors.putIfAbsent(field, violation.getMessage());
        }

        return validationFailure(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>>
            handleMethodArgumentTypeMismatchException(
                    MethodArgumentTypeMismatchException exception
            ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.PARAM_INVALID.getCode(),
                        "参数 " + exception.getName() + " 格式不正确"
                )
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>>
            handleMissingServletRequestParameterException(
                    MissingServletRequestParameterException exception
            ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.PARAM_INVALID.getCode(),
                        "缺少必要参数：" + exception.getParameterName()
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>>
            handleHttpMessageNotReadableException(
                    HttpMessageNotReadableException exception
            ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.PARAM_INVALID.getCode(),
                        "请求体格式不正确或缺少必要内容"
                )
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                Result.failure(ErrorCode.DOCUMENT_FILE_TOO_LARGE)
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestPart(
            MissingServletRequestPartException exception
    ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.DOCUMENT_FILE_INVALID.getCode(),
                        "缺少必要文件参数：" + exception.getRequestPartName()
                )
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> handleMultipartException(
            MultipartException exception
    ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.DOCUMENT_FILE_INVALID.getCode(),
                        "Multipart 上传请求格式不正确"
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(
            Exception exception
    ) {
        LOGGER.error("未处理的系统异常", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Result.failure(ErrorCode.SYSTEM_ERROR)
        );
    }

    @ExceptionHandler(BlacklistUnavailableException.class)
    public ResponseEntity<Result<Void>> handleBlacklistUnavailable(
            BlacklistUnavailableException exception
    ) {
        LOGGER.error(
                "JWT blacklist write failed: exceptionType={}",
                exception.getCause() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getCause().getClass().getSimpleName()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Result.failure(
                        ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
                )
        );
    }

    private ResponseEntity<Result<Map<String, String>>> validationFailure(
            Map<String, String> errors
    ) {
        return ResponseEntity.badRequest().body(
                Result.failure(
                        ErrorCode.PARAM_INVALID.getCode(),
                        "参数校验失败",
                        errors
                )
        );
    }

    private HttpStatus resolveBusinessStatus(int code) {
        if (code >= 40100 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 40400 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 40900 && code < 41000) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 41300 && code < 41400) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (code >= 42200 && code < 42300) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (code >= 42900 && code < 43000) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code >= 50300 && code < 50400) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (code >= 50000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private int validationPriority(FieldError fieldError) {
        return switch (fieldError.getCode()) {
            case "NotNull", "NotBlank" -> 0;
            case "Size" -> 1;
            default -> 2;
        };
    }
}

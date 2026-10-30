package com.backend.bangbangbwa.common.exception;

import com.backend.bangbangbwa.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException e
    ) {

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ApiResponse.fail(
                                errorCode.getStatus(),
                                errorCode.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e
    ) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();

        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(400, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e
    ) {
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.fail(
                                ErrorCode.IMAGE_SIZE_EXCEEDED.getStatus(),
                                ErrorCode.IMAGE_SIZE_EXCEEDED.getMessage()
                        )
                );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestPart(
            MissingServletRequestPartException e
    ) {
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.fail(
                                ErrorCode.INVALID_REQUEST.getStatus(),
                                ErrorCode.INVALID_REQUEST.getMessage()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e
    ) {
        return ResponseEntity.internalServerError()
                .body(
                        ApiResponse.fail(
                                500,
                                "서버 내부 오류가 발생했습니다."
                        )
                );
    }

}

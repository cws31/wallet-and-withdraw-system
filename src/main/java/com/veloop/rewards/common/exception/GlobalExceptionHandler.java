package com.veloop.rewards.common.exception;

import com.veloop.rewards.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authorization.AuthorizationDeniedException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(AuthenticationFailedException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationFailed(
                        AuthenticationFailedException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(
                                                ErrorResponse.of(
                                                                ex.getMessage(),
                                                                request.getRequestURI()));
        }

        @ExceptionHandler(WalletNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleWalletNotFound(
                        WalletNotFoundException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(
                                                ErrorResponse.of(
                                                                ex.getMessage(),
                                                                request.getRequestURI()));
        }

        @ExceptionHandler(WalletAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleWalletAlreadyExists(
                        WalletAlreadyExistsException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(
                                                ErrorResponse.of(
                                                                ex.getMessage(),
                                                                request.getRequestURI()));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(
                        BusinessException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(
                                                ErrorResponse.of(
                                                                ex.getMessage(),
                                                                request.getRequestURI()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.putIfAbsent(
                                                error.getField(),
                                                error.getDefaultMessage()));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(
                                                ErrorResponse.validation(
                                                                "Validation failed",
                                                                request.getRequestURI(),
                                                                errors));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getConstraintViolations()
                                .forEach(violation -> errors.put(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(
                                                ErrorResponse.validation(
                                                                "Validation failed",
                                                                request.getRequestURI(),
                                                                errors));
        }

        @ExceptionHandler(AuthorizationDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
                        AuthorizationDeniedException ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(
                                                ErrorResponse.of(
                                                                "Access denied",
                                                                request.getRequestURI()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnexpectedException(
                        Exception ex,
                        HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(
                                                ErrorResponse.of(
                                                                "An unexpected error occurred",
                                                                request.getRequestURI()));
        }
}
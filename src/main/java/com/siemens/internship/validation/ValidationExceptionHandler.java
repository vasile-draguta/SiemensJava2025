package com.siemens.internship.validation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ValidationExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<List<Map<String, String>>> handleValidationExceptions(
                        MethodArgumentNotValidException ex) {

                List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> Map.of(
                                                "field", error.getField(),
                                                "message", error.getDefaultMessage()))
                                .toList();

                return ResponseEntity.badRequest().body(errors);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<List<Map<String, String>>> handleIllegalArgumentException(
                        IllegalArgumentException illegalArgumentException) {

                List<Map<String, String>> errors = List.of(
                                Map.of(
                                                "field", "id",
                                                "message", illegalArgumentException.getMessage()));

                return ResponseEntity.badRequest().body(errors);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<List<Map<String, String>>> handleRuntimeException(
                        RuntimeException runtimeException) {

                List<Map<String, String>> errors = List.of(
                                Map.of(
                                                "error", "Internal Server Error",
                                                "message", runtimeException.getMessage()));

                return ResponseEntity.badRequest().body(errors);
        }
}
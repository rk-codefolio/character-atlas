package dev.characteratlas.archipelago.shared.presentation;

import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class UseCaseErrors {
    @ExceptionHandler(UseCaseException.class)
    ResponseEntity<Void> translate(UseCaseException error) {
        return ResponseEntity.status(switch (error.kind()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
        }).build();
    }
}

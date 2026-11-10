package dev.characteratlas.archipelago.shared.presentation;

import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 処理のユースケースのErrorsを表す。 */
@RestControllerAdvice
public class UseCaseErrors {
  /** translateを扱う。 */
  @ExceptionHandler(UseCaseException.class)
  ResponseEntity<Void> translate(UseCaseException error) {
    return ResponseEntity.status(
            switch (error.kind()) {
              case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
              case NOT_FOUND -> HttpStatus.NOT_FOUND;
            })
        .build();
  }
}

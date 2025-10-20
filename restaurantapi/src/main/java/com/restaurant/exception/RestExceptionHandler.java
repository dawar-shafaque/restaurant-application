package com.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(BadRequestException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(IllegalArgumentException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ExceptionErrorResponse> handleMissingParam(MissingServletRequestParameterException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage("Required parameter '" + exception.getParameterName() +
                "' of type '" + exception.getParameterType() + "' is missing");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(MethodArgumentTypeMismatchException exception) {
        // Extract relevant information from the original exception
        String parameterName = exception.getName();
        String requiredType = Optional.ofNullable(exception.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");

        String providedValue = Optional.ofNullable(exception.getValue())
                .map(Object::toString)
                .orElse("null");


        TypeMismatchException customException = new TypeMismatchException(
                parameterName,
                requiredType,
                providedValue
        );
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(customException.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(ForbiddenException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(NotFoundException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(UnAuthorizedException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(ConflictException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(InternalServerErrorException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(Exception exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
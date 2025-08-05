package com.fspl.medica_healthcare.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.naming.InvalidNameException;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
//	@ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getFieldErrors().forEach(error ->
//                errors.put(error.getField(), error.getDefaultMessage()));
//        return ResponseEntity.badRequest().body(errors);
//    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<?> RecordNotFoundException(RecordNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error",ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errors);
    }


    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<String> handleInvalidRoleException(InvalidRoleException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }


    @ExceptionHandler(InvalidUserAddException.class)
    public ResponseEntity<String> handleInvalidUserAdding(InvalidUserAddException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(AdminDeleteException.class)
    public ResponseEntity<String> adminDeleteException(AdminDeleteException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidUpdateUsernameException.class)
    public ResponseEntity<String> invalidUserAddException(InvalidUpdateUsernameException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        // Return a custom error message with HTTP status 401 (Unauthorized)
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LetterHeadMissingException.class)
    public ResponseEntity<String> handleLetterHeadMissing(LetterHeadMissingException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> userNotFoundException(UserNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Object> handleResourceConflictException(ResourceConflictException ex) {
        // Return a custom error message and 409 Conflict status
        return new ResponseEntity<>(new ErrorDetails("Patient already exists", ex.getMessage()), HttpStatus.CONFLICT);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleGenericException(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body("{\"error\": \"An unexpected error occurred\"}");
//    }

    @ExceptionHandler(PatientAlreadyExistsException.class)
    public ResponseEntity<?> patientAlreadyExistsException(PatientAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<String> handleFileSizeLimit(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File is too large. Please upload a smaller file.");
    }


    @ExceptionHandler(InvalidGenderException.class)
    public ResponseEntity<Map<String, String>> handleInvalidGenderException(InvalidGenderException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid Input");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler({ConversionFailedException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<String> handleEnumConversionException(Exception ex) {
        return new ResponseEntity<>("Your data is not correct. Please provide a valid data.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidNameException.class)
    public ResponseEntity<?> InvalidNameException(InvalidNameException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<?> InvalidFileTypeException(InvalidFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

    @ExceptionHandler(RecordAlreadyExitException.class)
    public ResponseEntity<?> RecordAlreadyExitException(RecordAlreadyExitException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error",ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgumentException(IllegalArgumentException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

    @ExceptionHandler(IllegalAccessException.class)
    public ResponseEntity<?> illegalAccessException(IllegalAccessException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\": \"" + ex.getMessage() + "\"}");
    }

 @ExceptionHandler({InvalidFormatException.class, DateTimeParseException.class})
    public ResponseEntity<Map<String, String>> handleInvalidDateFormat( DateTimeParseException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("Error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> HttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Map<String, String> response = new HashMap<>();
        Throwable cause = ex.getRootCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            response.put(capitalizeFieldName(fieldName), capitalizeFieldName(fieldName)+" is not valid");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mismatchedInputException) {
            String field = mismatchedInputException.getPath().stream().map(ref -> ref.getFieldName()).filter(Objects::nonNull).collect(Collectors.joining("."));
            if (mismatchedInputException.getMessage().contains("from Object value (token `JsonToken.START_OBJECT`)")) {
                response.put(capitalizeFieldName(field), capitalizeFieldName(field) + " must be plain text, not an object");
            }
            else {
                int chatAt = field.indexOf(".");
                field.replace(".", " ");
                field = field.substring(0, 1).toUpperCase() + field.substring(1, chatAt).toLowerCase() + field.substring(chatAt + 1, chatAt + 2).toUpperCase() + field.substring(chatAt + 2, field.length());
                response.put(capitalizeFieldName(field), "Invalid data format");
            }
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        } else {
            response.put("Error", "Invalid date format " + ex.getRootCause().getMessage() +
                    ". For Date of Birth, it should be 'yyyy-MM-dd'. For Appointment Date and Time, it should be 'yyyy-MM-dd hh:mm a'. Please provide valid dates.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
    private String capitalizeFieldName(String field) {
        if (field == null || field.isEmpty()) return field;
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }






    // Error details response structure
    public static class ErrorDetails {
        private String error;
        private String message;

        public ErrorDetails(String error, String message) {
            this.error = error;
            this.message = message;
        }}
}




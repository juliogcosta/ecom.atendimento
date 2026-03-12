package com.ecom.atendimento.adapter.exception;

import com.ecom.atendimento.adapter.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções para padronizar respostas de erro da API.
 *
 * Captura exceções lançadas em qualquer controller e retorna ErrorResponse padronizado.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Trata exceções de validação de argumentos de método (@Valid).
     * Ex: campos obrigatórios faltando, formato inválido, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Erro de validação na requisição: {}", request.getRequestURI());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldError)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Erro de validação nos campos da requisição")
                .path(request.getRequestURI())
                .exceptionType(ex.getClass().getSimpleName())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata exceções de constraint violation do Bean Validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("Constraint violation na requisição: {}", request.getRequestURI());

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Violação de restrições de validação")
                .path(request.getRequestURI())
                .exceptionType(ex.getClass().getSimpleName())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata exceções de tipo de argumento inválido (ex: UUID inválido em path param).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        log.warn("Tipo de argumento inválido na requisição: {} - parâmetro: {}",
                request.getRequestURI(), ex.getName());

        String message = String.format("Parâmetro '%s' deve ser do tipo %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconhecido");

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        errorResponse.setExceptionType(ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata exceções de elemento não encontrado (NoSuchElementException).
     * Usado quando agregado não existe no Event Store.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request) {

        log.warn("Elemento não encontrado na requisição: {} - mensagem: {}",
                request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Recurso não encontrado",
                request.getRequestURI()
        );
        errorResponse.setExceptionType(ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Trata exceções de regras de negócio (IllegalStateException, IllegalArgumentException).
     * Usadas pelo agregado para validar transições de estado inválidas.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            RuntimeException ex,
            HttpServletRequest request) {

        log.warn("Violação de regra de negócio na requisição: {} - mensagem: {}",
                request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage() != null ? ex.getMessage() : "Operação não permitida",
                request.getRequestURI()
        );
        errorResponse.setExceptionType(ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Trata exceções genéricas não capturadas pelos handlers específicos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Erro interno no servidor na requisição: {} - mensagem: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Erro interno no servidor. Contate o suporte se o problema persistir.",
                request.getRequestURI()
        );
        errorResponse.setExceptionType(ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Helper para construir FieldError a partir de Spring's FieldError.
     */
    private ErrorResponse.FieldError buildFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}

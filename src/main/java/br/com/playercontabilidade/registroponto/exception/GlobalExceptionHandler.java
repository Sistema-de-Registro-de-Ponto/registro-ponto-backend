package br.com.playercontabilidade.registroponto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ColaboratorNotFoundException.class)
    public ProblemDetail handleColaboratorNotFound(ColaboratorNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Colaborador não encontrado");
        return problem;
    }

    @ExceptionHandler(PlannedActivityNotFoundException.class)
    public ProblemDetail handlePlannedActivityNotFound(PlannedActivityNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Atividade planejada não encontrada");
        return problem;
    }

    @ExceptionHandler(JourneyPlannedActivityNotFoundException.class)
    public ProblemDetail handleJourneyPlannedActivityNotFound(JourneyPlannedActivityNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Atividade da jornada não encontrada");
        return problem;
    }

    @ExceptionHandler(JourneyAlreadyInProgressException.class)
    public ProblemDetail handleJourneyAlreadyInProgress(JourneyAlreadyInProgressException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Jornada em andamento");
        return problem;
    }

    @ExceptionHandler(JourneyNotFoundException.class)
    public ProblemDetail handleJourneyNotFound(JourneyNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Jornada não encontrada");
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        problem.setTitle("Não autorizado");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos");
        problem.setTitle("Requisição inválida");
        problem.setProperty("errors", errors);
        return problem;
    }
}

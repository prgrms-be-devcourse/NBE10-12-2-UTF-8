package com.back.global.globalExceptionHandler;

import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "404-1",
                        "해당 데이터가 존재하지 않습니다."
                ),
                NOT_FOUND
        );
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RsData<Void>> handle(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString().split("\\.", 2)[1];
                    String[] messageTemplateBits = violation.getMessageTemplate()
                            .split("\\.");
                    String code = messageTemplateBits[messageTemplateBits.length - 2];
                    String _message = violation.getMessage();

                    return "%s-%s-%s".formatted(field, code, _message);
                })
                .sorted(Comparator.comparing(String::toString))
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        message
                ),
                BAD_REQUEST
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        msg
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException ex) {
        // Jackson이 @JsonCreator에서 던진 예외(예: enum 화이트리스트 검증 실패)를
        // InvalidFormatException 등으로 감싸버리므로, cause 체인에서 ServiceException을 찾아
        // 원래 메시지를 그대로 내려준다. 못 찾으면 기존 범용 메시지로 fallback.
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ServiceException se) {
                RsData<Void> rsData = se.getRsData();
                return new ResponseEntity<>(rsData, BAD_REQUEST);
            }
            cause = cause.getCause();
        }
        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        "요청 본문이 올바르지 않습니다."
                ),
                BAD_REQUEST
        );
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RsData<Void>> handle(MissingRequestHeaderException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        "%s-%s-%s".formatted(
                                ex.getHeaderName(),
                                "NotBlank",
                                ex.getLocalizedMessage()
                        )
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(ServiceException.class)
    public RsData<Void> handle(ServiceException ex, HttpServletResponse response) {
        RsData<Void> rsData = ex.getRsData();

        response.setStatus(rsData.statusCode());

        return rsData;
    }
    // Jackson(HttpMessageNotReadableException)뿐 아니라, JPA/Hibernate가 AttributeConverter
    // 내부에서 던진 ServiceException을 자체 예외(예: DataAccessException 계열)로 감싸는 경우까지
    // 대비하는 범용 fallback. 어떤 종류로 감싸이든 cause 체인에서 ServiceException을 찾아
    // 원래 의도한 응답으로 복원하고, 못 찾으면 진짜 예상치 못한 버그이므로 500-1로 응답한다.
    // 단, ServiceException 자체와 위에서 이미 구체적으로 처리한 예외 타입들은 더 특이적인
    // 핸들러가 우선 매칭되므로 이 메서드까지 내려오지 않는다.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RsData<Void>> handle(RuntimeException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ServiceException se) {
                RsData<Void> rsData = se.getRsData();
                return new ResponseEntity<>(rsData, org.springframework.http.HttpStatusCode.valueOf(rsData.statusCode()));
            }
            cause = cause.getCause();
        }

        log.error("[GlobalExceptionHandler] 예상치 못한 예외 발생", ex);
        return new ResponseEntity<>(
                new RsData<>(
                        "500-1",
                        "서버 오류가 발생했습니다."
                ),
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
package dev.di.chatserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 채팅방이 없을 때 → 404
    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ProblemDetail handleChatRoomNotFound(
            ChatRoomNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("채팅방 조회 실패");
        problem.setProperty("code", ErrorCode.CHAT_ROOM_NOT_FOUND.name());

        return problem;
    }

    // @Valid 검사에 실패했을 때 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception) {

        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "입력값을 확인해 주세요."
        );

        problem.setTitle("입력값 오류");
        problem.setProperty("code", ErrorCode.INVALID_INPUT.name());
        problem.setProperty("errors", errors);

        return problem;
    }
    @ExceptionHandler(SelfChatNotAllowedException.class)
    public ProblemDetail handleSelfChatNotAllowed(
            SelfChatNotAllowedException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(422),
                exception.getMessage()
        );

        problem.setTitle("채팅방 생성 불가");
        problem.setProperty("code", ErrorCode.SELF_CHAT_NOT_ALLOWED.name());

        return problem;
    }
    @ExceptionHandler(ChatAccessDeniedException.class)
    public ProblemDetail handleChatAccessDenied(ChatAccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("채팅방 접근 불가");
        problem.setProperty("code", ErrorCode.CHAT_ACCESS_DENIED.name());
        return problem;
    }

    // 잘못된 JSON, 누락된 쿼리 값, 숫자로 변환할 수 없는 번호도 같은 형식으로 응답합니다.
    @ExceptionHandler({
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleInvalidRequest(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "JSON 형식과 요청 파라미터를 확인해 주세요.");
        problem.setTitle("입력값 오류");
        problem.setProperty("code", ErrorCode.INVALID_INPUT.name());
        return problem;
    }
    @ExceptionHandler(ChatMessageNotFoundException.class)
    public ProblemDetail handleMessageNotFound(ChatMessageNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("메시지 조회 실패");
        problem.setProperty("code", ErrorCode.CHAT_MESSAGE_NOT_FOUND.name());
        return problem;
    }
    @ExceptionHandler(ChatRequestException.class)
    public ProblemDetail handleChatRequest(ChatRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(exception.getStatus()), exception.getMessage());
        problem.setTitle("요청 처리 불가");
        problem.setProperty("code", exception.getCode().name());
        return problem;
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public org.springframework.http.ResponseEntity<ProblemDetail> handleMethod(
            org.springframework.web.HttpRequestMethodNotSupportedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED, "요청 메서드를 확인해 주세요.");
        problem.setProperty("code", "METHOD_NOT_ALLOWED");
        var methods = exception.getSupportedHttpMethods();
        return org.springframework.http.ResponseEntity.status(405)
                .allow(methods == null ? new org.springframework.http.HttpMethod[0]
                        : methods.toArray(org.springframework.http.HttpMethod[]::new)).body(problem);
    }
}

package wwn.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CustomControllerAdvice {

    /**
     * Service, Controller에서 던진 예외를 받기 위한
     *
     * @param ex
     * @return
     */

    @ExceptionHandler(IllegalArgumentException.class)

    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);

    }

    @ExceptionHandler(AccessDeniedException.class)

    public ResponseEntity<Map<String, String>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {

        Map<String, String> body = new HashMap<>();
        body.put("message", "접근 권한이 없습니다.");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    @ExceptionHandler(RuntimeException.class)

    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex
    ) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "서버 오류가 발생했습니다.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);

    }
}

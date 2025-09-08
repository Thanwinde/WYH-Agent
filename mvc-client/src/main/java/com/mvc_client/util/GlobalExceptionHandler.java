package com.mvc_client.util;

import com.mvc_client.controller.ChatController;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static com.mvc_client.util.ErrorUtils.safeExtractMessage;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        WebClientResponseException wcre = ErrorUtils.findWebClient(ex);
        if (wcre != null) {
            logger.error("DashScope {} body: {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", 400, "error", safeExtractMessage(wcre.getResponseBodyAsString())));
        }
        logger.error("IllegalState", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", 500, "error", ex.getMessage()));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClient(WebClientResponseException e) {
        logger.error("DashScope {} body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode())
            .body(Map.of("code", e.getRawStatusCode(), "error", safeExtractMessage(e.getResponseBodyAsString())));
    }

    // 复用上面的 findWebClient/safeExtractMessage
}

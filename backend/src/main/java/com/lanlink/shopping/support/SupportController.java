package com.lanlink.shopping.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanlink.shopping.common.R;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/support")
public class SupportController {
    private static final long MAX_BODY_BYTES = 4096L;

    private final SupportAnswerService answers;
    private final SupportRequestGuard guard;
    private final ObjectMapper objectMapper;

    public SupportController(SupportAnswerService answers, SupportRequestGuard guard, ObjectMapper objectMapper) {
        this.answers = answers;
        this.guard = guard;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/ask")
    public ResponseEntity<R<Map<String, String>>> ask(
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Vary", "Origin");

        String fallback = answers.fallback();
        if (!guard.isAuthorizedProxy(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(R.<Map<String, String>>fail(403, fallback));
        }
        if (!guard.allowRequest(request.getHeader("X-Support-Client-IP"))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(R.<Map<String, String>>fail(429, fallback));
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")
                || request.getContentLengthLong() > MAX_BODY_BYTES
                || rawBody == null || rawBody.length() > MAX_BODY_BYTES) {
            return ResponseEntity.ok(R.ok(Map.of("answer", fallback)));
        }

        try {
            JsonNode body = objectMapper.readTree(rawBody);
            if (body == null || !body.isObject() || !body.path("question").isTextual()) {
                return ResponseEntity.ok(R.ok(Map.of("answer", fallback)));
            }
            return ResponseEntity.ok(R.ok(Map.of("answer", answers.answer(body.path("question").asText()))));
        } catch (JsonProcessingException ex) {
            return ResponseEntity.ok(R.ok(Map.of("answer", fallback)));
        }
    }
}

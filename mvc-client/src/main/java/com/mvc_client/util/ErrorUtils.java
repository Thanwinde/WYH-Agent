package com.mvc_client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

public final class ErrorUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorUtils() {}

    /** 从异常链中抓到真正的 WebClientResponseException（好拿到响应体） */
    public static WebClientResponseException findWebClient(Throwable ex) {
        // 先把 Reactor 包的 ReactiveException 等包裹剥掉
        Throwable t = Exceptions.unwrap(ex);
        // 再顺着 cause 链往下找
        while (t != null) {
            if (t instanceof WebClientResponseException w) return w;
            t = t.getCause();
        }
        return null;
    }

    /** 尝试从 DashScope/DeepSeek 等返回的 JSON 里提取人能读的错误信息 */
    public static String safeExtractMessage(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonNode root = MAPPER.readTree(body);
            // 常见结构：{"error":{"message":"...", "code":"..."}} 或 {"message":"..."}
            if (root.has("error")) {
                JsonNode err = root.get("error");
                if (err.isTextual()) return err.asText();
                if (err.has("message")) return err.get("message").asText();
                if (err.has("type")) return err.get("type").asText();
            }
            if (root.has("message")) return root.get("message").asText();
        } catch (Exception ignore) {}
        // 不是 JSON 就原样返回
        return body;
    }
}

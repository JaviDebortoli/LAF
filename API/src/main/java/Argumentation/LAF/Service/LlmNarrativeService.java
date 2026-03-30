package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class LlmNarrativeService {
    public static final String TEMPORARILY_UNAVAILABLE_MESSAGE =
            "Narrative generation service is temporarily unavailable.";

    private static final String SYSTEM_PROMPT = """
            You are an expert in argumentation and formal reasoning.
            Write a concise narrative in English (a few short paragraphs) about the experiment results.

            You must use only the provided trace data:
            - final conclusions,
            - how they were derived,
            - conflicts and winners.

            Rules:
            - Do not invent literals, steps, conflicts, or winners.
            - Explain discoveries clearly and naturally.
            - Mention conflict outcomes explicitly when present.
            - Respect the quantitative acceptability semantics from trace data:
              * DEFEATED means all numeric delta labels are 0.0.
              * ADMISSIBLE means at least one numeric delta label is greater than 0.0.
            - Use these status labels exactly as written when referring to conclusion status.
            - Keep it focused on findings, not implementation details.

            Output must be strict JSON with a single key:
            { "narrative": "..." }
            """;

    private final ObjectMapper objectMapper;

    @Value("${laf.narration.llm.enabled:false}")
    private boolean enabled;

    @Value("${laf.narration.llm.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${laf.narration.llm.api-key:}")
    private String apiKey;

    @Value("${laf.narration.llm.model:gpt-4o-mini}")
    private String model;

    @Value("${laf.narration.llm.prompt-version:narrative-v1}")
    private String promptVersion;

    @Value("${laf.narration.llm.timeout-ms:15000}")
    private int timeoutMs;

    public LlmNarrativeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GenerationResult generateNarrative(NarrativeTraceResponse trace) {
        if (!isConfigured()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        try {
            String traceJson = objectMapper.writeValueAsString(trace);
            String requestBody = buildRequestBody(traceJson);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }
    }

    private boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private String buildRequestBody(String traceJson) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        root.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        root.set(
                "messages",
                objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode().put("role", "system").put("content", SYSTEM_PROMPT))
                        .add(objectMapper.createObjectNode().put("role", "user").put("content", "Trace JSON:\n" + traceJson)));

        return objectMapper.writeValueAsString(root);
    }

    private GenerationResult parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String resolvedModel = root.path("model").asText(model);
        String content = root.path("choices").path(0).path("message").path("content").asText("");

        if (content.isBlank()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        JsonNode parsed = objectMapper.readTree(content);
        String narrative = parsed.path("narrative").asText("");
        if (narrative.isBlank()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        return new GenerationResult(narrative, resolvedModel, promptVersion);
    }

    public record GenerationResult(String narrative, String model, String promptVersion) {
    }
}

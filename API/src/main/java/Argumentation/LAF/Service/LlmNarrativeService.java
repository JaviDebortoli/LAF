package Argumentation.LAF.Service;

import Argumentation.LAF.Config.LlmNarrationProperties;
import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private final LlmNarrationProperties properties;

    public LlmNarrativeService(ObjectMapper objectMapper, LlmNarrationProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public GenerationResult generateNarrative(NarrativeTraceResponse trace) {
        if (!isConfigured()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        try {
            String traceJson = objectMapper.writeValueAsString(trace);
            String requestBody = buildRequestBody(traceJson);

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
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
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    private String buildRequestBody(String traceJson) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("temperature", properties.getTemperature());
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
        String resolvedModel = root.path("model").asText(properties.getModel());
        String content = root.path("choices").path(0).path("message").path("content").asText("");

        if (content.isBlank()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        JsonNode parsed = objectMapper.readTree(content);
        String narrative = parsed.path("narrative").asText("");
        if (narrative.isBlank()) {
            throw new NarrativeServiceUnavailableException(TEMPORARILY_UNAVAILABLE_MESSAGE);
        }

        return new GenerationResult(narrative, resolvedModel, properties.getPromptVersion());
    }

    public record GenerationResult(String narrative, String model, String promptVersion) {
    }
}

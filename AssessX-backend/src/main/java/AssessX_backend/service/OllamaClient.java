package AssessX_backend.service;

import AssessX_backend.exception.OllamaUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;

    @Value("${app.ollama.model:qwen2.5-coder:7b}")
    private String model;

    public OllamaClient(@org.springframework.beans.factory.annotation.Qualifier("ollamaRestClient") RestClient ollamaRestClient) {
        this.restClient = ollamaRestClient;
    }

    public String generate(String prompt) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/api/generate")
                    .body(Map.of("model", model, "prompt", prompt, "stream", false))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("response")) {
                throw new OllamaUnavailableException();
            }

            return response.get("response").toString().trim();
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Ollama connection timeout or refused: {}", e.getMessage());
            throw new OllamaUnavailableException();
        } catch (Exception e) {
            log.warn("Ollama call failed: {}", e.getMessage());
            throw new OllamaUnavailableException();
        }
    }
}

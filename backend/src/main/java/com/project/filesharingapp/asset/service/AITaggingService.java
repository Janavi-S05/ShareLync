package com.project.filesharingapp.asset.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@Slf4j
public class AITaggingService {

    @Value("${groq.api-key}")
    private String groqApiKey;

    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL      = "llama-3.1-8b-instant";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AITaggingService(@Qualifier("groqRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> generateTags(String filename, String fileType, String ignoredText) {
        log.info("Generating Groq tags for: {}", filename);
        String prompt = "Filename: \"" + filename + "\"\n"
                + "Return ONLY a JSON array of 3-5 lowercase tags. "
                + "Example: [\"cover-letter\",\"finance\",\"job-application\"]\n"
                + "No explanation. No markdown. Just the JSON array.";
        return callGroq(prompt, 80);
    }

    public Map<String, List<String>> generateTagsBatch(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) return Collections.emptyMap();
        log.info("Batch tagging {} files in one Groq API call", filenames.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filenames.size(); i++) {
            sb.append(i).append(": \"").append(filenames.get(i)).append("\"\n");
        }
        String prompt = "For each filename below, return 3-5 lowercase tags.\n"
                + "Return ONLY a JSON object like: {\"0\":[\"tag1\",\"tag2\"],\"1\":[\"tag3\"]}\n"
                + "No explanation. No markdown. Just the JSON object.\n\n" + sb;
        Map<String, List<String>> result = new HashMap<>();
        try {
            String raw = callGroqRaw(prompt, 400);
            if (raw == null) return result;
            raw = raw.replaceAll("```json|```", "").trim();
            Map<String, List> parsed = objectMapper.readValue(raw, Map.class);
            for (Map.Entry<String, List> entry : parsed.entrySet()) {
                int idx = Integer.parseInt(entry.getKey());
                if (idx < filenames.size()) {
                    List<String> tags = new ArrayList<>();
                    for (Object t : entry.getValue()) tags.add(t.toString());
                    result.put(filenames.get(idx), tags);
                }
            }
            log.info("Batch tagged {} files via Groq", result.size());
        } catch (Exception e) {
            log.error("Batch tagging failed: {}", e.getMessage());
        }
        return result;
    }

    private List<String> callGroq(String prompt, int maxTokens) {
        try {
            String raw = callGroqRaw(prompt, maxTokens);
            if (raw == null) return Collections.emptyList();
            raw = raw.replaceAll("```json|```", "").trim();
            List<String> tags = objectMapper.readValue(raw, List.class);
            log.info("Tags generated: {}", tags);
            return tags;
        } catch (Exception e) {
            log.error("Tag parse failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String callGroqRaw(String prompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);
            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0);
            body.put("messages", List.of(
                    Map.of("role", "system",
                           "content", "You are a file tagger. Output only valid JSON. No prose. No markdown."),
                    Map.of("role", "user", "content", prompt)
            ));
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(GROQ_URL, HttpMethod.POST, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<?> choices = (List<?>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice  = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    String content = (String) message.get("content");
                    log.info("Groq raw response: {}", content);
                    return content;
                }
            }
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
        }
        return null;
    }
}

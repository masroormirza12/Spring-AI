package com.example.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class GeminiSpeechService{

    private static final Logger log = LoggerFactory.getLogger(GeminiSpeechService.class);
    @Autowired
    private WebClient webClient;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;


    public byte[] textToSpeech(String text) {
        Map<String, Object> request = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", text)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseModalities", List.of("AUDIO"),
                        "speechConfig", Map.of(
                                "voiceConfig", Map.of(
                                        "prebuiltVoiceConfig", Map.of("voiceName", "Erinome")
                                )
                        )
                ),
                "model", "gemini-2.5-flash-preview-tts"
        );

        String jsonResponse = webClient.post()
                .uri("/models/gemini-2.5-flash-preview-tts:generateContent")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info(jsonResponse);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            String base64Audio = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("inlineData")
                    .path("data")
                    .asText();
            log.info("restCall done for now");
            return Base64.getDecoder().decode(base64Audio);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini TTS response", e);
        }
    }



    //To upload file and get URL
    public String uploadAudio(byte[] audioBytes, String mimeType, String displayName) throws IOException {
        // Step 1a: Start resumable upload
        Map<String, Object> metadata = Map.of(
                "file", Map.of("display_name", displayName)
        );

        ClientResponse response = webClient.post()
                .uri("https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey)
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(audioBytes.length))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadata)
                .exchange()
                .block();

        if (response == null || !response.headers().header("x-goog-upload-url").isEmpty() == false) {
            throw new RuntimeException("Failed to initiate upload session");
        }

        String uploadUrl = response.headers().header("x-goog-upload-url").get(0);

        // Step 1b: Upload the actual file bytes
        Map<String, Object> fileInfo = webClient.post()
                .uri(uploadUrl)
                .header("X-Goog-Upload-Command", "upload, finalize")
                .header("X-Goog-Upload-Offset", "0")
                .header("Content-Length", String.valueOf(audioBytes.length))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioBytes)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (fileInfo == null || !fileInfo.containsKey("file")) {
            throw new RuntimeException("Upload failed, no file info returned");
        }

        Map<String, Object> file = (Map<String, Object>) fileInfo.get("file");
        return (String) file.get("uri");
    }


    //To get the text from Audio
    public String transcribeAudio(String fileUri, String mimeType) {
        Map<String, Object> request = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", "Transcribe this audio file."),
                                        Map.of("file_data", Map.of(
                                                "mime_type", mimeType,
                                                "file_uri", fileUri
                                        ))
                                )
                        )
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        log.info("Gemini response: {}", response);

        // Parse transcription text
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            for (Map<String, Object> part : parts) {
                if (part.containsKey("text")) {
                    log.info((String) part.get("text"));
                    return (String) part.get("text");
                }
            }
        }

        return null;
    }



}


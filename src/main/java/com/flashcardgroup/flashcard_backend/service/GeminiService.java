package com.flashcardgroup.flashcard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardgroup.flashcard_backend.dto.LookupDTO;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("!test")
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public GeminiService(@Value("${gemini.api.model}") String modelName, @Value("${gemini.api.key}") String apiKey) {
        this.client = new Client.Builder().apiKey(apiKey).build();
        this.objectMapper = new ObjectMapper();
        this.modelName = modelName;
        logger.info("GeminiService initialized with model: {}", modelName);
    }


    public LookupDTO lookup(String word) throws IOException {
        String prompt = String.format(
            "For the word '%s', provide the following information in JSON format:\n" +
            "{\n" +
            "  \"meaning\": \"definition and meaning of the word\",\n" +
            "  \"etymology\": \"origin and etymology of the word\",\n" +
            "  \"translation\": [\"translation1\", \"translation2\"],\n" +
            "  \"cognates\": [\"cognate1\", \"cognate2\"]\n" +
            "}\n" +
            "Only return valid JSON, no additional text.",
            word
        );

        GenerateContentResponse response = client.models.generateContent(modelName, prompt, null);
        String responseText = response.text();

        // Parse JSON response
        return parseGeminiResponse(responseText);
    }

    // New overload supporting response language and target translation languages
    public LookupDTO lookup(String word, String responseLang, List<String> translateTo) throws IOException {
        String targetsSection;
        if (translateTo == null || translateTo.isEmpty()) {
            targetsSection = "No translations are required; return an empty JSON array for the 'translation' field.";
        } else {
            // Build ordered instruction
            StringBuilder sb = new StringBuilder();
            sb.append("Provide translations in the 'translation' array in this exact order and length (one string per language, no labels): ");
            for (int i = 0; i < translateTo.size(); i++) {
                sb.append(translateTo.get(i));
                if (i < translateTo.size() - 1) sb.append(", ");
            }
            targetsSection = sb.toString();
        }

        String prompt = "You are a precise multilingual dictionary and etymology generator. " +
                "Respond only with strict JSON and no extra text. Use the given response language for narrative fields.\n\n" +
                String.format("WORD: '%s'\nRESPONSE_LANGUAGE: %s\n", word, responseLang) +
                targetsSection + "\n\n" +
                "Return JSON exactly in this shape (no extra fields):\n" +
                "{\n" +
                "  \"meaning\": \"short definition and meaning written in RESPONSE_LANGUAGE\",\n" +
                "  \"etymology\": \"brief origin and etymology written in RESPONSE_LANGUAGE\",\n" +
                "  \"translation\": [\"string per requested language, in order (or empty if none requested)\"],\n" +
                "  \"cognates\": [\"a few probable cognates]\n" +
                "}\n" +
                "Rules: Use plain text, no markdown. Ensure valid JSON only.";

        GenerateContentResponse response = client.models.generateContent(modelName, prompt, null);
        String responseText = response.text();
        return parseGeminiResponse(responseText);
    }

    private LookupDTO parseGeminiResponse(String responseText) throws IOException {
        try {
            // Remove markdown code blocks if present
            String cleanJson = responseText.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```") ) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            String meaning = jsonNode.has("meaning") ? jsonNode.get("meaning").asText() : "";
            String etymology = jsonNode.has("etymology") ? jsonNode.get("etymology").asText() : "";

            List<String> translation = new ArrayList<>();
            if (jsonNode.has("translation") && jsonNode.get("translation").isArray()) {
                jsonNode.get("translation").forEach(node -> translation.add(node.asText()));
            }

            List<String> cognates = new ArrayList<>();
            if (jsonNode.has("cognates") && jsonNode.get("cognates").isArray()) {
                jsonNode.get("cognates").forEach(node -> cognates.add(node.asText()));
            }

            return new LookupDTO(meaning, etymology, translation, cognates);
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}

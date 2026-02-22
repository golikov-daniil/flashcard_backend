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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return lookup(word, "en", List.of());
    }

    // New overload supporting response language and target translation languages
    public LookupDTO lookup(String word, String responseLang, List<String> translateTo) throws IOException {
        logger.info("GeminiService.lookup: word='{}' responseLang='{}' translateTo={}", word, responseLang, translateTo);
        String prompt = buildLookupPrompt(word, responseLang, translateTo);

        GenerateContentResponse response = client.models.generateContent(modelName, prompt, null);
        String responseText = response.text();
        return parseGeminiResponse(word, responseText);
    }

    private String buildLookupPrompt(String word, String responseLang, List<String> translateTo) {
        if (translateTo == null || translateTo.isEmpty()) {
            return "You generate flashcard fields. Respond only with strict JSON and no extra text.\n\n" +
                    String.format("WORD: '%s'\nRESPONSE_LANGUAGE: %s\n\n", word, responseLang) +
                    "Hard requirements:\n" +
                    "- Front MUST be a meaning/definition (NOT a translation) written in RESPONSE_LANGUAGE.\n" +
                    "- If RESPONSE_LANGUAGE is 'en', Front MUST be in English.\n\n" +
                    "Return JSON exactly in this shape (no extra fields):\n" +
                    "{\n" +
                    "  \"Front\": \"meaning/definition of WORD written in RESPONSE_LANGUAGE (1-2 short lines)\",\n" +
                    "  \"Back\": \"WORD exactly\",\n" +
                    "  \"Metadata\": \"\",\n" +
                    "  \"Example\": \"one short example sentence that contains WORD\",\n" +
                    "  \"OccurrenceIndices\": \"comma-separated 0-based word indices of WORD inside Example; no spaces; e.g. '3' or '3,4'\",\n" +
                    "  \"Synonyms\": \"3-6 synonyms separated by '; ' (empty string if none)\",\n" +
                    "  \"PartOfSpeech\": \"noun/verb/adjective/etc (empty string if unknown)\",\n" +
                    "  \"Classifiers\": \"classifier tags if relevant (otherwise empty string)\"\n" +
                    "}\n\n" +
                    "OccurrenceIndices rules:\n" +
                    "- Tokenize Example by splitting on single spaces. Indices refer to token positions in that list (0-based).\n" +
                    "- For matching, compare tokens after stripping leading/trailing punctuation characters .,!?:;\"'()[]{} and using case-insensitive comparison.\n" +
                    "- If WORD contains multiple space-separated tokens (a phrase), include indices for every token of each occurrence.\n" +
                    "- Ensure Example contains WORD exactly once whenever possible to keep indices simple.\n\n" +
                    "Rules: Use plain text in all string fields, no markdown. Ensure valid JSON only.";
        }

        String targets = String.join(",", translateTo);
        return "You generate flashcard fields. Respond only with strict JSON and no extra text.\n\n" +
                String.format("WORD: '%s'\nRESPONSE_LANGUAGE: %s\nTRANSLATION_TARGETS: %s\n\n", word, responseLang, targets) +
                "Hard requirements:\n" +
                "- Front MUST be a meaning/definition (NOT a translation) written in RESPONSE_LANGUAGE.\n" +
                "- Front MUST NOT be written in any language listed in TRANSLATION_TARGETS.\n" +
                "- Translations are ONLY placed in the Translations object; they must NOT affect Front/Example/Synonyms/etc.\n" +
                "- If RESPONSE_LANGUAGE is 'en', Front MUST be in English.\n\n" +
                "Example (for clarity only; do not include in output):\n" +
                "WORD='puckish' RESPONSE_LANGUAGE=en TRANSLATION_TARGETS=ru\n" +
                "Front could be: 'playfully mischievous; impish'\n" +
                "Translations could be: {\"ru\": \"ozornoy\"}\n\n" +
                "Return JSON exactly in this shape (no extra fields):\n" +
                "{\n" +
                "  \"Front\": \"meaning/definition of WORD written in RESPONSE_LANGUAGE (1-2 short lines)\",\n" +
                "  \"Back\": \"WORD exactly\",\n" +
                "  \"Metadata\": \"\",\n" +
                "  \"Example\": \"one short example sentence that contains WORD\",\n" +
                "  \"OccurrenceIndices\": \"comma-separated 0-based word indices of WORD inside Example; no spaces; e.g. '3' or '3,4'\",\n" +
                "  \"Synonyms\": \"3-6 synonyms separated by '; ' (empty string if none)\",\n" +
                "  \"PartOfSpeech\": \"noun/verb/adjective/etc (empty string if unknown)\",\n" +
                "  \"Classifiers\": \"classifier tags if relevant (otherwise empty string)\",\n" +
                "  \"Translations\": {\n" +
                "    \"<lang>\": \"translation of WORD into that language\"\n" +
                "  }\n" +
                "}\n\n" +
                "Translations rules:\n" +
                "- Create one entry per language code in TRANSLATION_TARGETS (comma-separated).\n" +
                "- Only include requested language codes as keys.\n" +
                "- Values are the best natural translation of WORD (not the definition).\n\n" +
                "OccurrenceIndices rules:\n" +
                "- Tokenize Example by splitting on single spaces. Indices refer to token positions in that list (0-based).\n" +
                "- For matching, compare tokens after stripping leading/trailing punctuation characters .,!?:;\"'()[]{} and using case-insensitive comparison.\n" +
                "- If WORD contains multiple space-separated tokens (a phrase), include indices for every token of each occurrence.\n" +
                "- Ensure Example contains WORD exactly once whenever possible to keep indices simple.\n\n" +
                "Rules: Use plain text in all string fields, no markdown. Ensure valid JSON only.";
    }

    private LookupDTO parseGeminiResponse(String word, String responseText) throws IOException {
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

            String front = jsonNode.has("Front") ? jsonNode.get("Front").asText() : "";
            String back = jsonNode.has("Back") ? jsonNode.get("Back").asText() : "";
            if (back == null || back.isBlank()) {
                back = word;
            }

            // Always keep metadata empty (client requested)
            String metadata = "";

            String example = jsonNode.has("Example") ? jsonNode.get("Example").asText() : "";
            String occurrenceIndices = jsonNode.has("OccurrenceIndices") ? jsonNode.get("OccurrenceIndices").asText() : "";
            String synonyms = jsonNode.has("Synonyms") ? jsonNode.get("Synonyms").asText() : "";
            String partOfSpeech = jsonNode.has("PartOfSpeech") ? jsonNode.get("PartOfSpeech").asText() : "";
            String classifiers = jsonNode.has("Classifiers") ? jsonNode.get("Classifiers").asText() : "";

            Map<String, String> translations = new LinkedHashMap<>();
            if (jsonNode.has("Translations") && jsonNode.get("Translations").isObject()) {
                JsonNode t = jsonNode.get("Translations");
                t.fields().forEachRemaining(entry -> {
                    JsonNode v = entry.getValue();
                    translations.put(entry.getKey(), (v == null || v.isNull()) ? "" : v.asText());
                });
            }

            return new LookupDTO(front, back, metadata, example, occurrenceIndices, synonyms, partOfSpeech, classifiers, translations);
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}

package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.dto.LookupDTO;
import com.flashcardgroup.flashcard_backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@CrossOrigin
public class LookupController {

    private final GeminiService geminiService;

    public LookupController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Endpoint to lookup a word and get its meaning, etymology, translation, and cognates.
     * Accepts optional query parameters to control the response and translation languages.
     *
     * @param word the word to look up (path variable)
     * @param responseLang optional language code for the meaning/etymology response (default: "en")
     * @param translateTo optional list of language codes to translate the word to (order preserved)
     * @return ResponseEntity containing LookupDTO with word information
     */
    @GetMapping("/lookup/{word}")
    public ResponseEntity<LookupDTO> lookupWord(
            @PathVariable String word,
            @RequestParam(value = "responseLanguage", required = false) String responseLang,
            @RequestParam(value = "translationLanguages", required = false) List<String> translateTo
    ) {
        try {
            String respLang = (responseLang == null || responseLang.isBlank()) ? "en" : responseLang;
            List<String> targets = (translateTo == null) ? Collections.emptyList() : translateTo;
            LookupDTO result = geminiService.lookup(word, respLang, targets);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            // Return empty DTO in case of error
            return ResponseEntity.status(500).body(
                new LookupDTO("Error: " + e.getMessage(), "", List.of(), List.of())
            );
        }
    }
}

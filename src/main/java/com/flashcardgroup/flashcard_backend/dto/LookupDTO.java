package com.flashcardgroup.flashcard_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record LookupDTO(
        @JsonProperty("Front") String Front,
        @JsonProperty("Back") String Back,
        @JsonProperty("Metadata") String Metadata,
        @JsonProperty("Example") String Example,
        @JsonProperty("OccurrenceIndices") String OccurrenceIndices,
        @JsonProperty("Synonyms") String Synonyms,
        @JsonProperty("PartOfSpeech") String PartOfSpeech,
        @JsonProperty("Classifiers") String Classifiers,
        @JsonProperty("Translations") Map<String, String> Translations
) {
}

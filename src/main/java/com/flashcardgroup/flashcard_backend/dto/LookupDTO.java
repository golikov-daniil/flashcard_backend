package com.flashcardgroup.flashcard_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LookupDTO(
        @JsonProperty("meaning") String meaning,
        @JsonProperty("etymology") String etymology,
        @JsonProperty("translation") List<String> translation,
        @JsonProperty("cognates") List<String> cognates
) {
}


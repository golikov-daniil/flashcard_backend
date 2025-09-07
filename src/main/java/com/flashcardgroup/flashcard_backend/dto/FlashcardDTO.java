
package com.flashcardgroup.flashcard_backend.dto;

import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FlashcardDTO(
        @JsonProperty("CardID") Integer CardID,
        @JsonProperty("DeckID") Integer DeckID,
        @JsonProperty("Front") String Front,
        @JsonProperty("Back") String Back,
        @JsonProperty("LastReviewed") Long LastReviewed,
        @JsonProperty("Stage") Integer Stage,
        @JsonProperty("Metadata") String Metadata,
        @JsonProperty("Example") String Example,
        @JsonProperty("OccurrenceIndices") String OccurrenceIndices,
        @JsonProperty("Synonyms") String Synonyms,
        @JsonProperty("PartOfSpeech") String PartOfSpeech,
        @JsonProperty("Classifiers") String Classifiers,
        @JsonProperty("ImageNo") String ImageNo
) {
    public static FlashcardDTO fromEntity(Flashcard flashcard) {
        return new FlashcardDTO(
                flashcard.getCardId(),
                flashcard.getDeck().getDeckId(),
                flashcard.getFront(),
                flashcard.getBack(),
                flashcard.getLastReviewed(),
                flashcard.getStage(),
                flashcard.getMetadata(),
                flashcard.getExample(),
                flashcard.getOccurrenceIndices(),
                flashcard.getSynonyms(),
                flashcard.getPartOfSpeech(),
                flashcard.getClassifiers(),
                flashcard.getImageNo()
        );
    }
}
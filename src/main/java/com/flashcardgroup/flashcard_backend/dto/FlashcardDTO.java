package com.flashcardgroup.flashcard_backend.dto;

import com.flashcardgroup.flashcard_backend.model.Flashcard;

public record FlashcardDTO(
    Integer cardId,
    Integer deckId,
    String front,
    String back,
    Long lastReviewed,
    Integer stage,
    String metadata,
    String example,
    String occurrenceIndices,
    String synonyms,
    String partOfSpeech,
    String classifiers,
    String imageNo
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

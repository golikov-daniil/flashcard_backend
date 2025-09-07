package com.flashcardgroup.flashcard_backend.dto;

import com.flashcardgroup.flashcard_backend.model.Deck;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DeckDTO(
        @JsonProperty("DeckID") Integer DeckID,
        @JsonProperty("UserID") Integer UserID,
        @JsonProperty("Name") String Name,
        @JsonProperty("Description") String Description,
        @JsonProperty("CardCount") int CardCount
) {
    public static DeckDTO fromEntity(Deck deck) {
        return new DeckDTO(
                deck.getDeckId(),
                deck.getUser().getUserId(),
                deck.getName(),
                deck.getDescription(),
                deck.getFlashcards().size()
        );
    }
}
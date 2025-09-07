
package com.flashcardgroup.flashcard_backend.dto;

import com.flashcardgroup.flashcard_backend.model.Deck;

public record DeckDTO(
    Integer deckId,
    Integer userId,
    String name,
    String description,
    int cardCount
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

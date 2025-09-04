// src/main/java/com/example/flashcards/repo/FlashcardRepository.java
package com.flashcardgroup.flashcard_backend.repo;

import com.flashcardgroup.flashcard_backend.model.Flashcard;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    List<Flashcard> findByDeck_DeckId(Integer deck_deckId);
    List<Flashcard> findByDeck_User_UserId(Integer deck_user_userId);
    Flashcard findByCardId(Integer cardId);
    boolean existsByBackAndDeck_DeckId(@NotBlank String back, Integer deck_deckId);
}

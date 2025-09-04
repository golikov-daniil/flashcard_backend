// src/main/java/com/example/flashcards/repo/DeckRepository.java
package com.flashcardgroup.flashcard_backend.repo;

import com.flashcardgroup.flashcard_backend.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByUser_UserId(Integer user_userId);
}

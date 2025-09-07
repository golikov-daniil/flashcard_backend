// src/main/java/com/example/flashcards/service/DataService.java
package com.flashcardgroup.flashcard_backend.service;

import com.flashcardgroup.flashcard_backend.model.Deck;
import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.repo.DeckRepository;
import com.flashcardgroup.flashcard_backend.repo.FlashcardRepository;
import com.flashcardgroup.flashcard_backend.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DataService {

    private final UserRepository userRepo;
    private final DeckRepository deckRepo;
    private final FlashcardRepository cardRepo;

    public DataService(UserRepository userRepo, DeckRepository deckRepo, FlashcardRepository cardRepo) {
        this.userRepo = userRepo;
        this.deckRepo = deckRepo;
        this.cardRepo = cardRepo;
    }

    // getAllUsers
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // addUser
    @Transactional
    public User addUser(User user) {
        return userRepo.save(user);
    }

    // addFlashcard
    @Transactional
    public Flashcard addFlashcard(Flashcard card) {
        return cardRepo.save(card);
    }

    // getReviewableCards(deckID) using the entity method shouldReview()
    public List<Flashcard> getReviewableCards(Integer deckId) {
        return cardRepo.findByDeck_DeckId(deckId)
                .stream()
                .filter(Flashcard::shouldReview)
                .toList();
    }

    // getFlashcardById
    public Optional<Flashcard> getFlashcardById(Long cardId) {
        return cardRepo.findById(cardId);
    }

    // getUserByUsername
    public Optional<User> getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    // getUserById
    public Optional<User> getUserById(Long userId) {
        return userRepo.findById(userId);
    }

    // updateFlashcard lastReviewed and stage
    @Transactional
    public void updateFlashcardReview(Long cardId, long lastReviewedSeconds, int stage) {
        Flashcard card = cardRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        card.setLastReviewed(lastReviewedSeconds);
        card.setStage(stage);
        cardRepo.save(card);
    }

    // getAllCards(deckID)
    public List<Flashcard> getAllCards(Integer deckId) {
        return cardRepo.findByDeck_DeckId(deckId);
    }

    // getDecksByUserId
    public List<Deck> getDecksByUserId(Integer userId) {
        return deckRepo.findByUser_UserId(userId);
    }

    // getDeckById - new method needed for flashcard creation
    public Optional<Deck> getDeckById(Integer deckId) {
        return deckRepo.findById(Long.valueOf(deckId));
    }

    // checkIfFlashcardExistsByBackAndDeckId
    public boolean checkIfFlashcardExistsByBackAndDeckId(String back, Integer deckId) {
        return cardRepo.existsByBackAndDeck_DeckId(back, deckId);
    }
}

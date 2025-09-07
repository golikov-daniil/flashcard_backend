package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.dto.DeckDTO;
import com.flashcardgroup.flashcard_backend.dto.FlashcardDTO;
import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.flashcardgroup.flashcard_backend.model.Deck;
import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.service.DataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class FlashcardController {

    private final DataService dataService;

    public FlashcardController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/reviewable-cards/{deckId}")
    public ResponseEntity<?> getReviewableCards(@PathVariable Integer deckId) {
        try {
            List<Flashcard> cards = dataService.getReviewableCards(deckId);
            List<FlashcardDTO> dtos = cards.stream()
                    .map(FlashcardDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving cards: " + e.getMessage());
        }
    }

    @PostMapping("/update-card")
    public ResponseEntity<?> updateCard(@RequestBody Map<String, Object> payload) {
        try {
            Long cardId = Long.valueOf(payload.get("CardID").toString());
            Boolean remembered = (Boolean) payload.get("Remembered");

            Optional<Flashcard> optionalCard = dataService.getFlashcardById(cardId);
            if (optionalCard.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Flashcard not found.");
            }

            Flashcard card = optionalCard.get();
            
            // Update logic based on whether the card was remembered
            if (remembered) {
                card.setStage(Math.min(card.getStage() + 1, 6)); // Assuming 6 is the maximum stage
            } else {
                card.setStage(Math.max(card.getStage() - 1, 1)); // Minimum stage is 1
            }
            
            // Update last reviewed time
            card.setLastReviewed(System.currentTimeMillis() / 1000);
            
            // Save the updated flashcard
            dataService.addFlashcard(card);
            
            return ResponseEntity.ok("Flashcard updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating flashcard: " + e.getMessage());
        }
    }

    @GetMapping("/all-cards/{deckId}")
    public ResponseEntity<?> getAllCards(@PathVariable Integer deckId) {
        try {
            List<Flashcard> cards = dataService.getAllCards(deckId);
            List<FlashcardDTO> dtos = cards.stream()
                .map(FlashcardDTO::fromEntity)
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving all cards: " + e.getMessage());
        }
    }

    @GetMapping("/user-decks/{userId}")
    public ResponseEntity<?> getUserDecks(@PathVariable Integer userId) {
        try {
            List<Deck> decks = dataService.getDecksByUserId(userId);
            List<DeckDTO> dtos = decks.stream()
                    .map(DeckDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving decks: " + e.getMessage());
        }
    }

    @GetMapping("/get-username/{userId}")
    public ResponseEntity<?> getUsername(@PathVariable Long userId) {
        try {
            Optional<User> userOptional = dataService.getUserById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "username", user.getUsername()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving user: " + e.getMessage());
        }
    }
}

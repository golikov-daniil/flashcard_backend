package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.dto.DeckDTO;
import com.flashcardgroup.flashcard_backend.dto.FlashcardDTO;
import com.flashcardgroup.flashcard_backend.dto.FlashcardForm;
import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.flashcardgroup.flashcard_backend.model.Deck;
import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.service.DataService;
import com.flashcardgroup.flashcard_backend.service.GcsImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
//@RequestMapping("/api")
@CrossOrigin
public class FlashcardController {

    private final DataService dataService;
    private final GcsImageService gcsImageService;

    public FlashcardController(DataService dataService, GcsImageService gcsImageService) {
        this.dataService = dataService;
        this.gcsImageService = gcsImageService;
    }

    @PostMapping(value = "/flashcards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addFlashcard(@ModelAttribute FlashcardForm form) {
        try {
            if (dataService.checkIfFlashcardExistsByBackAndDeckId(form.Back(), form.DeckID())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Flashcard with the same back already exists in this deck.");
            }

            var card = new Flashcard();
            card.setDeck(dataService.getDeckById(form.DeckID())
                    .orElseThrow(() -> new IllegalArgumentException("Deck not found")));
            card.setFront(form.Front());
            card.setBack(form.Back());
            card.setStage(1);
            card.setLastReviewed(System.currentTimeMillis() / 1000);

            card.setMetadata(form.Metadata());
            card.setExample(form.Example());
            card.setOccurrenceIndices(form.OccurrenceIndices());
            card.setSynonyms(form.Synonyms());
            card.setPartOfSpeech(form.PartOfSpeech());
            card.setClassifiers(form.Classifiers());

            var image = form.image();
            if (image != null && !image.isEmpty()) {
                String objectName = gcsImageService.uploadImage(image, form.Back());
                if (objectName != null) card.setImageNo(objectName);
            }

            dataService.addFlashcard(card);
            return ResponseEntity.status(HttpStatus.CREATED).body("Flashcard added successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding flashcard: " + e.getMessage());
        }
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

    @GetMapping("/user-decks")
    public ResponseEntity<?> getUserDecks(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Deck> decks = dataService.getDecksByUserId(userId.intValue());
            List<DeckDTO> dtos = decks.stream()
                    .map(DeckDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving decks: " + e.getMessage());
        }
    }

    @GetMapping("/get-username")
    public ResponseEntity<?> getUsername(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
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
package com.flashcardgroup.flashcard_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.flashcardgroup.flashcard_backend.model.Deck;
import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.service.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlashcardController.class)
@ContextConfiguration(classes = {FlashcardController.class, FlashcardControllerTest.TestConfig.class})
class FlashcardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataService dataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public DataService dataService() {
            return mock(DataService.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Reset the mock before each test to avoid interference between tests
        reset(dataService);
    }

    // Test data setup methods
    private Flashcard createMockFlashcard(Integer cardId, Integer deckId, String front, String back, Integer stage) {
        Flashcard flashcard = new Flashcard();
        flashcard.setCardId(cardId);
        flashcard.setFront(front);
        flashcard.setBack(back);
        flashcard.setStage(stage);
        flashcard.setLastReviewed(System.currentTimeMillis() / 1000);

        Deck deck = new Deck();
        deck.setDeckId(deckId);
        flashcard.setDeck(deck);

        return flashcard;
    }

    private Deck createMockDeck(Integer deckId, Integer userId, String name, String description) {
        Deck deck = new Deck();
        deck.setDeckId(deckId);
        deck.setName(name);
        deck.setDescription(description);

        // Initialize flashcards set to avoid null pointer when cardCount is calculated
        deck.setFlashcards(Collections.emptySet());

        User user = new User();
        user.setUserId(userId);
        deck.setUser(user);

        return deck;
    }

    private User createMockUser(Integer userId, String username) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        return user;
    }

    // Tests for getReviewableCards endpoint
    @Test
    @DisplayName("GET /api/reviewable-cards/{deckId} returns reviewable cards successfully")
    void getReviewableCards_success() throws Exception {
        Integer deckId = 1;
        List<Flashcard> mockCards = Arrays.asList(
                createMockFlashcard(1, deckId, "Hello", "Bonjour", 2),
                createMockFlashcard(2, deckId, "Goodbye", "Au revoir", 1)
        );

        when(dataService.getReviewableCards(deckId)).thenReturn(mockCards);

        mockMvc.perform(get("/api/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cardId").value(1))
                .andExpect(jsonPath("$[0].front").value("Hello"))
                .andExpect(jsonPath("$[0].back").value("Bonjour"))
                .andExpect(jsonPath("$[1].cardId").value(2))
                .andExpect(jsonPath("$[1].front").value("Goodbye"));

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    @Test
    @DisplayName("GET /api/reviewable-cards/{deckId} returns empty list when no reviewable cards")
    void getReviewableCards_emptyList() throws Exception {
        Integer deckId = 1;
        when(dataService.getReviewableCards(deckId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    @Test
    @DisplayName("GET /api/reviewable-cards/{deckId} handles service exception")
    void getReviewableCards_serviceException() throws Exception {
        Integer deckId = 1;
        when(dataService.getReviewableCards(deckId)).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving cards: Database connection failed"));

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    // Tests for updateCard endpoint
    @Test
    @DisplayName("POST /api/update-card updates flashcard when remembered")
    void updateCard_rememberedTrue_increasesStage() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 3);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Flashcard updated successfully."));

        verify(dataService, times(1)).getFlashcardById(cardId);
        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(4) && // Stage increased from 3 to 4
                        card.getLastReviewed() > 0
        ));
    }

    @Test
    @DisplayName("POST /api/update-card updates flashcard when not remembered")
    void updateCard_rememberedFalse_decreasesStage() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 3);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", false
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Flashcard updated successfully."));

        verify(dataService, times(1)).getFlashcardById(cardId);
        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(2) // Stage decreased from 3 to 2
        ));
    }

    @Test
    @DisplayName("POST /api/update-card caps stage at maximum value 6")
    void updateCard_stageCapAtMaximum() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 6);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(6) // Stage stays at 6
        ));
    }

    @Test
    @DisplayName("POST /api/update-card caps stage at minimum value 1")
    void updateCard_stageCapAtMinimum() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 1);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", false
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(1) // Stage stays at 1
        ));
    }

    @Test
    @DisplayName("POST /api/update-card returns 404 when flashcard not found")
    void updateCard_flashcardNotFound() throws Exception {
        Long cardId = 999L;
        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Flashcard not found."));

        verify(dataService, times(1)).getFlashcardById(cardId);
        verify(dataService, never()).addFlashcard(any());
    }

    @Test
    @DisplayName("POST /api/update-card handles service exception")
    void updateCard_serviceException() throws Exception {
        Long cardId = 1L;
        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error updating flashcard: Database error"));

        verify(dataService, times(1)).getFlashcardById(cardId);
    }

    // Tests for getAllCards endpoint
    @Test
    @DisplayName("GET /api/all-cards/{deckId} returns all cards successfully")
    void getAllCards_success() throws Exception {
        Integer deckId = 1;
        List<Flashcard> mockCards = Arrays.asList(
                createMockFlashcard(1, deckId, "Hello", "Bonjour", 2),
                createMockFlashcard(2, deckId, "Goodbye", "Au revoir", 1),
                createMockFlashcard(3, deckId, "Thank you", "Merci", 3)
        );

        when(dataService.getAllCards(deckId)).thenReturn(mockCards);

        mockMvc.perform(get("/api/all-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].cardId").value(1))
                .andExpect(jsonPath("$[1].cardId").value(2))
                .andExpect(jsonPath("$[2].cardId").value(3));

        verify(dataService, times(1)).getAllCards(deckId);
    }

    @Test
    @DisplayName("GET /api/all-cards/{deckId} handles service exception")
    void getAllCards_serviceException() throws Exception {
        Integer deckId = 1;
        when(dataService.getAllCards(deckId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/all-cards/{deckId}", deckId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving all cards: Database error"));

        verify(dataService, times(1)).getAllCards(deckId);
    }

    // Tests for getUserDecks endpoint
    @Test
    @DisplayName("GET /api/user-decks/{userId} returns user decks successfully")
    void getUserDecks_success() throws Exception {
        Integer userId = 1;
        List<Deck> mockDecks = Arrays.asList(
                createMockDeck(1, userId, "French Vocabulary", "Basic French words"),
                createMockDeck(2, userId, "Spanish Grammar", "Grammar rules and examples")
        );

        when(dataService.getDecksByUserId(userId)).thenReturn(mockDecks);

        mockMvc.perform(get("/api/user-decks/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].deckId").value(1))
                .andExpect(jsonPath("$[0].name").value("French Vocabulary"))
                .andExpect(jsonPath("$[1].deckId").value(2))
                .andExpect(jsonPath("$[1].name").value("Spanish Grammar"));

        verify(dataService, times(1)).getDecksByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/user-decks/{userId} returns empty list when no decks found")
    void getUserDecks_emptyList() throws Exception {
        Integer userId = 1;
        when(dataService.getDecksByUserId(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user-decks/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(dataService, times(1)).getDecksByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/user-decks/{userId} handles service exception")
    void getUserDecks_serviceException() throws Exception {
        Integer userId = 1;
        when(dataService.getDecksByUserId(userId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/user-decks/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving decks: Database error"));

        verify(dataService, times(1)).getDecksByUserId(userId);
    }

    // Tests for getUsername endpoint
    @Test
    @DisplayName("GET /api/get-username/{userId} returns username successfully")
    void getUsername_success() throws Exception {
        Long userId = 1L;
        User mockUser = createMockUser(1, "john_doe");

        when(dataService.getUserById(userId)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/get-username/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("john_doe"));

        verify(dataService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/get-username/{userId} returns 404 when user not found")
    void getUsername_userNotFound() throws Exception {
        Long userId = 999L;
        when(dataService.getUserById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/get-username/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found."));

        verify(dataService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/get-username/{userId} handles service exception")
    void getUsername_serviceException() throws Exception {
        Long userId = 1L;
        when(dataService.getUserById(userId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/get-username/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving user: Database error"));

        verify(dataService, times(1)).getUserById(userId);
    }
}
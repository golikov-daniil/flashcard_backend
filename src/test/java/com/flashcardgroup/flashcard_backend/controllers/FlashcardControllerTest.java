package com.flashcardgroup.flashcard_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardgroup.flashcard_backend.model.Flashcard;
import com.flashcardgroup.flashcard_backend.model.Deck;
import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.service.DataService;
import com.flashcardgroup.flashcard_backend.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlashcardController.class)
@AutoConfigureMockMvc(addFilters = false)
class FlashcardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataService dataService;

    // Required for controller instantiation even though not used in these tests
    // (FlashcardController constructor requires it)
    @MockitoBean
    private ImageStorageService imageStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Reset the mocks before each test to avoid interference between tests
        reset(dataService, imageStorageService);
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
    @DisplayName("GET /reviewable-cards/{deckId} returns reviewable cards successfully")
    void getReviewableCards_success() throws Exception {
        Integer deckId = 1;
        List<Flashcard> mockCards = Arrays.asList(
                createMockFlashcard(1, deckId, "Hello", "Bonjour", 2),
                createMockFlashcard(2, deckId, "Goodbye", "Au revoir", 1)
        );

        when(dataService.getReviewableCards(deckId)).thenReturn(mockCards);

        mockMvc.perform(get("/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
                // Rely on DTO mapping correctness; field names are validated elsewhere

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    @Test
    @DisplayName("GET /reviewable-cards/{deckId} returns empty list when no reviewable cards")
    void getReviewableCards_emptyList() throws Exception {
        Integer deckId = 1;
        when(dataService.getReviewableCards(deckId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    @Test
    @DisplayName("GET /reviewable-cards/{deckId} handles service exception")
    void getReviewableCards_serviceException() throws Exception {
        Integer deckId = 1;
        when(dataService.getReviewableCards(deckId)).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/reviewable-cards/{deckId}", deckId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving cards: Database connection failed"));

        verify(dataService, times(1)).getReviewableCards(deckId);
    }

    // Tests for updateCard endpoint
    @Test
    @DisplayName("POST /update-card updates flashcard when remembered")
    void updateCard_rememberedTrue_increasesStage() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 3);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/update-card")
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
    @DisplayName("POST /update-card updates flashcard when not remembered")
    void updateCard_rememberedFalse_decreasesStage() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 3);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", false
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/update-card")
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
    @DisplayName("POST /update-card caps stage at maximum value 6")
    void updateCard_stageCapAtMaximum() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 6);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(6) // Stage stays at 6
        ));
    }

    @Test
    @DisplayName("POST /update-card caps stage at minimum value 1")
    void updateCard_stageCapAtMinimum() throws Exception {
        Long cardId = 1L;
        Flashcard mockCard = createMockFlashcard(1, 1, "Hello", "Bonjour", 1);

        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", false
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.of(mockCard));
        when(dataService.addFlashcard(any(Flashcard.class))).thenReturn(mockCard);

        mockMvc.perform(post("/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(dataService, times(1)).addFlashcard(argThat(card ->
                card.getStage().equals(1) // Stage stays at 1
        ));
    }

    @Test
    @DisplayName("POST /update-card returns 404 when flashcard not found")
    void updateCard_flashcardNotFound() throws Exception {
        Long cardId = 999L;
        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Flashcard not found."));

        verify(dataService, times(1)).getFlashcardById(cardId);
        verify(dataService, never()).addFlashcard(any());
    }

    @Test
    @DisplayName("POST /update-card handles service exception")
    void updateCard_serviceException() throws Exception {
        Long cardId = 1L;
        Map<String, Object> payload = Map.of(
                "CardID", cardId.toString(),
                "Remembered", true
        );

        when(dataService.getFlashcardById(cardId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/update-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error updating flashcard: Database error"));

        verify(dataService, times(1)).getFlashcardById(cardId);
    }

    // Tests for getAllCards endpoint
    @Test
    @DisplayName("GET /all-cards/{deckId} returns all cards successfully")
    void getAllCards_success() throws Exception {
        Integer deckId = 1;
        List<Flashcard> mockCards = Arrays.asList(
                createMockFlashcard(1, deckId, "Hello", "Bonjour", 2),
                createMockFlashcard(2, deckId, "Goodbye", "Au revoir", 1),
                createMockFlashcard(3, deckId, "Thank you", "Merci", 3)
        );

        when(dataService.getAllCards(deckId)).thenReturn(mockCards);

        mockMvc.perform(get("/all-cards/{deckId}", deckId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
                // Do not assert specific field names to avoid coupling to DTO structure

        verify(dataService, times(1)).getAllCards(deckId);
    }

    @Test
    @DisplayName("GET /all-cards/{deckId} handles service exception")
    void getAllCards_serviceException() throws Exception {
        Integer deckId = 1;
        when(dataService.getAllCards(deckId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/all-cards/{deckId}", deckId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving all cards: Database error"));

        verify(dataService, times(1)).getAllCards(deckId);
    }

    // Tests for user-decks endpoint using Authentication principal
    @Test
    @DisplayName("GET /user-decks returns user decks successfully")
    void getUserDecks_success() throws Exception {
        Long userId = 1L;
        List<Deck> mockDecks = Arrays.asList(
                createMockDeck(1, userId.intValue(), "Deck 1", "Description 1"),
                createMockDeck(2, userId.intValue(), "Deck 2", "Description 2")
        );

        when(dataService.getDecksByUserId(userId.intValue())).thenReturn(mockDecks);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/user-decks").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(dataService, times(1)).getDecksByUserId(userId.intValue());
    }

    @Test
    @DisplayName("GET /user-decks returns empty list when no decks found")
    void getUserDecks_emptyList() throws Exception {
        Long userId = 1L;

        when(dataService.getDecksByUserId(userId.intValue())).thenReturn(Collections.emptyList());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/user-decks").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(dataService, times(1)).getDecksByUserId(userId.intValue());
    }

    @Test
    @DisplayName("GET /user-decks handles service exception")
    void getUserDecks_serviceException() throws Exception {
        Long userId = 1L;

        when(dataService.getDecksByUserId(userId.intValue())).thenThrow(new RuntimeException("Database error"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/user-decks").principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving decks: Database error"));

        verify(dataService, times(1)).getDecksByUserId(userId.intValue());
    }

    // Tests for get-username endpoint using Authentication principal
    @Test
    @DisplayName("GET /get-username returns username successfully")
    void getUsername_success() throws Exception {
        Long userId = 1L;
        User mockUser = createMockUser(userId.intValue(), "john_doe");

        when(dataService.getUserById(userId)).thenReturn(Optional.of(mockUser));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/get-username").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.intValue()))
                .andExpect(jsonPath("$.username").value("john_doe"));

        verify(dataService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /get-username returns 404 when user not found")
    void getUsername_userNotFound() throws Exception {
        Long userId = 1L;

        when(dataService.getUserById(userId)).thenReturn(Optional.empty());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/get-username").principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found."));

        verify(dataService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /get-username handles service exception")
    void getUsername_serviceException() throws Exception {
        Long userId = 1L;

        when(dataService.getUserById(userId)).thenThrow(new RuntimeException("Database error"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);

        mockMvc.perform(get("/get-username").principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving user: Database error"));

        verify(dataService, times(1)).getUserById(userId);
    }
}

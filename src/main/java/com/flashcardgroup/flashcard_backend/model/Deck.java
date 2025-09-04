// src/main/java/com/example/flashcards/model/Deck.java
package com.flashcardgroup.flashcard_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "decks",
        indexes = {
                @Index(name = "idx_decks_user", columnList = "userid")
        }
)
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deckid")
    private Integer deckId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", referencedColumnName = "userid", nullable = false, foreignKey = @ForeignKey(name = "fk_decks_user"))
    private User user;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 1000)
    @Column(name = "Description", length = 1000)
    private String description;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Flashcard> flashcards = new HashSet<>();

    public Deck() {}

    public Deck(Integer deckId, User user, String name, String description) {
        this.deckId = deckId;
        this.user = user;
        this.name = name;
        this.description = description;
    }

    // Getters and setters

    public Integer getDeckId() { return deckId; }
    public void setDeckId(Integer deckId) { this.deckId = deckId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Flashcard> getFlashcards() { return flashcards; }
    public void setFlashcards(Set<Flashcard> flashcards) { this.flashcards = flashcards; }
}

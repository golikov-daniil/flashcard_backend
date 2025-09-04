// src/main/java/com/example/flashcards/model/Flashcard.java
package com.flashcardgroup.flashcard_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
        name = "flashcards",
        indexes = {
                @Index(name = "idx_flashcards_deck", columnList = "deckid"),
                @Index(name = "idx_flashcards_stage", columnList = "stage"),
                @Index(name = "idx_flashcards_lastreviewed", columnList = "last_reviewed")
        }
)
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cardid")
    private Integer cardId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "deckid", referencedColumnName = "deckid", nullable = false, foreignKey = @ForeignKey(name = "fk_flashcards_deck"))
    private Deck deck;

    @NotBlank
    @Column(name = "front", nullable = false, columnDefinition = "TEXT")
    private String front;

    @NotBlank
    @Column(name = "back", nullable = false, columnDefinition = "TEXT")
    private String back;

    // Stored as UNIX seconds to match your original type
    @NotNull
    @Column(name = "last_reviewed", nullable = false)
    private Long lastReviewed;

    @NotNull
    @Column(name = "stage", nullable = false)
    private Integer stage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "example", columnDefinition = "TEXT")
    private String example;

    @Column(name = "occurrence_indices", columnDefinition = "TEXT")
    private String occurrenceIndices;

    @Column(name = "synonyms", columnDefinition = "TEXT")
    private String synonyms;

    @Column(name = "part_of_speech", length = 100)
    private String partOfSpeech;

    @Column(name = "classifiers", columnDefinition = "TEXT")
    private String classifiers;

    @Column(name = "image_no", length = 100)
    private String imageNo;

    public Flashcard() {}

    public Flashcard(
            Integer cardId,
            Deck deck,
            String front,
            String back,
            Long lastReviewed,
            Integer stage,
            String example,
            String metadata,
            String occurrenceIndices,
            String synonyms,
            String partOfSpeech,
            String classifiers,
            String imageNo
    ) {
        this.cardId = cardId;
        this.deck = deck;
        this.front = front;
        this.back = back;
        this.lastReviewed = lastReviewed;
        this.stage = stage;
        this.metadata = metadata;
        this.example = example;
        this.occurrenceIndices = occurrenceIndices;
        this.synonyms = synonyms;
        this.partOfSpeech = partOfSpeech;
        this.classifiers = classifiers;
        this.imageNo = imageNo;
    }

    // Review schedule identical to your TS logic
    private static final long DAY = 86400;
    private static final Map<Integer, Long> REVIEW_INTERVALS = Map.of(
            1, DAY,     // 1 day
            2, 4L * DAY,     // 4 days
            3, 7L * DAY,     // 7 days
            4, 14L * DAY,    // 14 days
            5, 60L * DAY,    // 60 days
            6, 180L * DAY    // 6 months approx
    );

    @Transient
    public boolean shouldReview() {
        Long interval = REVIEW_INTERVALS.get(this.stage);
        if (interval == null) {
            throw new IllegalStateException("Review interval not found for stage " + this.stage);
        }
        long now = Instant.now().getEpochSecond();
        long nextTime = this.lastReviewed + interval;
        return now >= nextTime;
    }

    // Getters and setters

    public Integer getCardId() { return cardId; }
    public void setCardId(Integer cardId) { this.cardId = cardId; }

    public Deck getDeck() { return deck; }
    public void setDeck(Deck deck) { this.deck = deck; }

    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }

    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }

    public Long getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(Long lastReviewed) { this.lastReviewed = lastReviewed; }

    public Integer getStage() { return stage; }
    public void setStage(Integer stage) { this.stage = stage; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getOccurrenceIndices() { return occurrenceIndices; }
    public void setOccurrenceIndices(String occurrenceIndices) { this.occurrenceIndices = occurrenceIndices; }

    public String getSynonyms() { return synonyms; }
    public void setSynonyms(String synonyms) { this.synonyms = synonyms; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getClassifiers() { return classifiers; }
    public void setClassifiers(String classifiers) { this.classifiers = classifiers; }

    public String getImageNo() { return imageNo; }
    public void setImageNo(String imageNo) { this.imageNo = imageNo; }
}

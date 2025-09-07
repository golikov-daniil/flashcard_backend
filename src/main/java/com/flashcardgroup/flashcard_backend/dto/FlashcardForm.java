package com.flashcardgroup.flashcard_backend.dto;

import org.springframework.web.multipart.MultipartFile;

// Matches your FormData keys exactly
public record FlashcardForm(
        Integer DeckID,
        String Front,
        String Back,
        String Metadata,
        String Example,
        String OccurrenceIndices,
        String Synonyms,
        String PartOfSpeech,
        String Classifiers,
        MultipartFile image
) {}

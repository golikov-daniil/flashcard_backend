// src/main/java/com/example/flashcards/repo/UserRepository.java
package com.flashcardgroup.flashcard_backend.repo;

import com.flashcardgroup.flashcard_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}

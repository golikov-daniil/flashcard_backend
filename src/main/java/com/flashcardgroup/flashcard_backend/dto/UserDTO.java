package com.flashcardgroup.flashcard_backend.dto;

import com.flashcardgroup.flashcard_backend.model.User;

/**
 * DTO for User entity that excludes sensitive information like password hash
 * and avoids circular references.
 */
public record UserDTO(
    Integer userId,
    String username,
    String email
) {
    /**
     * Creates a UserDTO from a User entity.
     * 
     * @param user the User entity
     * @return a new UserDTO containing non-sensitive user information
     */
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
            user.getUserId(),
            user.getUsername(),
            user.getEmail()
        );
    }
}

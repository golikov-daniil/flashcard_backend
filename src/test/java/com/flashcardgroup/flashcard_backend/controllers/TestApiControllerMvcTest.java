package com.flashcardgroup.flashcard_backend.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestApiController.class)
class TestApiControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/test/ping returns { status: 'ok', time: <string> }")
    void ping_returnsOkAndTime() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.time").isString()); // ensure the field exists and is a string
    }

    @Test
    @DisplayName("GET /api/test/hello without name returns 'Hello, world!'")
    void hello_defaultsToWorld() throws Exception {
        mockMvc.perform(get("/api/test/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Hello, world!"));
    }

    @Test
    @DisplayName("GET /api/test/hello?name=Alice returns 'Hello, Alice!'")
    void hello_withName() throws Exception {
        mockMvc.perform(get("/api/test/hello").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Hello, Alice!"));
    }
}

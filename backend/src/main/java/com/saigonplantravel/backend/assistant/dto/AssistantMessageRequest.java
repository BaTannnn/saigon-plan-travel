package com.saigonplantravel.backend.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssistantMessageRequest(
        @NotBlank @Size(min = 3, max = 1000) String message,
        @NotNull @Size(max = 10) List<@Valid ConversationMessage> history) {

    public record ConversationMessage(@NotNull Role role, @NotBlank @Size(max = 2000) String content) {}

    public enum Role {
        USER,
        ASSISTANT
    }
}

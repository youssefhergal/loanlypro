package com.projetfilrouge.loanmanagement.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiChatRequest {

    @NotNull
    @Size(min = 1, max = 50)
    @Valid
    private List<AiMessageDto> messages;

    @Getter
    @NoArgsConstructor
    public static class AiMessageDto {

        @NotNull
        @Pattern(regexp = "user|assistant")
        private String role;

        @NotNull
        @Size(max = 10000)
        private String content;
    }
}

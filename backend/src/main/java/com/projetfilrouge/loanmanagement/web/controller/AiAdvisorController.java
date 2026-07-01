package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AiAdvisorService;
import com.projetfilrouge.loanmanagement.web.dto.request.AiChatRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.AiChatResponse;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Advisor", description = "Conseiller financier IA")
public class AiAdvisorController {

    private final AiAdvisorService aiAdvisorService;

    @PostMapping("/chat")
    @Operation(summary = "Envoyer un message au conseiller financier IA")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            Principal principal) {
        try {
            String content = aiAdvisorService.chat(request.getMessages());
            return ResponseEntity.ok(AiChatResponse.builder().content(content).build());
        } catch (BusinessRuleException e) {
            return ResponseEntity.ok(AiChatResponse.builder().content(e.getMessage()).build());
        } catch (RuntimeException e) {
            return ResponseEntity.ok(AiChatResponse.builder().content(e.getMessage()).build());
        }
    }
}

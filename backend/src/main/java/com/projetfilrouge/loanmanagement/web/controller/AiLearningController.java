package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.AiLearningService;
import com.projetfilrouge.loanmanagement.web.dto.request.GenerateRoadmapRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.LearningSessionDto;
import com.projetfilrouge.loanmanagement.web.dto.response.StepContentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/ai/learning")
@RequiredArgsConstructor
@Tag(name = "AI Learning", description = "Formation sur mesure propulsée par IA")
public class AiLearningController {

    private final AiLearningService aiLearningService;

    @PostMapping("/roadmap")
    @Operation(summary = "Générer un nouveau parcours d'apprentissage")
    public ResponseEntity<LearningSessionDto> generateRoadmap(
            @Valid @RequestBody GenerateRoadmapRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                aiLearningService.generateRoadmap(request.getObjective(), principal.getName()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Récupérer les parcours de l'utilisateur")
    public ResponseEntity<List<LearningSessionDto>> getSessions(Principal principal) {
        return ResponseEntity.ok(aiLearningService.getUserSessions(principal.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Récupérer un parcours par son ID")
    public ResponseEntity<LearningSessionDto> getSession(
            @PathVariable Long sessionId,
            Principal principal) {
        return ResponseEntity.ok(aiLearningService.getSession(sessionId, principal.getName()));
    }

    @PostMapping("/sessions/{sessionId}/steps/{stepIndex}/content")
    @Operation(summary = "Générer le contenu d'une étape")
    public ResponseEntity<StepContentResponse> getStepContent(
            @PathVariable Long sessionId,
            @PathVariable int stepIndex,
            Principal principal) {
        String content = aiLearningService.generateStepContent(sessionId, stepIndex, principal.getName());
        return ResponseEntity.ok(StepContentResponse.builder().content(content).build());
    }

    @PatchMapping("/sessions/{sessionId}/steps/{stepIndex}/complete")
    @Operation(summary = "Marquer une étape comme terminée")
    public ResponseEntity<LearningSessionDto> completeStep(
            @PathVariable Long sessionId,
            @PathVariable int stepIndex,
            Principal principal) {
        return ResponseEntity.ok(
                aiLearningService.completeStep(sessionId, stepIndex, principal.getName()));
    }
}

package com.projetfilrouge.loanmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.entity.LearningSession;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.LearningSessionRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.LearningSessionDto;
import com.projetfilrouge.loanmanagement.web.dto.response.LearningStepDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiLearningService {

    private static final String ROADMAP_SYSTEM_PROMPT = """
            Tu es un expert pédagogue en éducation financière personnelle.
            Tu génères des parcours d'apprentissage structurés, progressifs et adaptés aux débutants.
            Tu réponds UNIQUEMENT avec du JSON valide, sans texte avant ou après, sans bloc markdown.
            """;

    private static final String STEP_SYSTEM_PROMPT = """
            Tu es un expert pédagogue en éducation financière personnelle.
            Tu génères des contenus pédagogiques complets, pratiques et engageants en français.
            Utilise **Titre** pour les titres de section en gras.
            Utilise des tirets (-) pour les listes.
            Donne des exemples chiffrés et concrets.
            """;

    private final LearningSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ai.anthropic-api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-haiku-4-5-20251001}")
    private String model;

    public AiLearningService(LearningSessionRepository sessionRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    @Transactional
    public LearningSessionDto generateRoadmap(String objective, String userEmail) {
        validateApiKey();
        User user = getUser(userEmail);

        String prompt = """
                L'utilisateur souhaite apprendre: "%s"

                Génère un parcours d'apprentissage personnalisé de 5 à 7 étapes progressives.
                Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ou après.

                Format exact:
                [{"index":0,"title":"...","description":"...","estimatedMinutes":15}]

                Les étapes doivent être progressives, concrètes et adaptées à quelqu'un qui commence.
                """.formatted(objective);

        String rawJson = callClaude(ROADMAP_SYSTEM_PROMPT, prompt, 1024);
        String cleanedJson = extractJson(rawJson);

        List<LearningStepDto> steps;
        try {
            steps = objectMapper.readValue(cleanedJson, new TypeReference<List<LearningStepDto>>() {});
        } catch (Exception e) {
            log.error("Erreur parsing roadmap JSON: {}", cleanedJson, e);
            throw new RuntimeException("Impossible de générer le parcours. Veuillez réessayer.");
        }

        String roadmapJson;
        try {
            roadmapJson = objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            throw new RuntimeException("Erreur interne lors de la sauvegarde du parcours.");
        }

        LearningSession session = LearningSession.builder()
                .user(user)
                .objective(objective)
                .roadmapJson(roadmapJson)
                .build();

        session = sessionRepository.save(session);
        return toDto(session, steps);
    }

    public String generateStepContent(Long sessionId, int stepIndex, String userEmail) {
        validateApiKey();
        LearningSession session = sessionRepository.findByIdAndUserId(sessionId, getUser(userEmail).getId())
                .orElseThrow(() -> new BusinessRuleException("Session introuvable."));

        List<LearningStepDto> steps = parseSteps(session.getRoadmapJson());
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            throw new BusinessRuleException("Étape invalide.");
        }

        LearningStepDto step = steps.get(stepIndex);

        String prompt = """
                Objectif de l'apprenant: %s
                Étape %d sur %d: %s
                Description: %s

                Génère un contenu pédagogique complet pour cette étape.
                Structure obligatoire (utilise exactement ces titres en gras):

                **Introduction**
                (2-3 phrases d'accroche motivantes)

                **Ce que vous allez apprendre**
                - point clé 1
                - point clé 2
                - point clé 3

                **Concepts essentiels**
                (Explications détaillées avec exemples chiffrés concrets)

                **Exemple pratique**
                (Un scénario réaliste avec chiffres)

                **À retenir**
                - point 1
                - point 2
                - point 3

                **Exercice**
                (Une action concrète à faire maintenant)
                """.formatted(
                session.getObjective(),
                stepIndex + 1, steps.size(),
                step.getTitle(),
                step.getDescription()
        );

        return callClaude(STEP_SYSTEM_PROMPT, prompt, 2048);
    }

    @Transactional
    public LearningSessionDto completeStep(Long sessionId, int stepIndex, String userEmail) {
        LearningSession session = sessionRepository.findByIdAndUserId(sessionId, getUser(userEmail).getId())
                .orElseThrow(() -> new BusinessRuleException("Session introuvable."));

        List<LearningStepDto> steps = parseSteps(session.getRoadmapJson());
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            throw new BusinessRuleException("Étape invalide.");
        }

        Set<Integer> completed = parseCompletedSteps(session.getCompletedSteps());
        completed.add(stepIndex);
        session.setCompletedSteps(serializeCompletedSteps(completed));

        if (completed.size() == steps.size()) {
            session.setStatus(LearningSession.SessionStatus.COMPLETED);
        }

        session = sessionRepository.save(session);
        return toDto(session, steps);
    }

    public List<LearningSessionDto> getUserSessions(String userEmail) {
        User user = getUser(userEmail);
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(s -> toDto(s, parseSteps(s.getRoadmapJson())))
                .collect(Collectors.toList());
    }

    public LearningSessionDto getSession(Long sessionId, String userEmail) {
        LearningSession session = sessionRepository.findByIdAndUserId(sessionId, getUser(userEmail).getId())
                .orElseThrow(() -> new BusinessRuleException("Session introuvable."));
        return toDto(session, parseSteps(session.getRoadmapJson()));
    }

    private String callClaude(String systemPrompt, String userMessage, int maxTokens) {
        var body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        try {
            var response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AnthropicResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new RuntimeException("Réponse vide de l'API Anthropic");
            }
            return response.content().get(0).text();
        } catch (RuntimeException e) {
            log.error("Erreur API Anthropic", e);
            throw new RuntimeException("Le service IA est temporairement indisponible. Veuillez réessayer.");
        }
    }

    private String extractJson(String raw) {
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n') + 1;
            int end = cleaned.lastIndexOf("```");
            if (end > start) cleaned = cleaned.substring(start, end).strip();
        }
        int arrayStart = cleaned.indexOf('[');
        int arrayEnd = cleaned.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            cleaned = cleaned.substring(arrayStart, arrayEnd + 1);
        }
        return cleaned;
    }

    private List<LearningStepDto> parseSteps(String roadmapJson) {
        if (roadmapJson == null || roadmapJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(roadmapJson, new TypeReference<List<LearningStepDto>>() {});
        } catch (Exception e) {
            log.error("Erreur parsing steps", e);
            return List.of();
        }
    }

    private Set<Integer> parseCompletedSteps(String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        Set<Integer> result = new HashSet<>();
        for (String part : raw.split(",")) {
            try { result.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private String serializeCompletedSteps(Set<Integer> completed) {
        return completed.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private LearningSessionDto toDto(LearningSession session, List<LearningStepDto> steps) {
        Set<Integer> completed = parseCompletedSteps(session.getCompletedSteps());
        return LearningSessionDto.builder()
                .id(session.getId())
                .objective(session.getObjective())
                .steps(steps)
                .completedSteps(completed)
                .status(session.getStatus())
                .completedCount(completed.size())
                .totalSteps(steps.size())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("Utilisateur introuvable."));
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessRuleException("Le service IA n'est pas configuré.");
        }
    }

    private record AnthropicResponse(List<ContentBlock> content) {}
    private record ContentBlock(String type, String text) {}
}

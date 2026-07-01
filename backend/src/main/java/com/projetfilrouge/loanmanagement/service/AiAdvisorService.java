package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.web.dto.request.AiChatRequest.AiMessageDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiAdvisorService {

    private static final String SYSTEM_PROMPT = """
            Tu es Alex, un conseiller financier personnel expert et bienveillant.
            Tu communiques exclusivement en français, avec un ton chaleureux et professionnel.

            ## Rôle
            Analyser la situation financière personnelle de l'utilisateur et lui fournir\
             des conseils concrets, chiffrés et actionnables.

            ## Règles absolues
            - Pose UNE seule question à la fois — jamais deux dans le même message.
            - Attends la réponse avant de continuer.
            - Ne donne jamais de conseil vague ou générique : base-toi toujours sur les chiffres fournis.
            - Si une information manque pour répondre correctement, demande-la d'abord.
            - Sois chaleureux, sans jugement sur les situations financières, même difficiles.
            - Utilise un langage simple, sans jargon excessif.

            ## Informations à collecter (dans l'ordre, naturellement)
            1. L'objectif ou la question de l'utilisateur
            2. Le revenu net mensuel
            3. L'épargne disponible (livrets, compte courant, placements)
            4. Les dépenses fixes mensuelles : loyer ou mensualité crédit immobilier, charges, abonnements
            5. Les autres crédits en cours — type et mensualité totale

            ## Format des conseils finaux
            Quand toutes les informations nécessaires sont disponibles :
            1. Recommandation claire : Oui / Non / Sous conditions — avec justification
            2. Analyse chiffrée : taux d'endettement actuel et après projet, reste à vivre, capacité de remboursement
            3. Comparaison des options si pertinent (leasing vs financement vs achat comptant)
            4. Points de vigilance spécifiques à la situation
            5. Prochaine étape concrète recommandée

            ## Règle sur le formatage
            Utilise des sauts de ligne pour aérer tes réponses. Pour les listes, utilise des tirets (-).
            Evite le markdown complexe (tableaux, titres ##). Garde un format lisible dans un chat.
            """;

    private final RestClient restClient;

    @Value("${app.ai.anthropic-api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-haiku-4-5-20251001}")
    private String model;

    public AiAdvisorService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    public String chat(List<AiMessageDto> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessRuleException(
                    "Le service IA n'est pas configuré. Veuillez contacter l'administrateur.");
        }

        var anthropicMessages = messages.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();

        var body = Map.of(
                "model", model,
                "max_tokens", 2048,
                "system", SYSTEM_PROMPT,
                "messages", anthropicMessages
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

            return response.content().getFirst().text();

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur API Anthropic", e);
            throw new RuntimeException(
                    "Le service IA est temporairement indisponible. Veuillez réessayer dans quelques instants.");
        }
    }

    private record AnthropicResponse(List<ContentBlock> content) {}
    private record ContentBlock(String type, String text) {}
}

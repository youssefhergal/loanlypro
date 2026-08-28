package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.MessagingService;
import com.projetfilrouge.loanmanagement.web.dto.request.SendMessageRequest;
import com.projetfilrouge.loanmanagement.web.dto.request.StartConversationRequest;
import com.projetfilrouge.loanmanagement.web.dto.response.ContactDto;
import com.projetfilrouge.loanmanagement.web.dto.response.ConversationDto;
import com.projetfilrouge.loanmanagement.web.dto.response.MessageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "Messagerie en temps réel")
public class MessagingController {

    private final MessagingService messagingService;

    @GetMapping("/contacts")
    @Operation(summary = "Liste des contacts disponibles")
    public ResponseEntity<List<ContactDto>> getContacts(Principal principal) {
        return ResponseEntity.ok(messagingService.getContacts(principal.getName()));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Liste des conversations de l'utilisateur courant")
    public ResponseEntity<List<ConversationDto>> getConversations(Principal principal) {
        return ResponseEntity.ok(messagingService.getConversations(principal.getName()));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Démarrer ou rejoindre une conversation")
    public ResponseEntity<ConversationDto> startConversation(
            @Valid @RequestBody StartConversationRequest request,
            Principal principal) {
        ConversationDto conv = messagingService.getOrCreateConversation(
                principal.getName(), request.getTargetUserId());

        // send the initial message if provided
        messagingService.sendMessage(principal.getName(), conv.getId(), request.getInitialMessage());

        // return updated conversation with preview
        List<ConversationDto> updated = messagingService.getConversations(principal.getName());
        return updated.stream()
                .filter(c -> c.getId().equals(conv.getId()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(conv));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Historique des messages d'une conversation")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(messagingService.getMessages(principal.getName(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Envoyer un message texte")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                messagingService.sendMessage(principal.getName(), id, request.getContent()));
    }

    @PostMapping(value = "/conversations/{id}/messages/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envoyer un message avec pièce jointe (multipart)")
    public ResponseEntity<MessageDto> sendMessageWithAttachment(
            @PathVariable Long id,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Principal principal) {
        return ResponseEntity.ok(
                messagingService.sendMessage(principal.getName(), id, content, file));
    }

    @GetMapping("/messages/{messageId}/attachment")
    @Operation(summary = "Télécharger la pièce jointe d'un message")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long messageId,
            Principal principal) {
        String[] meta = new String[2];
        byte[] data = messagingService.downloadAttachment(principal.getName(), messageId, meta);
        String filename = meta[0] != null ? meta[0] : "fichier";
        String ct = meta[1] != null ? meta[1] : "application/octet-stream";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ct));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "Marquer tous les messages d'une conversation comme lus")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Principal principal) {
        messagingService.markRead(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre total de messages non lus")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        long count = messagingService.getTotalUnread(principal.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}

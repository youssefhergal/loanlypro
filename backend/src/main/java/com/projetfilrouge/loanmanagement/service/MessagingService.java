package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.Conversation;
import com.projetfilrouge.loanmanagement.entity.Message;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.ConversationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.MessageRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.ContactDto;
import com.projetfilrouge.loanmanagement.web.dto.response.ConversationDto;
import com.projetfilrouge.loanmanagement.web.dto.response.MessageDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.LoanStorageException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain", "application/zip"
    );

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.storage.messaging-attachments-dir:uploads/messaging-attachments}")
    private String attachmentsDir;

    // ── Contacts ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContactDto> getContacts(String currentEmail) {
        User me = findByEmail(currentEmail);
        Set<String> myRoles = roleNames(me);
        List<User> contacts = new ArrayList<>();

        if (myRoles.contains("ROLE_CLIENT")) {
            // own assigned advisors + all admins
            contacts.addAll(loanApplicationRepository.findAdvisorsByClientEmail(me.getEmail()));
            contacts.addAll(userRepository.findAllAdmins());

        } else if (myRoles.contains("ROLE_CONSEILLER")) {
            // assigned clients + all admins + all other conseillers
            contacts.addAll(loanApplicationRepository.findClientsByAdvisorId(me.getId()));
            contacts.addAll(userRepository.findAllAdmins());
            contacts.addAll(userRepository.findAllConseillers());

        } else if (myRoles.contains("ROLE_ADMIN")) {
            // everyone except other admins if wanted — here: all conseillers + all admins
            contacts.addAll(userRepository.findAllConseillers());
            contacts.addAll(userRepository.findAllAdmins());
        }

        // exclude self
        return contacts.stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .distinct()
                .map(u -> ContactDto.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .role(primaryRole(u))
                        .build())
                .collect(Collectors.toList());
    }

    // ── Conversations ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(String currentEmail) {
        User me = findByEmail(currentEmail);
        return conversationRepository.findAllByParticipant(me.getId()).stream()
                .map(c -> toConversationDto(c, me))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationDto getOrCreateConversation(String currentEmail, Long targetUserId) {
        User me = findByEmail(currentEmail);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        validateCanMessage(me, target);

        Long uid1 = Math.min(me.getId(), target.getId());
        Long uid2 = Math.max(me.getId(), target.getId());

        Conversation conv = conversationRepository.findByParticipants(uid1, uid2)
                .orElseGet(() -> {
                    User p1 = me.getId().equals(uid1) ? me : target;
                    User p2 = me.getId().equals(uid2) ? me : target;
                    return conversationRepository.save(
                            Conversation.builder().participant1(p1).participant2(p2).build()
                    );
                });

        return toConversationDto(conv, me);
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(String currentEmail, Long conversationId) {
        User me = findByEmail(currentEmail);
        Conversation conv = findConversation(conversationId);
        assertParticipant(conv, me);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(m -> toMessageDto(m, me.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDto sendMessage(String currentEmail, Long conversationId, String content) {
        return sendMessage(currentEmail, conversationId, content, null);
    }

    @Transactional
    public MessageDto sendMessage(String currentEmail, Long conversationId, String content, MultipartFile file) {
        User me = findByEmail(currentEmail);
        Conversation conv = findConversation(conversationId);
        assertParticipant(conv, me);

        String attachmentPath = null;
        String attachmentName = null;
        String attachmentContentType = null;

        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_ATTACHMENT_SIZE) {
                throw new BusinessRuleException("Le fichier dépasse la taille maximale autorisée (10 Mo)");
            }
            String ct = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            if (!ALLOWED_TYPES.contains(ct)) {
                throw new BusinessRuleException("Type de fichier non autorisé");
            }
            String ext = "";
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String stored = UUID.randomUUID() + ext;
            try {
                Path dir = Paths.get(attachmentsDir);
                Files.createDirectories(dir);
                Files.write(dir.resolve(stored), file.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new LoanStorageException("Impossible de stocker la pièce jointe", e);
            }
            attachmentPath = stored;
            attachmentName = sanitizeFilename(original);
            attachmentContentType = ct;
        }

        if ((content == null || content.isBlank()) && attachmentPath == null) {
            throw new BusinessRuleException("Un message doit contenir du texte ou une pièce jointe");
        }

        Message msg = Message.builder()
                .conversation(conv)
                .sender(me)
                .content(content != null ? content : "")
                .attachmentPath(attachmentPath)
                .attachmentName(attachmentName)
                .attachmentContentType(attachmentContentType)
                .build();
        msg = messageRepository.save(msg);

        String previewBase = (content != null && !content.isBlank()) ? content : "📎 " + attachmentName;
        String preview = previewBase.length() > 100 ? previewBase.substring(0, 97) + "…" : previewBase;
        conv.setLastMessageAt(msg.getSentAt());
        conv.setLastMessagePreview(preview);
        conversationRepository.save(conv);

        User recipient = otherParticipant(conv, me);
        MessageDto dto = toMessageDto(msg, me.getId());

        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public byte[] downloadAttachment(String currentEmail, Long messageId, String[] outMeta) {
        User me = findByEmail(currentEmail);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable"));
        assertParticipant(msg.getConversation(), me);

        if (msg.getAttachmentPath() == null) {
            throw new ResourceNotFoundException("Ce message ne contient pas de pièce jointe");
        }
        Path file = Paths.get(attachmentsDir).resolve(msg.getAttachmentPath());
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Fichier introuvable sur le serveur");
        }
        outMeta[0] = msg.getAttachmentName();
        outMeta[1] = msg.getAttachmentContentType();
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de lire la pièce jointe", e);
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }

    @Transactional
    public void markRead(String currentEmail, Long conversationId) {
        User me = findByEmail(currentEmail);
        Conversation conv = findConversation(conversationId);
        assertParticipant(conv, me);
        messageRepository.markAllReadInConversation(conversationId, me.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public long getTotalUnread(String currentEmail) {
        User me = findByEmail(currentEmail);
        Long count = messageRepository.countTotalUnread(me.getId());
        return count == null ? 0L : count;
    }

    // ── Business rules ────────────────────────────────────────────────────────

    private void validateCanMessage(User sender, User target) {
        if (sender.getId().equals(target.getId())) {
            throw new BusinessRuleException("Impossible d'envoyer un message à soi-même");
        }
        Set<String> senderRoles = roleNames(sender);
        Set<String> targetRoles = roleNames(target);

        if (senderRoles.contains("ROLE_CLIENT")) {
            boolean targetIsAdmin = targetRoles.contains("ROLE_ADMIN");
            boolean targetIsAssignedAdvisor = targetRoles.contains("ROLE_CONSEILLER")
                    && loanApplicationRepository.existsByApplicantEmailAndAssignedAdvisorId(
                            sender.getEmail(), target.getId());
            if (!targetIsAdmin && !targetIsAssignedAdvisor) {
                throw new ForbiddenOperationException(
                        "Vous ne pouvez contacter que votre conseiller assigné ou un administrateur");
            }
        } else if (senderRoles.contains("ROLE_CONSEILLER") || senderRoles.contains("ROLE_ADMIN")) {
            boolean targetIsStaff = targetRoles.contains("ROLE_CONSEILLER") || targetRoles.contains("ROLE_ADMIN");
            boolean targetIsMyClient = targetRoles.contains("ROLE_CLIENT")
                    && loanApplicationRepository.existsByApplicantEmailAndAssignedAdvisorId(
                            target.getEmail(), sender.getId());
            if (!targetIsStaff && !targetIsMyClient) {
                throw new ForbiddenOperationException("Vous ne pouvez pas contacter cet utilisateur");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
    }

    private void assertParticipant(Conversation conv, User user) {
        boolean is1 = conv.getParticipant1().getId().equals(user.getId());
        boolean is2 = conv.getParticipant2().getId().equals(user.getId());
        if (!is1 && !is2) {
            throw new ForbiddenOperationException("Accès refusé à cette conversation");
        }
    }

    private User otherParticipant(Conversation conv, User me) {
        return conv.getParticipant1().getId().equals(me.getId())
                ? conv.getParticipant2()
                : conv.getParticipant1();
    }

    private ConversationDto toConversationDto(Conversation conv, User me) {
        User other = otherParticipant(conv, me);
        long unread = messageRepository.countUnread(conv.getId(), me.getId());
        return ConversationDto.builder()
                .id(conv.getId())
                .otherUserId(other.getId())
                .otherUserFirstName(other.getFirstName())
                .otherUserLastName(other.getLastName())
                .otherUserRole(primaryRole(other))
                .lastMessagePreview(conv.getLastMessagePreview())
                .lastMessageAt(conv.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private MessageDto toMessageDto(Message msg, Long currentUserId) {
        String downloadUrl = msg.getAttachmentPath() != null
                ? "/api/messaging/messages/" + msg.getId() + "/attachment"
                : null;
        return MessageDto.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .senderId(msg.getSender().getId())
                .senderFirstName(msg.getSender().getFirstName())
                .senderLastName(msg.getSender().getLastName())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .readAt(msg.getReadAt())
                .own(msg.getSender().getId().equals(currentUserId))
                .attachmentName(msg.getAttachmentName())
                .attachmentContentType(msg.getAttachmentContentType())
                .attachmentDownloadUrl(downloadUrl)
                .build();
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
    }

    private String primaryRole(User user) {
        Set<String> roles = roleNames(user);
        if (roles.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("ROLE_CONSEILLER")) return "ROLE_CONSEILLER";
        return "ROLE_CLIENT";
    }
}

package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;
import com.projetfilrouge.loanmanagement.entity.Notification;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.notification.NotificationContent;
import com.projetfilrouge.loanmanagement.repository.NotificationRepository;
import com.projetfilrouge.loanmanagement.repository.UserRepository;
import com.projetfilrouge.loanmanagement.web.dto.response.NotificationResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String REFERENCE_LOAN_APPLICATION = "LOAN_APPLICATION";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createInApp(
            User recipient,
            LoanApplicationEventType eventType,
            NotificationContent content,
            Long loanApplicationId
    ) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .eventType(eventType)
                .title(content.title())
                .message(content.message())
                .referenceType(REFERENCE_LOAN_APPLICATION)
                .referenceId(loanApplicationId)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getMyNotifications(String userEmail, Pageable pageable) {
        User user = requireUser(userEmail);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countUnread(String userEmail) {
        User user = requireUser(userEmail);
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markAsRead(String userEmail, Long notificationId) {
        User user = requireUser(userEmail);
        int updated = notificationRepository.markAsRead(notificationId, user.getId(), Instant.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification introuvable");
        }
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = requireUser(userEmail);
        notificationRepository.markAllAsRead(user.getId(), Instant.now());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ForbiddenOperationException("Utilisateur introuvable"));
    }

    private NotificationResponseDto toDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .eventType(notification.getEventType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .read(notification.getReadAt() != null)
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

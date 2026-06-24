package com.projetfilrouge.loanmanagement.web.controller;

import com.projetfilrouge.loanmanagement.service.NotificationService;
import com.projetfilrouge.loanmanagement.web.dto.response.NotificationResponseDto;
import com.projetfilrouge.loanmanagement.web.dto.response.UnreadNotificationCountDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Centre de notifications in-app")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @Operation(summary = "Mes notifications")
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<NotificationResponseDto> result = notificationService.getMyNotifications(
                authentication.getName(),
                PageRequest.of(page, Math.min(size, 50))
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<UnreadNotificationCountDto> getUnreadCount(Authentication authentication) {
        long count = notificationService.countUnread(authentication.getName());
        return ResponseEntity.ok(UnreadNotificationCountDto.builder().count(count).build());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Tout marquer comme lu")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

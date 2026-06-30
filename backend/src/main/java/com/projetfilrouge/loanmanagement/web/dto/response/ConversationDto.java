package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ConversationDto {
    private Long id;
    private Long otherUserId;
    private String otherUserFirstName;
    private String otherUserLastName;
    private String otherUserRole;
    private String lastMessagePreview;
    private Instant lastMessageAt;
    private long unreadCount;
}

package com.projetfilrouge.loanmanagement.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String content;
    private Instant sentAt;
    private Instant readAt;
    private boolean own;
    private String attachmentName;
    private String attachmentContentType;
    private String attachmentDownloadUrl;
}

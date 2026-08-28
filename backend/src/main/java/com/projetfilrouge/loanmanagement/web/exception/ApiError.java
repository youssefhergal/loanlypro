package com.projetfilrouge.loanmanagement.web.exception;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ApiError {
    String code;
    String message;
    List<FieldDetail> details;

    @Value
    @Builder
    public static class FieldDetail {
        String field;
        String message;
    }
}

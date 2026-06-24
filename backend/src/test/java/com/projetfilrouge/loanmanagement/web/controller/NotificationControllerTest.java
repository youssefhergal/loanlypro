package com.projetfilrouge.loanmanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetfilrouge.loanmanagement.service.NotificationService;
import com.projetfilrouge.loanmanagement.web.dto.response.NotificationResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.GlobalExceptionHandler;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final String CLIENT_EMAIL = "client@test.com";

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(
                new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getMyNotifications_returnsPagedList() throws Exception {
        NotificationResponseDto dto = NotificationResponseDto.builder()
                .id(1L)
                .eventType("APPLICATION_SUBMITTED")
                .title("Demande soumise")
                .message("Votre demande LF-001 a été transmise.")
                .referenceType("LOAN_APPLICATION")
                .referenceId(10L)
                .read(false)
                .createdAt(Instant.parse("2026-05-24T10:00:00Z"))
                .build();

        when(notificationService.getMyNotifications(eq(CLIENT_EMAIL), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/notifications/me").principal(principal(CLIENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Demande soumise"))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(notificationService.countUnread(CLIENT_EMAIL)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count").principal(principal(CLIENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void markAsRead_returns204() throws Exception {
        doNothing().when(notificationService).markAsRead(CLIENT_EMAIL, 5L);

        mockMvc.perform(patch("/api/v1/notifications/5/read").principal(principal(CLIENT_EMAIL)))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAsRead_returns404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Notification introuvable"))
                .when(notificationService).markAsRead(CLIENT_EMAIL, 99L);

        mockMvc.perform(patch("/api/v1/notifications/99/read").principal(principal(CLIENT_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification introuvable"));
    }

    @Test
    void markAllAsRead_returns204() throws Exception {
        doNothing().when(notificationService).markAllAsRead(CLIENT_EMAIL);

        mockMvc.perform(patch("/api/v1/notifications/read-all").principal(principal(CLIENT_EMAIL)))
                .andExpect(status().isNoContent());
    }

    private static UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
}

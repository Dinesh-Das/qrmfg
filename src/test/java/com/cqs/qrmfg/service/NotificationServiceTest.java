package com.cqs.qrmfg.service;

import com.cqs.qrmfg.config.NotificationConfig;
import com.cqs.qrmfg.dto.NotificationRequest;
import com.cqs.qrmfg.dto.NotificationResult;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationConfig notificationConfig;
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private TemplateEngine templateEngine;
    
    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    
    @InjectMocks
    private NotificationServiceImpl notificationService;
    
    private MaterialWorkflow testWorkflow;
    private Query testQuery;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
        testWorkflow = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow.setId(1L);
        testWorkflow.setMaterialName("Test Chemical");
        
        testQuery = new Query(testWorkflow, "What is the flash point?", QueryTeam.CQS, "plant.user");
        testQuery.setId(1L);
        
        testPreference = new NotificationPreference("test.user", "WORKFLOW_CREATED", "EMAIL", true);
        testPreference.setEmail("test.user@company.com");
        
        // Setup default mocks
        when(notificationConfig.isEnabled()).thenReturn(true);
        
        NotificationConfig.Email emailConfig = new NotificationConfig.Email();
        emailConfig.setEnabled(true);
        emailConfig.setFrom("noreply@qrmfg.com");
        when(notificationConfig.getEmail()).thenReturn(emailConfig);
        
        NotificationConfig.Slack slackConfig = new NotificationConfig.Slack();
        slackConfig.setEnabled(false);
        when(notificationConfig.getSlack()).thenReturn(slackConfig);
        
        NotificationConfig.Retry retryConfig = new NotificationConfig.Retry();
        retryConfig.setMaxAttempts(3);
        retryConfig.setDelayMillis(1000);
        when(notificationConfig.getRetry()).thenReturn(retryConfig);
    }

    @Test
    void testSendEmail_Success() {
        // Given
        List<String> recipients = Arrays.asList("user1@company.com", "user2@company.com");
        String subject = "Test Subject";
        String message = "Test Message";
        
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        NotificationResult result = notificationService.sendEmail(recipients, subject, message);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Email sent successfully", result.getMessage());
        assertEquals(recipients, result.getSuccessfulRecipients());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_Disabled() {
        // Given
        when(notificationConfig.getEmail().isEnabled()).thenReturn(false);
        
        List<String> recipients = Arrays.asList("user1@company.com");
        String subject = "Test Subject";
        String message = "Test Message";

        // When
        NotificationResult result = notificationService.sendEmail(recipients, subject, message);

        // Then
        assertTrue(result.isFailed());
        assertEquals("Email notifications are disabled", result.getMessage());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_Exception() {
        // Given
        List<String> recipients = Arrays.asList("user1@company.com");
        String subject = "Test Subject";
        String message = "Test Message";
        
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        NotificationResult result = notificationService.sendEmail(recipients, subject, message);

        // Then
        assertTrue(result.isFailed());
        assertTrue(result.getMessage().contains("Failed to send email"));
        assertEquals(recipients, result.getFailedRecipients());
    }

    @Test
    void testNotifyWorkflowCreated() {
        // Given
        when(preferenceRepository.findActivePreferencesForType("TEAM_JVC"))
                .thenReturn(Collections.singletonList(testPreference));

        // When
        notificationService.notifyWorkflowCreated(testWorkflow);

        // Then
        verify(preferenceRepository).findActivePreferencesForType("TEAM_JVC");
        // Note: In a real test, we'd verify the async call was made
    }

    @Test
    void testNotifyQueryRaised() {
        // Given
        when(preferenceRepository.findActivePreferencesForType("TEAM_CQS"))
                .thenReturn(Collections.singletonList(testPreference));
        when(preferenceRepository.findActivePreferencesForUser("plant.user"))
                .thenReturn(Collections.singletonList(testPreference));

        // When
        notificationService.notifyQueryRaised(testQuery);

        // Then
        verify(preferenceRepository).findActivePreferencesForType("TEAM_CQS");
        verify(preferenceRepository).findActivePreferencesForUser("plant.user");
    }

    @Test
    void testNotifyWorkflowStateChanged() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        when(preferenceRepository.findActivePreferencesForType("TEAM_PLANT_Plant A"))
                .thenReturn(Collections.singletonList(testPreference));

        // When
        notificationService.notifyWorkflowStateChanged(testWorkflow, WorkflowState.JVC_PENDING, "jvc.user");

        // Then
        verify(preferenceRepository).findActivePreferencesForType("TEAM_PLANT_Plant A");
    }

    @Test
    void testIsNotificationEnabled() {
        // When
        boolean result = notificationService.isNotificationEnabled();

        // Then
        assertTrue(result);
        verify(notificationConfig).isEnabled();
    }

    @Test
    void testIsEmailEnabled() {
        // When
        boolean result = notificationService.isEmailEnabled();

        // Then
        assertTrue(result);
        verify(notificationConfig).isEnabled();
        verify(notificationConfig).getEmail();
    }

    @Test
    void testIsSlackEnabled() {
        // When
        boolean result = notificationService.isSlackEnabled();

        // Then
        assertFalse(result);
        verify(notificationConfig).isEnabled();
        verify(notificationConfig).getSlack();
    }

    @Test
    void testSendNotification_UnsupportedType() {
        // Given
        NotificationRequest request = new NotificationRequest();
        request.setType("SMS");
        request.setRecipients(Arrays.asList("123456789"));
        request.setMessage("Test SMS");

        // When
        NotificationResult result = notificationService.sendNotification(request);

        // Then
        assertTrue(result.isFailed());
        assertTrue(result.getMessage().contains("Unsupported notification type"));
    }

    @Test
    void testSendNotification_Disabled() {
        // Given
        when(notificationConfig.isEnabled()).thenReturn(false);
        
        NotificationRequest request = new NotificationRequest();
        request.setType("EMAIL");
        request.setRecipients(Arrays.asList("user@company.com"));
        request.setMessage("Test message");

        // When
        NotificationResult result = notificationService.sendNotification(request);

        // Then
        assertTrue(result.isFailed());
        assertEquals("Notifications are disabled", result.getMessage());
    }

    @Test
    void testRenderTemplate_Success() {
        // Given
        String templateName = "test-template";
        Object data = Collections.singletonMap("key", "value");
        String expectedResult = "Rendered template content";
        
        when(templateEngine.process(eq(templateName), any())).thenReturn(expectedResult);

        // When
        String result = notificationService.renderTemplate(templateName, data);

        // Then
        assertEquals(expectedResult, result);
        verify(templateEngine).process(eq(templateName), any());
    }

    @Test
    void testRenderTemplate_NoTemplateEngine() {
        // Given - templateEngine is null (not autowired)
        notificationService = new NotificationServiceImpl();
        
        String templateName = "test-template";
        Object data = Collections.singletonMap("key", "value");

        // When
        String result = notificationService.renderTemplate(templateName, data);

        // Then
        assertEquals("", result);
    }

    @Test
    void testIsTemplateAvailable() {
        // When
        boolean result = notificationService.isTemplateAvailable("test-template");

        // Then
        assertTrue(result); // templateEngine is mocked and not null
    }

    @Test
    void testIsTemplateAvailable_NoTemplateEngine() {
        // Given - templateEngine is null
        notificationService = new NotificationServiceImpl();

        // When
        boolean result = notificationService.isTemplateAvailable("test-template");

        // Then
        assertFalse(result);
    }

    @Test
    void testNotifyUser() {
        // Given
        String username = "test.user";
        String subject = "Test Subject";
        String message = "Test Message";
        
        when(preferenceRepository.findActivePreferencesForUser(username))
                .thenReturn(Collections.singletonList(testPreference));

        // When
        notificationService.notifyUser(username, subject, message);

        // Then
        verify(preferenceRepository).findActivePreferencesForUser(username);
    }

    @Test
    void testRetryFailedNotification() {
        // Given
        NotificationRequest request = new NotificationRequest("EMAIL", 
                Arrays.asList("user@company.com"), "Subject", "Message");
        
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        NotificationResult result = notificationService.retryFailedNotification(request, 2);

        // Then
        assertTrue(result.isSuccess());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
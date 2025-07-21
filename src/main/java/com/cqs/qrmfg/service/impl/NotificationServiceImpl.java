package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.config.NotificationConfig;
import com.cqs.qrmfg.dto.NotificationRequest;
import com.cqs.qrmfg.dto.NotificationResult;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    @Autowired
    private NotificationConfig notificationConfig;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired(required = false)
    private TemplateEngine templateEngine;
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    // In-memory storage for failed notifications (in production, use database or Redis)
    private final Map<String, NotificationRequest> failedNotifications = new ConcurrentHashMap<>();
    
    @Override
    public NotificationResult sendNotification(NotificationRequest request) {
        if (!notificationConfig.isEnabled()) {
            logger.debug("Notifications are disabled");
            return NotificationResult.failure("Notifications are disabled");
        }
        
        try {
            switch (request.getType().toUpperCase()) {
                case "EMAIL":
                    return sendEmailNotification(request);
                case "SLACK":
                    return sendSlackNotification(request);
                default:
                    return NotificationResult.failure("Unsupported notification type: " + request.getType());
            }
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            failedNotifications.put(UUID.randomUUID().toString(), request);
            return NotificationResult.failure("Failed to send notification: " + e.getMessage());
        }
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendNotificationAsync(NotificationRequest request) {
        return CompletableFuture.completedFuture(sendNotification(request));
    }
    
    @Override
    public List<NotificationResult> sendBulkNotifications(List<NotificationRequest> requests) {
        return requests.stream()
                .map(this::sendNotification)
                .collect(Collectors.toList());
    }
    
    @Override
    public NotificationResult sendEmail(List<String> recipients, String subject, String message) {
        NotificationRequest request = new NotificationRequest("EMAIL", recipients, subject, message);
        return sendNotification(request);
    }
    
    @Override
    public NotificationResult sendEmailWithTemplate(List<String> recipients, String templateName, Object templateData) {
        NotificationRequest request = new NotificationRequest("EMAIL", recipients, templateName, 
                templateData instanceof Map ? (Map<String, Object>) templateData : 
                Collections.singletonMap("data", templateData));
        return sendNotification(request);
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendEmailAsync(List<String> recipients, String subject, String message) {
        return CompletableFuture.completedFuture(sendEmail(recipients, subject, message));
    }
    
    @Override
    public NotificationResult sendSlackMessage(String channel, String message) {
        NotificationRequest request = new NotificationRequest("SLACK", Collections.emptyList(), null, message);
        request.setChannel(channel);
        return sendNotification(request);
    }
    
    @Override
    public NotificationResult sendSlackMessage(List<String> users, String message) {
        NotificationRequest request = new NotificationRequest("SLACK", users, null, message);
        return sendNotification(request);
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendSlackMessageAsync(String channel, String message) {
        return CompletableFuture.completedFuture(sendSlackMessage(channel, message));
    }
    
    // Workflow-specific notification methods
    @Override
    public void notifyWorkflowCreated(MaterialWorkflow workflow) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("materialId", workflow.getMaterialId());
        data.put("materialName", workflow.getMaterialName());
        data.put("assignedPlant", workflow.getAssignedPlant());
        data.put("initiatedBy", workflow.getInitiatedBy());
        
        // Notify JVC team
        notifyTeam("JVC", "New Workflow Created", 
                String.format("New workflow created for material %s (%s)", 
                        workflow.getMaterialId(), workflow.getMaterialName()));
        
        // Notify assigned plant
        if (workflow.getAssignedPlant() != null) {
            notifyPlant(workflow.getAssignedPlant(), "New Material Assignment", 
                    String.format("Material %s has been assigned to your plant for MSDS workflow", 
                            workflow.getMaterialId()));
        }
    }
    
    @Override
    public void notifyWorkflowExtended(MaterialWorkflow workflow, String extendedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("extendedBy", extendedBy);
        
        notifyPlant(workflow.getAssignedPlant(), "Workflow Extended to Plant", 
                String.format("Material %s workflow has been extended to your plant. Please complete the questionnaire.", 
                        workflow.getMaterialId()));
    }
    
    @Override
    public void notifyWorkflowCompleted(MaterialWorkflow workflow, String completedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("completedBy", completedBy);
        
        // Notify all stakeholders
        notifyUser(workflow.getInitiatedBy(), "Workflow Completed", 
                String.format("Workflow for material %s has been completed", workflow.getMaterialId()));
        
        notifyPlant(workflow.getAssignedPlant(), "Workflow Completed", 
                String.format("MSDS workflow for material %s has been completed", workflow.getMaterialId()));
    }
    
    @Override
    public void notifyWorkflowStateChanged(MaterialWorkflow workflow, WorkflowState previousState, String changedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("previousState", previousState);
        data.put("currentState", workflow.getState());
        data.put("changedBy", changedBy);
        
        String message = String.format("Workflow for material %s changed from %s to %s", 
                workflow.getMaterialId(), previousState.getDisplayName(), workflow.getState().getDisplayName());
        
        // Notify relevant teams based on new state
        switch (workflow.getState()) {
            case PLANT_PENDING:
                notifyPlant(workflow.getAssignedPlant(), "Action Required", message);
                break;
            case CQS_PENDING:
                notifyTeam("CQS", "Query Resolution Required", message);
                break;
            case TECH_PENDING:
                notifyTeam("TECH", "Query Resolution Required", message);
                break;
            case COMPLETED:
                notifyWorkflowCompleted(workflow, changedBy);
                break;
        }
    }
    
    @Override
    public void notifyWorkflowOverdue(MaterialWorkflow workflow) {
        String message = String.format("URGENT: Workflow for material %s is overdue (%d days pending)", 
                workflow.getMaterialId(), workflow.getDaysPending());
        
        // Notify based on current state
        switch (workflow.getState()) {
            case JVC_PENDING:
                notifyTeam("JVC", "Overdue Workflow", message);
                break;
            case PLANT_PENDING:
                notifyPlant(workflow.getAssignedPlant(), "Overdue Workflow", message);
                break;
            case CQS_PENDING:
                notifyTeam("CQS", "Overdue Query Resolution", message);
                break;
            case TECH_PENDING:
                notifyTeam("TECH", "Overdue Query Resolution", message);
                break;
        }
        
        // Always notify admins for overdue workflows
        notifyAdmins("Overdue Workflow Alert", message);
    }
    
    // Query-specific notification methods
    @Override
    public void notifyQueryRaised(Query query) {
        String message = String.format("New query raised for material %s: %s", 
                query.getWorkflow().getMaterialId(), query.getQuestion());
        
        // Notify assigned team
        notifyTeam(query.getAssignedTeam().name(), "New Query Assigned", message);
        
        // Notify query raiser
        notifyUser(query.getRaisedBy(), "Query Submitted", 
                String.format("Your query for material %s has been submitted to %s team", 
                        query.getWorkflow().getMaterialId(), query.getAssignedTeam().getDisplayName()));
    }
    
    @Override
    public void notifyQueryResolved(Query query) {
        String message = String.format("Query resolved for material %s: %s\nResponse: %s", 
                query.getWorkflow().getMaterialId(), query.getQuestion(), query.getResponse());
        
        // Notify query raiser
        notifyUser(query.getRaisedBy(), "Query Resolved", message);
        
        // Notify plant team
        notifyPlant(query.getWorkflow().getAssignedPlant(), "Query Resolved", 
                String.format("Query for material %s has been resolved by %s team", 
                        query.getWorkflow().getMaterialId(), query.getAssignedTeam().getDisplayName()));
    }
    
    @Override
    public void notifyQueryAssigned(Query query, String assignedBy) {
        String message = String.format("Query assigned to %s team for material %s: %s", 
                query.getAssignedTeam().getDisplayName(), query.getWorkflow().getMaterialId(), query.getQuestion());
        
        notifyTeam(query.getAssignedTeam().name(), "Query Assigned", message);
    }
    
    @Override
    public void notifyQueryOverdue(Query query) {
        String message = String.format("URGENT: Query for material %s is overdue (%d days open): %s", 
                query.getWorkflow().getMaterialId(), query.getDaysOpen(), query.getQuestion());
        
        // Notify assigned team
        notifyTeam(query.getAssignedTeam().name(), "Overdue Query", message);
        
        // Notify admins
        notifyAdmins("Overdue Query Alert", message);
    }
    
    // User and team notification methods
    @Override
    public void notifyUser(String username, String subject, String message) {
        List<NotificationPreference> preferences = preferenceRepository.findActivePreferencesForUser(username);
        
        for (NotificationPreference pref : preferences) {
            NotificationRequest request = new NotificationRequest();
            request.setType(pref.getChannel());
            request.setRecipients(Collections.singletonList(getRecipientAddress(username, pref)));
            request.setSubject(subject);
            request.setMessage(message);
            
            sendNotificationAsync(request);
        }
    }
    
    @Override
    public void notifyTeam(String teamName, String subject, String message) {
        // Get team members from preferences
        List<NotificationPreference> teamPreferences = preferenceRepository.findActivePreferencesForType("TEAM_" + teamName);
        
        Map<String, List<String>> recipientsByChannel = teamPreferences.stream()
                .collect(Collectors.groupingBy(
                        NotificationPreference::getChannel,
                        Collectors.mapping(pref -> getRecipientAddress(pref.getUsername(), pref), Collectors.toList())
                ));
        
        for (Map.Entry<String, List<String>> entry : recipientsByChannel.entrySet()) {
            NotificationRequest request = new NotificationRequest(entry.getKey(), entry.getValue(), subject, message);
            sendNotificationAsync(request);
        }
    }
    
    @Override
    public void notifyPlant(String plantName, String subject, String message) {
        notifyTeam("PLANT_" + plantName, subject, message);
    }
    
    @Override
    public void notifyAdmins(String subject, String message) {
        notifyTeam("ADMIN", subject, message);
    }
    
    // Template management
    @Override
    public String renderTemplate(String templateName, Object data) {
        if (templateEngine == null) {
            logger.warn("Template engine not available, returning empty string");
            return "";
        }
        
        try {
            Context context = new Context();
            if (data instanceof Map) {
                context.setVariables((Map<String, Object>) data);
            } else {
                context.setVariable("data", data);
            }
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            logger.error("Failed to render template {}: {}", templateName, e.getMessage());
            return "";
        }
    }
    
    @Override
    public boolean isTemplateAvailable(String templateName) {
        // Simple check - in production, you might want to check if template file exists
        return templateEngine != null && templateName != null && !templateName.trim().isEmpty();
    }
    
    // Configuration and preferences
    @Override
    public boolean isNotificationEnabled() {
        return notificationConfig.isEnabled();
    }
    
    @Override
    public boolean isEmailEnabled() {
        return notificationConfig.isEnabled() && notificationConfig.getEmail().isEnabled() && mailSender != null;
    }
    
    @Override
    public boolean isSlackEnabled() {
        return notificationConfig.isEnabled() && notificationConfig.getSlack().isEnabled();
    }
    
    @Override
    public void updateNotificationPreferences(String username, String preferences) {
        // Implementation would parse preferences and update database
        logger.info("Updating notification preferences for user: {}", username);
    }
    
    // Retry and error handling
    @Override
    public NotificationResult retryFailedNotification(NotificationRequest request, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(notificationConfig.getRetry().getDelayMillis() * attempt);
                NotificationResult result = sendNotification(request);
                if (result.isSuccess()) {
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Retry attempt {} failed for notification: {}", attempt, e.getMessage());
            }
        }
        return NotificationResult.failure("All retry attempts failed");
    }
    
    @Override
    public List<NotificationResult> getFailedNotifications() {
        return failedNotifications.values().stream()
                .map(req -> NotificationResult.failure("Previously failed notification"))
                .collect(Collectors.toList());
    }
    
    @Override
    public void clearFailedNotifications() {
        failedNotifications.clear();
    }
    
    // Private helper methods
    private NotificationResult sendEmailNotification(NotificationRequest request) {
        if (!isEmailEnabled()) {
            return NotificationResult.failure("Email notifications are disabled");
        }
        
        try {
            String content = request.getMessage();
            if (request.getTemplateName() != null && isTemplateAvailable(request.getTemplateName())) {
                content = renderTemplate(request.getTemplateName(), request.getTemplateData());
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationConfig.getEmail().getFrom());
            message.setTo(request.getRecipients().toArray(new String[0]));
            message.setSubject(request.getSubject());
            message.setText(content);
            message.setSentDate(new Date());
            
            mailSender.send(message);
            
            logger.info("Email sent successfully to {} recipients", request.getRecipients().size());
            return NotificationResult.success("Email sent successfully", request.getRecipients());
            
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return NotificationResult.failure("Failed to send email: " + e.getMessage(), request.getRecipients());
        }
    }
    
    private NotificationResult sendSlackNotification(NotificationRequest request) {
        if (!isSlackEnabled()) {
            return NotificationResult.failure("Slack notifications are disabled");
        }
        
        // Placeholder implementation - in production, integrate with Slack API
        logger.info("Slack notification would be sent: {}", request.getMessage());
        return NotificationResult.success("Slack notification sent (simulated)");
    }
    
    private String getRecipientAddress(String username, NotificationPreference preference) {
        switch (preference.getChannel().toUpperCase()) {
            case "EMAIL":
                return preference.getEmail() != null ? preference.getEmail() : username + "@company.com";
            case "SLACK":
                return preference.getSlackId() != null ? preference.getSlackId() : username;
            default:
                return username;
        }
    }
}
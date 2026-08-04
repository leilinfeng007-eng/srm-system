package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import java.time.LocalDateTime;
import java.util.List;

/** Public application contract used by the system API layer. */
public interface SystemGovernanceFacade {
    record ApprovalView(Long id, String instanceCode, Long workflowId, String businessType,
                        String businessId, String businessSummary, Long submittedBy,
                        LocalDateTime submittedAt, String status, Long version) {}
    record ApprovalNodeView(Long id, Long instanceId, String nodeCode, String nodeName,
                            String assigneeType, String status, String decision,
                            LocalDateTime decidedAt, Integer durationHours,
                            LocalDateTime deadlineAt) {}
    record TaskView(Long id, String sourceType, String sourceId, Long approvalInstanceId,
                    String title, String status, String priority, String slaStatus,
                    LocalDateTime createdAt, LocalDateTime dueAt, LocalDateTime completedAt) {}

    PageResult<ApprovalView> approvals(int page, int pageSize, String status, Long userId);
    ApprovalView approval(Long id, Long userId);
    List<ApprovalNodeView> approvalNodes(Long id, Long userId);

    PageResult<?> auditLogs(int page, int pageSize, String actionCode, String targetType);

    PageResult<?> batchJobs(int page, int pageSize, String objectType);
    Object batchJob(Long id);
    List<?> batchErrors(Long id);
    Object createBatchJob(String jobType, String objectType, String idempotencyKey);

    List<?> dictionaries();
    Object dictionary(Long id);
    Object createDictionary(String code, String name, String description);
    void updateDictionary(Long id, String name, String description);
    void setDictionaryStatus(Long id, String status);
    List<?> dictionaryItems(Long dictionaryId);
    Object createDictionaryItem(Long dictionaryId, String code, String name, Integer sortOrder);
    void updateDictionaryItem(Long itemId, String name, Integer sortOrder);
    void setDictionaryItemStatus(Long itemId, String status);

    PageResult<?> messages(int page, int pageSize, Long userId, String status);
    long unreadMessages(Long userId);
    void markMessageRead(Long id, Long userId);
    int markAllMessagesRead(Long userId);

    List<?> numberRules();
    Object createNumberRule(String code, String name, String objectType, String prefix,
                            String dateFormat, Integer serialLength, String resetCycle,
                            Boolean organizationDimension);
    void updateNumberRule(Long id, String name, String prefix, String dateFormat,
                          Integer serialLength, String resetCycle);
    void setNumberRuleStatus(Long id, String status);
    String generateNumber(String ruleCode, Long organizationId);

    List<?> parameters();
    Object parameter(Long id);
    Object createParameter(String code, String name, String type, String defaultValue,
                           String validationRule, Boolean approvalRequired, String description);
    List<?> parameterVersions(Long parameterId);
    Object createParameterVersion(Long parameterId, String value);
    void submitParameterVersion(Long versionId);

    PageResult<TaskView> tasks(int page, int pageSize, Long userId, String status);
    long overdueTasks(Long userId);

    record WorkflowNodeCommand(String nodeCode, String nodeName, String nodeType,
                               Integer sortOrder, String assigneeType, String assigneeValue,
                               Integer durationHours) {}

    PageResult<?> workflows(int page, int pageSize, String status);
    Object workflow(Long id);
    List<?> workflowNodes(Long id);
    Object createWorkflow(String processCode, String processName, String businessType,
                          String description, List<WorkflowNodeCommand> nodes);
    void updateWorkflow(Long id, String processName, String description,
                        List<WorkflowNodeCommand> nodes);
    void publishWorkflow(Long id);
    void retireWorkflow(Long id);

    PageResult<?> documentTemplates(int page, int pageSize, String templateCode);
    Object documentTemplate(Long id);
    Object createDocumentTemplate(String code, String name, String purpose, String domainCode);
    void bindDocumentTemplateAttachment(Long id, Long attachmentId);
    void publishDocumentTemplate(Long id);
    void disableDocumentTemplate(Long id);
    Long documentTemplateAttachmentId(Long id);
}

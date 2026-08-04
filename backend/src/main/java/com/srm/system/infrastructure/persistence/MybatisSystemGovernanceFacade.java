package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.api.PageResult;
import com.srm.security.infrastructure.persistence.entity.SysOperationLogEntity;
import com.srm.security.infrastructure.persistence.mapper.OperationLogMapper;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.domain.repository.AttachmentRepository;
import com.srm.system.domain.service.AuditRecorder;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.infrastructure.persistence.entity.SysDocumentTemplateEntity;
import com.srm.system.infrastructure.persistence.entity.SysWorkflowNodeEntity;
import com.srm.system.infrastructure.persistence.mapper.SysDocumentTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisSystemGovernanceFacade implements SystemGovernanceFacade {
    private final MybatisWorkflowQueryRepository workflows;
    private final OperationLogMapper auditLogs;
    private final MybatisBatchJobRepository batchJobs;
    private final MybatisDictionaryRepository dictionaries;
    private final MybatisNumberRuleRepository numberRules;
    private final MybatisParameterRepository parameters;
    private final SysDocumentTemplateMapper templates;
    private final AttachmentRepository attachments;
    private final AuditRecorder audit;
    private final DataScopeAuthorizationService scopes;

    public MybatisSystemGovernanceFacade(MybatisWorkflowQueryRepository workflows,
                                         OperationLogMapper auditLogs,
                                         MybatisBatchJobRepository batchJobs,
                                         MybatisDictionaryRepository dictionaries,
                                         MybatisNumberRuleRepository numberRules,
                                         MybatisParameterRepository parameters,
                                         SysDocumentTemplateMapper templates,
                                         AttachmentRepository attachments,
                                         AuditRecorder audit,
                                         DataScopeAuthorizationService scopes) {
        this.workflows = workflows;
        this.auditLogs = auditLogs;
        this.batchJobs = batchJobs;
        this.dictionaries = dictionaries;
        this.numberRules = numberRules;
        this.parameters = parameters;
        this.templates = templates;
        this.attachments = attachments;
        this.audit = audit;
        this.scopes = scopes;
    }

    @Override public PageResult<ApprovalView> approvals(int p,int s,String status,Long userId){
        var page=workflows.listApprovals(p,s,status,userId);
        return PageResult.of(page.items().stream().map(this::approvalView).toList(),p,s,page.total());
    }
    @Override public ApprovalView approval(Long id,Long userId){workflows.requireApprovalVisible(id,userId);return approvalView(workflows.getApproval(id));}
    @Override public List<ApprovalNodeView> approvalNodes(Long id,Long userId){workflows.requireApprovalVisible(id,userId);return workflows.listApprovalNodes(id).stream().map(this::approvalNodeView).toList();}

    @Override
    public PageResult<?> auditLogs(int page, int pageSize, String actionCode, String targetType) {
        if (page < 1 || pageSize < 1 || pageSize > 200) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.VALIDATION_ERROR, "Invalid paging parameters");
        }
        var scope = scopes.requireRead("system:audit-log:view", "system", "OWNER");
        var count = new LambdaQueryWrapper<SysOperationLogEntity>();
        var items = new LambdaQueryWrapper<SysOperationLogEntity>();
        if (actionCode != null && !actionCode.isBlank()) {
            count.like(SysOperationLogEntity::getActionCode, actionCode);
            items.like(SysOperationLogEntity::getActionCode, actionCode);
        }
        if (targetType != null && !targetType.isBlank()) {
            count.eq(SysOperationLogEntity::getTargetType, targetType);
            items.eq(SysOperationLogEntity::getTargetType, targetType);
        }
        if (!scope.isAllScope()) {
            Long userId = scope.isSelfScope() ? scope.ownerUserId() : -1L;
            count.eq(SysOperationLogEntity::getUserId, userId);
            items.eq(SysOperationLogEntity::getUserId, userId);
        }
        long total = auditLogs.selectCount(count);
        List<SysOperationLogEntity> result = auditLogs.selectList(items
                .orderByDesc(SysOperationLogEntity::getOccurredAt)
                .last("LIMIT " + ((page - 1) * pageSize) + "," + pageSize));
        return PageResult.of(result, page, pageSize, total);
    }

    @Override public PageResult<?> batchJobs(int p,int s,String type){return batchJobs.listJobs(p,s,type);}
    @Override public Object batchJob(Long id){return batchJobs.getJob(id);}
    @Override public List<?> batchErrors(Long id){return batchJobs.listErrors(id);}
    @Override public Object createBatchJob(String jt,String ot,String key){return batchJobs.createJob(jt,ot,key);}

    @Override public List<?> dictionaries(){requireAll("system:dictionary:view",false);return dictionaries.listDictionaries();}
    @Override public Object dictionary(Long id){requireAll("system:dictionary:view",false);return dictionaries.getDictionary(id);}
    @Override public Object createDictionary(String c,String n,String d){requireAll("system:dictionary:create",true);Object value=dictionaries.createDict(c,n,d);audit.record("DICTIONARY_CREATED","DICTIONARY",entityId(value),"SUCCESS",null,"code="+c+",name="+n,null);return value;}
    @Override public void updateDictionary(Long id,String n,String d){requireAll("system:dictionary:update",true);dictionaries.updateDict(id,n,d);audit.record("DICTIONARY_UPDATED","DICTIONARY",String.valueOf(id),"SUCCESS",null,"name="+n,null);}
    @Override public void setDictionaryStatus(Long id,String s){requireAll("system:dictionary:"+("ACTIVE".equals(s)?"enable":"disable"),true);dictionaries.toggleDict(id,s);audit.record("DICTIONARY_STATUS_CHANGED","DICTIONARY",String.valueOf(id),"SUCCESS",null,"status="+s,null);}
    @Override public List<?> dictionaryItems(Long id){requireAll("system:dictionary:view",false);return dictionaries.listItems(id);}
    @Override public Object createDictionaryItem(Long id,String c,String n,Integer o){requireAll("system:dictionary:create",true);Object value=dictionaries.createItem(id,c,n,o == null ? 0 : o);audit.record("DICTIONARY_ITEM_CREATED","DICTIONARY_ITEM",entityId(value),"SUCCESS",null,"dictionaryId="+id+",code="+c,null);return value;}
    @Override public void updateDictionaryItem(Long id,String n,Integer o){requireAll("system:dictionary:update",true);dictionaries.updateItem(id,n,o);audit.record("DICTIONARY_ITEM_UPDATED","DICTIONARY_ITEM",String.valueOf(id),"SUCCESS",null,"name="+n,null);}
    @Override public void setDictionaryItemStatus(Long id,String s){requireAll("system:dictionary:"+("ACTIVE".equals(s)?"enable":"disable"),true);dictionaries.toggleItem(id,s);audit.record("DICTIONARY_ITEM_STATUS_CHANGED","DICTIONARY_ITEM",String.valueOf(id),"SUCCESS",null,"status="+s,null);}

    @Override public PageResult<?> messages(int p,int s,Long u,String status){return workflows.listMyMessages(p,s,u,status);}
    @Override public long unreadMessages(Long u){return workflows.countUnreadMessages(u);}
    @Override public void markMessageRead(Long id,Long u){workflows.markMessageRead(id,u);}
    @Override public int markAllMessagesRead(Long u){return workflows.markAllMessagesRead(u);}

    @Override public List<?> numberRules(){requireAll("system:number-rule:view",false);return numberRules.listAll();}
    @Override public Object createNumberRule(String c,String n,String o,String p,String d,Integer l,String r,Boolean g){requireAll("system:number-rule:create",true);Object value=numberRules.createRule(c,n,o,p,d,l,r,g);audit.record("NUMBER_RULE_CREATED","NUMBER_RULE",entityId(value),"SUCCESS",null,"code="+c+",objectType="+o,null);return value;}
    @Override public void updateNumberRule(Long id,String n,String p,String d,Integer l,String r){requireAll("system:number-rule:update",true);numberRules.updateRule(id,n,p,d,l,r);audit.record("NUMBER_RULE_UPDATED","NUMBER_RULE",String.valueOf(id),"SUCCESS",null,"name="+n,null);}
    @Override public void setNumberRuleStatus(Long id,String s){requireAll("system:number-rule:"+("ACTIVE".equals(s)?"enable":"disable"),true);numberRules.toggleRule(id,s);audit.record("NUMBER_RULE_STATUS_CHANGED","NUMBER_RULE",String.valueOf(id),"SUCCESS",null,"status="+s,null);}
    @Override public String generateNumber(String c,Long o){requireAll("system:number-rule:view",false);return numberRules.generateNumber(c,o);}

    @Override public List<?> parameters(){requireAll("system:parameter:view",false);return parameters.listAll();}
    @Override public Object parameter(Long id){requireAll("system:parameter:view",false);return parameters.getParam(id);}
    @Override public Object createParameter(String c,String n,String t,String d,String v,Boolean a,String x){requireAll("system:parameter:create",true);Object value=parameters.createParam(c,n,t,d,v,a,x);audit.record("PARAMETER_CREATED","PARAMETER",entityId(value),"SUCCESS",null,"code="+c+",type="+t,null);return value;}
    @Override public List<?> parameterVersions(Long id){requireAll("system:parameter:view",false);return parameters.listVersions(id);}
    @Override public Object createParameterVersion(Long id,String v){requireAll("system:parameter:update",true);Object value=parameters.createVersionWithApproval(id,v);audit.record("PARAMETER_VERSION_CREATED","PARAMETER_VERSION",entityId(value),"SUCCESS",null,"parameterId="+id,null);return value;}
    @Override public void submitParameterVersion(Long id){requireAll("system:parameter:submit",true);parameters.submitForApproval(id);audit.record("PARAMETER_VERSION_SUBMITTED","PARAMETER_VERSION",String.valueOf(id),"SUCCESS","status=DRAFT","status=PENDING_APPROVAL",null);}

    @Override public PageResult<TaskView> tasks(int p,int s,Long u,String status){
        var page=workflows.listMyTasks(p,s,u,status);
        var now=java.time.LocalDateTime.now();
        var items=page.items().stream().map(task -> new TaskView(task.getId(),task.getSourceType(),
                task.getSourceId(),workflows.approvalInstanceIdForTask(task),task.getTitle(),task.getStatus(),
                task.getPriority(),task.getDueAt()!=null&&task.getDueAt().isBefore(now)&&"PENDING".equals(task.getStatus())?"OVERDUE":"NORMAL",
                task.getCreatedAt(),task.getDueAt(),task.getCompletedAt())).toList();
        return PageResult.of(items,p,s,page.total());
    }
    @Override public long overdueTasks(Long u){return workflows.countOverdueTasks(u);}

    @Override public PageResult<?> workflows(int p,int s,String status){requireAll("system:workflow:view",false);return workflows.listWorkflows(p,s,status);}
    @Override public Object workflow(Long id){requireAll("system:workflow:view",false);return workflows.getWorkflow(id);}
    @Override public List<?> workflowNodes(Long id){requireAll("system:workflow:view",false);return workflows.listWorkflowNodes(id);}
    @Override public Object createWorkflow(String c,String n,String b,String d,List<WorkflowNodeCommand> nodes){requireAll("system:workflow:create",true);Object value=workflows.createWorkflow(c,n,b,d,toNodes(nodes));audit.record("WORKFLOW_CREATED","WORKFLOW",entityId(value),"SUCCESS",null,"processCode="+c+",businessType="+b,null);return value;}
    @Override public void updateWorkflow(Long id,String n,String d,List<WorkflowNodeCommand> nodes){requireAll("system:workflow:update",true);workflows.updateWorkflow(id,n,d,toNodes(nodes));audit.record("WORKFLOW_UPDATED","WORKFLOW",String.valueOf(id),"SUCCESS",null,"name="+n+",nodes="+(nodes==null?0:nodes.size()),null);}
    @Override public void publishWorkflow(Long id){requireAll("system:workflow:publish",true);workflows.publishWorkflow(id);audit.record("WORKFLOW_PUBLISHED","WORKFLOW",String.valueOf(id),"SUCCESS","status=DRAFT","status=PUBLISHED",null);}
    @Override public void retireWorkflow(Long id){requireAll("system:workflow:retire",true);workflows.retireWorkflow(id);audit.record("WORKFLOW_RETIRED","WORKFLOW",String.valueOf(id),"SUCCESS",null,"status=RETIRED",null);}

    private List<SysWorkflowNodeEntity> toNodes(List<WorkflowNodeCommand> commands) {
        if (commands == null) return null;
        return commands.stream().map(command -> {
            var node = new SysWorkflowNodeEntity();
            node.setNodeCode(command.nodeCode()); node.setNodeName(command.nodeName());
            node.setNodeType(command.nodeType()); node.setSortOrder(command.sortOrder());
            node.setAssigneeType(command.assigneeType()); node.setAssigneeValue(command.assigneeValue());
            node.setDurationHours(command.durationHours());
            return node;
        }).toList();
    }

    private ApprovalView approvalView(com.srm.system.infrastructure.persistence.entity.SysApprovalInstanceEntity value) {
        return new ApprovalView(value.getId(),value.getInstanceCode(),value.getWorkflowId(),
                value.getBusinessType(),value.getBusinessId(),value.getBusinessSummary(),
                value.getSubmittedBy(),value.getSubmittedAt(),value.getStatus(),value.getVersion());
    }

    private ApprovalNodeView approvalNodeView(com.srm.system.infrastructure.persistence.entity.SysApprovalNodeInstanceEntity value) {
        return new ApprovalNodeView(value.getId(),value.getInstanceId(),value.getNodeCode(),
                value.getNodeName(),value.getAssigneeType(),value.getStatus(),value.getDecision(),
                value.getDecidedAt(),value.getDurationHours(),value.getDeadlineAt());
    }

    @Override
    public PageResult<?> documentTemplates(int page, int pageSize, String templateCode) {
        requireAll("system:document-template:download", false);
        var query = new LambdaQueryWrapper<SysDocumentTemplateEntity>();
        if (templateCode != null && !templateCode.isBlank()) {
            query.eq(SysDocumentTemplateEntity::getTemplateCode, templateCode);
        }
        long total = templates.selectCount(query);
        var items = templates.selectList(query.orderByAsc(SysDocumentTemplateEntity::getTemplateCode)
                .orderByDesc(SysDocumentTemplateEntity::getTemplateVersion)
                .last("LIMIT " + ((page - 1) * pageSize) + "," + pageSize));
        return PageResult.of(items, page, pageSize, total);
    }

    @Override public Object documentTemplate(Long id) {
        requireAll("system:document-template:download", false);
        return requiredTemplate(id);
    }

    @Override
    @Transactional
    public Object createDocumentTemplate(String code, String name, String purpose, String domainCode) {
        requireAll("system:document-template:create", true);
        var latest = templates.selectOne(new LambdaQueryWrapper<SysDocumentTemplateEntity>()
                .eq(SysDocumentTemplateEntity::getTemplateCode, code)
                .orderByDesc(SysDocumentTemplateEntity::getTemplateVersion).last("LIMIT 1"));
        if (latest != null && "DRAFT".equals(latest.getStatus())) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.CONFLICT,
                    "A draft template version already exists");
        }
        var entity = new SysDocumentTemplateEntity();
        entity.setTemplateCode(code); entity.setTemplateName(name); entity.setPurpose(purpose);
        entity.setDomainCode(domainCode);
        entity.setTemplateVersion(latest == null ? 1 : latest.getTemplateVersion() + 1);
        entity.setStatus("DRAFT"); entity.setCreatedBy(actor()); entity.setUpdatedBy(actor());
        templates.insert(entity);
        audit.record("DOCUMENT_TEMPLATE_CREATED", "DOCUMENT_TEMPLATE", String.valueOf(entity.getId()),
                "SUCCESS", null, "code=" + code + ",version=" + entity.getTemplateVersion(), null);
        return entity;
    }

    @Override
    @Transactional
    public void bindDocumentTemplateAttachment(Long id, Long attachmentId) {
        requireAll("system:document-template:update", true);
        var template = requiredTemplate(id);
        if (!"DRAFT".equals(template.getStatus())) conflict("Only a draft template can be changed");
        var attachment = attachments.findActive(attachmentId);
        if (!"DOCUMENT_TEMPLATE".equals(attachment.ownerType())
                || !String.valueOf(id).equals(attachment.ownerId())) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.RESOURCE_NOT_FOUND);
        }
        template.setAttachmentId(attachmentId); template.setUpdatedBy(actor());
        templates.updateById(template);
        audit.record("DOCUMENT_TEMPLATE_ATTACHMENT_BOUND", "DOCUMENT_TEMPLATE", String.valueOf(id),
                "SUCCESS", null, "attachmentId=" + attachmentId, null);
    }

    @Override
    @Transactional
    public void publishDocumentTemplate(Long id) {
        requireAll("system:document-template:publish", true);
        var template = requiredTemplate(id);
        if (!"DRAFT".equals(template.getStatus())) conflict("Only a draft template can be published");
        if (template.getAttachmentId() == null) conflict("Template attachment is required before publish");
        var active = templates.selectList(new LambdaQueryWrapper<SysDocumentTemplateEntity>()
                .eq(SysDocumentTemplateEntity::getTemplateCode, template.getTemplateCode())
                .eq(SysDocumentTemplateEntity::getStatus, "PUBLISHED")
                .ne(SysDocumentTemplateEntity::getId, id));
        for (var previous : active) {
            previous.setStatus("INACTIVE"); previous.setUpdatedBy(actor()); templates.updateById(previous);
        }
        template.setStatus("PUBLISHED"); template.setUpdatedBy(actor()); templates.updateById(template);
        audit.record("DOCUMENT_TEMPLATE_PUBLISHED", "DOCUMENT_TEMPLATE", String.valueOf(id),
                "SUCCESS", "status=DRAFT", "status=PUBLISHED", null);
    }

    @Override
    @Transactional
    public void disableDocumentTemplate(Long id) {
        requireAll("system:document-template:disable", true);
        var template = requiredTemplate(id);
        if ("INACTIVE".equals(template.getStatus())) conflict("Template is already inactive");
        template.setStatus("INACTIVE"); template.setUpdatedBy(actor()); templates.updateById(template);
        audit.record("DOCUMENT_TEMPLATE_DISABLED", "DOCUMENT_TEMPLATE", String.valueOf(id),
                "SUCCESS", null, "status=INACTIVE", null);
    }

    @Override
    public Long documentTemplateAttachmentId(Long id) {
        requireAll("system:document-template:download", false);
        var template = requiredTemplate(id);
        if (template.getAttachmentId() == null) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.RESOURCE_NOT_FOUND, "Template attachment not found");
        }
        return template.getAttachmentId();
    }

    private SysDocumentTemplateEntity requiredTemplate(Long id) {
        var template = templates.selectById(id);
        if (template == null) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.RESOURCE_NOT_FOUND);
        }
        return template;
    }

    private void conflict(String message) {
        throw new com.srm.common.exception.BusinessException(
                com.srm.common.exception.ErrorCode.CONFLICT, message);
    }

    private String actor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private void requireAll(String permission, boolean write) {
        var scope = write ? scopes.requireWrite(permission, "system", "ORGANIZATION")
                : scopes.requireRead(permission, "system", "ORGANIZATION");
        if (!scope.isAllScope()) {
            throw new com.srm.common.exception.BusinessException(
                    com.srm.common.exception.ErrorCode.ACCESS_DENIED);
        }
    }

    private String entityId(Object value) {
        try {
            Object id = value.getClass().getMethod("getId").invoke(value);
            return String.valueOf(id);
        } catch (ReflectiveOperationException failure) {
            return "unknown";
        }
    }
}

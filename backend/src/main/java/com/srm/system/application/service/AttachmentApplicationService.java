package com.srm.system.application.service;

import com.srm.system.api.response.AttachmentResponse;
import com.srm.system.domain.model.Attachment;
import com.srm.system.domain.repository.AttachmentRepository;
import com.srm.system.domain.service.AuditRecorder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentApplicationService {

    public record AttachmentDownload(String originalName, String mimeType, byte[] content) {
        public AttachmentDownload {
            content = content.clone();
        }
        @Override public byte[] content() { return content.clone(); }
    }

    private final AttachmentRepository repository;
    private final AttachmentAuthorizationService authorization;
    private final AuditRecorder audit;

    public AttachmentApplicationService(AttachmentRepository repository,
                                        AttachmentAuthorizationService authorization,
                                        AuditRecorder audit) {
        this.repository = repository;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional
    public AttachmentResponse upload(String originalName, String mimeType, byte[] content,
                                     String ownerType, String ownerId) {
        authorization.require("system:attachment:upload", ownerType, ownerId, true);
        Attachment attachment = repository.store(originalName, mimeType, content,
                normalize(ownerType), ownerId);
        audit.record("ATTACHMENT_UPLOAD", "ATTACHMENT", String.valueOf(attachment.id()),
                "SUCCESS", null, metadataSummary(attachment), null);
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public AttachmentResponse get(Long id) {
        Attachment attachment = repository.findActive(id);
        authorization.require("system:attachment:view", attachment.ownerType(),
                attachment.ownerId(), false);
        return toResponse(attachment);
    }

    @Transactional
    public AttachmentDownload download(Long id) {
        Attachment attachment = repository.findActive(id);
        authorization.require("system:attachment:download", attachment.ownerType(),
                attachment.ownerId(), false);
        byte[] content = repository.loadContent(id);
        audit.record("ATTACHMENT_DOWNLOAD", "ATTACHMENT", String.valueOf(id), "SUCCESS",
                metadataSummary(attachment), metadataSummary(attachment), null);
        return new AttachmentDownload(attachment.originalName(), attachment.mimeType(), content);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listByOwner(String ownerType, String ownerId) {
        authorization.require("system:attachment:view", ownerType, ownerId, false);
        return repository.findActiveByOwner(normalize(ownerType), ownerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        Attachment attachment = repository.findActive(id);
        authorization.require("system:attachment:delete", attachment.ownerType(),
                attachment.ownerId(), true);
        repository.markDeleted(id);
        audit.record("ATTACHMENT_DELETE", "ATTACHMENT", String.valueOf(id), "SUCCESS",
                metadataSummary(attachment), "status=DELETED", null);
    }

    @Transactional
    public AttachmentResponse replace(Long id, String originalName, String mimeType, byte[] content) {
        Attachment attachment = repository.findActive(id);
        authorization.require("system:attachment:upload", attachment.ownerType(),
                attachment.ownerId(), true);
        Attachment updated = repository.replace(id, originalName, mimeType, content);
        audit.record("ATTACHMENT_REPLACE", "ATTACHMENT", String.valueOf(id), "SUCCESS",
                metadataSummary(attachment), metadataSummary(updated), null);
        return toResponse(updated);
    }

    private String normalize(String ownerType) {
        return ownerType.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(attachment.id(), attachment.fileName(),
                attachment.originalName(), attachment.mimeType(), attachment.fileSize(),
                attachment.fileSha256(), attachment.ownerType(), attachment.ownerId(),
                attachment.scanStatus(), attachment.status(), null, attachment.createdBy(),
                attachment.createdAt());
    }

    private String metadataSummary(Attachment attachment) {
        return "owner=" + attachment.ownerType() + ":" + attachment.ownerId()
                + ",mime=" + attachment.mimeType() + ",size=" + attachment.fileSize()
                + ",sha256=" + attachment.fileSha256();
    }
}

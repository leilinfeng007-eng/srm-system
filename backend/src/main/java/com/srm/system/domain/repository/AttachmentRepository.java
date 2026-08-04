package com.srm.system.domain.repository;

import com.srm.system.domain.model.Attachment;
import java.util.List;

public interface AttachmentRepository {
    Attachment store(String originalName, String mimeType, byte[] content,
                     String ownerType, String ownerId);

    Attachment findActive(Long id);

    byte[] loadContent(Long id);

    List<Attachment> findActiveByOwner(String ownerType, String ownerId);

    void markDeleted(Long id);

    Attachment replace(Long id, String originalName, String mimeType, byte[] content);
}

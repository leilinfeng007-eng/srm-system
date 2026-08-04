package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.repository.SystemAttachmentOwnerRepository;
import com.srm.system.infrastructure.persistence.mapper.SysBatchJobMapper;
import com.srm.system.infrastructure.persistence.mapper.SysDocumentTemplateMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisSystemAttachmentOwnerRepository implements SystemAttachmentOwnerRepository {

    private final SysBatchJobMapper batchJobMapper;
    private final SysDocumentTemplateMapper templateMapper;

    public MybatisSystemAttachmentOwnerRepository(SysBatchJobMapper batchJobMapper,
                                                  SysDocumentTemplateMapper templateMapper) {
        this.batchJobMapper = batchJobMapper;
        this.templateMapper = templateMapper;
    }

    @Override
    public java.util.Optional<Owner> findOwner(String ownerType, Long ownerId) {
        return switch (ownerType) {
            case "BATCH_JOB" -> java.util.Optional.ofNullable(batchJobMapper.selectById(ownerId))
                    .map(job -> new Owner(job.getCreatedBy(), false));
            case "DOCUMENT_TEMPLATE" -> java.util.Optional.ofNullable(templateMapper.selectById(ownerId))
                    .map(template -> new Owner(null, true));
            default -> java.util.Optional.empty();
        };
    }
}

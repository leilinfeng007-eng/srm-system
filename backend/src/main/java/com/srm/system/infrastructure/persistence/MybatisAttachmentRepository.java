package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.config.SrmAttachmentProperties;
import com.srm.system.domain.model.Attachment;
import com.srm.system.domain.repository.AttachmentRepository;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisAttachmentRepository implements AttachmentRepository {
    private final SysAttachmentMapper attachMapper;
    private final SysAttachmentVersionMapper attachVerMapper;
    private final String attachmentRoot;
    private final long maxFileSize;
    private final Set<String> allowedMimeTypes;

    private static final SecureRandom RANDOM = new SecureRandom();

    public MybatisAttachmentRepository(SysAttachmentMapper attachMapper,
                              SysAttachmentVersionMapper attachVerMapper,
                              SrmAttachmentProperties properties) {
        this.attachMapper = attachMapper;
        this.attachVerMapper = attachVerMapper;
        this.attachmentRoot = properties.getRoot();
        this.maxFileSize = org.springframework.util.unit.DataSize
                .parse(properties.getMaxFileSize()).toBytes();
        this.allowedMimeTypes = Set.copyOf(properties.getAllowedMimeTypes());
    }

    @Override
    public Attachment store(String originalName, String mimeType, byte[] content,
                            String ownerType, String ownerId) {
        return toDomain(upload(originalName, mimeType, content, ownerType, ownerId));
    }

    @Override
    public Attachment findActive(Long id) {
        return toDomain(getAttachment(id));
    }

    @Override
    public byte[] loadContent(Long id) {
        return download(id);
    }

    @Override
    public List<Attachment> findActiveByOwner(String ownerType, String ownerId) {
        return listByOwner(ownerType, ownerId).stream().map(this::toDomain).toList();
    }

    @Override
    public void markDeleted(Long id) {
        deleteAttachment(id);
    }

    @Override
    public Attachment replace(Long id, String originalName, String mimeType, byte[] content) {
        return toDomain(replaceVersion(id, originalName, mimeType, content));
    }

    @Transactional
    public SysAttachmentEntity upload(String originalName, String mimeType, byte[] content,
                                       String ownerType, String ownerId) {
        String verifiedMimeType = validateFile(originalName, mimeType, content);

        String storageKey = generateStorageKey();
        String sha256 = sha256(content);
        String safeName = sanitizeFileName(originalName);

        Path targetPath = Path.of(attachmentRoot, storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to save file");
        }

        var attach = new SysAttachmentEntity();
        attach.setFileName(safeName); attach.setOriginalName(originalName);
        attach.setStorageKey(storageKey); attach.setMimeType(verifiedMimeType);
        attach.setFileSize((long) content.length); attach.setFileSha256(sha256);
        attach.setOwnerType(ownerType); attach.setOwnerId(ownerId);
        attach.setScanStatus("NOT_CONFIGURED"); attach.setStatus("ACTIVE");
        attach.setCreatedBy(actor()); attach.setUpdatedBy(actor());
        try {
            attachMapper.insert(attach);

            var ver = new SysAttachmentVersionEntity();
            ver.setAttachmentId(attach.getId()); ver.setVersion(1);
            ver.setFileName(safeName); ver.setStorageKey(storageKey);
            ver.setMimeType(verifiedMimeType); ver.setFileSize((long) content.length);
            ver.setFileSha256(sha256); ver.setCreatedBy(actor());
            attachVerMapper.insert(ver);
        } catch (RuntimeException persistenceFailure) {
            deleteStoredFile(targetPath);
            throw persistenceFailure;
        }

        return attach;
    }

    public byte[] download(Long attachmentId) {
        var attach = attachMapper.selectById(attachmentId);
        if (attach == null || !"ACTIVE".equals(attach.getStatus())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Path path = Path.of(attachmentRoot, attach.getStorageKey());
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "File not found on disk");
        }
    }

    public SysAttachmentEntity getAttachment(Long id) {
        var attach = attachMapper.selectById(id);
        if (attach == null || !"ACTIVE".equals(attach.getStatus())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return attach;
    }

    public List<SysAttachmentEntity> listByOwner(String ownerType, String ownerId) {
        return attachMapper.selectList(new LambdaQueryWrapper<SysAttachmentEntity>()
                .eq(SysAttachmentEntity::getOwnerType, ownerType)
                .eq(SysAttachmentEntity::getOwnerId, ownerId)
                .eq(SysAttachmentEntity::getStatus, "ACTIVE")
                .orderByDesc(SysAttachmentEntity::getCreatedAt));
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        var attach = attachMapper.selectById(attachmentId);
        if (attach == null || !"ACTIVE".equals(attach.getStatus())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        attach.setStatus("DELETED"); attach.setUpdatedBy(actor());
        attachMapper.updateById(attach);
    }

    @Transactional
    public SysAttachmentEntity replaceVersion(Long attachmentId, String originalName,
                                               String mimeType, byte[] content) {
        var attach = attachMapper.selectById(attachmentId);
        if (attach == null || !"ACTIVE".equals(attach.getStatus())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        String verifiedMimeType = validateFile(originalName, mimeType, content);

        String storageKey = generateStorageKey();
        String sha256 = sha256(content);
        String safeName = sanitizeFileName(originalName);
        Path targetPath = Path.of(attachmentRoot, storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to save file");
        }

        int nextVer = attachVerMapper.selectList(new LambdaQueryWrapper<SysAttachmentVersionEntity>()
                .eq(SysAttachmentVersionEntity::getAttachmentId, attachmentId)).size() + 1;

        var ver = new SysAttachmentVersionEntity();
        ver.setAttachmentId(attachmentId); ver.setVersion(nextVer);
        ver.setFileName(safeName); ver.setStorageKey(storageKey);
        ver.setMimeType(verifiedMimeType); ver.setFileSize((long) content.length);
        ver.setFileSha256(sha256); ver.setCreatedBy(actor());
        try {
            attachVerMapper.insert(ver);

            attach.setFileName(safeName); attach.setOriginalName(originalName);
            attach.setStorageKey(storageKey); attach.setMimeType(verifiedMimeType);
            attach.setFileSize((long) content.length);
            attach.setFileSha256(sha256); attach.setUpdatedBy(actor());
            attachMapper.updateById(attach);
        } catch (RuntimeException persistenceFailure) {
            deleteStoredFile(targetPath);
            throw persistenceFailure;
        }

        return attach;
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String validateFile(String originalName, String claimedMimeType, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File content is empty");
        }
        if (content.length > maxFileSize) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File exceeds configured size limit");
        }
        if (originalName == null || originalName.isBlank() || originalName.length() > 255
                || originalName.indexOf('\0') >= 0 || originalName.contains("/")
                || originalName.contains("\\") || originalName.contains("..")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid file name");
        }
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String[] parts = lowerName.split("\\.");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File extension is required");
        }
        Set<String> dangerous = Set.of("exe", "com", "bat", "cmd", "sh", "js", "jar",
                "scr", "msi", "php", "html", "htm", "svg");
        for (int i = 1; i < parts.length; i++) {
            if (dangerous.contains(parts[i])) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Executable or active-content extension is not allowed");
            }
        }
        String extension = parts[parts.length - 1];
        String extensionMime = mimeForExtension(extension);
        if (extensionMime == null || !allowedMimeTypes.contains(extensionMime)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File type is not allowed");
        }
        if (claimedMimeType == null || !claimedMimeType.equalsIgnoreCase(extensionMime)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File MIME does not match its extension");
        }
        if (!signatureMatches(extension, content)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File content does not match its declared type");
        }
        return extensionMime;
    }

    private String mimeForExtension(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "zip" -> "application/zip";
            default -> null;
        };
    }

    private boolean signatureMatches(String extension, byte[] content) {
        return switch (extension) {
            case "pdf" -> startsWith(content, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "jpg", "jpeg" -> content.length >= 3 && (content[0] & 0xff) == 0xff
                    && (content[1] & 0xff) == 0xd8 && (content[2] & 0xff) == 0xff;
            case "png" -> startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case "gif" -> startsWith(content, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    || startsWith(content, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "doc", "xls" -> startsWith(content,
                    new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1});
            case "docx", "xlsx", "zip" -> startsWith(content, new byte[]{0x50, 0x4b});
            case "txt", "csv" -> isText(content);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean isText(byte[] content) {
        for (byte value : content) if (value == 0) return false;
        try {
            java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(content));
            return true;
        } catch (java.nio.charset.CharacterCodingException invalidText) {
            return false;
        }
    }

    private void deleteStoredFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The database transaction still fails; orphan cleanup is an operational follow-up.
        }
    }

    private String generateStorageKey() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String hex = java.util.HexFormat.of().formatHex(bytes);
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + hex;
    }

    private String sha256(byte[] content) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(content));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private Attachment toDomain(SysAttachmentEntity entity) {
        return new Attachment(entity.getId(), entity.getFileName(), entity.getOriginalName(),
                entity.getMimeType(), entity.getFileSize(), entity.getFileSha256(),
                entity.getOwnerType(), entity.getOwnerId(), entity.getScanStatus(),
                entity.getStatus(), entity.getCreatedBy(), entity.getCreatedAt());
    }
}

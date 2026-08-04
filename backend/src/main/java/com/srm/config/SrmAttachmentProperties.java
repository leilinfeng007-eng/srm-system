package com.srm.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "srm.attachment")
public class SrmAttachmentProperties {
    private String root = "/var/lib/srm/attachments";
    private String maxFileSize = "20MB";
    private Set<String> allowedMimeTypes = new LinkedHashSet<>(Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/gif",
            "text/plain", "text/csv", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip"));

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public String getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(String maxFileSize) { this.maxFileSize = maxFileSize; }
    public Set<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = new LinkedHashSet<>(allowedMimeTypes);
    }
}

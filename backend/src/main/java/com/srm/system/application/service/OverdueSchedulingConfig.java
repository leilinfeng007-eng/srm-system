package com.srm.system.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "srm.sla.scan-enabled", havingValue = "true", matchIfMissing = true)
public class OverdueSchedulingConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueSchedulingConfig.class);

    private final OverdueTaskScanner scanner;

    public OverdueSchedulingConfig(OverdueTaskScanner scanner) {
        this.scanner = scanner;
    }

    @Scheduled(fixedDelayString = "${srm.sla.scan-interval-ms:300000}")
    public void scheduledScan() {
        try {
            int processed = scanner.scanOnce();
            if (processed > 0) {
                LOG.info("Overdue scan processed {} overdue nodes", processed);
            }
        } catch (Exception e) {
            LOG.error("Overdue scan failed: {}", e.getMessage(), e);
        }
    }
}

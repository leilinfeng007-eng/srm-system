package com.srm.system.application.service;

import com.srm.system.domain.repository.OverdueTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OverdueTaskScanner {

    private final OverdueTaskRepository repository;

    @Value("${srm.sla.scan-batch-size:100}")
    private int batchSize;

    public OverdueTaskScanner(OverdueTaskRepository repository) {
        this.repository = repository;
    }

    public int scanOnce() {
        return repository.scanOnce(batchSize);
    }
}

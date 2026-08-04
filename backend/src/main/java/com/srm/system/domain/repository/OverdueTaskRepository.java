package com.srm.system.domain.repository;

public interface OverdueTaskRepository {
    int scanOnce(int batchSize);
}

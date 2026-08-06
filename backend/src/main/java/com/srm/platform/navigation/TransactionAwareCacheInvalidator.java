package com.srm.platform.navigation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TransactionAwareCacheInvalidator {

    private final NavigationCacheInvalidator delegate;

    public TransactionAwareCacheInvalidator(NavigationCacheInvalidator delegate) {
        this.delegate = delegate;
    }

    public void evictUser(long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delegate.evictUser(userId);
                }
            });
        } else {
            delegate.evictUser(userId);
        }
    }

    public void evictAll() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delegate.evictAll();
                }
            });
        } else {
            delegate.evictAll();
        }
    }
}

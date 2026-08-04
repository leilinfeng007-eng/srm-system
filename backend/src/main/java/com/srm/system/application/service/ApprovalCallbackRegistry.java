package com.srm.system.application.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ApprovalCallbackRegistry {

    public interface ApprovalCallback {
        String businessType();
        void onApproved(String businessId);
        void onRejected(String businessId);
        default void onWithdrawn(String businessId) { }
        default void onCancelled(String businessId) { onRejected(businessId); }
    }

    private final Map<String, ApprovalCallback> callbacks = new ConcurrentHashMap<>();

    public void register(ApprovalCallback callback) {
        callbacks.put(callback.businessType(), callback);
    }

    public ApprovalCallback find(String businessType) {
        return callbacks.get(businessType);
    }
}

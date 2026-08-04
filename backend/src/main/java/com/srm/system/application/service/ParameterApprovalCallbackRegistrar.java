package com.srm.system.application.service;

import com.srm.system.application.service.ApprovalCallbackRegistry.ApprovalCallback;
import com.srm.system.domain.repository.ParameterVersionRepository;
import org.springframework.stereotype.Component;

@Component
public class ParameterApprovalCallbackRegistrar {

    public ParameterApprovalCallbackRegistrar(ApprovalCallbackRegistry registry,
                                              ParameterVersionRepository parameterRepo) {
        registry.register(new ApprovalCallback() {
            @Override
            public String businessType() { return "SYSTEM_PARAMETER"; }

            @Override
            public void onApproved(String businessId) {
                parameterRepo.approveVersion(Long.parseLong(businessId));
            }

            @Override
            public void onRejected(String businessId) {
                parameterRepo.rejectVersion(Long.parseLong(businessId));
            }

            @Override
            public void onWithdrawn(String businessId) {
                parameterRepo.withdrawVersion(Long.parseLong(businessId));
            }
        });
    }
}

package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.platform.integration.InboxEventHandler;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InboxHandlerRegistry {

    private final List<InboxEventHandler> handlers;

    public InboxHandlerRegistry(List<InboxEventHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public InboxEventHandler require(String objectType) {
        return handlers.stream().filter(handler -> handler.supports(objectType)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "No Inbox handler registered for object type: " + objectType));
    }
}

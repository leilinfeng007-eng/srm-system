package com.srm.platform.integration;

public interface InboxEventHandler {

    boolean supports(String objectType);

    void handle(InboxEventPayload event);
}

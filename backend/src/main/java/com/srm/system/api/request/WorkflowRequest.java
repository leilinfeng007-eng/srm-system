package com.srm.system.api.request;

import com.srm.system.application.service.SystemGovernanceFacade.WorkflowNodeCommand;
import java.util.List;

public record WorkflowRequest(String processCode, String processName, String businessType,
                              String description, List<WorkflowNodeCommand> nodes) {}

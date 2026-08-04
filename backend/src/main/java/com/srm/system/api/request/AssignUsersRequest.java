package com.srm.system.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignUsersRequest(
        @NotNull List<Long> userIds) {
}

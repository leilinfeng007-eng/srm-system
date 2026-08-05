package com.srm.system.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceUserRolesRequest(
        @NotNull List<Long> roleIds) {
}

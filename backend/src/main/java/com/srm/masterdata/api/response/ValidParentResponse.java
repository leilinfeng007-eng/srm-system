package com.srm.masterdata.api.response;

import java.util.List;

public record ValidParentResponse(
        String orgType,
        List<OrganizationDetail> parents) {
}

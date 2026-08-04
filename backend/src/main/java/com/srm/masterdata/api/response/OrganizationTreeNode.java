package com.srm.masterdata.api.response;

import java.util.List;

public record OrganizationTreeNode(
        Long id,
        String orgCode,
        String orgName,
        String orgType,
        Integer sortOrder,
        String status,
        Integer level,
        List<OrganizationTreeNode> children) {
}

package com.srm.system.api.response;

import java.util.List;

public record DepartmentTreeNode(
        Long id,
        String deptCode,
        String deptName,
        Integer sortOrder,
        String status,
        Integer level,
        List<DepartmentTreeNode> children) {
}

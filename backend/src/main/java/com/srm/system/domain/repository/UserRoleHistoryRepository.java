package com.srm.system.domain.repository;

import com.srm.system.domain.model.UserRoleHistory;
import java.util.List;

public interface UserRoleHistoryRepository {

    UserRoleHistory save(UserRoleHistory history);

    List<UserRoleHistory> findByUserId(Long userId);

    List<UserRoleHistory> findByRoleId(Long roleId);
}

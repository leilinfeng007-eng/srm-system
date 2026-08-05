package com.srm.system.domain.repository;

import com.srm.system.domain.model.UserOperationLogEntry;
import java.util.List;

public interface UserOperationLogRepository {

    List<UserOperationLogEntry> findByTargetUser(Long userId, List<String> actionCodes);
}

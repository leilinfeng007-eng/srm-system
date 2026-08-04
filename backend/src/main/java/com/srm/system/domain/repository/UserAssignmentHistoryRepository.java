package com.srm.system.domain.repository;

import com.srm.system.domain.model.UserAssignmentHistory;
import java.util.List;

public interface UserAssignmentHistoryRepository {

    UserAssignmentHistory save(UserAssignmentHistory history);

    List<UserAssignmentHistory> findByUserId(Long userId);
}

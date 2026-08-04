package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.api.response.EffectivePermissionResponse;
import com.srm.system.api.response.MenuCatalogNode;
import com.srm.system.api.response.UserEffectivePermissionsResponse;
import com.srm.system.domain.model.EffectivePermissionGrant;
import com.srm.system.domain.model.MenuCatalogItem;
import com.srm.system.domain.model.PermissionCatalogItem;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.repository.AuthorizationCatalogRepository;
import com.srm.system.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationCatalogApplicationService {
    private final AuthorizationCatalogRepository repository;
    private final UserRepository users;
    private final DataScopeAuthorizationService scopes;

    public AuthorizationCatalogApplicationService(AuthorizationCatalogRepository repository,
            UserRepository users, DataScopeAuthorizationService scopes) {
        this.repository = repository;
        this.users = users;
        this.scopes = scopes;
    }

    @Transactional(readOnly = true)
    public PageResult<PermissionCatalogItem> permissions(int page, int pageSize) {
        requireAll("system:permission:view");
        validatePaging(page, pageSize);
        return PageResult.of(repository.findPermissions((page - 1) * pageSize, pageSize),
                page, pageSize, repository.countPermissions());
    }

    @Transactional(readOnly = true)
    public List<MenuCatalogNode> menus() {
        requireAll("system:role:view");
        List<MenuCatalogItem> items = repository.findEnabledMenus();
        Map<Long, List<MenuCatalogItem>> byParent = new LinkedHashMap<>();
        for (MenuCatalogItem item : items) {
            byParent.computeIfAbsent(item.parentId(), ignored -> new ArrayList<>()).add(item);
        }
        return nodes(byParent, null);
    }

    @Transactional(readOnly = true)
    public UserEffectivePermissionsResponse effectivePermissions(Long userId) {
        requireAll("system:permission:view");
        users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Map<String, List<EffectivePermissionGrant>> grouped = repository
                .findEffectivePermissionGrants(userId).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        EffectivePermissionGrant::permissionCode, LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        List<EffectivePermissionResponse> permissions = grouped.entrySet().stream().map(entry -> {
            List<String> roles = entry.getValue().stream().map(EffectivePermissionGrant::roleCode)
                    .distinct().sorted().toList();
            List<String> paths = entry.getValue().stream()
                    .map(g -> g.domainCode() + ":" + g.dimensionCode() + "="
                            + g.scopeType() + "/" + g.operationMode())
                    .distinct().sorted().toList();
            return new EffectivePermissionResponse(entry.getKey(), roles, paths);
        }).toList();
        return new UserEffectivePermissionsResponse(userId, permissions);
    }

    private List<MenuCatalogNode> nodes(Map<Long, List<MenuCatalogItem>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(MenuCatalogItem::sortOrder)
                        .thenComparing(MenuCatalogItem::id))
                .map(item -> new MenuCatalogNode(item.id(), item.menuCode(), item.label(),
                        item.route(), item.componentKey(), item.permissionCode(), item.sortOrder(),
                        nodes(byParent, item.id())))
                .toList();
    }

    private void requireAll(String permission) {
        if (!scopes.requireRead(permission, "system", "ORGANIZATION").isAllScope()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validatePaging(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 1000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid paging parameters");
        }
    }
}

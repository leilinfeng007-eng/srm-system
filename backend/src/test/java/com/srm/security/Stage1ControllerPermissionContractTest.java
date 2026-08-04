package com.srm.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.masterdata.api.controller.OrganizationController;
import com.srm.system.api.controller.RoleController;
import com.srm.system.api.controller.UserController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class Stage1ControllerPermissionContractTest {

    @Test
    void userMutationsUseTheirExactPermissionCodes() throws Exception {
        assertPermission(UserController.class, "update", "system:user:update", Long.class,
                com.srm.system.api.request.UpdateUserRequest.class);
        assertPermission(UserController.class, "enable", "system:user:enable", Long.class, Long.class);
        assertPermission(UserController.class, "disable", "system:user:disable", Long.class, Long.class);
        assertPermission(UserController.class, "resetPassword", "system:user:reset-password", Long.class);
    }

    @Test
    void roleAndOrganizationStatusMutationsUseExactPermissionCodes() throws Exception {
        assertPermission(RoleController.class, "enable", "system:role:enable", Long.class);
        assertPermission(RoleController.class, "disable", "system:role:disable", Long.class);
        assertPermission(OrganizationController.class, "enable", "masterdata:organization:enable", Long.class);
        assertPermission(OrganizationController.class, "disable", "masterdata:organization:disable", Long.class);
    }

    private void assertPermission(Class<?> type, String methodName, String permission,
                                  Class<?>... parameterTypes) throws Exception {
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('" + permission + "')");
    }
}

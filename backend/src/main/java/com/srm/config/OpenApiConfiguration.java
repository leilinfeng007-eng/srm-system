package com.srm.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "SRM 供应商协同管理系统 API",
        version = "v1",
        description = "阶段0工程基线接口；业务域接口由后续阶段按详细设计补充。"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfiguration {

    @Bean
    GroupedOpenApi internalOpenApi() {
        return GroupedOpenApi.builder()
                .group("internal")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(openApi -> applyInfo(
                        openApi,
                        "SRM 内部管理端 API",
                        "内部用户认证、RBAC、动态菜单及阶段0工程基线接口。",
                        true))
                .build();
    }

    @Bean
    GroupedOpenApi supplierOpenApi() {
        return GroupedOpenApi.builder()
                .group("supplier")
                .pathsToMatch("/supplier-api/v1/**")
                .addOpenApiCustomizer(openApi -> applyInfo(
                        openApi,
                        "SRM 供应商端 API",
                        "供应商端独立契约；阶段0未发布供应商业务操作。",
                        false))
                .build();
    }

    private void applyInfo(OpenAPI openApi, String title, String description, boolean securedDomain) {
        openApi.info(new io.swagger.v3.oas.models.info.Info()
                .title(title)
                .version("v1")
                .description(description));
        openApi.servers(List.of(new Server()
                .url("/")
                .description("通过当前站点的同源网关访问")));
        if (securedDomain) {
            openApi.security(List.of(new SecurityRequirement().addList("bearerAuth")));
            openApi.getPaths().get("/api/v1/auth/login").getPost().setSecurity(Collections.emptyList());
            openApi.getPaths().get("/api/v1/auth/refresh").getPost().setSecurity(Collections.emptyList());
        } else if (openApi.getComponents() != null
                && openApi.getComponents().getSecuritySchemes() != null) {
            openApi.getComponents().getSecuritySchemes().remove("bearerAuth");
        }
    }
}

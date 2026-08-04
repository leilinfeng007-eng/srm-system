package com.srm.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "SRM 供应商协同管理系统 API",
        version = "v1",
        description = "阶段1平台治理、组织身份、审批任务和主数据底座接口。"))
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
                .addOperationCustomizer(operationSummaryCustomizer())
                .addOpenApiCustomizer(openApi -> applyInfo(
                        openApi,
                        "SRM 内部管理端 API",
                        "内部用户认证、RBAC、动态菜单、审批任务、系统治理及阶段1主数据接口。",
                        true))
                .build();
    }

    @Bean
    GroupedOpenApi supplierOpenApi() {
        return GroupedOpenApi.builder()
                .group("supplier")
                .pathsToMatch("/supplier-api/v1/**")
                .addOperationCustomizer(operationSummaryCustomizer())
                .addOpenApiCustomizer(openApi -> applyInfo(
                        openApi,
                        "SRM 供应商端 API",
                        "供应商端独立契约；阶段1仍未发布供应商业务操作。",
                        false))
                .build();
    }

    private OperationCustomizer operationSummaryCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                operation.setSummary(actionLabel(handlerMethod.getMethod().getName()) + " "
                        + resourceLabel(handlerMethod.getBeanType().getSimpleName(),
                        handlerMethod.getMethod().getName()));
            }
            return operation;
        };
    }

    private String actionLabel(String method) {
        Map<String, String> exact = Map.ofEntries(
                Map.entry("unreadCount", "Count unread"),
                Map.entry("overdueCount", "Count overdue"),
                Map.entry("markAllRead", "Mark all read"),
                Map.entry("markRead", "Mark read"),
                Map.entry("listByOwner", "List by owner"),
                Map.entry("bindAttachment", "Bind attachment"),
                Map.entry("effective", "Resolve effective"),
                Map.entry("permissions", "List"),
                Map.entry("menus", "List"),
                Map.entry("template", "Download import template"),
                Map.entry("errors", "List errors"),
                Map.entry("versions", "List versions"));
        if (exact.containsKey(method)) return exact.get(method);
        for (var prefix : Map.ofEntries(
                Map.entry("create", "Create"), Map.entry("update", "Update"),
                Map.entry("delete", "Delete"), Map.entry("enable", "Enable"),
                Map.entry("disable", "Disable"), Map.entry("list", "List"),
                Map.entry("get", "Get"), Map.entry("upload", "Upload"),
                Map.entry("download", "Download"), Map.entry("replace", "Replace"),
                Map.entry("retry", "Retry"), Map.entry("start", "Start"),
                Map.entry("receive", "Receive"), Map.entry("generate", "Generate"),
                Map.entry("submit", "Submit"), Map.entry("publish", "Publish"),
                Map.entry("retire", "Retire"), Map.entry("approve", "Approve"),
                Map.entry("reject", "Reject"), Map.entry("withdraw", "Withdraw"),
                Map.entry("cancel", "Cancel")).entrySet()) {
            if (method.startsWith(prefix.getKey())) return prefix.getValue();
        }
        return "Execute";
    }

    private String resourceLabel(String controller, String method) {
        if ("MasterDataController".equals(controller)) {
            String suffix = method.replaceFirst("^(list|create|get|update|enable|disable)", "");
            return Map.of("PO", "purchasing organizations", "DL", "delivery locations",
                    "Cat", "categories", "Unit", "units", "Curr", "currencies",
                    "Tax", "tax codes", "Mat", "materials", "Map", "external mappings")
                    .getOrDefault(suffix, "master data");
        }
        String base = controller.replaceFirst("Controller$", "")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        if ("Dictionary".equals(base) && method.contains("Item")) return "dictionary item";
        if ("Batch Job".equals(base) && method.equals("errors")) return "batch job errors";
        return base.toLowerCase(java.util.Locale.ROOT);
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

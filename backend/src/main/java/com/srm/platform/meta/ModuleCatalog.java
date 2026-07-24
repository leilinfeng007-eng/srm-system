package com.srm.platform.meta;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModuleCatalog {

    private final List<ModuleMeta> modules = List.of(
            module("workbench", "工作台", "/workbench", 1, 1),
            module("supplier", "供应商管理", "/supplier", 2, 2),
            module("sourcing", "战略寻源", "/sourcing", 3, 4),
            module("contract", "合同与价格管理", "/contract", 4, 5),
            module("source", "供应源管理", "/supply-source", 5, 3),
            module("procurement", "采购协同", "/procurement", 6, 6),
            module("delivery", "交付与收货协同", "/delivery", 7, 7),
            module("quality", "供应商质量管理", "/quality", 8, 8),
            module("settlement", "对账与结算协同", "/settlement", 9, 9),
            module("performance", "绩效与风险管理", "/performance", 10, 10),
            module("masterdata", "基础数据中心", "/master-data", 11, 1),
            module("system", "系统管理", "/system", 12, 1));

    public List<ModuleMeta> all() {
        return modules;
    }

    private ModuleMeta module(String code, String label, String route, int sortOrder, int phase) {
        return new ModuleMeta(
                code,
                label,
                "com.srm." + code,
                route,
                code,
                sortOrder,
                phase,
                "SKELETON");
    }
}


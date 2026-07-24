# 模块详细设计索引

阶段0只冻结 12 个一级领域的目录、路由、权限前缀和页面骨架，不批量创建空白详细设计。进入相应业务阶段前，模块负责人必须基于蓝图补齐领域模型、状态机、权限矩阵、接口和数据库字段设计。

| 序号 | 一级域 | 后端包 | 前端目录 | 路由前缀 | 计划阶段 |
| --- | --- | --- | --- | --- | --- |
| 01 | 工作台 | `com.srm.workbench` | `views/workbench` | `/workbench` | 阶段1+ |
| 02 | 供应商管理 | `com.srm.supplier` | `views/supplier` | `/supplier` | 阶段2 |
| 03 | 战略寻源 | `com.srm.sourcing` | `views/sourcing` | `/sourcing` | 阶段4 |
| 04 | 合同与价格管理 | `com.srm.contract` | `views/contract` | `/contract` | 阶段5 |
| 05 | 供应源管理 | `com.srm.source` | `views/source` | `/supply-source` | 阶段3 |
| 06 | 采购协同 | `com.srm.procurement` | `views/procurement` | `/procurement` | 阶段6 |
| 07 | 交付与收货协同 | `com.srm.delivery` | `views/delivery` | `/delivery` | 阶段7 |
| 08 | 供应商质量管理 | `com.srm.quality` | `views/quality` | `/quality` | 阶段8 |
| 09 | 对账与结算协同 | `com.srm.settlement` | `views/settlement` | `/settlement` | 阶段9 |
| 10 | 绩效与风险管理 | `com.srm.performance` | `views/performance` | `/performance` | 阶段10 |
| 11 | 基础数据中心 | `com.srm.masterdata` | `views/masterdata` | `/master-data` | 阶段1 |
| 12 | 系统管理 | `com.srm.system` | `views/system` | `/system` | 阶段1 |

新模块详细设计从 [模块开发模板](模块开发模板.md) 开始，并遵守根 [AGENTS.md](../../AGENTS.md)。


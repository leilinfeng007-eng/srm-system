# 生产部署模板

`docker-compose.prod.yml`是阶段0的正式生产部署模板，包含MySQL 8.4、backend、
internal-web、supplier-web和nginx五个服务。它只定义单服务器容器拓扑，不代表已经
完成服务器部署、真实MySQL迁移或数据卷持久化验收。

## GitHub Actions发布

发布工作流位于`.github/workflows/srm-release.yml`，仓库需配置以下GitHub Actions
Secrets：

- `TCR_USERNAME`：腾讯云TCR个人版登录用户名。
- `TCR_PASSWORD`：腾讯云TCR个人版登录密码或访问凭证。

向`main`提交Pull Request时只执行后端编译测试、双前端检查与构建、部署/安全静态校验，
并在临时MySQL 8.4空库中执行V1至最新版本的Flyway迁移；PR工作流不登录TCR，也不推送
镜像。合并后对`main`的push会在全部校验成功后构建并推送三个固定为`linux/amd64`的
应用镜像。也可在GitHub Actions页面通过`workflow_dispatch`手动运行所选分支。

镜像仓库与标签规则：

- `ccr.ccs.tencentyun.com/leizi114/srm-backend:sha-<12位提交号>`
- `ccr.ccs.tencentyun.com/leizi114/srm-internal-web:sha-<12位提交号>`
- `ccr.ccs.tencentyun.com/leizi114/srm-supplier-web:sha-<12位提交号>`
- 当发布引用为`main`时，三个仓库还会同时更新`latest`；生产部署应优先固定不可变的
  `sha-<12位提交号>`标签。

工作流只构建和推送上述三个SRM应用镜像，不推送MySQL、Nginx、Maven、Node、Temurin
等基础镜像。工作流不SSH连接服务器；服务器后续由运维主动登录TCR并拉取指定版本。
生产数据库密码、JWT密钥、初始账号密码和服务器侧TCR拉取凭证仅保存在生产服务器的
受限环境或秘密管理系统中。用于构建推送的TCR凭证只存为上述GitHub Actions Secrets；
两类机密都不得写入工作流正文或仓库文件。

## 准备

1. 从成功的GitHub Actions发布记录选择一个共同的不可变`sha-<12位提交号>`标签。
2. 将`prod.env.example`复制为`deploy/.env.prod`。
3. 将`SRM_IMAGE_TAG`填写为所选标签；三个`SRM_*_IMAGE`保留对应TCR仓库地址，服务器
   先完成TCR登录，再拉取三个镜像。
4. 填写其余全部空值。JWT至少32字节，数据库密码至少12字符，MySQL root密码至少16字符，
   两个初始账号密码至少12字符并包含至少三类字符。
5. `SRM_CORS_ALLOWED_ORIGINS`填写生产网关的HTTPS来源，不得使用`*`。
6. 由外部秘密管理或受限文件权限保护`deploy/.env.prod`，不得提交仓库。

## 配置检查与启动

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml config
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up --detach --wait
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml ps
```

模板对必要变量使用Compose必填表达式。MySQL入口脚本和后端启动校验还会拒绝
`CHANGE_ME`、`REPLACE_WITH`、示例值、长度不足及相同的初始账号密码。生产配置固定
关闭OpenAPI并启用Secure Cookie。

后端每次启动都会由Flyway校验并迁移生产数据库。已经在任何环境成功执行的迁移文件
（当前为V1至V3）禁止修改、重命名或删除；数据库结构变化只能新增后续版本迁移。迁移
失败会阻止后端正常启动，不得通过手工改写`flyway_schema_history`绕过。

## 停止与数据

普通停止使用：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml down
```

命名卷由`SRM_MYSQL_VOLUME_NAME`固定，普通停止不会删除。删除卷、恢复备份、升级数据库
或替换正式容器均不属于阶段0自动操作，必须经过单独审批和备份确认。

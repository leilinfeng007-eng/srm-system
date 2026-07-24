# 生产部署模板

`docker-compose.prod.yml`是阶段0的正式生产部署模板，包含MySQL 8.4、backend、
internal-web、supplier-web和nginx五个服务。它只定义单服务器容器拓扑，不代表已经
完成服务器部署、真实MySQL迁移或数据卷持久化验收。

## 准备

1. 在受控构建环境生成并扫描三个应用镜像，使用不可变版本号或摘要。
2. 将`prod.env.example`复制为`deploy/.env.prod`。
3. 填写全部空值。JWT至少32字节，数据库密码至少12字符，MySQL root密码至少16字符，
   两个初始账号密码至少12字符并包含至少三类字符。
4. `SRM_CORS_ALLOWED_ORIGINS`填写生产网关的HTTPS来源，不得使用`*`。
5. 由外部秘密管理或受限文件权限保护`deploy/.env.prod`，不得提交仓库。

## 配置检查与启动

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml config
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up --detach --wait
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml ps
```

模板对必要变量使用Compose必填表达式。MySQL入口脚本和后端启动校验还会拒绝
`CHANGE_ME`、`REPLACE_WITH`、示例值、长度不足及相同的初始账号密码。生产配置固定
关闭OpenAPI并启用Secure Cookie。

## 停止与数据

普通停止使用：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml down
```

命名卷由`SRM_MYSQL_VOLUME_NAME`固定，普通停止不会删除。删除卷、恢复备份、升级数据库
或替换正式容器均不属于阶段0自动操作，必须经过单独审批和备份确认。

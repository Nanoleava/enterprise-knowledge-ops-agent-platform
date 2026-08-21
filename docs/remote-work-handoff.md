# 异地办公与项目恢复交接手册

> 快照日期：2026-08-21
> Git 分支：`main`
> 当前里程碑：阶段 3 Day2 + 阶段 4 DAY1
> 自动化基线：137 tests，0 failures，0 errors，0 skipped

## 1. 这份手册解决什么问题

本手册用于在另一台 Windows 电脑上，仅凭 Git 仓库恢复开发环境、理解当前架构、执行数据库迁移、启动服务、验证安全链路和继续阶段 4 DAY2。

仓库保存源码、SQL 迁移、测试、脚本、接口示例、实施报告和路线图，但刻意不保存真实密码、JWT 密钥、Token、MySQL 业务数据、Redis 临时数据和用户上传的原始文件。

## 2. 当前实现边界

已经完成：

- Spring Boot REST API、MyBatis、MySQL 数据持久化。
- 用户注册、登录、JWT 验签、角色授权和统一 JSON 错误响应。
- JWT `sub` 驱动的用户、知识库、文档、切片、会话和消息 owner 隔离。
- Redis JWT 退出黑名单，黑名单依赖故障时 fail-closed。
- Redis Lua 固定窗口消息限流，普通限流依赖故障时记录错误并临时放行。
- TXT/Markdown 安全上传、本地文件存储、SHA-256、元数据和失败补偿。
- TXT/Markdown UTF-8 解析、文本清洗、解析状态机、失败重试和状态查询。
- OpenAPI/Swagger、单元测试、MockMvc、真实 MySQL、真实 Redis和临时文件系统集成测试。

阶段 4 DAY1 有意未实现：

- PDF、Word、OCR。
- 自动文本切片和批量替换 chunk。
- Embedding、向量库和 RAG。
- 消息队列和对象存储。

下一步建议严格按路线图进入阶段 4 DAY2：PDF 解析、切片策略、批量写入、处理日志、重试和删除源文件清理。

## 3. 技术栈与前置软件

| 组件 | 当前要求 |
|---|---|
| Java | 26+ |
| Maven | 使用本机 `mvn`，依赖由 `pom.xml` 管理 |
| Spring Boot | 4.1.0 |
| MySQL | 8.0+ |
| Redis | 7+ |
| Docker Desktop | 可选，用于运行本地 Redis |
| Postman | 可选，用于手工接口验收 |

新电脑首先确认：

```powershell
java -version
mvn -version
mysql --version
docker --version
git --version
```

## 4. 仓库目录说明

```text
agent-backend/
├─ docs/                         阶段报告、测试教程、路线图与本交接手册
├─ http/                         IntelliJ/HTTP Client 手工请求样例
├─ scripts/run-dev.ps1           Windows 开发启动脚本
├─ sql/001...005                 必须按顺序执行的数据库迁移
├─ src/main/java                 生产代码
├─ src/main/resources            MyBatis XML、Redis Lua、Spring 配置
├─ src/test/java                 单元、Web、安全、MySQL、Redis和文件测试
├─ storage/uploads               本地上传目录，仅保留目录，不提交用户文件
├─ .env.example                  环境变量名称和安全占位符
└─ README.md                     项目总入口
```

## 5. 从零恢复开发环境

### 5.1 克隆并确认版本

```powershell
git clone https://github.com/Nanoleava/agent-backend.git
Set-Location agent-backend
git status
git log -5 --oneline
```

### 5.2 创建数据库并按顺序迁移

先创建 UTF-8 数据库：

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ljl_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

随后在仓库根目录执行全部迁移。每条命令都会提示输入新电脑上的 MySQL 密码：

```powershell
$repo = (Resolve-Path .).Path.Replace('\', '/')
mysql -u root -p --database=ljl_agent --execute="source $repo/sql/001_init_schema.sql"
mysql -u root -p --database=ljl_agent --execute="source $repo/sql/002_add_document_title_unique.sql"
mysql -u root -p --database=ljl_agent --execute="source $repo/sql/003_complete_chat_schema.sql"
mysql -u root -p --database=ljl_agent --execute="source $repo/sql/004_align_user_schema.sql"
mysql -u root -p --database=ljl_agent --execute="source $repo/sql/005_stage4_document_ingestion.sql"
```

`005_stage4_document_ingestion.sql` 可重复执行；历史手工文档会使用 `NOT_APPLICABLE` 兼容解析/切片状态。

### 5.3 启动 Redis

首次创建容器：

```powershell
docker run --name redis -p 6379:6379 -d redis:7-alpine
docker exec redis redis-cli ping
```

容器已经存在时：

```powershell
docker start redis
docker exec redis redis-cli ping
```

预期返回 `PONG`。

### 5.4 设置本地环境变量

真实值只设置在当前终端或新电脑的安全配置中，不要写入仓库：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<新电脑上的数据库密码>"
$env:DB_URL = "jdbc:mysql://localhost:3306/ljl_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DOCUMENT_STORAGE_ROOT = "C:\ljl-agent-data\uploads"
```

推荐把 `DOCUMENT_STORAGE_ROOT` 放在 Git 仓库外。若不设置，开发默认值为 `./storage/uploads`。

### 5.5 启动应用

```powershell
.\scripts\run-dev.ps1
```

脚本会启用 `dev` profile，并为当前进程生成缺失或非法的开发 JWT 密钥。生产环境必须提供稳定、安全的 `JWT_SECRET_BASE64`。

启动后检查：

```text
GET http://localhost:8080/api/health
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs/stage-4-day-1
```

## 6. 完整测试

基础测试：

```powershell
mvn clean test
```

包含真实 MySQL 与 Redis 的验收测试：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<测试数据库密码>"
$env:REDIS_INTEGRATION_TEST = "true"
.\scripts\run-dev.ps1 -PrepareOnly
mvn clean test
```

当前基线应看到：

```text
Tests run: 137, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

集成测试会创建并回滚/清理自己的数据和临时文件，不应污染长期业务记录。

## 7. Postman 最短验收链路

1. `POST /api/users/register` 注册普通用户。
2. `POST /api/auth/login` 获取 `accessToken`。
3. `POST /api/knowledge-bases` 创建知识库，记录返回的知识库 ID。
4. `POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload` 上传 UTF-8 `.txt` 或 `.md`。
5. `POST /api/documents/{documentId}/parse` 解析文档。
6. `GET /api/documents/{documentId}/processing-status` 查看处理状态。
7. `GET /api/documents/{documentId}` 查看 owner 校验后的正文 `content`。

上传接口必须选择 `Body -> form-data`：

| Key | 类型 | 内容 |
|---|---|---|
| `file` | File | UTF-8 TXT/Markdown 文件 |
| `title` | Text | 可选标题 |

不要手工设置 multipart `Content-Type`，Postman 必须自行生成 boundary。

首次解析请求：

```json
{
  "force": false
}
```

已成功文档需要重新解析时使用 `force: true`。状态查询是 GET，请选择 `Body: none`。

## 8. 文档摄取数据落点

上传后保存两份不同用途的数据：

1. 原始文件保存在 `DOCUMENT_STORAGE_ROOT/{userId}/{knowledgeBaseId}/{UUID}.{ext}`。
2. 清理后的完整正文保存在 MySQL `document.content`。

MySQL 同时保存原文件名、类型、字节数、相对路径、SHA-256、解析状态、切片状态、安全错误摘要和处理时间。原文件用于重试、审计和解析器升级；正文用于后续切片和 RAG。

当前 Markdown 解析是 UTF-8 文本提取与格式归一化，会保留标题、列表、引用、代码块等 Markdown 语义。它还不是 AI 总结、Embedding 或语义检索。

## 9. Git 不会迁移的本地数据

以下内容被 `.gitignore` 排除：

- `target/`
- `.idea/`、`*.iml`
- `.env`、本地环境文件
- `logs/`、`*.log`
- `storage/uploads/**` 中的用户原始文件

因此，在新办公地点克隆仓库只会得到可重建的工程，不会得到当前电脑上的用户账号、知识库、文档正文或上传源文件。

如果确实需要迁移本地业务数据，应在 Git 之外通过受保护渠道传输：

```powershell
mysqldump -u root -p --single-transaction --routines --triggers ljl_agent > ljl_agent-local-backup.sql
Compress-Archive -LiteralPath "C:\ljl-agent-data\uploads" -DestinationPath "ljl-agent-uploads.zip"
```

备份可能包含账号摘要、业务正文和用户文件，不得提交到公共 Git 仓库。恢复后必须保持文件相对路径不变，并让 `DOCUMENT_STORAGE_ROOT` 指向恢复目录。

## 10. 常见错误定位

| 现象 | 首要检查项 |
|---|---|
| 启动提示缺少 `DB_PASSWORD` | 当前 PowerShell 是否设置数据库密码 |
| JWT 配置启动失败 | `JWT_SECRET_BASE64` 是否为合法 Base64 且解码至少 32 字节 |
| 401 | Token 是否过期、格式是否为 `Bearer <token>`、是否已退出登录 |
| 403 | 知识库/文档是否属于 JWT `sub` 对应用户 |
| 上传提示缺少 `file` | form-data 的 Key 必须精确写成小写 `file`，不是 Description |
| 400/415 类文件错误 | 是否为允许的 UTF-8 `.txt`、`.md`、`.markdown` |
| 413 | 文件是否超过默认 10MB |
| 422 | 查看 `processing-status` 的安全错误摘要，修复后重试 |
| 重新解析冲突 | 成功文档使用 `{ "force": true }` |
| 找不到源文件 | 检查 `DOCUMENT_STORAGE_ROOT` 和数据库相对路径是否对应 |
| logout/鉴权返回 503 | Redis 黑名单依赖不可用，检查容器和连接配置 |
| 消息限流临时失效 | 检查 Redis；普通限流故障采用可用性优先策略 |

## 11. 详细资料索引

- `README.md`：项目总览、启动、接口和测试入口。
- `docs/stage3-day1-implementation-report.md`：JWT 与安全基线。
- `docs/stage3-day1-idea-manual-test-tutorial.md`：IDEA 手工验证教程。
- `docs/stage3-day2-implementation-report.md`：Redis 黑名单、限流和 owner 隔离。
- `docs/stage4-day1-implementation-report.md`：安全上传、解析、状态机和 Postman 教程。
- `docs/reference/java-backend-agent-project-roadmap.md`：完整学习与项目路线图。
- `docs/reference/stage4-day1-project-modification-guideline.md`：DAY1 具体改造约束。
- `http/day1-security-manual-test.http`：阶段 3 Day1 手工请求。
- `http/day2-security-redis-manual-test.http`：阶段 3 Day2 Redis 手工请求。
- `sql/001_init_schema.sql` 至 `sql/005_stage4_document_ingestion.sql`：数据库演进链。

## 12. 提交前和继续开发前检查

```powershell
git status --short
git diff --check
mvn clean test
```

继续开发时不要绕过现有的 Principal owner 校验、短事务、状态机、文件补偿和安全错误摘要。阶段报告中的历史测试数字只代表当时快照，任何新改动都应重新执行测试。

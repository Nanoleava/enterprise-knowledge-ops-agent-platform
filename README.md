# LJL Java Agent Backend

这是阶段 4 DAY 1 实施版的 Spring Boot REST API 项目。阶段 3 的 Spring Security、JWT、Principal owner 隔离、Redis 退出黑名单与 Lua 限流保持不变；DAY 1 新增 TXT/Markdown 安全上传、本地存储、解析清洗和处理状态。

当前认证入口为 `POST /api/auth/login`。核心业务 owner 只取 JWT `sub`，不信任 body/query 中的 `userId`。已认证用户可向自己的知识库上传 UTF-8 TXT/Markdown，再通过独立解析接口把清洗后的文本保存到 `document.content`；自动切片、PDF 和 RAG 属于 DAY 2 或后续阶段。

> 当前验收状态：阶段 4 DAY 1 已完成代码、真实 MySQL、真实 Redis、临时文件系统和 OpenAPI 自动化验证。详细证据与 Postman 教程见 `docs/stage4-day1-implementation-report.md`；在其他电脑恢复项目请从 `docs/remote-work-handoff.md` 开始。

## 技术栈

- Java 26
- Spring Boot 4.1.0
- Spring Security 7.1.0（由 Spring Boot BOM 管理）
- OAuth2 Resource Server + Nimbus JWT（HS256）
- MyBatis Spring Boot Starter 4.0.1
- MySQL 8.x
- Redis 7.x（本地可使用单独的 Docker 容器）
- springdoc-openapi 3.0.3
- Maven、JUnit 5、Mockito、MockMvc

## 分层与请求链路

```text
HTTP Request
-> Spring Security Filter Chain
-> Bearer JWT 验签、issuer/时间校验、角色转换
-> Redis `auth:blacklist:{jti}` 撤销检查
-> Authentication / SecurityContext
-> Controller：从 Principal 读取 currentUserId、绑定 DTO、触发 @Valid
-> 可选 Redis Lua `rate:http:{userId}:{windowStart}` 限流
-> Service：业务规则、对象归属校验、事务、Entity/VO 转换
-> DocumentIngestionService：上传/解析编排与补偿
-> FileStorageService / Parser / TextCleaner：受控文件 IO、格式策略与清洗
-> Mapper 接口 + MyBatis XML：带 user_id 边界执行 SQL
-> MySQL：约束、索引、持久化
-> Entity
-> VO
-> Result / PageResult
-> HTTP JSON Response
```

业务错误由 `BusinessException` 抛出，`GlobalExceptionHandler` 统一转换为稳定的 `Result` JSON。Security Filter 层的未认证和权限不足分别由独立 handler 返回相同的 `Result` 契约。整数错误码集中定义在 `ErrorCode`，HTTP 401/403 与五位业务错误码分层表达。

## 环境要求

```text
Java 26+
Maven 3.9+
MySQL 8.0+
Redis 7+
```

项目不会默认激活 `dev` profile，也不会在配置文件保存数据库密码。启动开发环境前应显式设置变量：

推荐使用开发启动脚本。它会激活 `dev` profile，并在 `JWT_SECRET_BASE64` 缺失、含有 `$` 等非法字符或解码后不足 32 字节时，为本次 Maven 进程生成新的 32 字节随机密钥；密钥不会写入文件或输出到终端：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
.\scripts\run-dev.ps1
```

也可以手工设置全部变量后启动：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"

# 必需：生成至少 32 字节随机密钥并保存为 Base64 环境变量（兼容 Windows PowerShell 5.1）
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtKeyBytes) } finally { $jwtRandom.Dispose() }
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtKeyBytes)
$env:JWT_ISSUER = "https://ljl-agent-backend.local"
$env:JWT_ACCESS_TOKEN_TTL = "30m"

$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
# 本地无密码容器不要设置 REDIS_PASSWORD；启用认证时只通过环境注入。
$env:REDIS_DATABASE = "0"

# 阶段 4 DAY 1 文档上传；以下均可省略并使用默认值
$env:DOCUMENT_STORAGE_ROOT = "./storage/uploads"
$env:DOCUMENT_MAX_FILE_SIZE = "10MB"
$env:DOCUMENT_MAX_REQUEST_SIZE = "11MB"
$env:DOCUMENT_ALLOWED_TYPES = "TXT,MARKDOWN"

# 可选；未设置时连接本机 3306 端口的 ljl_agent
$env:DB_URL = "jdbc:mysql://localhost:3306/ljl_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

mvn spring-boot:run
```

`.env.example` 只用于说明变量名，Spring Boot 不会自动加载该文件。不要提交真实 `.env`、数据库口令、JWT 密钥、原始密码、密码摘要或完整 token。`JWT_SECRET_BASE64` 缺失、不是合法 Base64 或解码后不足 32 字节时，应用会在启动阶段失败。

### 本地 Redis（Docker Desktop）

项目只把 Docker 作为 Day 2 的本地 Redis 运行方式，不包含 Dockerfile 或 Compose 部署：

```powershell
docker version
docker ps -a --filter "name=^/redis$"

# 已存在容器时
docker start redis

# 仅在尚未创建时使用
docker run --name redis -p 6379:6379 -d redis:7

docker ps --filter "name=^/redis$"
docker exec redis redis-cli PING
# 预期 PONG

docker stop redis
docker start redis
```

Redis 连接支持 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`REDIS_CONNECT_TIMEOUT` 和 `REDIS_TIMEOUT`。黑名单是认证安全链，Redis 不可用时 fail-closed 返回 503；普通限流是资源保护，Redis 不可用时记录 ERROR 并临时放行。

如果失败原因包含 `Illegal base64 character 24`，其中 `24` 是十六进制字符码 `0x24`，代表 `$`。这说明当前变量中误放了 PBKDF2 摘要、`${JWT_SECRET_BASE64}`、`$env:JWT_SECRET_BASE64` 等文本，而不是随机字节的 Base64 编码；直接使用上述脚本即可在开发启动时安全替换该值。

## 数据库初始化与迁移

阶段 2 的规范类型为：

```text
user.id      BIGINT       对应 Java Long
user.status  TINYINT 0/1  对应 Java Integer
```

六张核心表：

| 表 | 用途 | 主要约束/索引 |
|---|---|---|
| `user` | 用户 | username、email 唯一 |
| `knowledge_base` | 用户知识库 | `(user_id, name)` 唯一 |
| `document` | 知识库文档 | `(knowledge_base_id, title)` 唯一 |
| `document_chunk` | 手工文本切片 | `(document_id, chunk_index)` 唯一，删除文档/知识库时级联 |
| `chat_session` | 聊天会话 | user_id 索引，`(id, user_id)` 唯一 |
| `chat_message` | 聊天消息 | request_id 唯一，角色检查，会话所有者联合外键 |

空库初始化：

```powershell
$projectPath = (Get-Location).Path.Replace('\', '/')
mysql -u root -p --execute="CREATE DATABASE IF NOT EXISTS ljl_agent CHARACTER SET utf8mb4"
mysql -u root -p --database=ljl_agent --execute="source $projectPath/sql/001_init_schema.sql"
```

存量库按顺序执行幂等迁移：

```powershell
$projectPath = (Get-Location).Path.Replace('\', '/')
mysql -u root -p --database=ljl_agent --execute="source $projectPath/sql/002_add_document_title_unique.sql"
mysql -u root -p --database=ljl_agent --execute="source $projectPath/sql/003_complete_chat_schema.sql"
mysql -u root -p --database=ljl_agent --execute="source $projectPath/sql/004_align_user_schema.sql"
mysql -u root -p --database=ljl_agent --execute="source $projectPath/sql/005_stage4_document_ingestion.sql"
```

`004_align_user_schema.sql` 会把历史库的 `BIGINT UNSIGNED`/字符串状态收敛为规范类型。迁移前会检查 ID 是否超出 Java `Long` 范围、状态是否能转换为 0/1；发现不兼容数据时主动中止，不会静默截断。

`005_stage4_document_ingestion.sql` 为 `document` 增加原始/存储文件名、类型、大小、受控相对路径、SHA-256、`parse_status`、`chunk_status`、安全错误摘要和处理时间。历史手工文档迁移为 `NOT_APPLICABLE`；新上传文档从 `PENDING` 开始。

## REST API

所有业务接口都返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页接口的 `data` 为：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10,
  "totalPages": 0
}
```

| 模块 | 方法与路径 | 访问规则 | 说明 |
|---|---|---|---|
| 健康检查 | `GET /api/health` | 公开 | 应用存活检查 |
| 用户 | `POST /api/users/register` | 公开 | 只能注册普通 `USER` |
| 认证 | `POST /api/auth/login` | 公开 | 校验凭据并签发 Bearer JWT |
| 认证 | `POST /api/auth/logout` | 已认证 | 撤销当前 JWT jti，TTL 为剩余有效期 |
| 用户 | `GET /api/users/me` | 已认证 | 通过 Principal 查询本人 |
| 用户 | `GET /api/users` | `ADMIN` | 查询用户列表 |
| 用户 | `GET /api/users/{id}` | `ADMIN` | 按 ID 查询用户 |
| 知识库 | `POST /api/knowledge-bases` | 已认证 | 创建知识库 |
| 知识库 | `GET /api/knowledge-bases` | 已认证 | 查询知识库列表 |
| 知识库 | `GET /api/knowledge-bases/{id}` | 已认证 | 按 ID 查询知识库 |
| 文档 | `POST /api/documents` | 已认证 | 创建文档 |
| 文档 | `GET /api/documents` | 已认证 | 分页、关键字和知识库条件查询 |
| 文档 | `GET /api/documents/{id}` | 已认证 | 按 ID 查询文档 |
| 文档 | `DELETE /api/documents/{id}` | 已认证 | 删除文档并级联清理 chunk |
| 文档 | `GET /api/knowledge-bases/{id}/documents` | 已认证 | 查询知识库文档 |
| 摄取 | `POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload` | 已认证 + KB owner | Multipart 上传 TXT/Markdown，创建 `PENDING` 文档 |
| 摄取 | `POST /api/documents/{documentId}/parse` | 已认证 + Document owner | 解析/失败重试；`force=true` 可重解析成功文档 |
| 摄取 | `GET /api/documents/{documentId}/processing-status` | 已认证 + Document owner | 查询 parse/chunk 状态与安全错误摘要 |
| 切片 | `POST /api/documents/{id}/chunks` | 已认证 | 创建手工文本切片 |
| 切片 | `GET /api/documents/{id}/chunks` | 已认证 | 按顺序查询切片 |
| 会话 | `POST /api/chat/sessions` | 已认证 | 创建聊天会话 |
| 会话 | `GET /api/chat/sessions` | 已认证 | 只查询 Principal 当前用户会话 |
| 消息 | `POST /api/chat/sessions/{id}/messages` | 已认证 | owner 校验后保存，并执行 Redis Lua 限流 |
| 消息 | `GET /api/chat/sessions/{id}/messages` | 已认证 | 稳定排序查询消息 |

登录成功后，在后续受保护请求中携带：

```http
Authorization: Bearer <access-token>
```

JWT 只包含 `sub`、`jti`、`role`、`iss`、`iat`、`exp`。`sub` 是可信 userId，角色来自数据库；JWT 不包含密码、密码摘要、邮箱或密钥。

创建知识库、文档和会话的请求体不再包含 `userId`。知识库列表、Document 分页 `count/select`、会话列表直接在 SQL 层按 currentUserId 收口；详情、删除、Chunk 和 Message 子资源操作在 Service 校验具体 owner。跨用户访问统一返回 HTTP 403。

文档分页示例：

```http
GET /api/documents?page=1&size=10&keyword=Java&knowledgeBaseId=1
```

`size` 范围为 1～100。分页 SQL 使用 MyBatis `<where>` 和 `<if>` 组合可选条件，使用 `created_at DESC, id DESC` 保证稳定排序，并通过 `LIMIT/OFFSET` 截取当前页。

DAY 1 文档摄取链路：

```text
JWT sub -> KB owner 校验 -> 文件名/大小/扩展名/MIME/UTF-8 特征校验
-> {userId}/{knowledgeBaseId}/{UUID}.{ext} 临时写入并移动
-> document 元数据 PENDING
-> parse 条件更新 PROCESSING
-> ParserRegistry(TXT/Markdown) -> TextCleaner
-> document.content + parse_status=SUCCESS
```

数据库只保存相对路径，上传响应和处理状态接口不返回物理路径或正文；文档详情接口在 owner 校验后返回 `content`，便于核对解析结果。数据库插入失败时会补偿删除刚保存的文件；解析失败保留原文件并写入 `FAILED`，以便重试。

## OpenAPI / Swagger

应用使用 `dev` profile 启动后可以访问：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 阶段 4 DAY 1 分组 JSON：<http://localhost:8080/v3/api-docs/stage-4-day-1>

OpenAPI 声明 `bearerAuth` JWT Security Scheme。开发环境可在 Swagger UI 的 Authorize 中临时输入 token；公开接口不需要 token，Swagger 不会绕过实际权限。OpenAPI 描述接口契约，但不会替代 Service 业务校验。

## 日志约定

- `INFO`：注册成功、资源创建/删除、会话和消息保存，记录业务 ID。
- `WARN`：可预期的业务拒绝，记录错误码和可定位线索。
- `ERROR`：未处理的系统异常，保留异常堆栈。

日志禁止记录原始密码、密码摘要、JWT 密钥、完整 token、Authorization Header、完整文档正文、完整切片正文和完整消息正文。

## 测试

普通回归测试不默认连接开发库：

```powershell
mvn clean test
```

真实 MySQL 集成测试只有在提供 `DB_PASSWORD` 时才执行，建议指向临时数据库：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的数据库密码"
$env:DB_URL = "jdbc:mysql://localhost:3306/ljl_agent_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
mvn clean test
```

真实 Redis 集成测试通过显式开关运行，默认连接当前本机 `redis` 容器：

```powershell
docker start redis
$env:REDIS_INTEGRATION_TEST = "true"
mvn clean test
```

测试分工：

- Service + Mockito：验证业务分支、事务边界和安全日志。
- MockMvc：验证 DTO 校验、HTTP 状态和 `Result/PageResult` JSON 契约。
- Security 集成测试：验证标准登录、JWT claims、STATELESS、USER/ADMIN、logout 后旧 token 401、Redis 故障 503 和统一 JSON。
- 真实 MySQL：验证 Mapper/XML、动态 SQL、索引/字段类型和 document/chunk 级联。
- 文档摄取集成测试：验证 TXT/Markdown 上传解析、状态流转、真实文件、失败重试和越权零污染。
- Redis 集成测试：验证 blacklist value/TTL、Lua 计数、429 和 rate key TTL 非 `-1`。
- OpenAPI 集成测试：验证 `/v3/api-docs`、Swagger UI、Bearer Scheme 和阶段 4 DAY 1 核心路径。

## 阶段 4 DAY 1 实施说明

- 上传 Controller 只负责 HTTP、Principal 与参数绑定，文件 IO 和解析由摄取服务编排。
- 原文件名只作展示；磁盘使用 UUID 名和服务端目录，`normalize + startsWith` 阻断目录穿越。
- TXT/Markdown 同时校验扩展名、声明类型、二进制特征与严格 UTF-8，不接受空文件、双扩展名和明显伪装文件。
- 文件先落盘，Document 短事务失败时精确补偿删除；解析在事务外执行，状态和正文使用短事务更新。
- `parse_status` 与 `chunk_status` 分离，阶段 2 手工文档保持 `NOT_APPLICABLE` 兼容语义。
- 状态接口只返回 documentId、parse/chunk 状态、chunk 数量、安全错误摘要与时间。

详细设计、验证证据、人工 Postman 步骤和面试表达见 [`docs/stage4-day1-implementation-report.md`](docs/stage4-day1-implementation-report.md)。

## 阶段 3 Day 2 实施说明

- 标准 Maven/Spring Boot 结构和 Controller/Service/Mapper 分层已完成。
- Entity、DTO、VO、Result、PageResult 和统一异常链路已完成。
- 六张核心表均具备可演示的创建/查询链路。
- 阶段 2 数据与业务闭环保持回归。
- `/api/auth/login` 已使用 AuthenticationManager 和既有 PBKDF2 签发 JWT；旧 `/api/users/login` 已删除。
- 官方 Resource Server 完成 HS256 签名、issuer、时间校验并建立 SecurityContext。
- health/register/login 公开，核心业务 authenticated，用户查询要求 ADMIN。
- 401/403 使用独立 Security handler 返回统一 Result JSON，业务码与 HTTP 状态分层。
- JWT 配置启动即校验，密钥只从环境变量注入；OpenAPI 支持 Bearer Authorize。
- Document 按 ID 查询、删除和 chunk 级联已完成。
- Document 分页、关键字、知识库条件和动态 SQL 已完成。
- 唯一索引、并发重复写入兜底和事务边界已完成。
- 数据库类型漂移、安全配置、结构化业务日志和 OpenAPI 已收敛。
- 单元测试、HTTP 测试、真实数据库测试和 OpenAPI 测试已覆盖阶段 2 出口能力。

Day 2 的完整设计、自动化证据、MySQL/Redis CLI 验证、Postman 从零教程、故障排查和面试表达见 [`docs/stage3-day2-implementation-report.md`](docs/stage3-day2-implementation-report.md)。

阶段 3 的面试表达：

> 我先用 Spring Security Resource Server 完成无状态 JWT 认证和 USER/ADMIN 接口授权，再把 JWT sub 统一转换为 currentUserId，由 Controller 显式传给 Service。创建资源不再接受客户端 owner，列表和分页 SQL 强制带 user_id，子资源写入先校验父资源归属，从而关闭水平越权。退出时只把 jti 写入 Redis，TTL 等于 token 剩余有效期，并由 blacklist-aware JwtDecoder 统一检查，旧 token 返回 401；Redis 故障时黑名单安全优先返回 503。消息写接口用 Lua 原子执行 INCR 和首次 EXPIRE，超限返回 429，普通限流故障则可用性优先临时放行。MySQL 保存长期业务事实，Redis 只保存短期高频状态。

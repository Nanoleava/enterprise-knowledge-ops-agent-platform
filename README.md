# LJL Java Agent Backend

这是阶段 3 Day 1 完成版的 Spring Boot REST API 项目。阶段 2 的 Controller、Service、MyBatis Mapper/XML、MySQL 数据闭环保持不变，并新增 Spring Security + JWT 的无状态认证与接口级授权。

当前认证入口为 `POST /api/auth/login`：标准 `AuthenticationManager` 复用既有 PBKDF2 密码数据，成功后签发 Bearer JWT。核心业务 API 要求已认证，用户查询接口要求 `ADMIN`。Redis 黑名单、退出、限流和基于 Principal 的对象级资源归属属于阶段 3 Day 2；文件上传、解析和自动切片属于阶段 4。

## 技术栈

- Java 26
- Spring Boot 4.1.0
- Spring Security 7.1.0（由 Spring Boot BOM 管理）
- OAuth2 Resource Server + Nimbus JWT（HS256）
- MyBatis Spring Boot Starter 4.0.1
- MySQL 8.x
- springdoc-openapi 3.0.3
- Maven、JUnit 5、Mockito、MockMvc

## 分层与请求链路

```text
HTTP Request
-> Spring Security Filter Chain
-> Bearer JWT 验签、issuer/时间校验、角色转换
-> Authentication / SecurityContext
-> Controller：接收请求、绑定 DTO、触发 @Valid
-> Service：业务规则、归属校验、事务、Entity/VO 转换
-> Mapper 接口 + MyBatis XML：执行 SQL
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

# 可选；未设置时连接本机 3306 端口的 ljl_agent
$env:DB_URL = "jdbc:mysql://localhost:3306/ljl_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

mvn spring-boot:run
```

`.env.example` 只用于说明变量名，Spring Boot 不会自动加载该文件。不要提交真实 `.env`、数据库口令、JWT 密钥、原始密码、密码摘要或完整 token。`JWT_SECRET_BASE64` 缺失、不是合法 Base64 或解码后不足 32 字节时，应用会在启动阶段失败。

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
```

`004_align_user_schema.sql` 会把历史库的 `BIGINT UNSIGNED`/字符串状态收敛为规范类型。迁移前会检查 ID 是否超出 Java `Long` 范围、状态是否能转换为 0/1；发现不兼容数据时主动中止，不会静默截断。

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
| 切片 | `POST /api/documents/{id}/chunks` | 已认证 | 创建手工文本切片 |
| 切片 | `GET /api/documents/{id}/chunks` | 已认证 | 按顺序查询切片 |
| 会话 | `POST /api/chat/sessions` | 已认证 | 创建聊天会话 |
| 会话 | `GET /api/chat/sessions?userId={id}` | 已认证 | 查询用户会话；Day 2 改为 Principal 身份 |
| 消息 | `POST /api/chat/sessions/{id}/messages` | 已认证 | 保存 USER/ASSISTANT 消息 |
| 消息 | `GET /api/chat/sessions/{id}/messages` | 已认证 | 稳定排序查询消息 |

登录成功后，在后续受保护请求中携带：

```http
Authorization: Bearer <access-token>
```

JWT 只包含 `sub`、`jti`、`role`、`iss`、`iat`、`exp`。`sub` 是可信 userId，角色来自数据库；JWT 不包含密码、密码摘要、邮箱或密钥。

文档分页示例：

```http
GET /api/documents?page=1&size=10&keyword=Java&knowledgeBaseId=1
```

`size` 范围为 1～100。分页 SQL 使用 MyBatis `<where>` 和 `<if>` 组合可选条件，使用 `created_at DESC, id DESC` 保证稳定排序，并通过 `LIMIT/OFFSET` 截取当前页。

## OpenAPI / Swagger

应用使用 `dev` profile 启动后可以访问：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 阶段 3 Day 1 分组 JSON：<http://localhost:8080/v3/api-docs/stage-3-day-1>

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

测试分工：

- Service + Mockito：验证业务分支、事务边界和安全日志。
- MockMvc：验证 DTO 校验、HTTP 状态和 `Result/PageResult` JSON 契约。
- Security 集成测试：验证标准登录、JWT claims、无/坏/过期/错误 issuer token、STATELESS、USER/ADMIN 和统一 401/403。
- 真实 MySQL：验证 Mapper/XML、动态 SQL、索引/字段类型和 document/chunk 级联。
- OpenAPI 集成测试：验证 `/v3/api-docs`、Swagger UI、Bearer Scheme 和阶段 3 Day 1 核心路径。

## 阶段 3 Day 1 验收结论

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

阶段 3 Day 1 的面试表达：

> 阶段 2 的登录只能证明单次密码正确，所以我在阶段 3 Day 1 接入 Spring Security。登录由 AuthenticationManager 调用数据库 UserDetailsService，并通过 PasswordEncoder 适配原有 PBKDF2 数据；成功后用 JwtEncoder 签发包含 sub、jti、role、iss、iat、exp 的最小化 JWT。后续 Bearer Token 由官方 Resource Server 验签并建立 SecurityContext，系统采用 STATELESS，不启用表单登录和 HTTP Basic。接口分为公开、authenticated 和 ADMIN，Security 层使用独立 handler 返回统一 JSON 401/403。Day 2 再完成 Principal 驱动的对象级资源归属和 Redis 黑名单、限流。

下一步进入阶段 3 Day 2：用 Principal/currentUserId 收口知识库、文档、切片和聊天资源归属，并实现 Redis `jti` 退出黑名单与原子限流。Day 1 不提前实现这些能力。

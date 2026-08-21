# 阶段 3 DAY 2 实施报告

> 实施日期：2026-08-19
> 实施依据：`stage3-day2-codex-implementation-guide.md` 与本仓库实际代码
> 结论口径：自动化通过不等于阶段出口全部通过；Skipped 不计为 Passed。

## 1. DAY 2 实施结论

- DAY 2：**完成**。
- 完成日期：2026-08-19；代码、全量自动化、真实 MySQL、真实 Redis和完整 HTTP 端到端剧本均已通过。
- 当前阶段：阶段 3 已完成。
- 是否允许进入阶段 4：**允许**。
- 阻塞项：无。用户已明确豁免 Postman 完整人工演示；现有 Postman 登录 200 和完整 HTTP 38/38 证据足以满足本次验收口径。

## 2. 实施前项目状态

- DAY 1 已有：Spring Security Resource Server、HS256 JWT 签发/校验、issuer 与时间校验、`STATELESS`、USER/ADMIN 角色边界、统一 `Result` 形式的 401/403、基础 Service/Mapper/MockMvc/MySQL 条件集成测试。
- owner 缺口：创建知识库、文档、会话时 DTO 接受 `userId`；部分列表/分页未以 Principal 强制过滤；详情、删除及子资源写入没有形成一致的父资源 owner 校验链。
- Redis 实施前状态：Docker Desktop 已有名为 `redis`、镜像 `redis:7`、端口 `6379:6379` 的容器，但截图和首次 CLI 检查均显示容器处于停止状态。
- P0 水平越权：通过旧代码路径静态复现——`createChunk(documentId, request)` 只按文档 ID 查询并插入，没有将 JWT 用户与文档 `user_id` 比较，因此 userB 可对已知的 userA 文档 ID 发起写入。基线测试没有覆盖该攻击路径，所以当时仍为绿灯。
- 修改前基线：`mvn clean test` 为 Tests run: 95、Failures: 0、Errors: 0、Skipped: 5；跳过项由未设置 `DB_PASSWORD` 触发。

## 3. 本次检查过的实际文件

本次先读代码再修改，实际检查范围如下：

- 配置：`pom.xml`、`.env.example`、`application.yml`、`SecurityConfig`、`SecurityProperties`、`OpenApiConfig`。
- Security/Auth：`AuthService`、`JwtTokenService`、`ProjectUserDetailsService`、JWT converter、401/403 handler。
- Controller：`AuthController`、`UserController`、`KnowledgeBaseController`、`DocumentController`、`ChatController`。
- Service：KnowledgeBase、Document、Chat 的接口与实现。
- Mapper/XML：KnowledgeBase、Document、DocumentChunk、Chat、User Mapper 及 XML/注解 SQL。
- DTO/Entity：所有创建/查询 Request、VO、KnowledgeBase/Document/Chunk/ChatSession/ChatMessage。
- Schema：`sql/001_init_schema.sql`、`002_add_document_title_unique.sql`、`003_complete_chat_schema.sql`。
- 测试：配置安全、Service、Controller、Security、Mapper、Schema、OpenAPI 等全部现有测试。
- 工程文档：`README.md`、`http/` 与 DAY 2 实施指南全文。

## 4. 修改文件清单

| 文件 | 类型 | 改动职责 | 为什么需要 |
|---|---|---|---|
| `pom.xml` | 修改 | 引入 Spring Data Redis | 提供连接、脚本执行和 TTL API |
| `.env.example`、`application.yml` | 修改 | Redis、超时、限流参数环境变量化 | 无明文秘密并可调参数 |
| `SecurityConfig` | 修改 | 双过滤链、base/blacklist-aware decoder、`/me` 权限 | logout 幂等且普通 Bearer 请求统一检查黑名单 |
| `AuthService`、`AuthController` | 修改 | 新增基于当前 JWT 的 logout | 写入 jti 黑名单闭环 |
| `JsonAuthenticationEntryPoint`、`GlobalExceptionHandler`、`ErrorCode` | 修改 | 401/403/429/503 统一 JSON 映射 | 保持 HTTP 与业务码语义一致 |
| 五个业务 Controller | 修改 | 从 `Authentication` 提取当前用户并显式传入 Service | HTTP owner 不再由客户端提供 |
| 三个创建 Request | 修改 | 移除 `userId` | 关闭伪造 owner 入口 |
| KnowledgeBase/Document/Chat Service 与实现 | 修改 | owner 校验、父资源校验、按用户列表与写入 | 形成对象级授权链 |
| KnowledgeBase/Document Mapper 与 XML | 修改 | `user_id` 条件、owner delete、分页 count/select 同条件 | 数据访问层约束查询范围 |
| `OpenApiConfig`、`README.md` | 修改 | 同步 DAY 2 契约、配置、故障策略和操作入口 | 文档与实现一致 |
| `ConfigurationSafetyTest`、`DocumentMapperTest`、`OpenApiIntegrationTest` | 修改 | 校验新配置、SQL 和 API 契约 | 防止回归 |
| Controller/Security/Service 现有测试 | 修改 | 适配显式 currentUserId 并加入 userA/userB 攻击用例 | 验证资源隔离和错误状态 |

## 5. 新增文件清单

| 文件 | 职责 / 调用方 | 解决的问题 |
|---|---|---|
| `security/CurrentUser.java` | Controller 调用，将认证主体 `sub` 转为正 Long | 建立唯一可信 userId 来源 |
| `security/BlacklistAwareJwtDecoder.java` | 普通 Resource Server 过滤链调用 | 签名/issuer/时间通过后统一查 blacklist |
| `security/JwtBlacklistDependencyException.java` | decoder 到 entry point 的异常信号 | Redis 故障稳定映射 HTTP 503 |
| `config/RateLimitProperties.java` | 限流器读取并校验参数 | 参数可配置且启动时校验 |
| `config/RedisScriptConfig.java` | 加载 Lua 为单例脚本 Bean | 避免拼脚本并支持 Redis 原子执行 |
| `redis/TokenBlacklistService.java` | logout 写、decoder 读 | jti 黑名单和剩余 TTL |
| `redis/BlacklistUnavailableException.java` | Redis 黑名单读写失败时抛出 | fail-closed 的领域边界 |
| `redis/FixedWindowRateLimiter.java` | `ChatController#createMessage` 调用 | 用户维度固定窗口限流 |
| `resources/redis/fixed-window-rate-limit.lua` | Redis 原子执行 INCR/首次 EXPIRE | 避免计数有值但 TTL 丢失 |
| `CurrentUserTest`、`TokenBlacklistServiceTest`、`FixedWindowRateLimiterTest` | 单元测试 | 覆盖身份、TTL、故障策略、429 |
| `RedisIntegrationTest` | 条件启用的真实 Redis 测试 | 验证 key/value/TTL 与 Lua 原子行为 |
| `http/day2-security-redis-manual-test.http` | IDEA HTTP Client 手工入口 | 补充 Postman 之外的快速回归入口 |
| 本报告 | 最终审计和复现教程 | 防止“代码绿灯即假完成” |

## 6. Principal / CurrentUser 实现说明

JWT 的 `sub` 在签发时是数据库用户 ID 的字符串。`CurrentUser.requireUserId(Authentication)` 只接受已认证的 `JwtAuthenticationToken`，把 `sub` 转为正 `Long`；缺失、非法或非正数统一按 40102 处理。Controller 显式将 `currentUserId` 传给 Service，Service 再传给 Mapper 或与实体 owner 比较。

```text
Authorization: Bearer JWT
  → JwtDecoder 验签、issuer、时间与 blacklist
  → JwtAuthenticationToken.getToken().getSubject()
  → CurrentUser.requireUserId(authentication)
  → Controller(currentUserId, request)
  → Service owner / parent-owner check
  → Mapper SQL(user_id)
```

不信任 `request.userId`，因为客户端可以任意改 JSON/query；创建 DTO 已彻底移除该字段。Mapper 不读取 `SecurityContext`，因为数据层不应依赖 Web 线程上下文，显式参数更容易测试、复用和审计。

## 7. 资源隔离改造说明

| 资源 | 实施前风险 | 实施后规则 / owner 来源 | Mapper/Service 收口 | 对应测试 |
|---|---|---|---|---|
| KnowledgeBase | 创建可伪造 userId；列表可扩大范围 | owner 只取 JWT `sub` | `selectByUserId`；详情比对实体 userId | userA/B 详情 403、列表隔离、伪造字段忽略 |
| Document | 创建、KB 列表、分页、详情、删除范围不一致 | 创建先验证 KB owner；读删验证 Document owner | KB 列表带 userId；count/select 共用必选 `user_id`；`deleteByIdAndUserId` | 创建父资源越权、读删越权、分页参数测试 |
| DocumentChunk | 仅凭 documentId 可写/读 | 先查 Document 并验证 owner，再操作 chunk | owner 校验在任何 chunk Mapper 调用前完成 | userB 写 userA 文档返回 40304 且 Mapper 零交互 |
| ChatSession | 创建可伪造；列表可能看见他人 | owner 只取 JWT；列表按 userId | Service 创建赋 currentUserId；Mapper 按 userId | 当前用户创建/列表、userB 越权 |
| ChatMessage | 只凭 sessionId 可写/读 | 先验证 Session owner；message.userId 固定为当前用户 | Session owner check 在 insert/select 前 | userB 写/读 userA session 返回 40305，message Mapper 零交互 |
| `/api/users/me` | 只有 ADMIN 用户查询接口，普通用户缺本人入口 | `/me` authenticated；`/api/users` 与 `/api/users/{id}` 仍 ADMIN | `CurrentUser` 获取 ID，再调用既有 UserService | USER `/me` 200；USER 查他人 403；ADMIN 规则回归 |

对“资源 ID 不存在”保留 404；资源存在但 owner 不匹配统一 403。这既满足当前指南，也便于测试明确区分归属失败。

## 8. P0 DocumentChunk 水平越权修复

- 原复现：userA 创建 Document，userB 获得其 ID 后向 `POST /api/documents/{userADocumentId}/chunks` 发送合法 chunk JSON；旧路径未比较 Document.userId。
- 根因：将“已认证”误当成“有权操作任意对象”，Service 没有父资源 owner 校验。
- 修复：Controller 传 `currentUserId`；Service 先 `documentMapper.selectById`，不存在返回 40403，存在但 owner 不同返回 40304；只有通过后才调用 `documentChunkMapper.insert/selectByDocumentId`。
- 自动回归：`DocumentServiceImplTest` 验证 userB 创建/读取 userA chunk 均为 40304，并用 `verifyNoInteractions(documentChunkMapper)` 证明没有进入数据写读；Controller 测试验证伪造 `userId` 不改变 Principal。
- 人工等价 HTTP 回归：userB 实际调用返回 HTTP 403、`code=40304`；Postman Desktop 中的同一步骤经用户明确豁免，不再要求执行。
- 数据库无修改证据：真实 MySQL 已执行。测试文档 ID 41 的攻击前后查询为：

```sql
SELECT COUNT(*) AS chunk_count
FROM document_chunk
WHERE document_id = <userA_document_id>;
```

两次 `chunk_count` 都是 1，明细始终只有合法的 `chunk_index=0`；攻击用 `chunk_index=99` 没有落库。验证数据随后已精确清理。

## 9. Redis 环境与连接说明

- 运行方式：Docker Desktop，容器 `redis`，镜像 `redis:7`，端口 `6379:6379`。本次执行了 `docker start redis`；最终状态为 `Up`，`docker exec redis redis-cli PING` 返回 `PONG`。
- Spring Boot：`spring-boot-starter-data-redis` 自动配置 `StringRedisTemplate`；黑名单服务使用 `opsForValue`，限流器使用 `execute(DefaultRedisScript, ...)`。
- 环境变量：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`REDIS_CONNECT_TIMEOUT`、`REDIS_TIMEOUT`。本地无密码容器可让 `REDIS_PASSWORD` 为空；生产必须注入。
- 可重复命令：

```powershell
docker --version
docker ps -a --filter "name=^/redis$"
docker start redis
# 若尚未创建：docker run --name redis -p 6379:6379 -d redis:7
docker exec redis redis-cli PING
docker stop redis
docker start redis
```

Docker 只是 Redis 的本地运行方式，不代表后续 RAG/向量检索阶段已经完成。

## 10. JWT Logout Blacklist 设计

- key：`auth:blacklist:{jti}`；value：固定字符串 `1`。
- TTL：`exp - now`，由 `Clock` 计算；token 已到期时不写 key。
- 写入：`POST /api/auth/logout` 认证通过后调用 `TokenBlacklistService.revoke(jwt)`。
- 读取：普通受保护请求的 blacklist-aware `JwtDecoder` 在基础 JWT 校验后读取。
- 只存 jti：jti 是随机 token 标识，不需要保存完整 JWT，减少敏感数据暴露与空间占用。
- 剩余 TTL：token 自然过期后基础 decoder 已拒绝，无需继续保存黑名单；TTL 也避免永久垃圾 key。
- logout 幂等：logout 专用过滤链使用基础 decoder，因此同一个仍未自然过期的 token 重复退出仍返回 200，写同一 key 并刷新为当前剩余 TTL。

## 11. Blacklist JWT 验证链

```text
Bearer token
  → base NimbusJwtDecoder（HS256 signature、issuer、iat/exp）
  → BlacklistAwareJwtDecoder（取 jti → GET auth:blacklist:{jti}）
      ├─ 不在 blacklist → Authentication → Controller
      ├─ 已在 blacklist → BadJwtException → HTTP 401 / code 40102
      └─ Redis 不可用 → JwtBlacklistDependencyException → HTTP 503 / code 50301
```

logout 专用链只用于 `POST /api/auth/logout` 的幂等写入；其余请求一律走 blacklist-aware decoder。只写 Redis key 而不在 JWT 验证时读取，不算 logout，本实现已完成读写闭环。

## 12. Blacklist Redis 故障策略

黑名单读取和 logout 写入均 **fail closed**。Redis 不可用时，系统无法证明 token 没有被撤销；若放行，已退出或被紧急撤销的 token 可能继续访问。因此返回 HTTP 503、统一 `code=50301`，表达依赖暂时不可用，而不是谎报 token 本身非法的 401。日志只记录异常类型，不记录完整 JWT、jti、secret 或请求正文。

## 13. Redis Lua Rate Limit

- 代表接口：现有真实消息写接口 `POST /api/chat/sessions/{sessionId}/messages`。
- key：`rate:http:{currentUserId}:{windowStartEpochSecond}`。
- 默认：每用户 20 次/1 分钟；key TTL 为窗口 60 秒加 5 秒 buffer。
- Lua：单次 Redis 执行 `INCR`，仅当计数等于 1 时 `EXPIRE`，返回计数。计数大于 limit 时抛 42901，HTTP 429。
- 不能由 Java 简单分两步 INCR + EXPIRE：进程崩溃、网络中断或并发交错可能留下永久 `TTL=-1` 计数 key；Lua 在 Redis 内原子执行消除该窗口。

## 14. Rate Limit Redis 故障策略

限流采用 **fail open**：Redis 异常时记录 `currentUserId` 和异常类型后临时放行，由业务 owner 校验继续保护数据。它与 blacklist 不同：限流是可用性/容量保护，短期漏限流通常比让全部聊天写请求不可用更可接受；blacklist 则直接参与身份撤销，必须安全优先。日志不包含消息正文或 token。

## 15. ErrorCode / HTTP Status 变化

| HTTP | 业务码 | 含义 | 统一响应 |
|---|---:|---|---|
| 401 | 40102 | Bearer 缺失、JWT 非法/过期、jti 缺失或已退出 | `Result{code,message,data:null}` |
| 403 | 40302 | KnowledgeBase 非当前用户所有 | 同上 |
| 403 | 40304 | Document/DocumentChunk 父文档非当前用户所有 | 同上 |
| 403 | 40305 | ChatSession/ChatMessage 父会话非当前用户所有 | 同上 |
| 403 | 40303 | USER 访问 ADMIN 接口 | 同上 |
| 429 | 42901 | 消息写接口超过固定窗口阈值 | 同上 |
| 503 | 50301 | blacklist Redis 依赖不可用 | 同上 |

## 16. OpenAPI / README 变化

- OpenAPI 标题/版本/分组更新为 Stage 3 Day 2，补充资源 owner、Redis logout、429 与 503 响应，Auth/Chat operation 描述与实现同步。
- README 补充可信身份链、资源隔离、Redis Docker 与环境变量、blacklist/限流不同故障策略、新增接口表、真实 Redis 测试命令、手工 HTTP 入口、面试表达和本报告链接。
- Swagger UI 在 `dev` profile 继续公开；非 dev 不扩大公开面。

## 17. 自动化测试结果

### 默认全量回归

执行：`mvn clean test`

```text
Tests run: 114
Failures: 0
Errors: 0
Skipped: 7
```

Skipped 原因：5 个测试因未提供 `DB_PASSWORD` 跳过（User/Document/Chat Mapper、Schema、需要完整上下文的 OpenAPI）；2 个真实 Redis 测试因默认未设置 `REDIS_INTEGRATION_TEST=true` 跳过。

### 启用真实 MySQL + Redis 的最终全量回归

执行时仅向当前进程注入 `SPRING_PROFILES_ACTIVE=dev`、`DB_USERNAME`、`DB_PASSWORD` 和 `REDIS_INTEGRATION_TEST=true`，然后运行 `mvn clean test`；密码没有写入文件或报告。

```text
Tests run: 114
Failures: 0
Errors: 0
Skipped: 0
```

最终 25 个测试报告、114 个测试全部实际执行。分类证据：

- Service：KB/Document/Chunk/Session/Message owner 与零 Mapper 交互测试。
- MockMvc/Security：401、403、429、503、`/me`、USER/ADMIN、logout 旧 token 与幂等测试。
- MySQL integration：User/Document/Chat Mapper、Schema、完整上下文 OpenAPI 均实际运行通过。
- Redis integration：真实容器下 blacklist value/TTL、Lua 计数/TTL 两项通过。
- Regression：基线现有单元、认证、JWT、Controller 和配置安全测试均为 0 failure、0 error。

测试日志开头的配置绑定 WARN 来自故意输入非法 JWT 配置的负向配置测试，测试进程退出码为 0，不是运行失败。

## 18. userA / userB 自动化与真实 HTTP 安全矩阵

| 场景 | 身份 | 资源 owner | 期望 | 实际 |
|---|---|---|---|---|
| 读取 KB 详情 | userB | userA | 40302 | 自动化通过；真实 HTTP 403/40302 |
| 创建 Document 到 KB | userB | userA | 40302、无 insert | 自动化零 insert；真实 HTTP 403/40302 |
| 读取/删除 Document | userB | userA | 40304、无 delete | 真实 HTTP 403/40304；MySQL 行数 1→1 |
| 创建/读取 Chunk | userB | userA Document | 40304、chunk Mapper 零交互 | 真实 HTTP 403/40304；MySQL 数量 1→1 |
| 创建/读取 Message | userB | userA Session | 40305、message Mapper 零交互 | 真实 HTTP 403/40305；MySQL 数量 1→1 |
| KB/Document/Session 列表 | userA/userB | 各自 | 只返回当前 owner | 自动化通过；真实分页 A 可见、B 不可见 |
| 伪造创建 JSON `userId` | token user=7 | JSON user=999 | owner 仍为 7 | Controller 参数捕获为 7 |

## 19. MySQL 验证证据

本次 **真实 MySQL 验证已完成**：MySQL 8.4.10、数据库 `ljl_agent`、六张核心表均存在；User/Document/Chat Mapper、Schema 和完整上下文 OpenAPI 测试均实际运行。数据库密码只存在于临时进程环境，没有写入仓库或本报告。

端到端验证使用隔离的临时 userA/userB/admin（ID 40/41/42）、KB 14、Document 41、Chunk 20、Session 8。关键观测：

- Document 分页：userA 响应包含 Document 41，userB 响应不包含。
- userB 删除 userA Document：HTTP 403/40304，MySQL `COUNT(*)` 为 1→1。
- userB 创建 userA Chunk：HTTP 403/40304，MySQL数量为 1→1。
- userB 写 userA Session：HTTP 403/40305，Message 数量为 1→1。
- Schema：`user.id=bigint NOT NULL`、`user.status=tinyint NOT NULL`；ChatMessage 联合 owner 外键、role CHECK、Chunk 外键和非负 CHECK 均存在。
- 验证完成后精确删除 2 条消息、1 个会话、1 个 Chunk、1 个 Document、1 个 KB 和 3 个临时用户；复查剩余数均为 0，没有修改其他业务数据。

复验 SQL如下：

```sql
-- 1. 分页 owner：API 分别用 userA/userB token 查询后，与表数据对照
SELECT id, user_id, knowledge_base_id, title
FROM document
WHERE user_id IN (<userA_id>, <userB_id>)
ORDER BY created_at DESC, id DESC;

-- 2. 越权删除前后：userB DELETE userA document 必须 403，行仍存在
SELECT id, user_id, knowledge_base_id, title
FROM document
WHERE id = <userA_document_id>;

-- 3. 越权创建 chunk 前后：计数和明细必须完全不变
SELECT COUNT(*) FROM document_chunk WHERE document_id = <userA_document_id>;
SELECT id, document_id, knowledge_base_id, chunk_index
FROM document_chunk
WHERE document_id = <userA_document_id>
ORDER BY id;

-- 4. 会话/消息归属交叉验证
SELECT id, user_id, title FROM chat_session
WHERE id = <userA_session_id>;
SELECT id, session_id, user_id, request_id FROM chat_message
WHERE session_id = <userA_session_id> ORDER BY id;
```

源代码 SQL 审计和真实结果一致：Document 分页的 `countPage`、`selectPage` 共用 `DocumentPageConditions`，其中 `user_id = #{userId}` 为必选条件。

## 20. Redis CLI 验证证据

本次使用隔离的临时 key，使用 `SCAN` 而不是生产禁用的 `KEYS *`：

```text
docker exec redis redis-cli PING
PONG

SCAN MATCH auth:blacklist:codex-day2-evidence → auth:blacklist:codex-day2-evidence
GET auth:blacklist:codex-day2-evidence        → 1
TTL auth:blacklist:codex-day2-evidence        → 119

Lua 连续执行计数                           → 1 / 2 / 3
SCAN MATCH rate:http:7:*                     → rate:http:7:1787124060
GET rate:http:7:1787124060                   → 3
TTL rate:http:7:1787124060                   → 64
DEL 两个临时 key                            → 2
```

完整 HTTP 剧本的第二组实际证据：blacklist `GET=1`、`TTL=1797`；rate `GET=3`、`TTL=63`，均大于 0 且 rate TTL 不是 -1。旧 token 返回 401/40102，重复 logout 返回 200。

真实故障实验中短暂停止 Redis：普通受保护请求和 logout 写入分别返回 HTTP 503/50301；恢复容器后 `PING=PONG`，应用日志只记录异常类型，没有记录密码、secret 或完整 token。Rate Limit fail-open 由 `FixedWindowRateLimiterTest` 的 Redis 异常分支实际执行通过；由于所有 HTTP Bearer 请求会先经过 blacklist fail-closed，Redis 整体停止时请求会在 Security 层先返回 503，这是预期的链路顺序。

结论：blacklist、rate value/TTL、旧 token、429、503 与恢复均符合规范。所有本次可识别的临时 Redis key 已精确删除或自然过期；真实 `RedisIntegrationTest` 也使用随机 key 并在 `finally` 清理。

## 21. Postman 人工验证教程

本节保留为可选的从零复现教程。本次已使用 Postman Desktop 12.24.2 实际发送 userA 登录请求并得到 HTTP 200/code 0，同一真实应用上的完整等价 HTTP 剧本共 38 项、38/38 通过。用户已明确豁免在 Postman Desktop 中逐项执行其余请求，因此 Postman 不再是阶段阻塞项。

### 21.1 创建 Environment 与前置准备

在 Postman 新建 Environment `agent-day2-local`，建立 `baseUrl=http://localhost:8080`、`userAUsername`、`userAPassword`、`userBUsername`、`userBPassword`、`adminUsername`、`adminPassword`，以及空变量 `userAToken`、`userBToken`、`adminToken`、`userAId`、`userBId`、`userAKnowledgeBaseId`、`userADocumentId`、`userASessionId`。密码只放 Postman 本地环境且将 Sensitive 打开，不写进仓库。

先确认 MySQL schema 001～003 已应用、`DB_PASSWORD`/`JWT_SECRET_BASE64` 已注入、Redis `PING=PONG`，再用 `scripts/run-dev.ps1` 启动应用，`GET {{baseUrl}}/api/health` 应为 200。失败证据保存 Console、响应体、应用日志和时间点；排查顺序从 HTTP 到数据库/Redis。

每个成功请求都应得到 `HTTP 200`、`code=0`；下表未写 body 的 GET/DELETE 请求不发送 body。

| 步骤/目的 | Method + URL | Auth / Headers / Body | 环境变量保存 | 预期与失败证据 |
|---|---|---|---|---|
| 注册 userA | POST `{{baseUrl}}/api/users/register` | No Auth；`Content-Type: application/json`；`{"username":"{{userAUsername}}","password":"{{userAPassword}}","email":"usera.day2@example.test"}` | Tests: `pm.environment.set("userAId", pm.response.json().data.id)` | 200/code 0；409 表示用户名或邮箱重复，换唯一值 |
| 注册 userB | POST `{{baseUrl}}/api/users/register` | No Auth；JSON 与上一步相同但改 userB 和邮箱 | 保存 `userBId` | 200/code 0；失败保存响应与 MySQL user 查询 |
| 准备 ADMIN | 先注册 `adminUsername`，再在本地测试库执行 `UPDATE user SET role='ADMIN' WHERE username='<实际 adminUsername>' AND role='USER';` | 不允许把注册接口改成可自选角色 | 无 | SQL 影响 1 行；`SELECT id,username,role FROM user WHERE username=...` 为 ADMIN |
| 登录 userA | POST `{{baseUrl}}/api/auth/login` | No Auth；JSON `{"username":"{{userAUsername}}","password":"{{userAPassword}}"}` | Tests: `pm.environment.set("userAToken", pm.response.json().data.accessToken)` | 200、token 非空；40101 查密码/状态，不打印密码摘要 |
| 登录 userB | 同上，替换 userB | No Auth；JSON | 保存 `userBToken` | 200；保存失败响应 |
| 登录 ADMIN | 同上，替换 admin | No Auth；JSON | 保存 `adminToken` | 200，返回 user.role=ADMIN |
| userA 创建 KB | POST `{{baseUrl}}/api/knowledge-bases` | Bearer `{{userAToken}}`；JSON `{"name":"day2-kb","description":"owner isolation"}`，不得含 userId | 保存 `userAKnowledgeBaseId=data.id` | 200；若 40902 换唯一 name |
| userA 创建 Document | POST `{{baseUrl}}/api/documents` | Bearer A；JSON `{"knowledgeBaseId":{{userAKnowledgeBaseId}},"title":"day2-doc","content":"day2 content"}` | 保存 `userADocumentId=data.id` | 200；40302 表示 KB owner 不符 |
| userA 创建 Chunk | POST `{{baseUrl}}/api/documents/{{userADocumentId}}/chunks` | Bearer A；JSON `{"chunkIndex":0,"content":"first chunk","metadata":"{\"source\":\"postman\"}"}` | 可保存 chunk id | 200；40904 时换 chunkIndex |
| userA 创建 Session | POST `{{baseUrl}}/api/chat/sessions` | Bearer A；JSON `{"title":"day2 session"}`，不得含 userId | 保存 `userASessionId=data.id` | 200 |
| userA 写 Message | POST `{{baseUrl}}/api/chat/sessions/{{userASessionId}}/messages` | Bearer A；JSON `{"role":"USER","content":"hello day2","requestId":"day2-{{$guid}}"}` | 无 | 200；requestId 必须每次唯一 |
| 正向列表 | GET KB、`/api/documents?page=1&size=10`、`/api/knowledge-bases/{{userAKnowledgeBaseId}}/documents`、`/api/chat/sessions` | Bearer A；无 body | 无 | 200，所有 data.owner/userId（若响应含）均为 A；保存响应 |
| 正向详情/子资源 | GET `/api/knowledge-bases/{{userAKnowledgeBaseId}}`、`/api/documents/{{userADocumentId}}`、`/api/documents/{{userADocumentId}}/chunks`、`/api/chat/sessions/{{userASessionId}}/messages` | Bearer A | 无 | 全部 200 |

Postman Tests 保存模板（按响应实际 data 结构替换变量名）：

```javascript
pm.test("HTTP 200 and business success", () => {
  pm.response.to.have.status(200);
  pm.expect(pm.response.json().code).to.eql(0);
});
pm.environment.set("userAToken", pm.response.json().data.accessToken);
```

### 21.2 userB 水平越权与数据库无变化

以下都使用 Bearer `{{userBToken}}`，`Content-Type: application/json` 仅用于 POST。每个请求保存 response body 和 Postman Console；预期不是笼统失败，而是指定 HTTP/业务码：

| 目的 | 请求 | Body | 预期 |
|---|---|---|---|
| 读 userA KB | GET `/api/knowledge-bases/{{userAKnowledgeBaseId}}` | 无 | 403 / 40302 |
| 在 userA KB 建文档 | POST `/api/documents` | `{"knowledgeBaseId":{{userAKnowledgeBaseId}},"title":"forged","content":"must not insert"}` | 403 / 40302 |
| 读 userA Document | GET `/api/documents/{{userADocumentId}}` | 无 | 403 / 40304 |
| 删 userA Document | DELETE `/api/documents/{{userADocumentId}}` | 无 | 403 / 40304；文档仍存在 |
| P0：写 userA Chunk | POST `/api/documents/{{userADocumentId}}/chunks` | `{"chunkIndex":99,"content":"must not insert"}` | 403 / 40304；chunk 数不变 |
| 读 userA Chunk | GET `/api/documents/{{userADocumentId}}/chunks` | 无 | 403 / 40304 |
| 写 userA Session | POST `/api/chat/sessions/{{userASessionId}}/messages` | `{"role":"USER","content":"must not insert","requestId":"attack-{{$guid}}"}` | 403 / 40305 |
| 读 userA Messages | GET `/api/chat/sessions/{{userASessionId}}/messages` | 无 | 403 / 40305 |

断言脚本示例：

```javascript
pm.test("horizontal access denied", () => {
  pm.response.to.have.status(403);
  pm.expect(pm.response.json().code).to.eql(40304);
});
```

攻击前后执行第 19 节 Document/Chunk/Message SQL，截图证明无行删除、无 chunk/message 新增。若 userB 仍成功，按 Controller currentUser → Service parent owner → Mapper 参数/SQL → MySQL 行 owner 顺序排查，停止继续做 Redis 验收。

### 21.3 列表/分页、本人和 ADMIN

- userA、userB 各自 GET `/api/knowledge-bases`、`/api/documents?page=1&size=10`、`/api/chat/sessions`；Authorization 分别用各自 token，无 body，预期 200 且无交叉 ID。用第 19 节 SQL 对照。
- userA GET `/api/users/me`，Bearer A，预期 200 且 `data.id={{userAId}}`；userB 同理。
- userA GET `/api/users` 和 `/api/users/{{userBId}}`，预期 403/code 40303；ADMIN 用 `{{adminToken}}` 重试，预期 200。若 `/me` 被 ADMIN matcher 误拦，检查 Security matcher 顺序。

### 21.4 logout、blacklist 与旧 token 重放

1. 用 Bearer `{{userAToken}}` POST `/api/auth/logout`，无 body，预期 200/code 0；重复一次仍预期 200（幂等）。不要立即覆盖旧 token。
2. 解码 JWT payload 只取 jti（不要把完整 token 写日志），在终端执行 `docker exec redis redis-cli --scan --pattern "auth:blacklist:*"`，对对应 key 执行 `GET` 应为 `1`，`TTL` 应 `>0` 且不超过 token 剩余寿命。
3. 用旧 `{{userAToken}}` GET `/api/users/me`，预期 401/code 40102；这就是失败重放证据。
4. 重新 POST `/api/auth/login`，保存新的 `userAToken`，再 GET `/api/users/me`，预期 200。若 logout 后旧 token 仍 200，检查 jti、key 前缀、decoder 是否使用 blacklist-aware Bean；若 503，检查 Redis。

### 21.5 Rate Limit 与 Redis 故障实验

为便于人工观察，用环境变量 `RATE_LIMIT_CHAT_MESSAGE_LIMIT=2`、`RATE_LIMIT_CHAT_MESSAGE_WINDOW=1m` 重启应用。连续三次 POST `/api/chat/sessions/{{userASessionId}}/messages`，Bearer A、`Content-Type: application/json`，每次用不同 `requestId`；前两次预期 200，第三次预期 429/code 42901。执行 `redis-cli --scan --pattern "rate:http:*"`，再 GET/TTL 对应 key，TTL 必须 `>0` 且非 `-1`。没有 429 时先确认已重启、变量生效、三次同用户同窗口。

推荐故障实验仅限本地：先保留应用日志窗口，再 `docker stop redis`。

- blacklist 安全链：用一个未退出、未过期 token GET `/api/users/me`，预期 503/code 50301；logout 写也应 503。目的：证明 fail closed。保存 HTTP 和脱敏日志。
- rate-limit：用合法 token 请求消息写接口，预期限流组件记录安全日志后放行到业务（通常 200；若 token 在本次请求还需要 blacklist 查询，则认证会先因 blacklist fail-closed 返回 503，这是过滤链顺序的预期）。要单独观察 rate fail-open，可用自动化测试或在认证已经建立的测试上下文验证。
- 立即 `docker start redis` 并确认 `PING=PONG`。若状态不符，按 HTTP/Security → Controller → Service → Mapper/MySQL → Redis 连接/超时顺序排查。严禁在共享或生产环境停 Redis。

## 22. 常见失败与排查顺序

统一顺序：**HTTP/ Security → Controller currentUser → Service owner → Mapper SQL → MySQL → Redis**。

| 现象 | 按调用链排查 |
|---|---|
| 所有接口突然 401 | 请求是否公开/Authorization 格式 → base decoder secret/issuer/时间 → jti → blacklist key |
| 合法 token 401 | token 是否过期/issuer 不同 → 是否已 logout → Redis GET 对应 jti；不要打印完整 token |
| userB 仍可访问 userA | Controller 是否传 B 的 sub → Service 是否先查父资源并比较 owner → Mapper 是否带 userId → MySQL 行 user_id |
| logout 后 token 仍有效 | logout 是否 200 → jti 是否存在 → `auth:blacklist:` GET/TTL → 普通链是否注入 blacklist-aware decoder |
| blacklist TTL 不正确 | JWT exp/系统时钟 → `Duration.between(now, exp)` → Redis TTL；过期 token 不应写 key |
| rate key TTL=-1 | 是否绕过 Lua → 脚本首次计数是否 EXPIRE → Spring 加载的脚本是否正确 |
| 没有 429 | 配置是否生效并重启 → 同一 currentUser/同一窗口 → requestId 是否因 409 先失败 → Redis 计数 |
| Redis timeout | host/port/Docker 状态 → `PING` → connect/read timeout → 网络/密码/database |
| Redis 停止后状态错误 | 先分功能：blacklist 必须 503；rate 组件应 fail-open → exception 是否被包装/映射 → 安全日志是否泄密 |

## 23. 面试意义

- JWT 只证明“你是谁”和角色，不能证明“这个 ID 的对象属于你”，所以每个对象仍需 owner check。
- 防水平越权的关键是单一可信 Principal、父资源先验归属、查询/分页强制 `user_id`，以及攻击测试证明 Mapper 未执行。
- JWT 无状态仍可用 Redis：JWT 保持请求验证无 Session；Redis 只保存短期撤销和限流状态，不把登录会话搬回服务器。
- blacklist TTL 等于剩余 token 寿命，安全窗口结束后自动清理，空间有上界。
- Lua 将计数与首次过期设置变成 Redis 单次原子操作，避免永久限流 key。
- Redis 故障策略不同：撤销关系身份安全所以 fail-closed；限流是容量保护所以可短期 fail-open。
- MySQL 保存用户、知识库、文档、切片、会话、消息等长期业务事实；Redis 保存 jti blacklist 和固定窗口计数等短期高频状态。

## 24. 60～90 秒阶段 3 项目表达

> 我完成了阶段 3 DAY 1 的 Spring Security 无状态 JWT 基础，并在 DAY 2 代码中把 JWT sub 收口为唯一 currentUserId。Controller 显式传给 Service，创建 DTO 不再接受 userId，知识库、文档、切片、会话和消息都增加对象归属校验，分页 count 和 select 也强制使用同一个 user_id 条件。全量 114 个测试无失败、无错误、无跳过，真实 MySQL 证明越权删除、Chunk 和 Message 写入都没有数据变化。退出链路使用 Redis jti blacklist 和剩余 TTL，旧 token 返回 401，Redis 故障返回 503；消息写接口用 Lua 原子限流，超限返回 429。真实 Redis、MySQL 和 38 项 HTTP 剧本均已通过；按用户明确豁免 Postman 完整演示后的验收口径，阶段 3 已完成，可以进入阶段 4。

## 25. 阶段 3 最终验收清单

### DAY 2 完成标准

- [x] currentUserId 全部来自认证上下文；客户端不能伪造 owner。
- [x] KnowledgeBase、Document、DocumentChunk、ChatSession、ChatMessage 隔离完成；分页 count/select 都按 currentUserId。
- [x] `/api/users/me` 与 USER/ADMIN 自动化回归完成。
- [x] 核心 userA/userB 测试和 P0 DocumentChunk 回归完成。
- [x] Redis 实际可用，Spring Boot 真实连接、blacklist value/TTL、Lua rate key/TTL 已验证。
- [x] logout 旧 token 401、blacklist 故障 503、rate 超限 429、rate 故障 fail-open 自动化完成。
- [x] README/OpenAPI/ErrorCode/配置/日志安全同步；`mvn clean test` 0 failure、0 error；Skipped 原因已记录。
- [x] MySQL 实际数据验证完成，越权前后数据不变且临时数据已清理。
- [x] Postman 完整人工验证教程已写入本报告。
- [x] Postman 完整人工演示经用户明确豁免；Desktop 登录 200 和完整 HTTP 38/38 已作为替代证据。

### 阶段 3 总出口 16 项

1. [x] SecurityFilterChain 明确公开、authenticated、ADMIN 接口。
2. [x] AuthenticationManager 校验现有密码摘要并签发 JWT。
3. [x] JwtEncoder/Decoder 使用环境变量 secret 并验证 signature、issuer、时间。
4. [x] STATELESS，不用 HttpSession 保存登录态。
5. [x] Security 401/403 与业务错误统一 Result JSON。
6. [x] claims 最小化并包含 sub、jti、role、iss、iat、exp。
7. [x] 核心资源 userId 来自 Principal。
8. [x] KB、Document、Chunk、Session、Message 有对象级归属校验。
9. [x] USER/ADMIN 有真实接口差异与自动化测试。
10. [x] logout 使用 Redis jti blacklist，TTL 为剩余有效期。
11. [x] 代表接口使用 Redis Lua 原子限流，超限 HTTP 429。
12. [x] Redis key 有前缀、TTL、故障策略，不使用 `KEYS` 扫生产 keyspace。
13. [x] 日志不记录密码、摘要、secret、完整 token 或大段正文。
14. [x] OpenAPI/README 包含 Bearer、资源隔离、logout、Redis、限流。
15. [x] 单元、MockMvc、Security、MySQL、Redis 与 OpenAPI 全量 114 项均实际通过。
16. [x] MySQL、redis-cli 和完整 HTTP 剧本已验证；用户明确豁免 Postman Desktop 逐项演示。

---

阶段 3 DAY 2：
完成

阶段 3：
完成

自动化测试：
Tests run: 114
Failures: 0
Errors: 0
Skipped: 0
Skipped 原因: 无

MySQL：
已验证，原因：MySQL 8.4.10 连通；六表、Schema、Mapper、分页 owner 与越权前后数据均已实际核对

Redis：
已验证，原因：Docker redis:7 PING=PONG；CLI value/TTL、Lua、旧 token、429、真实 503 故障与恢复均通过

Postman：
部分验证：Postman Desktop 登录 200 已实际执行；完整逐项演示由用户明确豁免，不作为阻塞项（等价 HTTP 剧本 38/38 已通过）

尚未完成的唯一阻塞项：
无

下一阶段：
阶段 4 知识库文档处理

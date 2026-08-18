# 阶段 3 Day 1：Spring Security + JWT 实施报告

## 1. 实施结论

阶段 3 Day 1 已在当前真实项目中完成：唯一登录入口迁移为 `POST /api/auth/login`，账号密码交给标准 `AuthenticationManager` 认证链；阶段 2 的 PBKDF2 密码摘要通过 `PasswordEncoder` 适配继续使用；登录成功签发 HS256 JWT；后续 Bearer JWT 由 Spring Security OAuth2 Resource Server 验签并建立 `SecurityContext`；公开、authenticated、ADMIN 三类 URL 边界和统一 JSON 401/403 已由自动化测试证明。

Day 1 没有实现 Redis 黑名单、退出、限流和对象级资源归属。这些仍属于 Day 2。

## 2. 实施前基线

| 项目 | 实际结果 |
|---|---|
| 检查日期 | 2026-08-17（Asia/Hong_Kong） |
| 项目路径 | `C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend` |
| 包名 | `com.ljl.agent` |
| Git 信息 | 当前目录不是 Git 仓库，无法取得分支和提交号 |
| 阶段状态 | 阶段 2 已完成；阶段 3 尚无 Security/JWT 实现 |
| 原登录入口 | `POST /api/users/login`，Service 手工查用户并调用 `PasswordUtils.matches`，只返回 `UserVO` |
| 原密码格式 | `PBKDF2WithHmacSHA256`，格式为 `pbkdf2_sha256$iterations$salt$hash` |
| 修改前命令 | `mvn clean test` |
| 修改前结果 | Tests run: 85；Failures: 0；Errors: 0；Skipped: 5；BUILD SUCCESS |

修改前跳过的 5 个测试为 `ChatMapperTest`、`DocumentMapperTest`、`UserMapperTest`、`SchemaIntegrationTest`、`OpenApiIntegrationTest`。原因均为未提供 `DB_PASSWORD`，所以修改前没有宣称真实 MySQL 集成已在本机执行。

实施前实际检查了 `pom.xml`、`application.yml`、`application-dev.yml`、用户 Controller/Service/Mapper/XML、登录/注册 DTO、`User`、`UserVO`、`PasswordUtils`、`Result`、`ErrorCode`、`GlobalExceptionHandler`、`OpenApiConfig`、全部 Controller 和相关测试。

## 3. 实际修改文件

| 文件路径 | 类型 | 修改内容 | 负责目标 |
|---|---|---|---|
| `pom.xml` | 修改 | 加入 Security、Resource Server、Security Test；版本由 Boot BOM 管理 | 标准认证/JWT/测试 |
| `.env.example` | 新增 | 记录 issuer、Base64 secret、TTL 格式和安全生成方式 | 配置安全 |
| `README.md` | 修改 | 更新启动变量、权限矩阵、登录方式、Bearer 使用和 Day 1 状态 | 使用说明 |
| `src/main/resources/application.yml` | 修改 | 通过环境变量绑定 JWT issuer、secret、TTL | 配置安全 |
| `src/main/java/com/ljl/agent/config/SecurityProperties.java` | 新增 | 启动期校验 issuer、Base64、密钥长度和正 TTL | 失败前置 |
| `src/main/java/com/ljl/agent/config/SecurityConfig.java` | 新增 | STATELESS、URL 规则、JWT encoder/decoder、认证管理器和 401/403 handler | 安全入口 |
| `src/main/java/com/ljl/agent/config/OpenApiConfig.java` | 修改 | Stage 3 分组、Bearer Security Scheme、401/403 描述 | OpenAPI |
| `src/main/java/com/ljl/agent/common/ErrorCode.java` | 修改 | 新增 `40102` 未认证/Token 无效和 `40303` 角色权限不足 | 错误契约 |
| `src/main/java/com/ljl/agent/security/ProjectPasswordEncoder.java` | 新增 | 适配既有 `PasswordUtils` PBKDF2 格式 | 历史密码兼容 |
| `src/main/java/com/ljl/agent/security/ProjectUserDetailsService.java` | 新增 | 从 `user` 表加载 userId、passwordHash、status、role | 数据库身份 |
| `src/main/java/com/ljl/agent/security/JwtAuthorityConverter.java` | 新增 | 只映射 USER/ADMIN，拒绝双前缀和非法 role 提权 | 角色授权 |
| `src/main/java/com/ljl/agent/security/JsonAuthenticationEntryPoint.java` | 新增 | Filter 层输出 HTTP 401 + `Result` JSON | 统一 401 |
| `src/main/java/com/ljl/agent/security/JsonAccessDeniedHandler.java` | 新增 | Filter 层输出 HTTP 403 + `Result` JSON | 统一 403 |
| `src/main/java/com/ljl/agent/auth/LoginUser.java` | 新增 | 保存可信 userId/role/status 并提供安全 `UserVO` | 认证 Principal |
| `src/main/java/com/ljl/agent/auth/JwtTokenService.java` | 新增 | 签发 `sub/jti/role/iss/iat/exp` 最小化 JWT | JWT 签发 |
| `src/main/java/com/ljl/agent/auth/AuthService.java` | 新增 | 编排 AuthenticationManager 和 JWT 签发 | 标准登录 |
| `src/main/java/com/ljl/agent/dto/response/LoginResponse.java` | 新增 | 返回 accessToken、Bearer、expiresIn、安全用户信息 | 登录响应 |
| `src/main/java/com/ljl/agent/controller/AuthController.java` | 新增 | 唯一 `POST /api/auth/login` 入口 | 登录 API |
| `src/main/java/com/ljl/agent/controller/UserController.java` | 修改 | 删除旧登录入口；注册标记为 OpenAPI 公开操作 | 消除旁路 |
| `src/main/java/com/ljl/agent/controller/HealthController.java` | 修改 | 标记为 OpenAPI 公开操作 | 公开接口 |
| `src/main/java/com/ljl/agent/service/UserService.java` | 修改 | 删除阶段 2 手工 login 契约 | 消除第二套认证 |
| `src/main/java/com/ljl/agent/service/impl/UserServiceImpl.java` | 修改 | 删除手工查库/比密码 login 实现 | 标准认证收口 |
| `src/test/java/com/ljl/agent/AbstractIntegrationTest.java` | 新增 | 为完整上下文测试运行时生成随机测试 JWT 密钥 | 测试隔离 |
| `src/test/java/com/ljl/agent/ChatMapperTest.java` | 修改 | 继承集成测试 JWT 配置 | 回归兼容 |
| `src/test/java/com/ljl/agent/DocumentMapperTest.java` | 修改 | 继承集成测试 JWT 配置 | 回归兼容 |
| `src/test/java/com/ljl/agent/UserMapperTest.java` | 修改 | 继承集成测试 JWT 配置 | 回归兼容 |
| `src/test/java/com/ljl/agent/SchemaIntegrationTest.java` | 修改 | 继承集成测试 JWT 配置 | 回归兼容 |
| `src/test/java/com/ljl/agent/OpenApiIntegrationTest.java` | 修改 | 更新分组、标准登录路径和 Bearer Scheme 断言 | OpenAPI 回归 |
| `src/test/java/com/ljl/agent/ConfigurationSafetyTest.java` | 修改 | 断言 JWT secret 只使用环境占位 | 配置安全 |
| `src/test/java/com/ljl/agent/controller/UserControllerValidationTest.java` | 修改 | 删除已迁移的旧登录 Controller 测试 | 路径迁移 |
| `src/test/java/com/ljl/agent/service/UserServiceImplTest.java` | 修改 | 删除已迁移的手工登录 Service 测试 | 职责迁移 |
| `src/test/java/com/ljl/agent/config/SecurityPropertiesTest.java` | 新增 | 验证 issuer、Base64、长度、TTL | 配置单测 |
| `src/test/java/com/ljl/agent/config/SecurityPropertiesBindingTest.java` | 新增 | 验证错误配置导致上下文启动失败 | 启动期校验 |
| `src/test/java/com/ljl/agent/security/ProjectPasswordEncoderTest.java` | 新增 | 正确/错误密码、摘要不可作为原密码 | PBKDF2 适配 |
| `src/test/java/com/ljl/agent/security/ProjectUserDetailsServiceTest.java` | 新增 | USER、ADMIN、禁用、不存在、非法 role | 数据库身份 |
| `src/test/java/com/ljl/agent/security/JwtAuthorityConverterTest.java` | 新增 | USER/ADMIN 和非法角色转换 | 角色映射 |
| `src/test/java/com/ljl/agent/auth/JwtTokenServiceTest.java` | 新增 | claims、TTL、jti 唯一、敏感字段缺失 | JWT 单测 |
| `src/test/java/com/ljl/agent/security/SecurityWebIntegrationTest.java` | 新增 | 登录、Bearer、401/403、坏/过期/错误 issuer token、角色、STATELESS、OpenAPI | 安全闭环 |

规划中的职责没有合并为空类；所有新增类都有真实职责。没有新增自定义 JWT Filter，也没有加入第二套 JWT 库。

## 4. 新增功能与工程意义

### 4.1 标准用户名密码认证

阶段 2 的 `UserServiceImpl.login` 自己查询数据库并比较密码，只能证明当前请求的密码正确。Day 1 删除这条旁路，`AuthService` 现在只把 username/password 交给 `AuthenticationManager`。

`DaoAuthenticationProvider` 调用 `ProjectUserDetailsService` 读取数据库 `User`，再调用 `ProjectPasswordEncoder`。适配器最终复用 `PasswordUtils.hash/matches`，因此已有 `password_hash` 不需要迁移，用户也不需要重置密码。`User.status` 通过 `LoginUser.isEnabled()` 参与认证，USER/ADMIN 均来自数据库。

不存在用户、错误密码、禁用用户对外统一为 HTTP 401、业务码 `40101`、消息“用户名或密码错误”。数据库或用户加载组件系统故障不会伪装成账号密码错误，而是继续进入 500 系统异常链。

### 4.2 JWT 签发

认证成功后 `JwtTokenService` 使用 Spring Security `JwtEncoder` 和 HS256 签发 access token。Claims 只有：

```text
sub = 数据库 userId
jti = 每次签发的新 UUID
role = 数据库 USER / ADMIN
iss = 配置的绝对 HTTP(S) issuer URL
iat = 签发时间
exp = iat + 配置 TTL
```

JWT 不包含 username、email、原始密码、passwordHash、secret 或业务正文。`LoginResponse.expiresIn` 直接来自同一个 TTL 配置，不另行硬编码。

### 4.3 Bearer JWT 验证

项目使用官方 OAuth2 Resource Server。`NimbusJwtDecoder` 只信任当前服务端 SecretKey 的 HS256，并组合 Spring 默认时间校验和 issuer 校验。合法 token 由官方 Bearer 过滤链转换成 `Authentication` 并写入 `SecurityContext`；没有自定义 `OncePerRequestFilter`，Controller 也不解析 token。

### 4.4 URL 权限矩阵

| 范围 | Day 1 规则 |
|---|---|
| `GET /api/health` | 公开 |
| `POST /api/users/register` | 公开；原 Service 仍强制角色为 USER |
| `POST /api/auth/login` | 公开；唯一登录入口 |
| `/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html` | 仅 `dev` profile 公开 |
| `GET /api/users`、`GET /api/users/**` | `ROLE_ADMIN` |
| 其他 `/api/**` | authenticated |
| 其他未声明路径 | denyAll |

这是接口级授权。知识库、文档、切片和聊天中的具体对象是否属于 `sub` 对应用户，仍是 Day 2 的资源归属任务。

### 4.5 统一 401/403 与错误码取舍

HTTP 状态和项目业务码承担不同职责，因此没有向 `ErrorCode` 加入裸 `401/403`：

| 场景 | HTTP | Result.code | Result.message |
|---|---:|---:|---|
| 登录用户名/密码/状态认证失败 | 401 | 40101 | 用户名或密码错误 |
| 缺 token、坏 token、过期、issuer 错误 | 401 | 40102 | 未认证或登录状态无效 |
| 身份合法但角色不足 | 403 | 40303 | 权限不足 |
| 既有禁用业务错误 | 403 | 40301 | 用户已被禁用 |
| 既有资源归属错误 | 403 | 40302 | 知识库不属于当前用户 |

这保留了现有五位错误码分段设计，也避免认证、角色授权和业务资源归属复用同一个模糊 code。

Security 异常发生在 DispatcherServlet/Controller 之前，因此不能依赖 `GlobalExceptionHandler`。`JsonAuthenticationEntryPoint` 和 `JsonAccessDeniedHandler` 直接序列化项目 `Result`，避免默认 HTML、302、空响应和底层 JWT 异常泄露。

### 4.6 无状态与 CSRF 边界

项目使用 `SessionCreationPolicy.STATELESS`，关闭 request cache、form login、HTTP Basic 和默认 logout。每个受保护请求都必须显式发送 `Authorization: Bearer <token>`；第一次带 token 成功后，第二次去掉 token 仍返回 401。

当前 API 使用 JSON 请求，认证载体由客户端显式放入 Authorization Header，不依赖浏览器自动携带 Session Cookie，所以 Day 1 关闭 CSRF。将来如果改为 Cookie 自动携带认证信息，需要重新评估 CSRF，而不能沿用当前结论。

### 4.7 OpenAPI Bearer JWT

OpenAPI 版本和分组更新为 `stage-3-day-1`，全局声明 `bearerAuth` HTTP bearer/JWT Scheme；health、register、login 使用空 `@SecurityRequirements` 标记为公开操作。Swagger Authorize 只接收运行时临时 token，不在源码或配置中保存 token。

## 5. 实际调用链

### 5.1 登录链

```text
POST /api/auth/login
-> SecurityFilterChain permitAll
-> AuthController.login
-> AuthService.login
-> AuthenticationManager / ProviderManager
-> DaoAuthenticationProvider
-> ProjectUserDetailsService.loadUserByUsername
-> UserMapper.selectByUsername / UserMapper.xml / MySQL
-> ProjectPasswordEncoder.matches
-> PasswordUtils.matches (既有 PBKDF2)
-> LoginUser + authorities
-> JwtTokenService.issue
-> NimbusJwtEncoder
-> LoginResponse
-> Result<LoginResponse>
```

### 5.2 受保护请求链

```text
Authorization: Bearer <token>
-> BearerTokenAuthenticationFilter
-> NimbusJwtDecoder（HS256 + exp/时间 + issuer）
-> JwtAuthorityConverter（USER/ADMIN）
-> JwtAuthenticationToken
-> SecurityContext
-> URL authorization
-> Controller -> Service -> Mapper
```

### 5.3 401 链

```text
无 token / 格式错误 / 签名篡改 / 过期 / issuer 错误
-> 无法建立合法 Authentication
-> JsonAuthenticationEntryPoint
-> HTTP 401 + Result(code=40102)
```

登录用户名、密码或 status 失败则由 `AuthService` 转成 `BusinessException(40101)`，再由 `GlobalExceptionHandler` 返回 HTTP 401。两类 401 的外部信息均不暴露底层认证细节。

### 5.4 403 链

```text
合法 USER JWT
-> Authentication 已建立
-> 请求 GET /api/users
-> hasRole("ADMIN") 不满足
-> JsonAccessDeniedHandler
-> HTTP 403 + Result(code=40303)
```

## 6. 关键设计选择

1. 使用官方 Resource Server：复用经过框架验证的 Bearer 解析、验签、时间校验和 SecurityContext 建立，不维护重复 JwtFilter。
2. 复用 PBKDF2：保护历史数据兼容性；`PasswordEncoder` 是 Spring Security 与原密码格式之间的适配边界。
3. 使用最小 claims：JWT Payload 可读，不作为隐私或秘密存储区。
4. 使用 STATELESS：服务端不保存登录 Session，每个请求自带可验证凭证。
5. 独立 401/403 handler：Security Filter 异常通常不进入 Controller Advice。
6. issuer 使用绝对 HTTPS URL：Spring Security 7.1 的 issuer 访问器按 URL 处理，启动配置提前验证格式。
7. 资源归属留 Day 2：Day 1 只回答“是谁”和“角色能否调用接口”，不伪称已解决水平越权。
8. 不加入 Redis：Day 1 无 blacklist/rate limit 业务，不制造外部启动依赖。

## 7. 自动化验证矩阵

| 编号 | 场景 | 实际测试/请求 | 实际结果 | 结论 |
|---|---|---|---|---|
| T01 | 正确账号密码登录 | `SecurityWebIntegrationTest.shouldLoginThroughAuthenticationManagerAndReturnSafeJwt` | 200；Bearer；expiresIn=1800；安全 UserVO | 完成 |
| T02 | 不存在 username | `shouldUseSame401ForUnknownWrongPasswordAndDisabledUser` | HTTP 401 / code 40101 | 完成 |
| T03 | 错误 password | 同上 | 与 T02 相同外部语义 | 完成 |
| T04 | 禁用用户 | 同上 | HTTP 401 / code 40101 | 完成 |
| T05 | JWT claims | `JwtTokenServiceTest` | sub/jti/role/iss/iat/exp 全部断言 | 完成 |
| T06 | JWT 敏感字段 | `JwtTokenServiceTest`、登录响应断言 | 无 password/passwordHash/secret | 完成 |
| T07 | 正确 JWT 访问业务 API | USER token -> `GET /api/knowledge-bases` | 200，进入业务 Controller | 完成 |
| T08 | 无 token | `GET /api/knowledge-bases` | 401 JSON / code 40102 | 完成 |
| T09 | 篡改 token | 修改签名后请求 | 401 JSON / code 40102 | 完成 |
| T10 | 过期 token | 同密钥签发过去 exp | 401 JSON / code 40102 | 完成 |
| T11 | issuer 错误 | 同密钥、错误 issuer | 401 JSON / code 40102 | 完成 |
| T12 | USER -> ADMIN | USER token -> `GET /api/users` | 403 JSON / code 40303 | 完成 |
| T13 | ADMIN -> ADMIN | ADMIN token -> `GET /api/users` | 200，通过角色检查 | 完成 |
| T14 | health 无 token | `GET /api/health` | 200 / `data=OK` | 完成 |
| T15 | register 无 token | `POST /api/users/register` | 200；Service 回归测试证明只能 USER | 完成 |
| T16 | login 无 token | `POST /api/auth/login` | 可到达标准登录链 | 完成 |
| T17 | dev Swagger/OpenAPI 无 token | `/v3/api-docs/stage-3-day-1`、`/swagger-ui/index.html` | 200 | 完成 |
| T18 | 401 格式 | 无/坏 token | JSON Result，不是 HTML/302/空 body | 完成 |
| T19 | 403 格式 | USER 请求 ADMIN | JSON Result，不是 HTML/空 body | 完成 |
| T20 | 全量回归 | `mvn clean test` | 95 tests；0 failure；0 error | 完成 |

附加验证：旧 `POST /api/users/login` 在无 token 下返回 401，且 OpenAPI 不再包含该路径；非法 `ROLE_ADMIN`/`SUPER_ADMIN` claim 不会映射为 ADMIN authority；带 token 成功后去掉 token 再请求返回 401，响应不设置 Session Cookie。

## 8. 全量测试结果

最终命令：

```text
mvn clean test
Tests run: 95
Failures: 0
Errors: 0
Skipped: 5
BUILD: SUCCESS
```

跳过项仍是 5 个需要 `DB_PASSWORD` 的真实 MySQL 集成测试。未执行真实 MySQL HTTP 登录演示，因此本报告不把数据库端到端登录写成“已验证”。不依赖数据库的完整 Spring MVC + Security + OpenAPI 测试使用真实 Filter Chain、AuthenticationManager、JwtEncoder/JwtDecoder 和运行时随机测试密钥，UserMapper 在测试中被 mock。

## 9. 安全信息检查

- `application.yml` 只含 `${JWT_SECRET_BASE64}` 环境变量占位，没有真实 secret。
- `.env.example` 只有格式、占位和本机生成命令。
- 测试密钥在测试进程中通过 `SecureRandom` 运行时生成，不写入源码和测试输出。
- 依赖树只有官方 Spring Security Resource Server/Nimbus，没有 JJWT/Auth0 等第二套 JWT 库。
- JWT claims、登录响应和 `UserVO` 不包含 password、passwordHash、secret。
- 日志没有输出原始密码、密码摘要、secret、完整 JWT 或 Authorization Header。
- 报告没有记录完整 token 或真实数据库口令。
- `rg` 检查确认主代码不存在旧 `/api/users/login`、自定义 `OncePerRequestFilter`、Redis blacklist/rate limit 实现或硬编码 Bearer token。
- 当前目录不是 Git 仓库，因此无法执行基于 Git 历史/索引的 secret 扫描；已对当前工作区文件执行文本检查。

## 10. 问题与排查记录

### 10.1 旧登录测试在迁移后失败

- 症状：旧 Controller/Service 测试继续调用已删除的 `/api/users/login` 和 `UserService.login`。
- 根因：职责已经迁移到 AuthController/AuthService/AuthenticationManager，旧测试仍绑定阶段 2 API。
- 修复：删除旧旁路职责测试，新增标准认证链和 Security Filter 集成测试；没有恢复旧入口或扩大 permitAll。
- 快速识别：接口迁移后先全局搜索旧路径和旧方法签名。

### 10.2 issuer 简单字符串与 Spring Security 7.1 访问器不兼容

- 症状：JWT 能被部分校验，但 `Jwt#getIssuer()` 无法把 `ljl-agent-backend` 转换为 URL。
- 根因：当前 Spring Security issuer 访问器按 URL 语义读取。
- 修复：默认 issuer 改为 `https://ljl-agent-backend.local`，`SecurityProperties` 启动时要求绝对 HTTP(S) URL。
- 快速识别：签发后同时测试 decoder 和标准 claim accessor，而不只检查原始 Map。

### 10.3 固定历史测试时间导致 token 被判定过期

- 症状：JWT 单测用固定过去时间签发后，由系统时钟 decoder 判断已过期。
- 根因：签发时钟和校验时钟不一致。
- 修复：单测以当前秒为固定签发时刻，保持 token 在 decoder 当前有效窗口内；过期行为由独立请求测试覆盖。
- 快速识别：涉及 `iat/exp` 时明确列出签发时钟、校验时钟和允许偏差。

### 10.4 开发启动时 JWT 密钥包含非法 `$` 字符

- 症状：配置绑定失败，底层异常为 `Illegal base64 character 24`。
- 根因：十六进制 `24` 代表 `$`；终端中的 `JWT_SECRET_BASE64` 实际是 PBKDF2 摘要、环境变量占位文本或其他非 Base64 内容。
- 修复：保留生产环境启动期严格校验；新增 `scripts/run-dev.ps1`，在开发启动时仅为当前进程替换缺失、非法或过短的 JWT 密钥，不写文件、不输出密钥。配置异常同时改为可操作的中文提示，不再把底层字符码作为主要原因暴露。
- 兼容性补充：脚本保持纯 ASCII，避免 Windows PowerShell 5.1 将无 BOM UTF-8 中文误解码后破坏字符串语法；随机密钥生成使用 Windows PowerShell 5.1 与 PowerShell 7 均支持的实例 API。
- 快速识别：先 Base64 解码，再检查解码长度至少 32 字节；JWT 签名密钥与用户 PBKDF2 密码摘要不是同一类数据。

## 11. 面试价值

| 知识点 | 项目实际文件 | 解决的问题 | 面试表达 |
|---|---|---|---|
| AuthenticationManager | `AuthService`、`SecurityConfig` | 收口用户名密码认证 | Controller 不查库比密码，交给 ProviderManager/DaoAuthenticationProvider |
| UserDetailsService | `ProjectUserDetailsService`、`LoginUser` | 把数据库 User 变为框架身份 | userId/status/role 来自服务端数据库 |
| PasswordEncoder 适配 | `ProjectPasswordEncoder` | 保留 PBKDF2 历史数据 | 用适配器接入框架，不强迫用户重置密码 |
| JWT 签发 | `JwtTokenService` | 后续请求携带可验证身份 | 最小 claims、配置 TTL、唯一 jti |
| JwtDecoder | `SecurityConfig` | 防篡改、过期、issuer 错误 | HS256 签名 + 时间 + issuer 三层校验 |
| SecurityContext | 官方 Resource Server Filter Chain | Controller 前建立可信身份 | Bearer token 验证成功后形成 Authentication |
| 401/403 | 两个 JSON handler、`ErrorCode` | 区分未认证与权限不足 | HTTP 状态与五位业务码分层 |
| ROLE_USER/ADMIN | `JwtAuthorityConverter`、URL 规则 | 真实角色差异 | USER 请求用户管理 API 403，ADMIN 通过 |
| STATELESS | `SecurityConfig` | 不依赖服务器 Session | 每次请求都必须显式带 Bearer JWT |

## 12. 60～90 秒可复述项目表达

> 阶段 2 的登录只是 Service 查用户并校验 PBKDF2，只能证明单次密码正确，后续请求没有可信身份。
>
> **阶段 3 Day 1 我接入了 Spring Security：AuthController 把账号密码交给 AuthenticationManager，DaoAuthenticationProvider 通过项目 UserDetailsService 查询 user 表，并用自定义 PasswordEncoder 适配原有 PBKDF2，所以历史用户不需要重置密码。认证成功后 JwtTokenService 使用官方 JwtEncoder 签发 HS256 token，claims 只保留 sub、jti、role、iss、iat、exp。后续 Bearer token 由 Resource Server 的 JwtDecoder 验证签名、issuer 和时间，再建立 Authentication 和 SecurityContext。我把接口分为 health/register/login 公开、核心业务 authenticated、用户管理 ADMIN，并使用 STATELESS，关闭 form login 和 HTTP Basic。Security Filter 层的 401/403 由独立 handler 返回统一 Result JSON。Day 1 只完成认证和接口级授权，Day 2 还要用 Principal 收口对象归属，并用 Redis 实现退出黑名单和限流。**

## 13. Day 1 出口清单

- [完成] 已读取并记录修改前真实项目、登录/密码实现和测试基线。
- [完成] `/api/auth/login` 使用 AuthenticationManager 标准认证链。
- [完成] 既有 PBKDF2/PasswordUtils 历史密码格式继续使用。
- [完成] 登录成功签发可验证 JWT。
- [完成] JWT 包含 sub/jti/role/iss/iat/exp，且无敏感 claims。
- [完成] JwtDecoder 验证 HS256 签名、issuer 和时间。
- [完成] Bearer JWT 建立 Authentication/SecurityContext。
- [完成] STATELESS，不依赖 HttpSession，关闭 form login/HTTP Basic。
- [完成] health/register/login 公开；dev Swagger/OpenAPI 公开。
- [完成] 核心业务 API authenticated；真实用户查询接口 ADMIN。
- [完成] USER -> ADMIN 为统一 JSON 403；ADMIN 通过角色检查。
- [完成] 无、篡改、过期、issuer 错误 token 为统一 JSON 401。
- [完成] 旧 `/api/users/login` 不再形成旁路。
- [完成] JWT secret 只通过环境变量注入，错误配置启动失败。
- [完成] OpenAPI 声明 Bearer JWT Security Scheme。
- [完成] 新增测试覆盖登录、claims、坏 token、角色和公开接口。
- [完成] 阶段 2 Service/Controller 测试继续回归。
- [完成] 最终 `mvn clean test` 为 0 failure、0 error。
- [环境阻塞] 5 个真实 MySQL 集成测试未运行；原因是未提供 `DB_PASSWORD`。影响范围是本机真实数据库/HTTP 端到端演示，不影响已执行的 Security Filter 自动化矩阵。下一动作是在临时 MySQL 库设置 `DB_PASSWORD` 后重跑全量测试。
- [完成] 没有提前实现 Redis、logout、限流、Principal 对象归属、RAG、SSE、Tool Calling 或 Docker。

## 14. Day 2 明确未实现

下一步只能进入阶段 3 Day 2：从 `Authentication`/JWT `sub` 提取可信 currentUserId，系统性替换知识库、文档、切片和聊天接口中客户端可控的 userId；补 user A/user B 越权测试；使用 Redis 保存退出 token 的 jti，TTL 等于 token 剩余有效期；使用 Redis Lua 完成原子固定窗口限流和 HTTP 429。

Day 1 结束时 JWT 仍然只能等待自然过期，主动 logout/黑名单尚不存在；业务 DTO 中的客户端 userId 也尚未完成 Day 2 收口。

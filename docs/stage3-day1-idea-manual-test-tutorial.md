# 阶段 3 Day 1：IntelliJ IDEA 手动验证教程

本文只验证当前项目已经实现的阶段 3 Day 1：标准用户名密码认证、JWT 签发与验证、无状态认证、公开/authenticated/ADMIN 三类接口边界，以及统一 JSON 401/403。

本文依据当前项目真实代码编写。Day 2 的对象级资源归属、logout、Redis 黑名单和限流不在本次验收范围内。

配套请求文件：`http/day1-security-manual-test.http`。

## 1. 最终要得到的结果

完成教程后，应能证明：

1. 应用使用 `dev` profile 连接真实 MySQL 并正常启动。
2. health、register、login 不需要 token。
3. 正确账号密码能获得 HS256 Bearer JWT。
4. 不存在用户、错误密码、禁用用户都返回 HTTP 401 / 业务码 `40101`。
5. 受保护接口缺少、格式错误或被篡改的 token 时返回 HTTP 401 / 业务码 `40102`。
6. USER 可以访问 authenticated 接口，但访问 ADMIN 接口返回 HTTP 403 / 业务码 `40303`。
7. ADMIN 可以访问用户查询接口。
8. 请求不会依赖 Session；带 token 成功后，再去掉 token 仍然返回 401。
9. JWT 只有 `iss/sub/jti/iat/exp/role`，没有密码、密码摘要或密钥。
10. IDEA 中的 Day 1 自动化安全测试全部通过。

## 2. IDEA 和数据库准备

### 2.1 打开项目

在 IDEA 中打开：

```text
C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend
```

确认：

- Project SDK 为 Java 26。
- Maven 已完成 Reload，没有红色依赖。
- 主类为 `com.ljl.agent.AgentBackendApplication`。
- MySQL 8.x 已启动。
- 8080 端口没有被其他进程占用。

### 2.2 建议创建隔离测试数据库

不要用重要业务库做手工验收。推荐在 IDEA 的 Database Console 中执行：

```sql
CREATE DATABASE IF NOT EXISTS ljl_agent_day1_manual
    CHARACTER SET utf8mb4;
```

然后把 `sql/001_init_schema.sql` 连接到 `ljl_agent_day1_manual` 并执行全部语句。

如果使用的是已有历史库，则按照 README 的顺序检查并执行 `002`、`003`、`004` 迁移，而不是重新执行会与现有对象冲突的初始化操作。

也可以在 IDEA Terminal 中执行：

```powershell
$projectPath = (Get-Location).Path.Replace('\', '/')
mysql -u root -p --execute="CREATE DATABASE IF NOT EXISTS ljl_agent_day1_manual CHARACTER SET utf8mb4"
mysql -u root -p --database=ljl_agent_day1_manual --execute="source $projectPath/sql/001_init_schema.sql"
```

## 3. 在 IDEA 中启动应用

### 3.1 生成本地 JWT 密钥

在 IDEA Terminal 的 PowerShell 中执行以下代码。它兼容 Windows PowerShell 5.1 和 PowerShell 7：

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtKeyBytes) } finally { $jwtRandom.Dispose() }
[Convert]::ToBase64String($jwtKeyBytes)
```

复制最后输出的 Base64 字符串，只用于本机 IDEA Run Configuration。不要提交到源码、`.env` 或共享配置。

JWT 签名密钥不是用户的 PBKDF2 密码摘要。不要填写带 `$` 的 `pbkdf2_sha256$...`，也不要填写 `${JWT_SECRET_BASE64}` 或 `$env:JWT_SECRET_BASE64` 这类占位文本。

### 3.2 创建 Run Configuration

打开 `Run -> Edit Configurations`，新建 Spring Boot 或 Application 配置：

| 配置项 | 值 |
|---|---|
| Name | `AgentBackend-Day1-Manual` |
| Main class | `com.ljl.agent.AgentBackendApplication` |
| Use classpath of module | `agent-backend` |
| Working directory | 项目根目录 |
| Active profiles | `dev` |

如果配置类型没有 `Active profiles` 输入框，就在环境变量中加入 `SPRING_PROFILES_ACTIVE=dev`。

在 Environment variables 编辑器中逐项加入：

| 变量 | 示例/说明 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev`；已填写 Active profiles 时可省略 |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | 你的真实本地 MySQL 密码 |
| `DB_URL` | `jdbc:mysql://localhost:3306/ljl_agent_day1_manual?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true` |
| `JWT_SECRET_BASE64` | 上一步生成的 Base64 字符串 |
| `JWT_ISSUER` | `https://ljl-agent-backend.local` |
| `JWT_ACCESS_TOKEN_TTL` | `30m` |

不要勾选把包含口令/密钥的 Run Configuration 存成可共享项目文件。

### 3.3 启动判定

点击 Debug，后续可以使用断点观察认证链。控制台应出现：

```text
The following 1 profile is active: "dev"
Started AgentBackendApplication
```

如果只想快速启动，也可以在 IDEA Terminal 中设置数据库变量后运行：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的真实 MySQL 密码"
$env:DB_URL = "jdbc:mysql://localhost:3306/ljl_agent_day1_manual?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
.\scripts\run-dev.ps1
```

这种启动适合快速请求验证；需要 Java 断点时使用前面的 IDEA Debug 配置。

## 4. 准备 USER、ADMIN 和禁用用户

打开 `http/day1-security-manual-test.http`。每个 `###` 是一个独立请求，点击请求左侧绿色运行图标即可执行。

如果以前执行过本教程，可以先在隔离测试库中删除三个同名测试用户：

```sql
DELETE FROM `user`
WHERE username IN (
    'day1_manual_user',
    'day1_manual_admin',
    'day1_manual_disabled'
);
```

这条清理语句只应在教程专用数据库中执行。

按顺序执行 HTTP 文件中的：

1. `M02-1 register USER`
2. `M02-2 register future ADMIN`
3. `M02-3 register future disabled user`

三个注册响应都应该是 HTTP 200、`code=0`、`role=USER`、`status=1`。这证明公开注册接口不能直接创建 ADMIN。

然后在 IDEA Database Console 中执行：

```sql
UPDATE `user`
SET role = 'ADMIN', status = 1
WHERE username = 'day1_manual_admin';

UPDATE `user`
SET role = 'USER', status = 0
WHERE username = 'day1_manual_disabled';

SELECT id, username, role, status
FROM `user`
WHERE username LIKE 'day1_manual_%'
ORDER BY id;
```

查询结果应满足：

| username | role | status |
|---|---|---:|
| `day1_manual_user` | USER | 1 |
| `day1_manual_admin` | ADMIN | 1 |
| `day1_manual_disabled` | USER | 0 |

必须在更新角色后重新登录 ADMIN，因为角色在登录时写入 JWT；旧 token 不会因数据库字段改变而自动变成 ADMIN。

## 5. 执行 HTTP 验收

### 5.1 建议执行顺序

按 HTTP 文件中的编号执行：

| 编号 | 操作 | 预期结果 |
|---|---|---|
| M01 | 无 token 请求 health | HTTP 200，`code=0`，`data=OK` |
| M02 | 注册三个用户 | HTTP 200，全部只能注册为 USER |
| M03-1 | 正确 USER 登录 | HTTP 200，获得 Bearer JWT，`expiresIn=1800` |
| M03-2 | 正确 ADMIN 登录 | HTTP 200，响应用户角色为 ADMIN |
| M04 | 错误密码、不存在用户、禁用用户登录 | 都是 HTTP 401 / `40101` |
| M05 | 无 token 请求知识库列表 | HTTP 401 / `40102` |
| M06 | USER token 请求知识库列表 | HTTP 200 |
| M07 | 紧接着去掉 token 再请求 | HTTP 401 / `40102`，证明无状态 |
| M08 | USER token 请求用户列表 | HTTP 403 / `40303` |
| M09 | ADMIN token 请求用户列表 | HTTP 200 |
| M10 | 篡改或乱写 token | HTTP 401 / `40102` |
| M11 | 无 token 请求旧 `/api/users/login` | HTTP 401；OpenAPI 中不存在旧入口 |
| M12 | 获取 Day 1 OpenAPI 和 Swagger | HTTP 200，存在 `bearerAuth` |

配套 HTTP 文件会自动把正确登录返回的 token 保存为 IDEA Client Global 变量 `user_token` 和 `admin_token`。因此必须先运行登录请求，再运行需要相应 token 的请求。

### 5.2 重点检查统一错误契约

错误密码、未知用户和禁用用户应该得到相同外部响应，避免泄露账号是否存在：

```json
{
  "code": 40101,
  "message": "用户名或密码错误",
  "data": null
}
```

缺少、格式错误、被篡改、过期或 issuer 错误的 Bearer token 应统一为：

```json
{
  "code": 40102,
  "message": "未认证或登录状态无效",
  "data": null
}
```

合法 USER 身份请求 ADMIN 接口应为：

```json
{
  "code": 40303,
  "message": "权限不足",
  "data": null
}
```

注意 HTTP 状态与 JSON 中的五位业务码是两层信息：未认证是 HTTP 401，权限不足是 HTTP 403。

### 5.3 检查无状态行为

执行 M06 后检查响应头，不应存在 `Set-Cookie: JSESSIONID=...`。随后执行没有 Authorization Header 的 M07，仍必须返回 401。

如果第二次无 token 也能成功，说明错误地依赖了 Session，不符合 Day 1 的 `STATELESS` 目标。

## 6. 本地检查 JWT 内容

从 M03-1 登录响应中复制 `data.accessToken`。不要把真实 token 粘贴到第三方 JWT 网站；直接在 IDEA Terminal 本地解码 Payload：

```powershell
$token = '把 accessToken 粘贴到这里'
$payload = $token.Split('.')[1].Replace('-', '+').Replace('_', '/')
switch ($payload.Length % 4) {
    2 { $payload += '==' }
    3 { $payload += '=' }
}
$json = [Text.Encoding]::UTF8.GetString(
    [Convert]::FromBase64String($payload)
)
$json | ConvertFrom-Json | Format-List
```

应该只看到：

```text
iss   https://ljl-agent-backend.local
sub   当前数据库用户 ID 的字符串
jti   每次登录新生成的 UUID
iat   签发时间
exp   过期时间
role  USER 或 ADMIN
```

检查：

- `exp - iat = 1800` 秒。
- 连续登录两次时 `jti` 不同。
- 没有 `password`、`passwordHash`、`secret`、邮箱或业务正文。
- JWT Payload 只是 Base64URL 编码，并不是加密；不要在 claims 中放隐私或秘密。

## 7. 使用 Swagger UI 复核

浏览器打开：

```text
http://localhost:8080/swagger-ui/index.html
```

也可以打开：

```text
http://localhost:8080/swagger-ui.html
```

步骤：

1. 在认证接口执行 `/api/auth/login`。
2. 复制 `accessToken`。
3. 点击右上角 Authorize。
4. 输入 token 本身；通常不要再手工添加 `Bearer `，Swagger 会根据 bearer scheme 自动添加。
5. 执行知识库列表和用户列表，复核 USER/ADMIN 差异。

OpenAPI JSON：

```text
http://localhost:8080/v3/api-docs/stage-3-day-1
```

在 JSON 中搜索：

- 存在 `/api/auth/login`。
- 不存在 `/api/users/login`。
- 存在 `bearerAuth`。
- security scheme 的 `type` 为 `http`，`scheme` 为 `bearer`。

Swagger 只在 `dev` profile 下公开。如果返回 401，先检查 IDEA 是否真正激活了 `dev`。

## 8. 在 IDEA 中运行 Day 1 自动化测试

普通安全测试不需要 MySQL，也不需要手工设置 JWT 密钥。测试代码会运行时生成随机测试密钥。

在 Project 窗口中逐个右键测试类，选择 `Run`：

| 测试类 | 验证内容 |
|---|---|
| `SecurityPropertiesTest` | issuer、Base64、密钥长度、TTL 校验 |
| `SecurityPropertiesBindingTest` | 错误 JWT 配置让上下文提前失败 |
| `ProjectPasswordEncoderTest` | 既有 PBKDF2 密码兼容 |
| `ProjectUserDetailsServiceTest` | USER、ADMIN、禁用、不存在、非法角色 |
| `JwtAuthorityConverterTest` | 只接受 USER/ADMIN，不允许角色前缀提权 |
| `JwtTokenServiceTest` | 最小 claims、TTL、唯一 jti、无敏感字段 |
| `SecurityWebIntegrationTest` | 登录、JWT、401/403、角色、无状态、Swagger |
| `ConfigurationSafetyTest` | 源码中没有硬编码 JWT 密钥 |

`SecurityPropertiesBindingTest` 运行时出现预期的 Context 初始化 WARN 日志不代表测试失败；最终状态为绿色才是判断标准。

### 8.1 必跑的 SecurityWebIntegrationTest 方法

在类中点击方法左侧绿色图标，可以单独验证：

- `shouldLoginThroughAuthenticationManagerAndReturnSafeJwt`
- `shouldUseSame401ForUnknownWrongPasswordAndDisabledUser`
- `shouldEnforcePublicAuthenticatedAdminAndStatelessBoundaries`
- `shouldRejectTamperedExpiredWrongIssuerAndIllegalRoleTokens`
- `shouldExposeDevOpenApiAndBearerSecurityScheme`

手工客户端很难在不知道服务端密钥的情况下正确签发“过期但签名正确”“issuer 错误但签名正确”“非法 role 但签名正确”的 token。因此这三类场景应以第四个方法的真实 JwtEncoder/Decoder 测试结果为准，而不是使用没有有效签名的伪 token 冒充验证。

### 8.2 运行全部 95 个测试

不配置数据库环境变量时，从 IDEA 运行全部测试的预期结果是：

```text
Tests run: 95
Failures: 0
Errors: 0
Skipped: 5
```

5 个 skipped 是需要真实 MySQL 的测试，不是 Day 1 Security 失败。

如果希望 5 个 MySQL 测试也执行，新建一个 JUnit `All in project` 或 Maven `clean test` Run Configuration，并加入：

```text
DB_USERNAME=root
DB_PASSWORD=你的测试数据库密码
DB_URL=jdbc:mysql://localhost:3306/ljl_agent_day1_manual?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

然后运行。数据库结构正确时应为 95 个测试、0 failure、0 error、0 skipped。Mapper 测试使用事务回滚，但仍建议始终指向隔离测试库。

## 9. 建议断点：观察真实认证链

用 IDEA Debug 启动应用，然后按顺序设置断点：

1. `AuthController.login`：公开登录请求进入 Controller。
2. `AuthService.login`：把 username/password 交给 `AuthenticationManager`。
3. `ProjectUserDetailsService.loadUserByUsername`：从数据库加载用户、角色和状态。
4. `ProjectPasswordEncoder.matches`：适配项目既有 PBKDF2 格式。
5. `JwtTokenService.issue`：构造最小 claims 并签发 HS256 token。
6. `JwtAuthorityConverter.convert`：后续 Bearer 请求把 JWT role 转成 authority。
7. `JsonAuthenticationEntryPoint.commence`：观察 401 JSON。
8. `JsonAccessDeniedHandler.handle`：观察 403 JSON。

登录链应是：

```text
AuthController
-> AuthService
-> AuthenticationManager
-> DaoAuthenticationProvider
-> ProjectUserDetailsService
-> ProjectPasswordEncoder
-> JwtTokenService
```

Bearer 请求由 Spring Security 官方过滤器先验签、验证 issuer/时间并建立 `SecurityContext`，Controller 不应手工解析 token。

## 10. 常见问题

### 10.1 `Illegal base64 character 24`

`24` 是十六进制 `0x24`，代表 `$`。说明 `JWT_SECRET_BASE64` 中放入了 PBKDF2 摘要或占位文本。重新生成 Base64 密钥，或者在 IDEA Terminal 使用 `scripts/run-dev.ps1`。

### 10.2 正确密码仍返回 40101

检查：

```sql
SELECT id, username, role, status, password_hash
FROM `user`
WHERE username = 'day1_manual_user';
```

- `status` 必须为 1。
- `password_hash` 应以 `pbkdf2_sha256$` 开头。
- 本教程密码是 `Day1Test123!`。
- 不要把数据库摘要当作登录密码。

### 10.3 ADMIN 仍返回 40303

- 确认数据库 role 精确为 `ADMIN`，不是 `ROLE_ADMIN`。
- 更新数据库角色后重新登录并使用新 token。
- 解码 token，确认 claim `role=ADMIN`。

### 10.4 应用重启后旧 token 返回 40102

如果使用 `run-dev.ps1` 且每次启动生成了新密钥，旧 token 的签名会失效，这是正常现象。重新登录获取新 token。

### 10.5 Swagger 返回 401

确认控制台显示 active profile 为 `dev`。Swagger 只在 dev profile 下放行。

### 10.6 IDEA 中 MySQL 测试仍显示 skipped

`@EnabledIfEnvironmentVariable` 检查的是测试 Run Configuration 进程中的 `DB_PASSWORD`。只给应用启动配置填写变量，不会自动传给另一个 JUnit/Maven 配置。

## 11. Day 1 手工验收记录表

可以在完成后勾选：

- [ ] 应用以 dev profile 启动并连接隔离 MySQL。
- [ ] health 无 token 返回 200。
- [ ] 公开注册只能创建 USER。
- [ ] 正确 USER/ADMIN 登录返回 Bearer JWT。
- [ ] 未知用户、错误密码、禁用用户统一返回 40101。
- [ ] JWT claims 只有 iss/sub/jti/iat/exp/role。
- [ ] 无 token、坏 token、篡改 token 返回 40102。
- [ ] USER token 能访问 authenticated API。
- [ ] USER token 访问用户管理 API 返回 40303。
- [ ] ADMIN token 访问用户管理 API 返回 200。
- [ ] 带 token 成功后去掉 token 返回 401，且没有 JSESSIONID。
- [ ] 旧 `/api/users/login` 不再是公开登录入口。
- [ ] OpenAPI 存在 bearerAuth 和 `/api/auth/login`。
- [ ] Day 1 安全相关测试在 IDEA 中全部为绿色。
- [ ] 全量测试 0 failure、0 error。

以上项目全部满足，才能把当前 Day 1 表述为“认证与接口级授权已经完成”；对象级资源归属和 token 主动失效仍应明确留到 Day 2。

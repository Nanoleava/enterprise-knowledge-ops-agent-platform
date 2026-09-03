# Java 后端 + Agent 开发项目源指引

> 文档版本：v4.1
> 更新日期：2026-09-01
> 依据文件：`C:\Users\nano\Desktop\LJL-Java-Agent\java_backend_agent_learning_plan.md`
> 当前项目检查路径：`C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend`
> 阶段 1 历史项目路径：`C:\Users\nano\Desktop\lijunling\10services_data\nano_personal1`
> 当前实施证据：`agent-backend/docs/stage4-day1-implementation-report.md`

本文档是后续每天让 ChatGPT 指导学习和写代码的项目源。ChatGPT 需要把它当作“路线图 + 阶段验收标准 + 目录结构规范”，不要只按单个知识点零散教学。

重要原则：本文档中出现的历史快照只代表对应日期的检查结果，不是永久状态。以后每次使用本文档指导学习时，必须先重新检查项目文件、目录和已完成功能，再判断当前阶段。

当前默认指导状态：

```text
截至 2026-09-01，阶段 1、阶段 2、阶段 3 与阶段 4 DAY 1 已全部完成。
阶段 4 DAY 1 已形成安全 Multipart 上传、配置化本地存储、文件元数据与处理状态、TXT/Markdown 解析清洗、失败重试和状态查询闭环。
2026-09-01 使用真实 MySQL、真实 Redis 和文件系统重新执行全量测试：137 项，0 失败，0 错误，0 跳过，BUILD SUCCESS。
当前精确位置：阶段 4 DAY 2 开始前。
下一主线：完成 PDF 解析、固定长度 + overlap 自动切片、批量原子替换、处理日志、失败恢复和删除物理文件清理，执行阶段 4 总验收。
```

---

## 1. ChatGPT 使用规则

本节是阶段 4 的硬性指导规则。阶段 1～阶段 3 已完成，后续默认围绕阶段 4 的两天项目主线推进：

```text
[DAY 1] 主线 A：文档上传、文件安全校验、本地存储、元数据与处理状态落库
[DAY 1] 主线 B：TXT / Markdown 解析，跑通“上传 -> 解析 -> content”最小闭环
[DAY 2] 主线 C：PDF 解析、文本清洗、固定长度切片、批量写入 document_chunk
[DAY 2] 主线 D：失败状态、重试、文件清理、自动化与真实 MySQL 阶段验收
```

阶段 3 的认证和资源隔离继续作为所有阶段 4 接口的安全底座：客户端不得传入或决定 owner，所有文档操作都必须从 JWT Principal 得到 `currentUserId`，并验证知识库和文档归属。

### 1.1 当前默认指导状态

```text
当前日期基线：2026-09-01
当前默认阶段：阶段 4（DAY 2 开始前）
当前默认项目：C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend
当前默认包名：com.ljl.agent
阶段 1：已完成
阶段 2：已完成
阶段 3：已完成；原始基线 114 tests，安全能力在当前 137 项全量回归中继续通过
阶段 4 DAY 1：已完成；137 tests、0 failure、0 error、0 skipped，真实 MySQL/Redis/文件系统已复验
当前默认任务：按第 7.11 节完成阶段 4 DAY 2 的 PDF、自动切片、批量入库、失败恢复和删除清理闭环
当前第一优先级：先复跑 DAY 1 基线，再完成 PdfParser 失败边界与 FixedLengthChunkStrategy 纯算法测试
当前禁止任务：Embedding、Qdrant、RAG 问答、SSE、Tool Calling、消息队列、Docker 化
```

每次指导前仍须重新检查项目。只有阶段 3 回归失败时才修复认证、资源隔离或 Redis 缺陷；不得重复实现已经通过验收的 JWT、logout、限流，也不得为追求 CRUD 对称而插入知识库更新/删除、用户更新/删除等非阻塞任务。

### 1.2 实际代码前置与缺失代码索取规则

实际项目代码是具体教学、判断和修改的前置事实，不能用本文档中的规划或常见写法替代。

1. 涉及阶段判断、代码生成、文件修改或排查时，先读取 `pom.xml`、`application*.yml`、Document/DocumentChunk/KnowledgeBase 的 Controller、Service、Mapper、Entity、DTO、SQL 和测试。
2. 涉及文件上传时，还要确认 Spring Multipart 配置、存储根目录、文件大小限制、允许类型和目录穿越防护。
3. 涉及解析或切片时，必须先确认当前 `document.content`、`document_chunk` 约束、唯一索引、批量 SQL 与事务边界。
4. 涉及删除或重试时，必须同时检查数据库记录、物理文件、旧 chunk 和处理日志，不能只看一张表。
5. 无法访问项目或缺少关键文件时，应暂停具体补丁并索要最小必要文件；不能虚构类名、字段、接口、依赖版本或测试结果。
6. 修改前说明依据文件和调用链；修改后必须运行与风险相称的单元、Controller、Mapper/集成和全量回归测试。
7. 尚未由代码或运行结果确认的信息必须标记为“待确认”，不能把计划中的目录当成已经存在。
8. 用户提供的数据库、Redis 或第三方密钥只能注入当前进程环境，不得写入文档、源码、配置、命令记录或日志。

缺少代码时，默认使用下面的阻断式回复：

```text
我目前没有看到完成这项教学或修改所需的实际项目代码，因此不能基于猜测继续。
请提供或允许我读取以下文件：<最小必要文件清单>。
读取并确认现有结构、调用链、数据库约束和测试后，我再给出与当前项目一致的步骤或修改。
```

### 1.3 证据优先与阶段推进规则

1. 先运行最近一次可信全量回归；阶段 3 原始基线为 114 项，阶段 4 DAY 1 后当前基线为 137 项，任一既有测试失败都要先判断是否由当前改造引入。
2. “能收到 MultipartFile”不等于完成上传；必须同时有 owner 校验、类型/大小校验、安全文件名、存储结果和失败清理。
3. “文件已保存”不等于完成文档处理；必须形成“文件 -> 解析文本 -> 清洗 -> chunk -> MySQL”的可验证链路。
4. `document.status` 是业务生命周期；`parseStatus` 和 `chunkStatus` 是处理状态，不得混成一个含义模糊的字段。
5. 解析和切片应通过策略接口隔离格式差异，不在 Controller 中堆积 `if/else` 和文件 IO。
6. 文件系统不参与数据库事务，必须明确补偿策略：数据库失败如何删文件、处理失败如何留原文件重试、删除文档如何清理物理文件。
7. 文件名、Content-Type 和扩展名都不可信；存储路径必须由服务端生成并校验位于配置的根目录下。
8. 阶段 4 默认使用同步处理以便学清调用链；异步队列、分布式对象存储和 OCR 不作为两天出口项。
9. 自动 chunk 是派生数据。重试时应原子替换该文档旧 chunk，不通过开放单条 chunk 更新/删除 API 维护。
10. 阶段 3 的 `currentUserId` 和 owner 校验必须贯穿上传、处理、状态查询、chunk 查询和文档删除。

### 1.4 每天必须遵守的输出格式

ChatGPT 每次指导阶段 4 时必须按下面结构输出：

```text
今日阶段：阶段 4 DAY 1 / DAY 2 / 验收补缺
阶段判断依据：刚检查的代码、SQL、配置、测试和外部服务
今日唯一主目标：一个可运行、可断言的文档处理闭环
今天明确不做：推迟到另一 DAY 或后续阶段的内容

一、教学
1. 今天的上传、存储、解析、清洗、切片或事务知识点
2. 它解决当前项目的什么真实问题
3. 它位于 Controller -> Ingestion -> Storage/Parser/Cleaner/Chunker -> Mapper 的哪里
4. 与现有 Document、DocumentChunk、currentUserId 如何衔接
5. 高频面试问题和关键取舍

二、实操
1. 修改/新增文件的绝对路径与职责
2. 数据库迁移、状态流转与索引
3. API 请求、响应、错误码
4. 文件存储路径和安全规则
5. 调用链、事务边界与失败补偿
6. 为什么这样设计

三、验证
1. 单元、MockMvc、Mapper/集成和 mvn 全量命令
2. TXT、Markdown、PDF 的成功案例
3. 空文件、超限、伪扩展名、解析失败、越权和重试案例
4. MySQL document/document_chunk/process_log 证据
5. 文件系统路径、数量和失败清理证据
6. 失败时排查顺序

四、面试闭环
1. 原理
2. 项目实现点
3. 对应文件
4. 一致性、安全和性能取舍
5. 60～90 秒项目表达

今日验收标准：必须是可运行、可查询、可断言的条件
明日衔接：只写尚未完成的下一闭环
```

### 1.5 阶段 4 固定 API 边界

下表是阶段 4 推荐边界；实际修改前仍要与当前 Controller 和 OpenAPI 再次核对。

| 接口 | 作用 | DAY | 安全与状态要求 |
|---|---|---:|---|
| `POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload` | Multipart 上传并创建文档元数据 | 1 | JWT 用户必须拥有知识库；服务端生成存储名 |
| `POST /api/documents/{documentId}/parse` | 解析、清洗并保存正文 | 1 | 必须校验文档 owner；失败写入 parse 状态 |
| `POST /api/documents/{documentId}/process` | 生成或原子替换 chunk | 2 | 复用已解析正文；必要时先重新解析 |
| `GET /api/documents/{documentId}/processing-status` | 查询解析/切片状态与可公开错误摘要 | 1 | 只允许 owner；不返回服务器绝对路径 |
| `GET /api/documents/{documentId}/chunks` | 查询自动生成的 chunk | 2 | 复用现有接口和 owner 校验 |
| `DELETE /api/documents/{documentId}` | 删除文档、chunk，并清理物理文件 | 2 | 扩展现有接口语义，不新增重复删除接口 |

阶段 2 的 `POST /api/documents` 手工正文和 `POST /api/documents/{id}/chunks` 手工切片接口保留作回归与教学对照，但不再代表阶段 4 主链路。阶段 4 不新增独立的 Chunk CRUD；重新切片统一由 `process` 原子替换。

### 1.6 阶段 4 代码原则

```text
DocumentUploadController：接收 multipart、Principal 和简单参数，不直接解析文件或拼接磁盘路径。
DocumentIngestionService：编排 owner 校验、存储、解析、清洗、切片、状态更新和失败补偿。
FileStorageService：生成安全文件名，保证 resolvedPath 位于 storage root，负责保存、读取和删除。
DocumentParser：按文件类型选择实现；TxtParser、MarkdownParser、PdfParser 只负责把文件转成文本。
TextCleaner：统一换行、BOM/NUL、首尾空白和过量空行，不随意破坏 Markdown 标题结构。
ChunkStrategy：输入清洗后的文本，输出有序 ChunkResult；固定长度策略必须校验 chunkSize 与 overlap。
DocumentMapper：保存文件元数据、处理状态、正文和错误摘要，owner 条件不能缺失。
DocumentChunkMapper：支持 batchInsert、deleteByDocumentId，重处理时在同一数据库事务中替换。
DocumentProcessLogMapper：记录 UPLOAD/PARSE/CLEAN/CHUNK/PERSIST 的开始、成功或失败摘要。
现有 Security/CurrentUser：继续提供唯一可信 currentUserId，不在上传请求中接收 userId。
```

优先使用 Spring MVC 自带 Multipart 支持；TXT/Markdown 使用 JDK IO，PDF 使用一个经过项目兼容性验证的 PDF 解析依赖。阶段 4 不引入通用大型文档框架、OCR、对象存储 SDK 或异步任务框架。

### 1.7 每天与阶段完成判定

当天完成必须同时满足：

```text
1. 能画出当天新增的数据流和失败流。
2. 成功路径与至少三个关键失败分支有自动化测试。
3. owner 校验发生在任何文件读取、处理或 Mapper 写操作之前。
4. 响应和日志不泄露服务器绝对路径、文件正文、密码、密钥或完整 token。
5. 数据库状态与磁盘结果一致；若无法原子一致，补偿和可重试状态已验证。
6. mvn clean test 为 0 failure、0 error；MySQL 集成测试不得因已知凭据而跳过。
7. 能结合实际文件说明格式策略、事务边界和切片取舍。
```

DAY 1 只有在上传、存储、状态和 TXT/Markdown 解析最小闭环全部通过后才结束。DAY 2 必须完成固定长度切片、批量入库、PDF、失败恢复、文件清理和第 7 章总验收，之后才能进入阶段 5。

---

## 2. 阶段识别方法与项目快照记录

本章只保存阶段判断方法、简短历史索引和最新可信快照。完成阶段的详细实现不在路线文档中重复，统一由实施报告、测试和 Git 历史承担证据职责。

### 2.1 每次使用前必须重新识别阶段

当前阶段定义为“最早一个尚未满足出口标准的阶段”，而不是目录中出现的最高级技术名词。检查顺序如下：

```text
1. 读取 pom.xml、application*.yml 和 SQL 增量脚本，确认真实依赖与数据结构。
2. 检查 controller、service、mapper、entity、dto、config、security、ingestion 等实际目录。
3. 检查 API、文件系统、数据库和外部服务是否形成可运行链路。
4. 检查测试断言、条件跳过原因、最近一次构建与外部集成结果。
5. 检查 README、实施报告与代码是否一致，不能只依据自报进度。
6. 对照阶段出口标准，找出第一个未闭环项。
7. 用户口述进度作为线索，但尽量由文件与运行结果复核。
```

| 证据 | 阶段判断 |
|---|---|
| Java 类、集合、控制台入口，无 Spring Boot | 阶段 1 |
| Spring Boot + REST + MyBatis/MySQL 核心业务闭环 | 阶段 2 |
| JWT + Spring Security + Principal 资源隔离 + Redis 可运行闭环 | 阶段 3 |
| 文件上传 + 存储 + 解析 + 清洗 + 自动切片 + 处理状态 | 阶段 4 |
| Embedding + 向量库 + 检索增强回答 | 阶段 5 |
| 多轮历史 + SSE 流式输出 | 阶段 6 |
| Tool Calling + Workflow + Eval | 阶段 7 |
| Docker Compose + Nginx + Actuator + 部署验证 | 阶段 8 |
| 简历、架构图、演示材料与面试稿完整 | 阶段 9 |

仅出现依赖、空类或单一成功 Demo 时，应记录为“该阶段已开始但未完成”。

### 2.2 历史快照索引

```text
2026-06-26：阶段 1 早期，纯 Java 内存版原型。
2026-07-10：阶段 1 完成，准备迁移 Spring Boot/MySQL。
2026-08-13：阶段 2 Day 1～Day 4 完成，进入工程化收尾。
2026-08-15：阶段 2 复核完成，阶段 3 DAY 1 开始前。
2026-08-18：阶段 3 DAY 1 完成。
2026-08-19：阶段 3 DAY 2、阶段 3 总出口完成。
2026-08-20：真实 MySQL + Redis 全量复验通过，正式进入阶段 4 DAY 1。
2026-08-21：阶段 4 DAY 1 完成，形成安全上传与 TXT/Markdown 解析闭环，137 项全量测试通过并提交 `2679565`。
2026-09-01：真实 MySQL、Redis、文件系统与 137 项测试再次复验通过，正式进入阶段 4 DAY 2。
```

旧快照不再驱动当前任务。需要复盘时查 `docs/stage3-day1-implementation-report.md`、`docs/stage3-day2-implementation-report.md` 或 Git 历史。

### 2.3 已完成阶段摘要

```text
阶段 1：已完成 Java 内存版后端原型。
阶段 2：已完成 Spring Boot + MySQL + MyBatis REST API、六个核心模块和工程化测试闭环。
阶段 3：已完成 Spring Security + JWT、USER/ADMIN、Principal owner 隔离、Redis logout 黑名单和 Lua 限流。
```

阶段 3 的教学步骤不再在本路线文档重复。其最终证据入口为 `agent-backend/docs/stage3-day2-implementation-report.md`。

### 2.4 当前快照：2026-08-20

检查路径：

```text
C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend
```

本次检查覆盖参考文档、阶段 3 DAY 2 报告、`pom.xml`、应用配置、Security/Auth/Redis、Controller/Service/Mapper、SQL、README 和全部测试。

当前可信证据：

```text
1. Docker redis:7 容器运行中，6379 映射正常，redis-cli PING 返回 PONG。
2. 使用 dev profile、真实 MySQL 与 REDIS_INTEGRATION_TEST=true 执行 mvn clean test。
3. Tests run: 114；Failures: 0；Errors: 0；Skipped: 0；BUILD SUCCESS。
4. MySQL Mapper、Schema、OpenAPI 完整上下文测试均真实执行。
5. Redis blacklist value/TTL 与 Lua 计数/TTL 集成测试真实执行。
6. currentUserId 来自 JWT sub；KB、Document、Chunk、Session、Message owner 隔离已覆盖。
7. logout 旧 token、401/403/429/503、USER/ADMIN 和配置/日志安全闭环已覆盖。
8. Postman 完整逐项演示已由用户明确豁免；它不是阶段 3 阻塞项。
9. 当前 Git worktree 仍包含阶段 3 的已修改/新增文件；这不影响功能验收，但进入阶段 4 前应先人工确认 diff 并创建可回退的阶段 3 commit/tag。
```

当前阶段判断：

```text
阶段 1：已完成。
阶段 2：已完成。
阶段 3：已完成。
阶段 4：尚未开始编码，当前精确位置为 DAY 1 开始前。
阶段 5～阶段 9：未开始。
```

### 2.5 2026-08-20 快照的使用方式与阶段 4 起点

默认下一步是第 7.10 节阶段 4 DAY 1，不再重做阶段 3。当前项目可直接复用：

- `CurrentUser` 与所有 owner 校验；
- `DocumentController/Service/Mapper` 的创建、详情、分页和删除；
- `DocumentChunk` 的插入和按 `documentId` 有序查询；
- MySQL、统一 `Result`、错误码、OpenAPI 与测试基线。

当前尚不存在、必须由阶段 4 新增的能力：

```text
1. Multipart 文档上传 API。
2. 配置化本地存储根目录和安全文件名。
3. document 文件元数据、parse_status、chunk_status、错误摘要字段。
4. TXT、Markdown、PDF parser。
5. 文本清洗和自动 ChunkStrategy。
6. chunk 批量插入、重处理时原子替换。
7. 处理状态查询、失败状态和重试。
8. 文档删除后的物理文件清理。
9. 阶段 4 专用单元、Controller、Mapper 和端到端测试。
```

当前 `document.content` 为 `NOT NULL`，上传后解析前可暂存空字符串；是否调整列约束必须在 DAY 1 迁移设计中明确。当前文档删除只删除数据库记录并依赖外键级联 chunk，阶段 4 加入文件后必须扩展清理语义。

### 2.6 当前快照：2026-09-01（阶段 4 DAY 1 完成）

检查路径：

```text
C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend
```

本次按第 7.10 节逐项复核源码、SQL 005、配置、Controller/Service/Mapper、Storage/Parser/Cleaner、OpenAPI、测试、真实 MySQL、Redis 和现有上传文件。

当前可信证据：

```text
1. Git main/origin/main 位于提交 2679565：feat: complete secure document ingestion milestone；复核前 worktree 干净。
2. Redis 容器 PING 返回 PONG；dev profile + 真实 MySQL + REDIS_INTEGRATION_TEST=true 执行 mvn clean test。
3. Tests run: 137；Failures: 0；Errors: 0；Skipped: 0；BUILD SUCCESS。
4. SQL 005 所需 10 个 document 字段、idx_document_user_parse_status 和 3 个 CHECK 约束在真实 MySQL 中存在。
5. 当前数据库有 22 条 document：19 条历史手工文档保持 NOT_APPLICABLE/NOT_APPLICABLE，3 条上传文档为 1 条 PENDING/PENDING、2 条 SUCCESS/PENDING；旧数据未丢失。
6. 3 个上传文件均位于受控相对路径，磁盘大小与数据库 file_size 一致，SHA-256 与 file_checksum 全部匹配。
7. TXT/Markdown 上传、解析到 document.content、FAILED 重试、processing-status、userB 越权零污染均有自动化证据。
8. 用户已使用 Postman 跑通真实 Markdown 上传、parse 与 status；document 102、112 的 content 已在 MySQL 验证非空。
9. README、OpenAPI stage-4-day-1 分组、环境变量示例、错误码和阶段 4 DAY 1 实施报告已经同步。
10. 数据库密码、JWT、Token、用户上传原文件和构建产物均未进入 Git。
```

当前阶段判断：

```text
阶段 1：已完成。
阶段 2：已完成。
阶段 3：已完成并持续回归通过。
阶段 4 DAY 1：第 7.10.4 全部出口项通过。
阶段 4 DAY 2：尚未开始，当前精确位置为 DAY 2 开始前。
阶段 4 总验收：尚未执行；必须等待 PDF、自动切片、批量原子替换、处理日志、失败恢复和删除清理完成。
阶段 5～阶段 9：未开始。
```

非阻塞技术债：JDK 26 测试输出包含 Mockito 动态 agent 与少量 deprecated API 编译警告；它们不影响 DAY 1 功能或测试结论，但后续依赖升级时应处理。默认 `./storage/uploads` 仅用于开发，生产部署应通过 `DOCUMENT_STORAGE_ROOT` 指向仓库外数据卷或后续替换对象存储。

下一唯一行动：按第 7.11 节复跑 DAY 1 后进入 DAY 2；不提前进入 Embedding、Qdrant 或 RAG。

### 2.7 后续快照固定格式

每个 DAY 和阶段出口只追加一份简短快照：

```text
日期：
检查路径：
当前阶段与 DAY：
本次新增闭环：
API 与数据状态：
自动化测试：总数 / 失败 / 错误 / 跳过及原因
外部验证：MySQL / Redis / 文件系统 / 其他
安全与失败路径：
尚未完成的出口项：
下一唯一行动：
```

快照只记录事实和证据，不复制大段教学计划；计划始终以第 7、14、16、17 章为准。

---

## 3. 总体阶段路线

项目路线从早期纯 Java 练习演进到可面试展示的 Agent 后端，分为 9 个阶段：

| 阶段 | 名称 | 对应学习计划 | 结束时项目状态 |
|---|---|---|---|
| 阶段 1 | Java 内存版后端原型 | 第 1 周 | 控制台可运行，使用集合保存用户、文档、消息 |
| 阶段 2 | Spring Boot + MySQL CRUD | 第 2-3 周 | 有 REST API、MySQL、MyBatis、基础 CRUD |
| 阶段 3 | 登录权限 + Redis | 第 4 周 | 有 JWT、权限隔离、Redis 退出黑名单/限流 |
| 阶段 4 | 知识库文档处理 | 第 5 周前半 | 支持上传、解析、切片、保存文档块 |
| 阶段 5 | RAG 检索问答 | 第 5 周后半 | 支持 embedding、向量检索、prompt 增强 |
| 阶段 6 | Agent 对话 + SSE | 第 6 周 | 支持会话、历史、流式输出、引用来源 |
| 阶段 7 | Tool Calling + Workflow + Eval | 第 7-8 周 | Agent 能调用工具，有日志和评估 |
| 阶段 8 | Docker 部署上线 | 第 9 周 | Docker Compose、Nginx、Actuator、生产配置 |
| 阶段 9 | 面试包装 | 第 10 周 | README、架构图、接口文档、面试问答完整 |

当前路线校准（2026-08-20）：阶段 1～阶段 3 已完成并通过真实 MySQL/Redis 全量复验；当前进入阶段 4 DAY 1，目标是先完成安全上传、存储、状态和 TXT/Markdown 解析闭环。

---

## 4. 阶段 1：Java 内存版后端原型（已完成）

### 4.1 阶段目标

这一阶段的目标不是做 Web 项目，而是把 Java 基础落到业务代码里。

结束时应该能演示：

```text
1. 创建用户
2. 新增文档
3. 查询文档
4. 发送聊天消息
5. 查看聊天消息
6. 按用户查询消息
7. 按关键词搜索文档和消息
8. 演示业务异常
```

### 4.2 本阶段最终目录结构

这是阶段 1 的最终结构，用于回看和复盘。当前项目已经达到这个方向，不应继续把它作为每日默认学习任务：

```text
nano_personal1/
`-- src/
    `-- main/
        `-- java/
            `-- com/
                `-- ljl/
                    |-- Main.java
                    |-- cli/
                    |   |-- ConsoleMenu.java
                    |   `-- InputReader.java
                    |-- exception/
                    |   `-- BusinessException.java
                    |-- model/
                    |   |-- BaseEntity.java
                    |   |-- ChatMessage.java
                    |   |-- Document.java
                    |   |-- Searchable.java
                    |   `-- User.java
                    |-- repository/
                    |   |-- ChatMessageRepository.java
                    |   |-- DocumentRepository.java
                    |   |-- MemoryRepository.java
                    |   `-- UserRepository.java
                    |-- service/
                    |   |-- ChatMessageService.java
                    |   |-- DocumentService.java
                    |   |-- UserService.java
                    |   `-- impl/
                    |       |-- ChatMessageServiceImpl.java
                    |       |-- DocumentServiceImpl.java
                    |       `-- UserServiceImpl.java
                    `-- util/
                        `-- TextUtils.java
```

### 4.3 文件职责

| 文件或目录 | 职责 |
|---|---|
| `Main.java` | 只负责启动控制台菜单 |
| `cli/ConsoleMenu.java` | 控制台菜单、用户输入、调用 Service |
| `model` | 业务实体和接口，不直接处理输入输出 |
| `repository` | 内存存储，使用 `Map`、`List`、`Set` |
| `service` | 业务逻辑、参数校验、异常处理 |
| `exception/BusinessException.java` | 表示业务错误 |
| `util/TextUtils.java` | 文本拼接、关键词判断等简单工具 |

### 4.4 本阶段不要出现的目录

```text
controller
mapper
config/SecurityConfig.java
application.yml
Dockerfile
redis
rag
agent/tool
```

这些在阶段 1 当时属于后续阶段。阶段 2 已经完成 Spring Boot、Controller、配置文件、Maven、MySQL 和 MyBatis；当前进入阶段 3，只引入 JWT、Spring Security 和 Redis，仍不要提前引入 RAG、Tool Calling 或 Docker。

### 4.5 阶段验收标准

```text
1. 所有 Java 文件 package 与目录一致
2. 类名符合 Java 命名规范
3. Main.java 不再堆积大量注释实验代码
4. 能通过控制台完成用户、文档、聊天消息的完整演示
5. 非法输入能抛出或展示 BusinessException
6. 能说清楚 ArrayList、HashMap、HashSet 在项目中的用途
7. 能说清楚抽象类和接口在项目中的区别
```

### 4.6 面试表达

```text
阶段 1 完成时，我做的是一个 Java 控制台版知识库和聊天消息管理项目。它使用 User、Document、ChatMessage 表示业务实体，用 BaseEntity 抽取公共字段，用 Searchable 接口抽象可搜索能力，用 Service 和 Repository 分层组织业务逻辑和内存存储。这个版本使用 Java 集合保存数据，目的是训练面向对象、集合、异常和分层思想。随后我在阶段 2 已经把输入输出迁移到 HTTP 接口，把内存集合存储迁移到 MySQL/MyBatis，并保留了 Service 分层和 BusinessException 等设计思想。
```

---

## 5. 阶段 2：Spring Boot + MySQL + MyBatis REST API（已完成）

### 5.1 完成结论

阶段 2 已于 2026-08-15 复核完成。项目已经从阶段 1 的控制台/内存实现升级为 Spring Boot REST API + MySQL/MyBatis 后端，满足进入阶段 3 的前置条件。

```text
状态：已完成
完成范围：用户、知识库、文档、手工文本切片、聊天会话、聊天消息
工程能力：统一响应、参数校验、全局异常、错误码、事务、分页动态 SQL、安全日志、OpenAPI、README、自动化测试
阶段边界：登录只完成密码校验；JWT、Spring Security、Redis 和真实用户隔离从阶段 3 开始
```

### 5.2 验收摘要

| 验收维度 | 结论 |
|---|---|
| Maven/Spring Boot 结构与三层分层 | 完成 |
| 六张核心表与 Mapper/XML 主链路 | 完成 |
| DTO/VO/Entity、Result/PageResult、异常处理 | 完成 |
| 密码摘要与账号密码校验 | 完成 |
| 分页、关键词条件、动态 SQL、稳定排序 | 完成 |
| 唯一索引、事务、幂等和 Document/chunk 级联 | 完成 |
| 环境变量、安全日志、OpenAPI、README | 完成 |
| 2026-08-15 普通回归测试 | 85 项，0 失败，0 错误；5 项外部数据库条件测试因无凭据跳过 |

阶段 2 的具体接口、数据库初始化方法和运行命令以项目根目录 `README.md` 为准，本路线文档不再重复保存 Day 1～Day 5 的过程细节。

### 5.3 进入阶段 3 时复用的基线

```text
User.passwordHash + PasswordUtils：继续兼容现有 PBKDF2 数据，阶段 3 通过 PasswordEncoder 适配器接入认证链。
User.role / status：作为 ROLE_USER、ROLE_ADMIN 和禁用账号判断的数据来源，不新增复杂 RBAC 表。
GlobalExceptionHandler + Result：业务异常继续复用；Security Filter 层另补 401/403 JSON 处理器。
knowledge_base.user_id、document.user_id、chat_session.user_id：阶段 3 改造成 Principal 资源隔离条件。
ErrorCode：阶段 3 增加 token、权限、Redis、限流相关错误码。
OpenAPI：阶段 3 增加 Bearer Security Scheme，并更新公开/受保护接口说明。
```

### 5.4 不再重复的工作

除非阶段 3 改造导致回归失败，否则不再新增阶段 2 的普通 CRUD，不重写 Mapper 基础教学，不把用户更新/删除等非阻塞 backlog 插到 JWT + Redis 主线之前。

阶段 2 面试总表达：

```text
我把 Java 控制台内存项目升级为 Spring Boot REST 后端，使用 Controller、Service、MyBatis Mapper/XML 和 MySQL 组织请求链路；DTO/VO/Entity 分离，Result 和全局异常处理统一接口契约，事务与数据库约束保证一致性，并通过 Mockito、MockMvc、真实数据库集成测试和 OpenAPI 验证业务、HTTP、SQL 与接口文档。
```

---

## 6. 阶段 3：登录权限 + Redis（已完成）

> 完成日期：2026-08-19；2026-08-20 再次复验通过。

阶段 3 已完成 Spring Security 无状态 JWT、统一 401/403、USER/ADMIN、JWT `sub` 到 `currentUserId`、知识库/文档/Chunk/会话/消息 owner 隔离、Redis `jti` 退出黑名单、剩余 TTL、Lua 原子限流及 429/503 故障策略。

最终证据：

```text
Tests run: 114
Failures: 0
Errors: 0
Skipped: 0
MySQL：真实 Mapper、Schema、owner 隔离和 OpenAPI 上下文通过
Redis：redis:7 PING、blacklist value/TTL、Lua count/TTL 通过
HTTP：完整等价剧本 38/38 通过
Postman：逐项人工演示由用户明确豁免，不构成阻塞
```

后续阶段只保留四条不可回退的基线：

1. owner 只来自已验证 JWT Principal，上传和处理请求不得接收可信 `userId`。
2. 所有知识库、文档及子资源在文件 IO 或数据库写入前完成 owner 校验。
3. 密码、数据库口令、Redis 口令、JWT secret 和完整 token 不进入仓库或日志。
4. 阶段 4 每次改造后继续保证阶段 3 全量回归为 0 failure、0 error。

详细实现、测试矩阵和复现证据统一查 `agent-backend/docs/stage3-day2-implementation-report.md`；本路线文档不再重复 DAY 1/DAY 2 教学细节。

---

## 7. 阶段 4：知识库文档处理（当前阶段）

> 当前精确位置：DAY 1 已完成，DAY 2 开始前。
> 两天总目标：把现有“手工创建 Document/Chunk”升级成安全、可重试、可测试的真实文档摄取流水线。

### 7.1 当前项目起点与阶段目标

当前可复用基线已经由代码确认：

- `DocumentController` 已有手工创建、分页、详情、删除和 chunk 查询；
- `DocumentService` 已有 `currentUserId` 参数与 owner 校验；
- `DocumentMapper` 已有按用户分页和文档删除；
- `DocumentChunkMapper` 已有单条插入与按文档有序查询；
- 数据库已有 `document`、`document_chunk`、唯一索引和 chunk 外键级联；
- Spring Security/JWT/Redis 和统一错误响应已经通过阶段 3 验收；
- DAY 1 已新增 SQL 005、文档文件元数据、parse/chunk 独立状态和 Schema 验证；
- DAY 1 已新增配置化 Multipart、本地安全存储、UUID 相对路径、SHA-256 和失败补偿；
- DAY 1 已新增上传、parse、processing-status API，以及 TXT/Markdown Parser、Registry 与 TextCleaner；
- DAY 1 已用单元、MockMvc、真实 MySQL/Redis/文件系统和 137 项全量回归完成验收。

进入 DAY 2 前的剩余缺口已经确认：

```text
没有 PDF 解析依赖与 PdfParser
没有自动 ChunkStrategy
没有 chunk batchInsert / replace
没有 process 总编排、chunk 失败恢复与 DocumentProcessLog
现有文档删除不会清理物理文件
```

阶段结束时必须具备：

```text
1. 已认证用户向自己的知识库上传 TXT、Markdown、PDF。
2. 文件按服务端生成的安全路径保存，原文件名只作为元数据。
3. 文档文本可解析、清洗并保存到 document.content。
4. 清洗后的文本按固定长度和 overlap 自动切片。
5. chunk 按 0..n-1 稳定排序批量保存到 document_chunk。
6. 文档可查询 parseStatus、chunkStatus 和安全的失败摘要。
7. 失败文档可重试，重试不会留下重复或半套 chunk。
8. 删除文档时数据库和物理文件都有明确清理策略。
9. owner、非法文件、解析失败、事务失败均有自动化证据。
10. 阶段 3 原始 114 项基线与当前阶段 4 测试共同组成的 137 项全量回归继续全部通过。
```

### 7.2 两天范围与完成物

| DAY | 唯一主目标 | 必须完成 | 明确推迟 |
|---|---|---|---|
| DAY 1 | 安全上传并得到可追踪文档 | 迁移、配置、存储、上传 API、TXT/Markdown 解析、状态查询、测试 | PDF、自动切片、批量替换 |
| DAY 2 | 完整摄取并形成 chunk | PDF、清洗、固定长度切片、batch insert、失败/重试、删除清理、总验收 | OCR、对象存储、异步队列、Embedding |
| 阶段出口 | 可供阶段 5 向量化 | 文档与 chunk 数据正确、可溯源、可按 owner 查询 | Qdrant、RAG 回答 |

P0 是 TXT/Markdown + 固定长度切片的完整闭环；PDF 属于本阶段目标，安排在 DAY 2。Markdown 标题切片属于 P1 增强，只有固定长度策略和全部测试已经通过后再做，不得反向阻塞两天主线。

### 7.3 推荐调用链与状态机

上传链路：

```text
Bearer JWT
-> Security Filter / blacklist 校验
-> DocumentUploadController
-> CurrentUser.requireUserId
-> KnowledgeBase owner 校验
-> 文件大小/扩展名/内容特征校验
-> FileStorageService 生成安全路径并保存
-> DocumentMapper 写元数据、content=""、parse=PENDING、chunk=PENDING
-> 返回 documentId 和处理状态
```

解析链路：

```text
POST /api/documents/{id}/parse
-> 校验 Document owner
-> 条件更新 parseStatus=PROCESSING，拒绝重复解析
-> 从 storage root 安全解析相对路径
-> ParserRegistry 选择 TXT / Markdown / PDF Parser
-> TextCleaner
-> 更新 document.content、parseStatus=SUCCESS
```

切片处理链路：

```text
POST /api/documents/{id}/process
-> 校验 Document owner
-> parseStatus 非 SUCCESS 时先复用解析链路
-> 条件更新 chunkStatus=PROCESSING
-> FixedLengthChunkStrategy
-> 事务内删除旧 chunk + batch insert 新 chunk
-> 更新 chunkStatus=SUCCESS、processedAt
-> 写成功日志并返回状态
```

失败链路：

```text
存储失败：不插入 Document，清理临时/残留文件
Document 插入失败：补偿删除刚保存的文件
解析失败：parse=FAILED，chunk 保持 PENDING，不写 chunk，保留原文件供重试
切片/入库失败：parse=SUCCESS，chunk=FAILED，旧 chunk 不被半替换
重复处理：条件状态更新失败，返回 409
删除：先校验 owner，事务删除数据库；提交后清理物理文件，失败只写安全 WARN
```

推荐状态值：

| 状态 | 含义 |
|---|---|
| `NOT_APPLICABLE` | 阶段 2 手工文档，不走文件摄取 |
| `PENDING` | 文件已上传，等待处理 |
| `PROCESSING` | 当前正在执行，防止重复处理 |
| `SUCCESS` | 当前步骤完成 |
| `FAILED` | 当前步骤失败，可根据错误类型重试 |

### 7.4 最终目录结构增量

以下是结合当前 `com.ljl.agent` 结构的阶段最终目标增量。带上传、TXT/Markdown、Storage、Parser、Cleaner 和 DAY 1 测试的文件已经存在；PDF、ChunkStrategy、ProcessLog 与 process 编排仍是 DAY 2 待办：

```text
agent-backend/.gitignore
agent-backend/storage/uploads/
agent-backend/sql/005_stage4_document_ingestion.sql

src/main/java/com/ljl/agent/config/DocumentIngestionProperties.java
src/main/java/com/ljl/agent/controller/DocumentUploadController.java
src/main/java/com/ljl/agent/dto/request/DocumentParseRequest.java
src/main/java/com/ljl/agent/dto/request/DocumentProcessRequest.java
src/main/java/com/ljl/agent/dto/request/DocumentUploadRequest.java
src/main/java/com/ljl/agent/dto/response/DocumentProcessStatusVO.java
src/main/java/com/ljl/agent/dto/response/DocumentUploadResponse.java
src/main/java/com/ljl/agent/entity/DocumentProcessLog.java

src/main/java/com/ljl/agent/ingestion/DocumentIngestionService.java
src/main/java/com/ljl/agent/ingestion/DefaultDocumentIngestionService.java
src/main/java/com/ljl/agent/ingestion/clean/TextCleaner.java
src/main/java/com/ljl/agent/ingestion/clean/DefaultTextCleaner.java
src/main/java/com/ljl/agent/ingestion/chunk/ChunkResult.java
src/main/java/com/ljl/agent/ingestion/chunk/ChunkStrategy.java
src/main/java/com/ljl/agent/ingestion/chunk/FixedLengthChunkStrategy.java
src/main/java/com/ljl/agent/ingestion/chunk/MarkdownHeadingChunkStrategy.java  # P1，可延后
src/main/java/com/ljl/agent/ingestion/parser/DocumentParser.java
src/main/java/com/ljl/agent/ingestion/parser/ParserRegistry.java
src/main/java/com/ljl/agent/ingestion/parser/TxtParser.java
src/main/java/com/ljl/agent/ingestion/parser/MarkdownParser.java
src/main/java/com/ljl/agent/ingestion/parser/PdfParser.java
src/main/java/com/ljl/agent/ingestion/storage/StoredFile.java
src/main/java/com/ljl/agent/ingestion/storage/FileStorageService.java
src/main/java/com/ljl/agent/ingestion/storage/LocalFileStorageService.java

src/main/java/com/ljl/agent/mapper/DocumentProcessLogMapper.java
src/main/resources/mapper/DocumentProcessLogMapper.xml

src/test/java/com/ljl/agent/controller/DocumentUploadControllerTest.java
src/test/java/com/ljl/agent/ingestion/DefaultDocumentIngestionServiceTest.java
src/test/java/com/ljl/agent/ingestion/clean/DefaultTextCleanerTest.java
src/test/java/com/ljl/agent/ingestion/chunk/FixedLengthChunkStrategyTest.java
src/test/java/com/ljl/agent/ingestion/parser/DocumentParserTest.java
src/test/java/com/ljl/agent/ingestion/storage/LocalFileStorageServiceTest.java
src/test/java/com/ljl/agent/DocumentIngestionIntegrationTest.java
```

继续复用并修改：

```text
Document.java / DocumentVO.java
DocumentMapper.java / DocumentMapper.xml
DocumentChunk.java / DocumentChunkVO.java
DocumentChunkMapper.java / DocumentChunkMapper.xml
DocumentService.java / DocumentServiceImpl.java
DocumentController.java
ErrorCode.java / GlobalExceptionHandler.java
application.yml / application-dev.yml
pom.xml / README.md / OpenAPI tests
```

不新增独立 `DocumentChunkController` 或 `DocumentChunkService`。自动 chunk 的生命周期属于文档处理，由 `DocumentIngestionService` 统一编排。

### 7.5 数据迁移与状态设计

DAY 1 新建 `sql/005_stage4_document_ingestion.sql`，只能做向前兼容增量，不能重写 `001_init_schema.sql` 破坏已有环境。

`document` 建议增加：

| 字段 | 建议类型/约束 | 用途 |
|---|---|---|
| `original_file_name` | `VARCHAR(255) NULL` | 展示用户上传名称，不参与路径拼接 |
| `stored_file_name` | `VARCHAR(255) NULL` | 服务端 UUID 文件名 |
| `file_type` | `VARCHAR(32) NULL` | TXT、MARKDOWN、PDF |
| `file_size` | `BIGINT NULL` | 大小校验和审计 |
| `file_path` | `VARCHAR(500) NULL` | 相对 storage root 的路径 |
| `file_checksum` | `CHAR(64) NULL` | SHA-256，可用于排查和后续去重 |
| `parse_status` | `VARCHAR(32) NOT NULL` | NOT_APPLICABLE/PENDING/PROCESSING/SUCCESS/FAILED |
| `chunk_status` | `VARCHAR(32) NOT NULL` | 同上 |
| `process_error` | `VARCHAR(500) NULL` | 可公开、截断、无堆栈和绝对路径的摘要 |
| `processed_at` | `DATETIME NULL` | 最近成功处理时间 |

保留现有 `content LONGTEXT NOT NULL`。上传创建元数据时先写空字符串，解析成功后再更新正文，避免第一天同时修改列空值语义。阶段 2 手工创建的历史文档迁移为 `parse_status=NOT_APPLICABLE`、`chunk_status=NOT_APPLICABLE`。

`document_chunk` 建议增加：

| 字段 | 处理 |
|---|---|
| `user_id` | DAY 1/2 可直接补齐，便于阶段 5 向量 metadata 按 owner 过滤 |
| `token_count` | 可为空；阶段 4 不伪造精确 tokenizer 结果 |
| `metadata` | 继续使用，保存 sourceFile、startOffset、endOffset、page/heading 等可用信息 |

`user_id` 迁移不能直接对已有表强加 NOT NULL：先增加可空列，使用 `document_chunk.document_id -> document.user_id` 回填，核对无 NULL 后再改为 NOT NULL 并加 owner 索引。任何回填数量不一致都必须停止迁移。

`document_process_log` 最小字段：

```text
id
document_id
user_id
stage          # UPLOAD / PARSE / CLEAN / CHUNK / PERSIST
status         # STARTED / SUCCESS / FAILED
message        # 安全摘要，不存正文、堆栈和绝对路径
started_at
finished_at
created_at
```

迁移必须验证：

1. 旧文档、旧 chunk 数量不变；
2. 新字段、默认值、索引和约束存在；
3. 手工文档原 API 仍可创建和查询；
4. 新上传文档可以写 `PENDING`；
5. rollback 或失败不会留下半条 process log；
6. `SchemaIntegrationTest` 同步更新并真实连接 MySQL。

### 7.6 API 契约

#### 7.6.1 上传

```http
POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=<required>
title=<optional；空时使用去扩展名后的原文件名>
```

规则：

- `knowledgeBaseId` 必须为正整数且属于当前用户；
- 文件不能为空，默认最大 10 MB，实际值由配置决定；
- DAY 1 允许 `.txt`、`.md`、`.markdown`，DAY 2 加入 `.pdf`；
- 同时校验扩展名、声明类型和基础内容特征，不能只信任 `Content-Type`；
- 存储名使用 UUID，响应不返回绝对路径；
- 同一知识库标题冲突沿用 `DOCUMENT_DUPLICATE`；
- 成功返回 `documentId`、原文件名、文件类型、大小、`parseStatus`、`chunkStatus`。

#### 7.6.2 解析或解析重试

```http
POST /api/documents/{documentId}/parse
Authorization: Bearer <token>
Content-Type: application/json

{
  "force": false
}
```

规则：

- 只有 owner 可以解析；
- `PROCESSING` 状态禁止重复提交；
- 成功保存清洗后的 `document.content`，并设置 `parseStatus=SUCCESS`；
- 失败设置 `parseStatus=FAILED`、保留原文件并返回安全错误摘要；
- `force=true` 只允许 owner 对 SUCCESS/FAILED 文档重新解析。

#### 7.6.3 切片处理或重试

```http
POST /api/documents/{documentId}/process
Authorization: Bearer <token>
Content-Type: application/json

{
  "chunkSize": 800,
  "overlap": 100,
  "strategy": "FIXED"
}
```

规则：

- 只有 owner 可以处理；
- `chunkSize` 建议 100～4000，`overlap` 必须大于等于 0 且小于 `chunkSize`；
- 首次处理和失败重试共用一个接口；
- `PROCESSING` 状态禁止重复提交，返回 409；
- `parseStatus` 非 SUCCESS 时先调用同一解析服务，不复制 parser 逻辑；
- 成功后 `parseStatus=SUCCESS`、`chunkStatus=SUCCESS`；
- 解析失败与入库失败使用不同错误码和状态；
- 同步接口返回最终状态和 chunkCount，不引入异步轮询假象。

#### 7.6.4 状态与现有查询

```http
GET /api/documents/{documentId}/processing-status
GET /api/documents/{documentId}
GET /api/documents/{documentId}/chunks
DELETE /api/documents/{documentId}
```

`processing-status` 返回：

```json
{
  "documentId": 1,
  "parseStatus": "SUCCESS",
  "chunkStatus": "SUCCESS",
  "chunkCount": 12,
  "errorMessage": null,
  "processedAt": "2026-08-20T12:00:00"
}
```

不返回 `filePath` 绝对值、堆栈、正文或底层异常。现有 `DELETE` 在 DAY 2 扩展物理文件清理，不另造 `/delete-file`。

#### 7.6.5 错误契约

| 场景 | 建议 ErrorCode | HTTP |
|---|---|---:|
| 空文件、非法文件名或参数 | `DOCUMENT_FILE_INVALID` | 400 |
| 不支持的类型/内容特征不符 | `DOCUMENT_FILE_TYPE_UNSUPPORTED` | 400 |
| 文件超过上限 | `DOCUMENT_FILE_TOO_LARGE` | 413 |
| 文档正在处理 | `DOCUMENT_PROCESSING_CONFLICT` | 409 |
| 文件可读但无法解析 | `DOCUMENT_PARSE_FAILED` | 422 |
| 存储或数据库摄取失败 | `DOCUMENT_INGESTION_FAILED` | 500 |

实际数值编码在 DAY 1 修改 `ErrorCode` 时分配，必须保留现有错误码数值不变。Spring Multipart 的超限异常也要经过 `GlobalExceptionHandler` 转为统一 `Result`，不能返回默认 HTML。

### 7.7 文件存储与安全规则

配置建议：

```yaml
app:
  document:
    storage-root: ${DOCUMENT_STORAGE_ROOT:./storage/uploads}
    max-file-size: ${DOCUMENT_MAX_FILE_SIZE:10MB}
    allowed-types: TXT,MARKDOWN,PDF

spring:
  servlet:
    multipart:
      max-file-size: ${DOCUMENT_MAX_FILE_SIZE:10MB}
      max-request-size: ${DOCUMENT_MAX_REQUEST_SIZE:11MB}
```

实现规则：

1. 启动时把 storage root 转成绝对、规范化路径并创建目录；创建失败应快速失败。
2. 最终路径使用 `{userId}/{knowledgeBaseId}/{uuid}.{ext}`，数据库只保存相对路径。
3. 对任何 `resolve` 结果再次 `normalize`，并断言 `startsWith(storageRoot)`。
4. 原文件名经过 `Paths.get(name).getFileName()` 和长度限制后仅用于展示。
5. 使用临时文件写入，成功后原子移动到最终路径；不把半写文件当成成功。
6. 空文件、目录名、`..`、绝对路径、双扩展名和 NUL 字符必须拒绝。
7. PDF 至少检查 `%PDF-` 头；文本文件拒绝明显二进制内容和 NUL。
8. 日志只记 documentId、userId、类型、大小和阶段，不记正文、完整磁盘路径或 token。
9. `storage/uploads/**` 加入 `.gitignore`，测试使用临时目录，不能污染真实存储。
10. 阶段 4 不提供任意路径下载接口；后续若下载，只能按 documentId + owner 解析受控路径。

### 7.8 解析、清洗与切片规则

Parser 接口建议：

```java
interface DocumentParser {
    boolean supports(String fileType);
    ParsedDocument parse(Path path);
}
```

格式约束：

- TXT：UTF-8，支持去除 UTF-8 BOM；无法按约定解码时明确失败；
- Markdown：保留标题、列表、代码块的换行语义，不在 parser 中切片；
- PDF：提取文本和页码；加密 PDF、纯扫描件或提取为空时返回明确错误，本阶段不做 OCR；
- parser 只负责读取和解析，不写数据库、不决定 owner、不生成 HTTP 响应。

清洗顺序：

```text
CRLF/CR -> LF
移除 BOM 和 NUL
删除每行无意义尾空格
连续空行压缩到最多 2 个
整体 trim
空文本检查
```

禁止为了“看起来干净”删除 Markdown 标题符号、列表结构或所有换行。

固定长度切片不按 token 冒充精确结果，阶段 4 按字符实现：

```text
start = 0
while start < text.length:
    end = min(start + chunkSize, text.length)
    生成非空 chunk(start, end)
    下一 start = end - overlap
    当 end == text.length 时结束
```

必须断言：

- `chunkSize > 0`；
- `0 <= overlap < chunkSize`；
- 空文本不生成 chunk；
- `chunkIndex` 从 0 连续递增；
- 不出现死循环、空 chunk 或完全重复尾块；
- 结果按索引稳定；
- metadata 至少包含字符起止位置和来源文件名；
- PDF 可额外记录页码，Markdown 可额外记录标题。

### 7.9 事务、一致性与并发边界

文件系统与 MySQL 不能共享本地事务，因此分段处理：

```text
上传：
校验 -> 写临时文件 -> 移动最终文件 -> 事务插入 Document
如果插入失败 -> 补偿删除最终文件

处理：
短事务把状态从 PENDING/FAILED 条件更新为 PROCESSING
事务外执行文件读取、解析、清洗、切片
短事务执行 delete old chunks + batch insert + update content/status
任何异常 -> 独立短事务写 FAILED 和安全错误摘要

删除：
查询并校验 owner，取得受控相对路径
事务删除 Document，数据库级联 chunk/process log
事务提交后删除物理文件
文件删除失败 -> 写不含绝对路径的应用 WARN，不回滚已提交的数据库事务
```

不能把耗时 PDF 解析包在长数据库事务中。重处理替换 chunk 时，`deleteByDocumentId`、`batchInsert` 和成功状态更新必须同一事务；失败时旧 chunk 要么完整保留，要么按明确策略完整回滚，不能出现只删未插的半状态。

用条件更新或同等机制防止两个请求同时处理同一文档。两天阶段不引入分布式锁；单实例下数据库状态机足够，面试时要说明这个边界。

### 7.10 DAY 1：安全上传、存储、状态与 TXT/Markdown

> 完成状态：已于 2026-08-21 实现，并于 2026-09-01 按本节出口重新复验通过。详细证据见 `agent-backend/docs/stage4-day1-implementation-report.md` 与第 2.6 节快照。

#### 7.10.1 DAY 1 学习目标

学懂并实现：

```text
Multipart 请求与 Spring Boot 大小限制
原文件名与存储文件名分离
Path normalize / startsWith 防目录穿越
文件系统与数据库的补偿式一致性
Parser Strategy
处理状态为何必须显式建模
```

#### 7.10.2 DAY 1 实施顺序

1. 运行阶段 3 基线，记录测试总数和 Redis PING；确认当前 diff 后先创建阶段 3 Git 快照，避免与阶段 4 改动混在同一提交。
2. 画出现有 `DocumentController -> DocumentService -> Mapper` 以及 `currentUserId` 流动。
3. 设计并执行 `005_stage4_document_ingestion.sql`，先更新实体、ResultMap、Schema 测试。
4. 增加 PDF 解析依赖的计划项，但 DAY 1 先只实现 TXT/Markdown；不要一次写完所有 parser。
5. 增加 `DocumentIngestionProperties` 与 Spring Multipart 限制，配置全部支持环境变量覆盖。
6. 实现 `StoredFile`、`FileStorageService`、`LocalFileStorageService`，使用测试临时目录。
7. 实现上传 DTO/Response 和 `DocumentUploadController`，复用 `CurrentUser` 与知识库 owner 校验。
8. Mapper 增加“文件文档插入、状态查询/更新”能力；上传成功写 `PENDING`。
9. 实现 `DocumentParser`、`ParserRegistry`、`TxtParser`、`MarkdownParser` 和基础 `TextCleaner`。
10. 调用 parse API，把 TXT/Markdown 正文写入 `document.content`，`parseStatus=SUCCESS`、`chunkStatus=PENDING`。
11. 增加 processing-status API，错误信息安全截断。
12. 完成单元、MockMvc、真实 MySQL 和全量回归，更新 README/OpenAPI 和 DAY 1 快照。

#### 7.10.3 DAY 1 必测用例

```text
成功上传 UTF-8 TXT
成功上传 Markdown，标题与换行保留
知识库不存在 -> 404
userB 上传到 userA KB -> 403，磁盘和数据库均无新增
空文件 -> 400
超限文件 -> 413 或统一项目错误映射
.exe 改名 .txt / MIME 不符 -> 400
文件名包含 ../ 或绝对路径 -> 被安全化或拒绝，最终路径仍在 storage root
存储失败 -> Document 不落库
Document 插入失败 -> 已存文件被补偿删除
解析失败 -> parse=FAILED、chunk=PENDING、无 chunk
状态查询越权 -> 403
阶段 3 原始 114 项基线无回归，加入 DAY 1 测试后的 137 项全量回归全部通过
```

#### 7.10.4 DAY 1 出口

```text
[x] SQL 005 在真实 MySQL 成功执行，旧数据不丢。
[x] TXT/Markdown 能经受保护 API 上传并安全保存。
[x] currentUserId 与 KB owner 校验先于文件写入。
[x] 原文件名不参与真实路径，响应不泄露绝对路径。
[x] document 元数据、content、parse/chunk 状态正确。
[x] 状态 API 可查询成功与失败。
[x] 单元、Controller、Mapper/集成和全量测试 0 failure、0 error。
[x] README/OpenAPI 与配置示例同步。
```

以上已全部勾选，可以进入 DAY 2。DAY 1 的 `parseStatus=SUCCESS`、`chunkStatus=PENDING` 是正确边界；chunk 生成与 `chunkStatus=SUCCESS` 属于 DAY 2。

### 7.11 DAY 2：PDF、清洗、自动切片、批量入库和总验收

#### 7.11.1 DAY 2 学习目标

```text
PDF 文本提取边界
清洗与语义保留的取舍
固定长度 + overlap 切片算法
MyBatis batch insert
重处理的原子替换
长任务状态机与失败恢复
数据库事务和文件补偿的边界
```

#### 7.11.2 DAY 2 实施顺序

1. 复跑 DAY 1 测试，确认上传目录和临时测试文件已清理。
2. 增加并验证 PDF 解析依赖，实现 `PdfParser`；明确不支持加密 PDF 与扫描件 OCR。
3. 完成 `DefaultTextCleaner`，用 TXT/Markdown/PDF 样例验证不会破坏必要结构。
4. 实现 `ChunkResult`、`ChunkStrategy`、`FixedLengthChunkStrategy`，先做纯单元测试。
5. `DocumentChunkMapper` 增加 `deleteByDocumentId` 与 `batchInsert`；更新 XML 和真实 MySQL 测试。
6. 实现 `DocumentIngestionService.process`：owner -> PROCESSING -> parse -> clean -> chunk -> 事务替换 -> SUCCESS。
7. 增加 `DocumentProcessLog` 和 Mapper，记录阶段与安全摘要。
8. 处理 FAILED 重试、PROCESSING 重复提交、空文本、非法 chunk 参数。
9. 扩展现有文档 DELETE：数据库提交后清理受控物理文件，并测试清理失败日志。
10. 完成 TXT、Markdown、文本型 PDF 的端到端摄取；查询 chunk 顺序、数量、正文和 metadata。
11. 运行真实 MySQL/Redis 全量回归；Postman 可选，不作为阶段阻塞，优先使用 MockMvc/集成测试和可复现 HTTP 脚本。
12. 更新 README、OpenAPI、错误码、阶段快照和面试表达。

#### 7.11.3 DAY 2 必测用例

```text
TXT/Markdown/PDF 完整处理成功
chunkIndex 从 0 连续递增
最后一块不为空，overlap 正确
chunkSize=0、overlap<0、overlap>=chunkSize -> 400
空白文档 -> parse/chunk 明确失败
加密 PDF / 扫描 PDF / 损坏 PDF -> parse=FAILED，无半套 chunk
同一文档重复 process -> 状态机拒绝并发
FAILED 文档重试成功
已有 chunk 重处理 -> 原子替换，无重复索引
batch insert 中途失败 -> 事务回滚，状态为 FAILED
userB process/status/chunks/delete userA 文档 -> 403，文件与数据不变
删除文档 -> Document、Chunk、日志按设计删除，物理文件清理
日志/响应不含正文、绝对路径、token 或密钥
阶段 3 安全、Redis、MySQL 回归继续通过
```

#### 7.11.4 DAY 2 出口

```text
[ ] PDF 文本解析成功，失败边界明确。
[ ] 清洗结果可解释且不破坏 Markdown 基本结构。
[ ] 固定长度 + overlap 切片算法通过边界测试。
[ ] batchInsert 与原子替换在真实 MySQL 通过。
[ ] process 状态机、失败状态、重试和处理日志可验证。
[ ] 文档删除包含物理文件清理策略。
[ ] owner 越权前后数据库与文件系统均不变。
[ ] 全量测试 0 failure、0 error、无未解释 skipped。
[ ] README/OpenAPI/配置/错误码/快照同步。
```

### 7.12 自动化测试矩阵

| 层级 | 核心测试 | 不可替代的证据 |
|---|---|---|
| Storage 单元 | 安全路径、UUID 名、临时文件、补偿删除、越界路径 | 测试临时目录内真实文件 |
| Parser 单元 | UTF-8/BOM、Markdown、PDF、空/损坏/加密 | 解析正文和异常类型 |
| Cleaner 单元 | 换行、NUL、空行、标题/列表保留 | 输入输出精确断言 |
| Chunk 单元 | 长短文本、overlap、边界、连续索引 | 每个 chunk 内容与 offset |
| Service 单元 | owner 在 IO 前、状态流转、失败补偿、重试 | Mapper/Storage 调用顺序与零交互 |
| Controller/MockMvc | multipart、JWT、400/403/404/409/413/500 | HTTP 状态与统一 Result |
| Mapper/MySQL | 迁移字段、状态更新、batch、原子替换 | 真实 MySQL 行数与约束 |
| 端到端 | 上传 -> process -> status -> chunks -> delete | DB 与磁盘同时核对 |
| 全量回归 | 阶段 1～3 所有测试 | 0 failure、0 error |

集成测试文件必须小而确定，放测试资源或运行时临时生成；测试结束精确删除自己创建的 Document、Chunk、日志和文件，不清理整个共享目录或业务表。

### 7.13 阶段 4 最终验收标准

以下全部满足才能进入阶段 5：

```text
1. 阶段 3 的 JWT、currentUserId、owner、logout 和限流没有回归。
2. TXT、Markdown、文本型 PDF 均可上传。
3. 存储路径由服务端生成，目录穿越和伪类型被拒绝。
4. 文件元数据、正文和处理状态正确落库。
5. Parser/Registry 可以按类型扩展，没有把解析逻辑写进 Controller。
6. 文本清洗结果稳定，并保留必要结构。
7. 固定长度 + overlap 产生连续、非空、稳定 chunk。
8. chunk 使用批量写入，重处理原子替换。
9. 根据 documentId 查询 chunk 与原文对应。
10. parse/chunk 失败有区分、状态和安全错误摘要。
11. FAILED 可重试，PROCESSING 不被并发重复执行。
12. userB 不能上传到、处理、查看或删除 userA 的资源。
13. 删除文档后数据库和文件系统结果符合设计。
14. 日志/响应不泄露正文、绝对路径、密码、secret 或完整 token。
15. README、OpenAPI、SQL、配置示例和错误码一致。
16. 单元、Controller、Mapper、真实 MySQL/Redis 和端到端回归全部通过。
17. 能用 60～90 秒讲清流水线、事务边界、安全和阶段 5 衔接。
```

### 7.14 常见失败与排查顺序

| 症状 | 排查顺序 |
|---|---|
| Multipart 直接 4xx | Spring max-file-size -> Controller 参数名 -> Content-Type -> 全局异常映射 |
| 文件保存但无 Document | 插入异常 -> 补偿删除日志 -> storage 权限 -> 唯一标题 |
| 路径跑出 storage root | 原文件名使用点 -> normalize -> startsWith -> 相对路径入库 |
| TXT 中文乱码 | 字符集约定 -> BOM -> 读取 API -> 样例文件编码 |
| PDF 解析为空 | 是否扫描件/加密 -> PDF 是否损坏 -> parser 异常 -> OCR 不在范围 |
| 一直 PROCESSING | try/catch/finally 状态更新 -> 进程中断 -> 恢复策略 |
| chunk 重复/死循环 | overlap >= chunkSize -> end/start 推进 -> 尾块退出条件 |
| 重试后唯一索引冲突 | 是否事务内先删旧 chunk -> batch 索引是否从 0 重建 |
| userB 能处理 userA 文件 | owner 校验是否早于 storage.read 和 Mapper 写入 |
| 删除后磁盘残留 | after-commit 清理 -> 路径是否受控 -> cleanup log |
| 全量测试跳过 | DB_PASSWORD -> dev profile -> Redis integration flag -> 外部服务状态 |

### 7.15 阶段 4 面试闭环

高频问题：

| 问题 | 项目回答要点 |
|---|---|
| 为什么元数据、原文和 chunk 分开？ | 生命周期与查询粒度不同；chunk 为阶段 5 向量化输入 |
| 如何防目录穿越？ | 不信原文件名，UUID 存储名，normalize 后 startsWith root |
| 文件和数据库如何保证一致？ | 无法共享事务，使用临时文件、短事务、补偿删除和失败状态 |
| 为什么解析不放 Controller？ | 隔离 HTTP 与格式策略，便于单测和扩展 |
| 为什么用 overlap？ | 降低语义在边界处被截断的损失，但会增加存储和检索重复 |
| 为什么不在长事务中解析 PDF？ | 避免长时间占用连接和锁；解析后短事务批量替换 |
| 如何防止重处理半套数据？ | 状态机 + 条件更新 + delete/batch/update 同一事务 |
| 为什么暂不做 OCR/异步？ | 两天目标先跑通可解释的同步文本摄取闭环 |

可直接复述：

```text
我把原来手工创建 Document 和 Chunk 的接口升级成了文档摄取流水线。上传时先从 JWT Principal 获取 currentUserId 并校验知识库归属，再校验文件大小、类型和安全路径，使用 UUID 文件名保存到受控目录，并把文件元数据和处理状态写入 document。处理时通过 Parser Strategy 支持 TXT、Markdown 和文本型 PDF，统一清洗后按固定长度和 overlap 切片。解析在事务外完成，旧 chunk 删除、批量插入和状态更新在短事务中原子提交；失败会写明确状态并允许重试。由于文件系统不参与数据库事务，我使用临时文件和补偿删除处理一致性，同时用 owner 越权、路径穿越、损坏文件和批量回滚测试验证边界。最终 chunk 会作为下一阶段 Embedding 和向量检索的稳定输入。
```

### 7.16 本阶段明确不做

```text
不做 OCR、图片/表格结构恢复、Office 全格式解析。
不做 MinIO、S3、云 OSS 或分布式文件系统。
不做 Kafka/RabbitMQ、异步任务、定时补偿平台。
不做杀毒引擎和复杂内容审核；只做必要类型与安全校验。
不做 Embedding、Qdrant、相似度检索和 RAG 回答。
不做 SSE、Agent、Tool Calling。
不补知识库/聊天/用户的对称 CRUD。
不开放任意磁盘路径下载。
不把精确 tokenCount 建立在不存在的 tokenizer 上。
```

### 7.17 当前进度记录

```text
[x] 阶段 1 完成
[x] 阶段 2 完成
[x] 阶段 3 完成并于 2026-08-20 复验
[x] 阶段 4 DAY 1 完成并于 2026-09-01 复验
[ ] 阶段 4 DAY 2
[ ] 阶段 4 总验收
```

当前下一唯一行动：按第 7.11 节复跑 DAY 1 基线并检查测试临时目录清理，然后进入 PdfParser 失败边界与 FixedLengthChunkStrategy 单元测试；不提前进入 Embedding、Qdrant 或 RAG。

---

## 8. 阶段 5：RAG 检索问答

### 8.1 阶段目标

让系统具备 RAG 核心能力：文档向量化、问题检索、prompt 增强、模型回答。

结束时应该具备：

```text
1. 调用 embedding 模型
2. 保存 chunk 向量
3. 接入 Qdrant 向量数据库
4. 用户问题向量化
5. topK 相似度检索
6. 构造 RAG prompt
7. 调用大模型生成回答
8. 返回引用来源
```

### 8.2 本阶段最终目录结构增量

```text
agent-backend/
`-- src/
    `-- main/
        |-- java/
        |   `-- com/ljl/agent/
        |       |-- rag/
        |       |   |-- answer/
        |       |   |   |-- RagAnswer.java
        |       |   |   `-- RagAnswerService.java
        |       |   |-- embedding/
        |       |   |   |-- EmbeddingClient.java
        |       |   |   |-- EmbeddingRequest.java
        |       |   |   |-- EmbeddingResponse.java
        |       |   |   `-- OpenAiEmbeddingClient.java
        |       |   |-- llm/
        |       |   |   |-- LlmClient.java
        |       |   |   |-- LlmRequest.java
        |       |   |   |-- LlmResponse.java
        |       |   |   `-- OpenAiLlmClient.java
        |       |   |-- prompt/
        |       |   |   |-- PromptBuilder.java
        |       |   |   |-- PromptContext.java
        |       |   |   `-- RagPromptBuilder.java
        |       |   |-- retrieval/
        |       |   |   |-- RetrievalRequest.java
        |       |   |   |-- RetrievalResult.java
        |       |   |   |-- RetrievalService.java
        |       |   |   `-- VectorRetrievalService.java
        |       |   `-- vector/
        |       |       |-- VectorDocument.java
        |       |       |-- VectorStoreService.java
        |       |       `-- QdrantVectorStoreService.java
        |       |-- controller/
        |       |   `-- RagController.java
        |       `-- dto/
        |           |-- request/
        |           |   `-- RagAskRequest.java
        |           `-- response/
        |               |-- CitationVO.java
        |               `-- RagAskResponse.java
        `-- resources/
            |-- prompts/
            |   |-- rag-system.md
            |   `-- rag-user-template.md
            `-- application-dev.yml
```

### 8.3 推荐 RAG 流程

```text
用户问题
-> 校验用户身份
-> 查询用户可访问的知识库
-> 问题 embedding
-> Qdrant topK 检索
-> metadata 过滤 userId / knowledgeBaseId
-> 组装引用来源
-> 构造 prompt
-> 调用大模型
-> 返回回答和引用
```

### 8.4 阶段验收标准

```text
1. Qdrant 向量数据库能跑通
2. 文档 chunk 能生成 embedding
3. 用户提问时能检索到相关 chunk
4. 回答中能返回引用来源
5. 没有检索结果时明确回答知识库中未找到依据
6. 能解释 topK、相似度阈值、metadata 过滤
```

### 8.5 面试表达

```text
我的 RAG 流程是文档解析、文本切片、embedding、向量入库、问题向量化、相似度检索、prompt 增强和大模型回答。检索结果会带上 documentId、chunkId、标题等 metadata，用于权限过滤和引用溯源。对于没有检索依据的问题，系统会明确提示不确定，减少模型幻觉。
```

---

## 9. 阶段 6：Agent 对话 + SSE

### 9.1 阶段目标

把单次 RAG 问答升级为真实 AI 产品的对话体验。

结束时应该具备：

```text
1. 创建会话
2. 保存用户问题和 Agent 回答
3. 查询历史消息
4. SSE 流式输出
5. 回答带引用来源
6. 多轮上下文窗口控制
```

### 9.2 本阶段最终目录结构增量

```text
agent-backend/
`-- src/
    `-- main/
        |-- java/
        |   `-- com/ljl/agent/
        |       |-- agent/
        |       |   |-- chat/
        |       |   |   |-- AgentChatRequest.java
        |       |   |   |-- AgentChatResponse.java
        |       |   |   |-- AgentChatService.java
        |       |   |   `-- DefaultAgentChatService.java
        |       |   |-- memory/
        |       |   |   |-- ConversationMemoryService.java
        |       |   |   |-- MemoryWindow.java
        |       |   |   `-- MysqlConversationMemoryService.java
        |       |   `-- stream/
        |       |       |-- ChatStreamEvent.java
        |       |       |-- SseEmitterFactory.java
        |       |       `-- StreamChatService.java
        |       |-- controller/
        |       |   `-- ChatController.java
        |       |-- dto/
        |       |   |-- request/
        |       |   |   |-- ChatCreateSessionRequest.java
        |       |   |   |-- ChatHistoryRequest.java
        |       |   |   `-- ChatStreamRequest.java
        |       |   `-- response/
        |       |       |-- ChatMessageVO.java
        |       |       |-- ChatSessionVO.java
        |       |       `-- StreamCitationVO.java
        |       `-- service/
        |           |-- ChatSessionService.java
        |           |-- ChatMessageService.java
        |           `-- impl/
        |               |-- ChatSessionServiceImpl.java
        |               `-- ChatMessageServiceImpl.java
        `-- resources/
            `-- prompts/
                `-- chat-system.md
```

### 9.3 核心接口

```text
POST /api/chat/sessions
GET  /api/chat/sessions?userId={userId}
GET  /api/chat/sessions/{id}/messages
POST /api/chat/stream
```

### 9.4 阶段验收标准

```text
1. 能创建会话
2. 能保存多轮消息
3. 能查询历史消息
4. /api/chat/stream 能用 SSE 返回流式内容
5. 数据库能保存用户问题和 Agent 回答
6. 回答能带引用来源
7. 能说明 SSE 和普通阻塞接口的区别
```

### 9.5 面试表达

```text
我使用 SSE 实现流式输出，后端在接收模型 token 的同时逐步推送给前端。对话数据会保存到 chat_session 和 chat_message 表中，并通过 ConversationMemoryService 控制上下文窗口，避免把无限历史全部塞进 prompt。
```

---

## 10. 阶段 7：Tool Calling + Workflow + Eval

### 10.1 阶段目标

让 Agent 不只是问答，而是能安全地调用后端工具，并具备基础流程编排和评估能力。

结束时应该具备：

```text
1. 工具注册
2. 工具参数 schema
3. 工具权限校验
4. 工具调用日志
5. 最大调用次数限制
6. 简单意图识别
7. RAG / Tool / 兜底流程选择
8. RAG 评估数据记录
```

### 10.2 本阶段最终目录结构增量

```text
agent-backend/
`-- src/
    `-- main/
        |-- java/
        |   `-- com/ljl/agent/
        |       |-- agent/
        |       |   |-- tool/
        |       |   |   |-- ToolCallContext.java
        |       |   |   |-- ToolCallResult.java
        |       |   |   |-- ToolDefinition.java
        |       |   |   |-- ToolExecutor.java
        |       |   |   |-- ToolRegistry.java
        |       |   |   `-- ToolPermissionChecker.java
        |       |   |-- workflow/
        |       |   |   |-- AgentExecutor.java
        |       |   |   |-- AgentPlan.java
        |       |   |   |-- AgentState.java
        |       |   |   |-- IntentClassifier.java
        |       |   |   `-- WorkflowEngine.java
        |       |   `-- guardrail/
        |       |       |-- MaxToolCallGuard.java
        |       |       |-- PromptInjectionGuard.java
        |       |       `-- ToolArgumentValidator.java
        |       |-- tools/
        |       |   |-- CreateTicketTool.java
        |       |   |-- CreateTodoTool.java
        |       |   |-- QuerySystemAlertTool.java
        |       |   |-- QueryUserDocumentTool.java
        |       |   `-- SearchKnowledgeBaseTool.java
        |       |-- eval/
        |       |   |-- EvalQuestion.java
        |       |   |-- EvalResult.java
        |       |   |-- RagEvalService.java
        |       |   `-- RagEvalRunner.java
        |       |-- controller/
        |       |   |-- AgentToolController.java
        |       |   `-- EvalController.java
        |       |-- entity/
        |       |   |-- AgentToolLog.java
        |       |   |-- EvalQuestionEntity.java
        |       |   `-- EvalRunEntity.java
        |       `-- mapper/
        |           |-- AgentToolLogMapper.java
        |           |-- EvalQuestionMapper.java
        |           `-- EvalRunMapper.java
        `-- resources/
            `-- mapper/
                |-- AgentToolLogMapper.xml
                |-- EvalQuestionMapper.xml
                `-- EvalRunMapper.xml
```

### 10.3 推荐工具

```text
searchKnowledgeBaseTool：检索知识库
queryUserDocumentTool：查询用户文档
createTodoTool：创建待办事项
createTicketTool：创建模拟工单
querySystemAlertTool：查询模拟系统告警
```

### 10.4 阶段验收标准

```text
1. Agent 能根据问题决定是否调用工具
2. 工具调用前校验用户身份和参数
3. 工具调用后保存 agent_tool_log
4. 工具失败有重试或明确失败返回
5. 单次 Agent 请求有最大工具调用次数限制
6. 能准备 eval_question 并记录评估结果
7. 能解释为什么不能让模型直接操作数据库
```

### 10.5 面试表达

```text
我的 Tool Calling 设计中，模型不会直接访问数据库或业务系统，而是只能请求调用后端注册过的工具。后端会校验用户身份、工具权限和参数合法性，真正执行工具逻辑，并记录 agent_tool_log。这样既能让 Agent 做事，也能控制安全边界。
```

---

## 11. 阶段 8：Docker 部署上线

### 11.1 阶段目标

让项目成为可部署、可演示、可排查的服务器项目。

结束时应该具备：

```text
1. Dockerfile
2. docker-compose.yml
3. MySQL / Redis / Qdrant / 后端服务编排
4. Nginx 反向代理
5. 生产配置
6. 日志配置
7. Actuator 健康检查
8. 服务器部署文档
```

### 11.2 本阶段最终目录结构增量

```text
agent-backend/
|-- Dockerfile
|-- docker-compose.yml
|-- .env.example
|-- deploy/
|   |-- nginx/
|   |   `-- nginx.conf
|   |-- mysql/
|   |   `-- init.sql
|   |-- scripts/
|   |   |-- build.sh
|   |   |-- deploy.sh
|   |   |-- restart.sh
|   |   `-- view-logs.sh
|   `-- server/
|       `-- firewall-notes.md
|-- logs/
|   `-- .gitkeep
`-- src/
    `-- main/
        `-- resources/
            |-- application-prod.yml
            |-- application-dev.yml
            `-- logback-spring.xml
```

### 11.3 docker-compose 服务

```text
agent-backend
mysql
redis
qdrant
nginx
```

### 11.4 阶段验收标准

```text
1. 本地 docker compose up 能启动所有服务
2. 服务器可以拉代码、构建镜像、启动服务
3. Nginx 能反向代理到后端
4. /actuator/health 能访问
5. 能查看后端日志
6. 数据库和 Redis 配置通过环境变量注入
7. README 中有部署步骤
```

### 11.5 面试表达

```text
我的项目使用 Docker Compose 管理 Spring Boot、MySQL、Redis 和 Qdrant，Nginx 做反向代理，Spring Boot Actuator 暴露健康检查接口。生产环境配置和本地开发配置分离，敏感信息通过环境变量注入，日志可用于线上问题排查。
```

---

## 12. 阶段 9：面试包装与项目材料

### 12.1 阶段目标

把项目整理成能放到简历、GitHub、面试中讲清楚的作品。

需要准备：

```text
1. README
2. 项目架构图
3. 数据库设计文档
4. API 文档
5. RAG 流程文档
6. Tool Calling 流程文档
7. 部署文档
8. 面试问答文档
9. 演示截图或视频
```

### 12.2 本阶段最终文档结构

```text
agent-backend/
|-- README.md
|-- docs/
|   |-- 01-project-overview.md
|   |-- 02-architecture.md
|   |-- 03-database-design.md
|   |-- 04-api-list.md
|   |-- 05-rag-flow.md
|   |-- 06-tool-calling-flow.md
|   |-- 07-deployment.md
|   |-- 08-interview-qa.md
|   |-- 09-learning-notes.md
|   `-- diagrams/
|       |-- architecture.mmd
|       |-- rag-flow.mmd
|       `-- tool-calling-flow.mmd
`-- screenshots/
    |-- api-doc.png
    |-- chat-stream.png
    |-- knowledge-base.png
    `-- deployment-health.png
```

### 12.3 README 必须包含

```text
1. 项目背景
2. 技术栈
3. 核心功能
4. 系统架构
5. 数据库设计
6. RAG 流程
7. Tool Calling 流程
8. 本地启动
9. Docker 部署
10. 接口文档地址
11. 效果截图
12. 面试亮点
```

### 12.4 简历项目描述

```text
基于 Spring Boot + Spring AI 的企业知识库智能 Agent 系统

使用 Spring Boot 构建后端服务，集成 MySQL、Redis、Qdrant 与大模型 API，实现文档上传、文本切片、Embedding 入库、RAG 检索增强问答、SSE 流式输出、Tool Calling 工具调用、JWT 权限认证和 Docker Compose 部署。系统支持知识库管理、会话历史、引用溯源、工具调用日志和服务健康检查。
```

---

## 13. 10 周学习安排

### 第 1 周：阶段 1

```text
主题：Java 基础 + 内存版项目
重点：类、对象、继承、多态、接口、异常、集合
项目：完成控制台版用户、文档、聊天消息管理
输出：能运行、能讲清楚 Java 基础如何落到项目
```

### 第 2 周：数据库设计

```text
主题：MySQL + SQL + 表设计
重点：DDL、CRUD、索引、事务、表关系
项目：设计 user、knowledge_base、document、document_chunk、chat_session、chat_message 表
输出：sql/001_init_schema.sql
```

### 第 3 周：阶段 2

```text
主题：Spring Boot REST API
重点：Controller、Service、Mapper、DTO、统一响应、异常处理
项目：完成用户、知识库、文档、文本 chunk、会话和消息的数据闭环
输出：能通过 API 创建和查询核心资源，至少一个列表支持分页/动态 SQL，并有测试、日志和 API 文档
```

2026-08-15 校准：阶段 2 已完成并通过最新复核；第 3 周内容结束，不再追加普通 CRUD。

### 第 4 周：阶段 3

```text
主题：登录权限 + Redis（两天项目冲刺）
DAY 1：Spring Security、AuthenticationManager、JWT 签发/验签、无状态认证、统一 401/403
DAY 2：Principal 用户资源隔离、Redis token 黑名单、Lua 原子限流、角色与综合测试
输出：可登录、可鉴权、可防越权、可退出失效、可限流的真实后端系统雏形
```

2026-08-20 校准：阶段 3 两天内容和总验收已全部完成；真实 MySQL/Redis 下 114 项测试全部执行通过，第 4 周内容结束。

### 第 5 周：阶段 4 和阶段 5

```text
主题：RAG 知识库系统
当前：阶段 4 DAY 1 已完成，DAY 2 开始前
阶段 4 DAY 1：迁移、文件安全、上传、存储、TXT/Markdown 解析和状态
阶段 4 DAY 2：PDF、清洗、固定长度切片、批量入库、失败重试和删除清理
阶段 4 输出：稳定、可溯源、可按 owner 查询的 document_chunk
阶段 5：Embedding、Qdrant、topK、Prompt 增强和引用来源
第 5 周最终输出：用户提问时能检索自己的知识库并生成带引用回答
```

### 第 6 周：阶段 6

```text
主题：Agent 对话与流式输出
重点：会话历史、Prompt Template、SSE、引用来源
项目：完成 /api/chat/stream
输出：具备真实 AI 产品的对话体验
```

### 第 7 周：阶段 7 前半

```text
主题：Tool Calling
重点：工具注册、参数校验、权限控制、工具日志
项目：实现 searchKnowledgeBaseTool、querySystemAlertTool、createTicketTool
输出：Agent 能安全调用后端工具
```

### 第 8 周：阶段 7 后半

```text
主题：Workflow + 记忆 + 评估
重点：意图识别、流程选择、失败兜底、RAG Eval
项目：普通问答走 RAG，任务型问题走 Tool Calling，复杂问题先计划再执行
输出：项目从 Demo 变成工程化 Agent 系统
```

### 第 9 周：阶段 8

```text
主题：Docker + 服务器部署
重点：Dockerfile、docker-compose、Nginx、环境变量、日志、健康检查
项目：本地和服务器部署
输出：公网可访问或至少服务器可运行
```

### 第 10 周：阶段 9

```text
主题：面试整理 + 简历包装
重点：README、架构图、接口文档、数据库设计、面试问答
项目：整理可展示材料
输出：GitHub 项目、简历描述、面试讲解稿
```

---

## 14. 每日学习固定节奏

阶段 4 每天必须同时形成四类可验证结果：

```text
知识：能解释当天机制和边界。
代码：机制落到明确文件、状态和调用链。
证据：成功、失败、安全和一致性都能运行断言。
表达：能把实现与取舍讲成面试回答。
```

### 14.1 标准 4 小时节奏

```text
00:00～00:30：阶段检查、基线回归和当天知识教学
00:30～01:00：API/SQL/状态机/失败策略设计
01:00～02:35：核心实现
02:35～03:15：单元、Controller、Mapper/外部联调
03:15～03:40：失败路径、安全与全量回归
03:40～04:00：README/OpenAPI、面试表达、快照和下一日衔接
合计：240 分钟
```

不足 4 小时时不得把未验证内容标记完成；快照记录精确停点，下次继续同一闭环。

### 14.2 阶段 4 DAY 1 时间盒

```text
00:00～00:20：真实 MySQL/Redis 基线回归，核对现有 Document/Chunk/currentUserId
00:20～00:45：讲懂 Multipart、路径安全、补偿一致性和处理状态
00:45～01:20：设计 SQL 005、状态值、上传/状态 API、配置与错误码
01:20～02:10：DocumentIngestionProperties + LocalFileStorageService + 存储测试
02:10～02:55：上传 Controller/Service/Mapper + owner/补偿测试
02:55～03:25：TXT/Markdown Parser + Cleaner + content/status 更新
03:25～03:45：MockMvc、真实 MySQL、文件系统与全量测试
03:45～04:00：README/OpenAPI、DAY 1 出口勾选和面试表达
```

DAY 1 必须留下一个可查询的真实结果：文件位于受控目录，`document` 有安全元数据和正确状态，TXT/Markdown 正文已解析，越权/失败没有残留。

### 14.3 阶段 4 DAY 2 时间盒

```text
00:00～00:20：复跑 DAY 1，检查磁盘/数据库无临时残留
00:20～00:45：讲懂 PDF 边界、overlap、batch insert、短事务与重试
00:45～01:20：PdfParser + Cleaner 完善和格式失败测试
01:20～02:00：FixedLengthChunkStrategy + 边界单元测试
02:00～02:40：delete old chunks + batchInsert + 原子替换事务
02:40～03:10：process 状态机、失败日志、重试和重复提交
03:10～03:30：文档删除物理文件清理与越权/补偿验证
03:30～03:50：TXT/Markdown/PDF 端到端和全量 MySQL/Redis 回归
03:50～04:00：阶段 4 验收、README/OpenAPI、快照和面试总表达
```

### 14.4 每日四组必答问题

```text
知识点：今天的上传、解析、清洗、切片或事务机制解决什么问题？
项目实现：哪些文件参与，currentUserId、文件、状态和文本如何流动？
运行验证：成功、越权、非法文件、解析失败和数据库/磁盘一致性如何证明？
面试表达：为什么这样分层，哪些能力主动留到后续阶段？
```

### 14.5 每日证据清单

```text
1. 实际检查、修改和新增文件清单。
2. mvn 测试总数、失败、错误、跳过及原因。
3. 至少一条成功上传/处理和三条关键失败路径。
4. MySQL document、document_chunk、process_log 的前后证据。
5. 测试存储目录中的文件数量、相对路径和失败清理证据。
6. userA/userB 越权前后数据库与磁盘均不变。
7. 日志/响应不含绝对路径、正文、密码、secret 或完整 token。
8. 60～90 秒面试表达。
9. 下一次只安排一个尚未完成闭环。
```

### 14.6 时间不足时的处理

若只有 2 小时，按半日记录，不降低出口标准：

```text
20 分钟：检查、回归与机制教学
30 分钟：完成 API/SQL/状态或算法设计
50 分钟：只实现一个最小闭环
15 分钟：成功/失败自动化验证
5 分钟：记录证据、阻塞和续接位置
```

可拆分但不能跨越依赖顺序：先迁移和状态，再存储与上传；先 parser/cleaner，再 chunk；先单元测试，再编排和端到端。不得因时间不足跳过 owner、路径安全、失败补偿或全量回归。

---

## 15. 每阶段不要做的事

### 阶段 1 不要做

```text
不要接 Spring Boot
不要接 MySQL
不要写 Controller
不要纠结 Docker
不要调用大模型
```

### 阶段 2 不要做

```text
不要急着做 RAG
不要加 Tool Calling
不要上复杂权限模型
不要做微服务拆分
```

### 阶段 3 不要做

```text
不要过度研究 Spring Security 源码
不要做复杂 RBAC 后台
不要把 Redis 用到所有地方
```

### 阶段 4 不要做

```text
不要一开始同时实现所有格式和复杂版面恢复。
不要信任原文件名、扩展名或 Content-Type。
不要把文件 IO、PDF 解析和长计算放进长数据库事务。
不要只保存文件而没有处理状态和失败补偿。
不要逐条开放自动 chunk 的 CRUD。
不要提前接 OCR、对象存储、消息队列、Embedding 或 Qdrant。
```

### 阶段 5 不要做

```text
不要同时接多个向量数据库。
不要在没有 owner metadata 过滤时开放检索。
不要跳过“无结果”与引用来源。
不要堆太多 Agent 框架，先跑通最小 RAG 闭环。
```

### 阶段 7 不要做

```text
不要让模型直接执行 SQL
不要跳过权限校验
不要允许无限工具调用
不要只记录模型回答而不记录工具日志
```

---

## 16. ChatGPT 每次指导代码时的检查清单

本清单从阶段 4 开始默认执行。阶段 3 安全能力作为回归基线，不再作为每日新功能重复教学。

### 16.1 给出代码前

```text
1. 已读取哪些实际代码、SQL、配置和测试；当前是 DAY 1、DAY 2 还是验收补缺？
2. 最近一次真实 MySQL/Redis 全量结果是什么；阶段 3 原始 114 项与当前 137 项全量基线是否保持？
3. 今天的唯一闭环属于迁移、存储、上传、解析、清洗、切片、状态还是清理？
4. 当前 document/content/status/chunk 约束是什么；迁移是否向前兼容旧数据？
5. API 契约、Multipart 参数、响应字段、错误码和 HTTP 状态是否先定义？
6. currentUserId 在哪里取得；KB/Document owner 是否在任何文件 IO 前验证？
7. storage root、相对路径、服务端文件名、最大大小和允许类型是否已经定义？
8. 原文件名、扩展名、Content-Type 和文件内容分别如何校验？
9. parseStatus/chunkStatus 的合法状态与转移是什么；如何拒绝重复 PROCESSING？
10. parser、cleaner、chunker 和 ingestion 的职责是否清晰，是否能独立单测？
11. chunkSize/overlap 的范围、尾块、空文本和索引不变量是否明确？
12. 文件系统与数据库失败如何补偿；哪些操作必须位于同一短事务？
13. 是否需要 batchInsert、delete old chunks、状态条件更新和 process log？
14. 响应/日志会不会暴露绝对路径、正文、堆栈、密码、secret 或 token？
15. 今天明确不做什么；是否误把 OCR、对象存储、Embedding、RAG 或 SSE 混入？
16. 完成后用哪些单元、MockMvc、MySQL、文件系统和全量证据验收？
```

### 16.2 编写代码时

```text
1. Controller 只做 HTTP 参数、认证身份和响应转换，不直接读写磁盘或解析 PDF。
2. 上传/处理 DTO 不接受可信 userId；身份只来自 CurrentUser。
3. owner 校验必须早于 storage.save/read/delete 和任何写 Mapper。
4. 路径使用服务端生成名；resolve 后 normalize 并验证 startsWith storage root。
5. FileStorageService 返回相对路径和受控元数据，不把 Path 实现细节泄露给 API。
6. Parser 只解析；Cleaner 只规范文本；ChunkStrategy 只产生有序结果。
7. TXT/Markdown 明确 UTF-8/BOM；PDF 明确加密、损坏、扫描件和空文本边界。
8. chunkSize > 0 且 0 <= overlap < chunkSize；循环每次必须向前推进。
9. batchInsert 前的旧 chunk 删除与成功状态更新放在同一事务。
10. 耗时文件读取/解析不放在长数据库事务中。
11. 每个异常分支更新明确 FAILED 状态，错误摘要截断且不含底层敏感信息。
12. 数据库插入失败时补偿文件；测试临时文件使用精确路径清理。
13. 处理日志只记录 ID、阶段、状态、类型、大小和安全摘要。
14. 新错误码、HTTP 状态、Result JSON 和 OpenAPI 描述保持一致。
15. 保留阶段 2 手工文档兼容性和阶段 3 owner/Redis/Security 行为。
```

### 16.3 给出代码后

```text
1. 列出修改/新增文件以及每个文件的单一职责。
2. 画出请求 -> Security -> Controller -> Ingestion -> Storage/Parser/Cleaner/Chunker -> Mapper。
3. 列出 SQL 迁移字段、旧数据处理、索引和状态转移。
4. 给出环境变量和启动/测试命令，但不回显真实密码与 secret。
5. 给出成功上传、处理、状态、chunk 查询和删除的可执行请求。
6. 给出空/超限/伪类型/损坏 PDF/非法参数/重复处理的预期状态与错误码。
7. 给出 userA/userB 越权请求，证明数据库和磁盘前后均不变。
8. 给出 MySQL document/chunk/process_log 查询和受控目录文件证据。
9. 验证失败补偿、FAILED 重试、旧 chunk 原子替换与物理文件清理。
10. 报告测试总数、失败、错误、跳过及原因，不能只报 BUILD SUCCESS。
11. 重新检查响应、OpenAPI 与日志没有绝对路径、正文或秘密。
12. 对照 DAY 出口和阶段 4 总验收逐项标记，不凭文件数量宣布完成。
13. 输出 60～90 秒面试闭环和下一唯一行动。
```

### 16.4 禁止出现的指导方式

```text
未读取现有 Document/Chunk/SQL 就直接生成整套上传代码。
只让 MultipartFile 写到磁盘，不做 owner、路径、状态和补偿。
把原文件名直接拼进服务器路径。
只检查扩展名或 Content-Type 就认为文件安全。
把 PDF 解析、数据库删除和批量插入全塞进一个长事务。
在 Controller 中写 parser、cleaner 和 chunk while 循环。
使用 overlap >= chunkSize 仍继续运行。
重处理只追加 chunk，不删除/替换旧数据。
发生异常只返回 500，不写可重试状态。
把绝对路径、堆栈或文档正文返回给客户端或写进日志。
只测 TXT 200，不测越权、损坏文件、失败补偿和批量回滚。
为了“完整”提前接 OCR、MinIO、消息队列、Embedding 或 Qdrant。
阶段 4 未验收就进入 RAG。
```

---

## 17. 动态下一步行动判定

当前默认下一步：阶段 4 DAY 2。阶段 1～阶段 3 与阶段 4 DAY 1 已完成，不再执行旧 CRUD 收尾、JWT 重写、Redis 扩张或重复建设上传/解析入口。

### 17.1 每次开始时的判定树

```text
第一步：验证已完成基线
  -> 项目不能编译或阶段 3 回归失败：先定位是否由当前改动引入
  -> MySQL/Redis 外部环境不可用：记录唯一条件并修复环境，不能虚报集成通过
  -> 当前 137 项全量基线与 Redis PING 正常：进入阶段 4 判断

第二步：检查 DAY 1
  -> 没有 SQL 005/文件状态字段：先设计迁移与状态机
  -> 只有 Multipart Controller：补 owner、存储服务、路径安全、补偿与测试
  -> 文件能保存但 TXT/Markdown 未解析到 content：DAY 1 未完成
  -> 没有状态查询或失败状态：DAY 1 未完成
  -> DAY 1 出口全部通过：进入 DAY 2

第三步：检查 DAY 2
  -> 没有 FixedLengthChunkStrategy：先完成纯算法和边界测试
  -> 逐条 insert、没有 batch/原子替换：补 Mapper 与短事务
  -> 无 PDF 或失败边界：补文本型 PDF，明确不做 OCR
  -> 无 FAILED 重试/PROCESSING 防重：补状态机
  -> 删除只删数据库：补物理文件清理语义
  -> DAY 2 出口全部通过：执行阶段 4 总验收

第四步：阶段出口
  -> 第 7.13 任一项未完成：只补该项，不扩张功能
  -> 全部通过：记录阶段 4 完成快照，进入阶段 5 Embedding + Qdrant
```

当前命中结果：阶段 3 与阶段 4 DAY 1 全部通过；SQL 005、上传、存储、TXT/Markdown parser、cleaner、状态和 DAY 1 测试均已存在，因此从第三步 DAY 2 开始。

### 17.2 当前固定执行顺序

```text
1. [完成] 阶段 1～阶段 3 与 2026-08-20 全量复验。
2. [完成] 提交阶段 3/阶段 4 DAY 1 Git 里程碑 `2679565` 并推送 origin/main。
3. [完成] DAY 1：读取 Document/Chunk 基线，设计并执行 SQL 005、状态机、API 和配置。
4. [完成] DAY 1：安全 LocalFileStorageService 与路径/补偿测试。
5. [完成] DAY 1：上传 API + currentUserId/KB owner + 元数据落库。
6. [完成] DAY 1：TXT/Markdown Parser + Cleaner + content/status。
7. [完成] DAY 1：processing-status、MockMvc、真实 MySQL、README/OpenAPI 和 DAY 1 出口；2026-09-01 以 137 项测试复验。
8. [当前] DAY 2：PdfParser 与失败边界。
9. [待办] DAY 2：FixedLengthChunkStrategy + overlap 边界。
10. [待办] DAY 2：delete old chunks + batchInsert + 原子替换。
11. [待办] DAY 2：process 状态机、日志、重试和重复提交。
12. [待办] DAY 2：文档删除物理文件清理。
13. [待办] DAY 2：端到端、真实 MySQL/Redis 全量回归和阶段 4 总验收。
```

顺序只可因实际代码已经存在且证据完整而跳过；不能因任务看起来简单而跳过设计、失败路径或测试。

### 17.3 阻塞项优先级

```text
P0：项目不能编译/启动；迁移破坏旧数据；owner 越权；目录穿越；任意文件覆盖/删除；秘密或正文泄露；数据库失败后文件残留且无补偿。
P1：上传/解析/chunk 主链路不可用；状态永久 PROCESSING；重试产生重复 chunk；批量替换半提交；删除导致数据库/磁盘明显不一致。
P2：PDF 特殊失败文案、OpenAPI/README 不一致、process log 不完整、测试覆盖不足但主链路可运行。
P3：Markdown 标题切片、checksum 去重、OCR、对象存储、异步队列、批量上传、知识库级联删除等增强。
```

处理规则：先 P0，再 P1，再 P2；P3 进入 backlog。阶段 4 的“详细”指主链路和边界证据详细，不等于技术范围无限扩大。

### 17.4 防止错误推进

```text
1. 只有 Multipart Controller、没有安全存储和元数据，不算上传完成。
2. 只有文件落盘、没有解析和状态，不算 DAY 1 完成。
3. 只有手工 POST chunk，不算自动切片。
4. 只有 while 循环、没有 overlap 边界与单元测试，不算 ChunkStrategy 完成。
5. 只有逐条 insert、没有事务替换与失败回滚，不算摄取闭环。
6. 只有 TXT 成功、没有空/伪类型/越权/存储失败，不算安全上传闭环。
7. 只有 PDF 正常样例、没有损坏/加密/扫描件边界，不算 PDF 支持完整。
8. 只有数据库状态、没有磁盘证据，不算文件一致性验证。
9. 只有 parser 单测、没有 owner API 与 MySQL 结果，不算阶段验收。
10. 只有新增测试通过、阶段 3 基线失败，不得推进。
11. 手工 DocumentChunk 是阶段 2 基线，不得据此把阶段 4 标成已开始。
12. 出现 Embedding、Qdrant、RAG 回答才进入阶段 5。
```

### 17.5 每次结束时的下一步选择

| 当前结果 | 下一唯一行动 |
|---|---|
| SQL/状态未定 | 完成迁移设计与 Schema 测试 |
| 存储服务未通过 | 只修路径、类型、补偿和临时目录 |
| 上传成功但解析失败 | 只完成当前格式 Parser/Cleaner |
| DAY 1 通过 | 按第 7.11 节先复跑基线，再完成 PdfParser 边界与 FixedLengthChunkStrategy 单元测试 |
| chunk 算法通过 | 完成 batchInsert 与原子替换 |
| 主链路通过但失败项缺失 | 只补对应 FAILED/重试/越权证据 |
| 第 7.13 全通过 | 记录完成快照，进入阶段 5 |

阶段 1 历史项目不影响当前主线。只有用户明确要求维护阶段 1，才单独检查该目录。

---

## 18. 最终目标

最终项目不是“会调大模型 API 的 Demo”，而是一个完整 Java 后端工程：

```text
Spring Boot REST API
MySQL 业务数据
Redis 缓存、限流、黑名单
JWT 权限认证
知识库文档上传和切片
Qdrant 向量检索
RAG 问答
SSE 流式输出
Tool Calling 工具调用
Agent Workflow
评估与日志
Docker Compose 部署
README 和面试材料
```

最终面试时应该能说清楚：

```text
1. 一个 HTTP 请求如何经过 Controller、Service、Mapper 到 MySQL
2. 为什么用户、文档、chunk、会话、消息要分表
3. Redis 在项目中解决什么问题
4. RAG 的完整流程是什么
5. Tool Calling 的安全边界是什么
6. SSE 为什么适合流式问答
7. 项目如何部署和排查线上问题
```

这份文档就是后续每天学习和编码时的阶段地图。每次让 ChatGPT 帮忙，都应该先重新识别当前阶段，再围绕实际阶段、实际目录结构和对应验收标准推进。

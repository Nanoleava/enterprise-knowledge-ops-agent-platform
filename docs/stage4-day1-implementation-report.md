# 阶段 4 DAY 1 实施报告

日期：2026-08-21
项目：`C:\Users\nano\Desktop\LJL-Java-Agent\agent-backend`
阶段：阶段 4 DAY 1
依据：项目源参考文档第 7.10 节与 `stage4_day1_project_modification_guideline.md`

## 1. 阶段结论

DAY 1 已完成“安全上传 -> 本地存储 -> Document 元数据 -> TXT/Markdown 解析 -> `document.content` -> 处理状态查询”的同步闭环。

本次明确未实现 PDF、自动切片、chunk 批量替换、Embedding、向量库、RAG、OCR、对象存储和异步队列。这些不属于 DAY 1 出口。

最终自动化结果：真实 MySQL、真实 Redis 与临时文件系统下执行 `mvn clean test`，Tests run: 137，Failures: 0，Errors: 0，Skipped: 0，BUILD SUCCESS。

## 2. 修改与新增文件

### 2.1 配置、迁移与文档

- `.env.example`：补充文档存储、大小和允许类型变量示例。
- `.gitignore`：忽略真实上传目录。
- `src/main/resources/application.yml`：增加 Spring Multipart 限制和 `app.document` 配置。
- `sql/005_stage4_document_ingestion.sql`：增加文件元数据、解析/切片状态、错误摘要、时间、索引和检查约束。
- `README.md`：同步 DAY 1 链路、配置、迁移、API、测试与范围。
- `docs/stage4-day1-implementation-report.md`：本报告与人工验证教程。

### 2.2 Controller、DTO 与配置类

- `DocumentUploadController`：上传、解析和处理状态三个受保护接口。
- `DocumentParseRequest`：`force` 重解析参数。
- `DocumentUploadResponse`：不含服务器路径的上传结果。
- `DocumentProcessStatusVO`：不含正文/堆栈的处理状态。
- `DocumentIngestionProperties`、`DocumentIngestionConfig`：强类型配置与启动注册。
- `ErrorCode`、`GlobalExceptionHandler`：400/409/413/422/500 文档错误与 Multipart 统一 JSON。
- `OpenApiConfig`：阶段 4 DAY 1 分组、路径和响应码。

### 2.3 摄取、存储、解析与清洗

- `DocumentIngestionService`、`DefaultDocumentIngestionService`：owner、校验、存储、解析、状态与补偿编排。
- `DocumentIngestionPersistenceService`：把元数据和状态更新限制为短事务。
- `DocumentFileType`、`ValidatedUpload`、`DocumentUploadValidator`：文件边界模型和多维校验。
- `FileStorageService`、`LocalFileStorageService`、`StoredFile`、`DocumentStorageException`：UUID 文件名、临时写入、受控路径、SHA-256 和精确删除。
- `DocumentParser`、`ParserRegistry`、`TxtParser`、`MarkdownParser`、`Utf8FileReader`、`DocumentParsingException`：格式策略与严格 UTF-8 解析。
- `TextCleaner`、`DefaultTextCleaner`：BOM/NUL、换行、行尾空白和空行归一化。

### 2.4 Entity、Mapper 与查询模型

- `Document`、`DocumentVO`：新增内部文件元数据和公开处理状态；VO 不暴露存储路径/存储名。
- `DocumentMapper`、`DocumentMapper.xml`：上传文档插入、全字段映射、条件进入 `PROCESSING`、成功/失败更新。
- `DocumentChunkMapper`、`DocumentChunkMapper.xml`：状态查询所需的 chunk 数量。

### 2.5 测试

- 新增 `DocumentUploadValidatorTest`。
- 新增 `LocalFileStorageServiceTest`。
- 新增 `DocumentParserTest`。
- 新增 `DefaultTextCleanerTest`。
- 新增 `DefaultDocumentIngestionServiceTest`。
- 新增 `DocumentUploadControllerTest`。
- 新增 `DocumentIngestionIntegrationTest`。
- 扩展 `DocumentMapperTest`、`SchemaIntegrationTest`、`OpenApiIntegrationTest`、`SecurityWebIntegrationTest` 与 `ConfigurationSafetyTest`。

## 3. 功能实现说明

### 3.1 上传能力

接口：

```http
POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=<required>
title=<optional>
```

处理顺序：

```text
Security Filter
-> JWT sub 转 currentUserId
-> KnowledgeBase owner 校验
-> 文件名/大小/扩展名/MIME/内容特征/UTF-8 校验
-> LocalFileStorageService
-> Document 元数据短事务
-> PENDING 响应
```

未传或传空 `title` 时，使用去扩展名后的安全原文件名。响应返回 `documentId`、显示文件名、类型、大小、`parseStatus` 和 `chunkStatus`，不返回相对或绝对物理路径。

### 3.2 文件存储能力

默认根目录为 `./storage/uploads`，可由 `DOCUMENT_STORAGE_ROOT` 覆盖。最终相对路径固定为：

```text
{userId}/{knowledgeBaseId}/{32位UUID}.{txt|md}
```

原文件名仅用于展示，绝不参与最终磁盘文件名。所有路径经 `resolve -> normalize -> startsWith(storageRoot)` 检查。写入先落到同目录唯一临时文件，成功后原子移动；同时计算 SHA-256。数据库只保存相对路径。

### 3.3 解析与清洗能力

接口：

```http
POST /api/documents/{documentId}/parse
Authorization: Bearer <token>
Content-Type: application/json

{"force": false}
```

`ParserRegistry` 根据数据库中的 `file_type` 选择 `TxtParser` 或 `MarkdownParser`，客户端不能指定解析器。解析器只负责 `Path -> String`，不写数据库、不处理 owner、不生成 HTTP 响应。

清洗顺序：

1. CRLF/CR 转 LF。
2. 移除 BOM 与 NUL。
3. 删除每行行尾空格/Tab。
4. 连续空行压缩到最多 2 行。
5. 删除首尾空行并拒绝空文本。

Markdown 标题、列表、代码块和缩进保留。

### 3.4 状态查询能力

接口：

```http
GET /api/documents/{documentId}/processing-status
Authorization: Bearer <token>
```

返回字段：`documentId`、`parseStatus`、`chunkStatus`、`chunkCount`、`errorMessage`、`processedAt`。不返回正文、服务器路径或异常堆栈。

## 4. 数据库迁移与状态设计

`005_stage4_document_ingestion.sql` 为 `document` 增加：

| 字段 | 作用 |
|---|---|
| `original_file_name` | 客户端显示名 |
| `stored_file_name` | 服务端 UUID 文件名 |
| `file_type` | TXT/MARKDOWN，DAY 2 扩展 PDF |
| `file_size` | 实际字节数 |
| `file_path` | storage root 下相对路径 |
| `file_checksum` | SHA-256 |
| `parse_status` | 独立解析状态 |
| `chunk_status` | 独立切片状态 |
| `process_error` | 最多 500 字符的安全摘要 |
| `processed_at` | 最近成功解析/处理时间 |

状态值：`NOT_APPLICABLE`、`PENDING`、`PROCESSING`、`SUCCESS`、`FAILED`。

- 阶段 2 手工正文：parse/chunk 均为 `NOT_APPLICABLE`。
- DAY 1 新上传：parse/chunk 均为 `PENDING`。
- 解析中：parse 为 `PROCESSING`，条件更新防止重复请求。
- 解析成功：正文落库，parse 为 `SUCCESS`，chunk 仍为 `PENDING`。
- 解析失败：parse 为 `FAILED`，chunk 仍为 `PENDING`，原文件保留。

迁移在真实 MySQL 8.4 重复执行成功。执行前后均为 19 条历史 Document、9 条历史 Chunk；19 条历史文档均为 `NOT_APPLICABLE`。`idx_document_user_parse_status` 唯一索引名存在 1 个，3 个状态/大小检查约束均存在。

## 5. 安全与失败补偿

### 5.1 文件安全

- DAY 1 只允许 `.txt`、`.md`、`.markdown`。
- 同时检查扩展名、Content-Type、MZ/ELF/ZIP/PDF 等明显二进制头、控制字符和严格 UTF-8。
- 拒绝空文件、超限文件、双扩展名、NUL 文件名和明显伪装文件。
- Multipart 框架超限统一映射为 HTTP 413/code 41301。
- 缺失文件 part 与非法 Multipart 统一返回 `Result` JSON，而不是默认 HTML。

### 5.2 owner 安全

- 上传先校验 KB owner，再读取上传内容或创建目录。
- parse/status 先校验 Document owner，再解析文件路径或执行写 Mapper。
- userB 对 userA 知识库的上传返回 403，数据库行数与文件数前后不变。

### 5.3 一致性与事务

- 文件系统和 MySQL 不共享事务。
- 上传采用“安全落盘 -> Document 短事务”；插入失败精确删除刚保存的相对路径。
- 解析状态用独立短事务从 PENDING/FAILED 条件更新为 PROCESSING。
- 文件读取、解析和清洗在事务外执行。
- 成功正文与状态使用短事务更新；失败通过新事务写 FAILED 和安全摘要。
- 解析失败保留原文件，允许相同接口重试。

## 6. 错误契约

| 场景 | 业务码 | HTTP |
|---|---:|---:|
| 空文件/非法文件名/缺失文件 part | 40002 | 400 |
| 不支持类型/MIME 或内容特征不符/非法 UTF-8 | 40003 | 400 |
| 正在解析或成功文档未使用 force | 40906 | 409 |
| 文件超过上限 | 41301 | 413 |
| 文件存在但解析/清洗失败 | 42201 | 422 |
| 存储或元数据/正文持久化失败 | 50008 | 500 |

现有错误码数值保持不变。

## 7. 自动化验证证据

最终命令在当前进程注入 dev/MySQL/Redis/JWT 测试环境变量后执行：

```powershell
mvn clean test
```

结果：

```text
Tests run: 137
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
Redis PING: PONG
```

覆盖：

- UTF-8 TXT、BOM、中文和换行清洗。
- Markdown 标题、列表、代码块结构保留。
- 空文件、超限、扩展名/MIME 不匹配、二进制伪装、双扩展名、非法 UTF-8。
- UUID 路径、目录穿越、相对路径、SHA-256、真实文件删除。
- owner 校验早于文件 IO、数据库失败补偿删除。
- PENDING -> PROCESSING -> SUCCESS 与 PENDING -> PROCESSING -> FAILED。
- FAILED 再次调用 parse 可重试，失败时正文仍为空且原文件仍在。
- userB 越权前后数据库/文件系统零变化。
- 真实 MySQL 字段、默认值、条件更新、索引和约束。
- 新接口未认证 401、OpenAPI Bearer Scheme、413/422 契约。
- 阶段 3 JWT、owner、logout blacklist、Redis Lua 限流完整回归。

测试结束后，真实 MySQL 恢复为 19 条历史 Document、9 条历史 Chunk；项目默认上传目录文件数为 0；已知本地数据库口令字面量扫描命中数为 0。

## 8. 人工 Postman 验证教程

以下步骤是可复现教程；自动化测试已经承担 DAY 1 验收，Postman 不替代自动化证据。

### 8.1 准备环境

1. 按顺序执行数据库迁移到 `005_stage4_document_ingestion.sql`。
2. 启动本机 Redis，确认 `docker exec redis redis-cli PING` 返回 `PONG`。
3. 仅在当前终端设置 `SPRING_PROFILES_ACTIVE=dev`、`DB_USERNAME`、`DB_PASSWORD` 和合法 `JWT_SECRET_BASE64`，不要写入配置文件。
4. 执行 `mvn spring-boot:run`。
5. 准备 `guide.txt` 与 `readme.md`，均保存为 UTF-8。

### 8.2 注册、登录并获得 Token

如已有可用用户，可直接登录。

```http
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
  "username": "stage4_user_a",
  "password": "Test@123456",
  "email": "stage4_user_a@example.com"
}
```

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "stage4_user_a",
  "password": "Test@123456"
}
```

在 Postman Environment 新建 `baseUrl=http://localhost:8080` 和 `tokenA=<响应 data.accessToken>`。不要把真实 token 保存到 Git。

### 8.3 创建知识库

```http
POST {{baseUrl}}/api/knowledge-bases
Authorization: Bearer {{tokenA}}
Content-Type: application/json

{
  "name": "Stage4 DAY1",
  "description": "文档摄取验证"
}
```

保存响应 `data.id` 为 `knowledgeBaseId`。

### 8.4 Multipart 上传 TXT

1. 新建 `POST {{baseUrl}}/api/knowledge-bases/{{knowledgeBaseId}}/documents/upload`。
2. Authorization 选择 Bearer Token，值为 `{{tokenA}}`。
3. Body 选择 `form-data`。
4. 新增 key `file`，类型从 Text 改为 File，选择 `guide.txt`。
5. 可选新增 key `title`，类型 Text，值为 `Java 上传指南`。
6. 不要手工填写 `Content-Type`；Postman 会生成带 boundary 的 multipart header。

预期 HTTP 200，`data.parseStatus=PENDING`、`data.chunkStatus=PENDING`，且响应没有 `filePath`。保存 `data.documentId`。

### 8.5 触发解析

```http
POST {{baseUrl}}/api/documents/{{documentId}}/parse
Authorization: Bearer {{tokenA}}
Content-Type: application/json

{
  "force": false
}
```

预期 HTTP 200，`parseStatus=SUCCESS`、`chunkStatus=PENDING`、`chunkCount=0`。

### 8.6 查询状态

```http
GET {{baseUrl}}/api/documents/{{documentId}}/processing-status
Authorization: Bearer {{tokenA}}
```

预期不返回 `content`、`filePath` 或堆栈。成功文档 `errorMessage=null`。

### 8.7 验证 Markdown

把第 8.4～8.6 步的文件换成 `readme.md`，正文包含标题、列表和 fenced code block。解析后调用 `GET /api/documents/{id}`，验证 `content` 仍包含 `#`、`-` 和代码围栏。

### 8.8 验证数据库与文件目录

数据库只查看测试文档 ID：

```sql
SELECT id, user_id, knowledge_base_id, title,
       original_file_name, stored_file_name, file_type, file_size,
       file_path, file_checksum, parse_status, chunk_status,
       process_error, processed_at,
       CHAR_LENGTH(content) AS content_length
FROM document
WHERE id = <documentId>;
```

预期：原文件名与存储文件名不同，`file_path` 为相对路径，checksum 为 64 个十六进制字符，parse 为 SUCCESS，chunk 为 PENDING，正文长度大于 0。

在项目目录执行：

```powershell
$rowRelativePath = "<数据库 file_path>"
$fullPath = Join-Path (Resolve-Path .\storage\uploads) $rowRelativePath
Test-Path -LiteralPath $fullPath
```

预期 `True`。不要把绝对路径复制到接口响应或业务日志。

### 8.9 关键失败场景

1. `file` 选择 0 字节文件：400/code 40002，无 Document、无新文件。
2. 上传 `.exe`、`.pdf`、`run.exe.txt` 或 MZ 头伪装 `.txt`：400/code 40002 或 40003。
3. 上传超过配置上限文件：413/code 41301。
4. 上传只包含空白的 `blank.txt` 后调用 parse：422/code 42201；状态查询为 parse FAILED、chunk PENDING，原文件仍存在，再次 parse 仍可重试。
5. 创建 userB/tokenB，使用 tokenB 向 userA 的 `knowledgeBaseId` 上传：403/code 40302；前后数据库行数与目录文件数不变。
6. 成功文档再次 `force=false` 解析：409/code 40906；使用 `force=true` 可重解析。

## 9. 设计思路与面试价值

### 9.1 为什么分离原文件名和存储名

原文件名不可信，可能包含路径穿越、冲突或特殊字符。保留它只为展示，磁盘使用 UUID，能同时解决安全、覆盖与并发命名问题。

### 9.2 为什么引入 StorageService

Controller 不应掌握磁盘根目录。StorageService 集中处理受控路径、临时文件、原子移动、checksum 和补偿删除，便于单元测试和后续替换对象存储。

### 9.3 为什么引入 Parser 接口

TXT/Markdown/PDF 的格式差异属于解析策略，不属于 HTTP 或数据库。Parser Registry 让 DAY 2 增加 PDF 时无需修改 Controller 主逻辑。

### 9.4 为什么拆分 parseStatus 与 chunkStatus

解析和切片是不同生命周期。解析成功不代表 chunk 已生成；拆分后才能准确表达失败点、重试边界与下一阶段进度。

### 9.5 60～90 秒表达

> 阶段 4 DAY 1 我把原来的手工 Document 创建扩展成了真实文档摄取入口。请求通过 JWT Principal 获得 currentUserId，先校验知识库 owner，再校验文件大小、扩展名、声明类型、二进制特征和 UTF-8。原文件名只作展示，磁盘路径由服务端按 userId、knowledgeBaseId 和 UUID 生成，并经过 normalize 和 startsWith 校验，数据库只保存相对路径与 SHA-256。上传完成后 Document 进入 PENDING，解析接口用条件更新进入 PROCESSING，通过 Parser Strategy 支持 TXT 和 Markdown，再做保留结构的基础清洗并保存 content。解析在事务外执行，状态和正文只用短事务；数据库插入失败会补偿删除文件，解析失败会写 FAILED、保留源文件以便重试。最终用 owner 越权、伪类型、非法 UTF-8、数据库补偿、真实 MySQL/Redis 和文件系统测试验证，作为 DAY 2 PDF 与自动切片的稳定入口。

## 10. DAY 1 验收清单

- [x] 文件上传成功。
- [x] 文件安全存储。
- [x] owner 校验先于文件 IO。
- [x] 原文件名与存储名分离。
- [x] TXT 解析成功。
- [x] Markdown 解析成功且结构保留。
- [x] `document.content` 保存成功。
- [x] parse 状态正确流转。
- [x] 状态查询可用且不泄露路径/正文。
- [x] 数据库失败补偿文件。
- [x] 解析失败保留原文件并可重试。
- [x] 越权无数据库/文件污染。
- [x] SQL 005 真实 MySQL 幂等执行。
- [x] 阶段 3 JWT/Redis/owner 回归全部通过。
- [x] README、OpenAPI、错误码和配置示例同步。
- [x] 全量测试 137/137 通过。

## 11. DAY 2 唯一衔接

下一步只进入阶段 4 DAY 2：先实现文本型 PDF Parser 与失败边界，再实现固定长度 + overlap 切片、`document_chunk` batch insert/原子替换、处理日志、失败恢复和文档删除后的物理文件清理。DAY 2 之前不接 Embedding 或 Qdrant。

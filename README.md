# TOMO 助手 - 多模态 RAG 知识库问答系统

面向企业内部/个人知识管理场景，构建支持图文混合检索的**私有化部署**智能问答系统。

## 核心特色

### 🔒 完全私有化部署

系统支持完全离线部署，所有数据（文档、向量、对话记录）均存储在用户自有的服务器上，不依赖任何外部云服务。适合对数据安全有严格要求的企业内部场景。

### 🔄 双模型运行时 — 在线 API + 本地 Ollama 一键切换

系统内置两种 LLM 运行模式，通过前端侧边栏或 API 接口一键切换：

| 模式 | 说明                  | 适用场景 |
|------|---------------------|---------|
| **在线模式** | 调用openai支持的各大厂商的模型  | 快速体验、模型能力要求高 |
| **本地模式** | 调用 Ollama 本地部署的开源模型 | 数据不出域、完全离线、隐私优先 |

- 切换至在线模式时，系统弹出 **5 秒倒计时隐私警告**，提醒用户数据将上传至云端
- 本地模式下所有推理均在本机完成，适合处理敏感数据
- 两种模式共享同一套对话和知识库数据，切换无感知

### 📄 多模态 RAG 知识库

- 支持 PDF/Word/TXT/Markdown 文档及图片上传
- 图片由多模态 LLM 自动生成描述文本，实现"以文搜图"的跨模态检索
- 文档自动解析、智能切片、向量化存储，构建可检索的私有知识库

### 💬 流式对话 & 引用溯源

- SSE 流式返回生成结果，前端实时打字机效果
- 回答附带引用文档片段及相似度评分，可点击溯源原文

## 系统架构

```
前端 (Vue3 + Element Plus + Pinia)
    |  SSE / REST API
后端 (Spring Boot 3.5.14 + Java 21)
    |
    +-- ChatController             SSE 流式对话
    +-- DocumentController         文档上传/管理
    +-- KnowledgeBaseController    知识库 CRUD
    +-- ConversationController     会话历史
    +-- SettingsController         LLM 模式切换 (在线/本地)
    |
    +-- LlmModelHolder             LLM 模型管理 (智谱 API / Ollama 双模式)
    +-- StreamingChatService       RAG 编排 + 流式输出
    +-- DocumentService            文档解析 -> 切片 -> 向量化 -> 存储
    +-- RetrievalService           向量检索 + 重排序
    +-- ImageDescriptionService    多模态图片描述
    +-- ConversationMemoryService  会话记忆 (Redis + MySQL)
    +-- EmbeddingService           向量化 & Milvus 操作
    |
    +-- Milvus     向量数据库
    +-- MinIO      对象存储
    +-- MySQL      元数据存储
    +-- Redis      缓存加速
```

## 技术选型

| 模块 | 技术方案                                                                |
|------|---------------------------------------------------------------------|
| 后端框架 | Spring Boot 3.xx, Java 21                                           |
| LLM 编排 | LangChain4j 1.0.0-beta1                                             |
| 文档解析 | Apache Tika (PDF/Word/PPT/TXT/Markdown)                             |
| Embedding | OpenAI 兼容 API (openai支持厂商embedding-model/ Ollama 本地embedding-model) |
| 向量数据库 | Milvus 2.5.6                                                        |
| 对象存储 | MinIO (S3 兼容)                                                       |
| 缓存 | Redis                                                               |
| 关系数据库 | MySQL 8.0+                                                          |
| 前端框架 | Vue 3.5, Element Plus 2.8, Pinia 2.2                                |
| 流式通信 | SSE (SseEmitter)                                                    |
| API 文档 | Knife4j (OpenAPI 3)                                                 |

## 快速开始

### 环境要求

- Java 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Docker & Docker Compose（用于部署 Milvus + MinIO）
- [可选] Ollama（本地模型部署，参见 [Ollama 官网](https://ollama.com/)）

### 1. 启动基础设施 (Milvus + MinIO)

```bash
cd docker
docker-compose up -d
```

启动的服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| Milvus | 19530 | 向量数据库 |
| MinIO | 9000 / 9001 | 对象存储 (API / 控制台) |
| etcd | 2379 | Milvus 元数据存储 |
| Attu | 8000 | Milvus 可视化管理界面 |

MinIO 默认账号密码：`minioadmin` / `minioadmin`

### 2. 初始化数据库

执行 `src/main/resources/db/migration/V1__init_schema.sql` 创建数据库和表：

```sql
CREATE DATABASE IF NOT EXISTS tomo_assistant DEFAULT CHARACTER SET utf8mb4;
USE tomo_assistant;
-- 然后执行 V1__init_schema.sql 中的建表语句
```

### 3. 配置应用

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tomo_assistant?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

tomo:
  # ==================== MinIO ====================
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: tomo-knowledge

  # ==================== Milvus ====================
  milvus:
    uri: http://localhost:19530
    collection-prefix: tomo_kb_
    dimension: 2048

  llm:
    # 在线模式
    chat:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-CHAT-MODEL-NAME>
    embedding:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-EMBEDDING-MODEL-NAME>
      dimension: 2048
    multimodal:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-MULTI-MODEL-NAME>
    # 本地模式 - Ollama 本地部署
    ollama:
      base-url: http://localhost:11434/v1
      chat-model-name: <YOUR-CHAT-MODEL-NAME>
      embedding-model-name: <YOUR-EMBEDDING-MODEL-NAME>
      multimodal-model-name: <YOUR-MULTI-MODEL-NAME>
  # ==================== RAG ====================
  rag:
    chunk-size: 512
    chunk-overlap: 64
    top-k: 5
    rerank:
      enabled: false
      base-url: http://localhost:8081
      model-name: bge-reranker-v2-m3

```

### 4. [可选] 部署本地模型

如果需要使用本地模式，先安装并启动 Ollama，然后拉取所需模型：

```bash
# 安装 Ollama 后拉取模型
ollama pull <MODELNAME>      # 对话模型
ollama pull <MODELNAME>      # Embedding 模型
ollama pull <MODELNAME>      # 多模态模型（图片理解）
```

### 5. 启动后端

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问地址：http://localhost:5173

### 7. 访问 API 文档

启动后端后访问 Knife4j 文档：http://localhost:8080/doc.html

## API 接口

### 对话接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/stream` | SSE 流式对话（支持图片消息） |

### 知识库接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge-bases` | 创建知识库 |
| GET | `/api/knowledge-bases` | 获取知识库列表 |
| GET | `/api/knowledge-bases/{id}` | 获取知识库详情 |
| PUT | `/api/knowledge-bases/{id}` | 更新知识库 |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库 |

### 文档接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge-bases/{kbId}/documents/upload` | 上传文档（支持多文件） |
| GET | `/api/knowledge-bases/{kbId}/documents` | 获取文档列表 |
| DELETE | `/api/knowledge-bases/{kbId}/documents/{docId}` | 删除文档 |
| POST | `/api/knowledge-bases/{kbId}/documents/{docId}/reprocess` | 重新处理文档 |

### 会话接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/conversations` | 创建会话 |
| GET | `/api/conversations` | 获取会话列表 |
| GET | `/api/conversations/{id}/messages` | 获取消息列表 |
| DELETE | `/api/conversations/{id}` | 删除会话 |

### 设置接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/settings/llm-mode` | 获取当前 LLM 模式（online / ollama） |
| PUT | `/api/settings/llm-mode?mode={online\|ollama}` | 切换 LLM 模式 |

## 项目结构

```
TOMOAssistant/
├── docker/
│   └── docker-compose.yml              # Milvus + MinIO + etcd + Attu
├── frontend/                           # Vue3 前端项目
│   ├── src/
│   │   ├── api/                        # API 请求模块 (axios + fetch SSE)
│   │   ├── components/
│   │   │   ├── chat/                   # 对话组件 (ChatInput, MessageBubble)
│   │   │   └── common/                 # 公共组件 (Sidebar)
│   │   ├── stores/                     # Pinia 状态管理
│   │   └── views/                      # 页面视图 (Chat, KnowledgeBase, Document)
│   └── package.json
├── src/main/java/com/tomo/tomoassistant/
│   ├── config/                         # 配置类
│   │   ├── properties/                 # 配置属性绑定 (Llm, Milvus, Minio, Rag)
│   │   └── LlmModelHolder.java        # LLM 模型管理 (在线/本地双模式切换)
│   ├── controller/                     # REST 控制器
│   ├── service/
│   │   ├── chat/                       # 对话 & 流式输出
│   │   ├── document/                   # 文档处理管线 (Tika 解析 + 切片)
│   │   ├── embedding/                  # 向量化 & Milvus 操作
│   │   ├── retrieval/                  # RAG 检索
│   │   ├── storage/                    # MinIO 文件存储
│   │   ├── multimodal/                 # 图片描述生成
│   │   └── memory/                     # 会话记忆管理 (Redis + MySQL)
│   ├── model/
│   │   ├── entity/                     # JPA 实体
│   │   ├── dto/                        # 请求 DTO
│   │   ├── vo/                         # 响应 VO
│   │   └── enums/                      # 枚举定义
│   ├── repository/                     # Spring Data JPA
│   └── common/
│       ├── result/                     # 统一响应封装
│       └── exception/                  # 全局异常处理
└── src/main/resources/
    ├── application.yml                 # 主配置
    ├── application-dev.yml             # 开发环境配置
    └── db/migration/
        └── V1__init_schema.sql         # 数据库初始化脚本
```

## 配置说明

### LLM 配置

系统支持两种运行模式，通过 `/api/settings/llm-mode` 接口或前端侧边栏切换：

**在线模式**：

```yaml
tomo:
  llm:
    chat:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-CHAT-MODEL-NAME>
    embedding:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-EMBEDDING-MODEL-NAME>
      dimension: 2048
    multimodal:
      base-url: <YOUR-MODEL-BUSE-URL>
      api-key: <YOUR-API-KEY>
      model-name: <YOUR-MULTI-MODEL-NAME>
```

**本地模式**（Ollama，数据不出域）：

```yaml
tomo:
  llm:
    ollama:
      base-url: http://localhost:11434/v1
      chat-model-name: <YOUR-CHAT-MODEL-NAME>
      embedding-model-name: <YOUR-EMBEDDING-MODEL-NAME>
      multimodal-model-name: <YOUR-MULTI-MODEL-NAME>
```

### RAG 配置

```yaml
tomo:
  rag:
    chunk-size: 512                 # 文本切片大小
    chunk-overlap: 64               # 切片重叠长度
    top-k: 5                        # 检索返回数量
    rerank:
      enabled: false                # 是否启用重排序
      base-url: http://localhost:8081
      model-name: bge-reranker-v2-m3
```

## 核心流程

### 文档处理管线

```
文档上传 -> MinIO 存储 -> Apache Tika 解析 -> 智能切片 -> Embedding 向量化 -> Milvus 存储
```

### 图片处理管线

```
图片上传 -> MinIO 存储 -> 多模态 LLM 生成描述 -> 描述文本向量化 -> Milvus 存储
```

### RAG 问答流程

```
用户提问 -> 查询向量化 -> Milvus 向量检索 -> (可选重排序) -> 构建 Prompt + 上下文 -> LLM 流式生成 -> SSE 返回结果 + 引用来源
```

### LLM 模式切换

```
用户切换模式 -> Redis 持久化模式选择 -> LlmModelHolder 重建模型实例 -> 后续对话使用新模式
```

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `knowledge_base` | 知识库元数据（名称、描述、Embedding 模型、切片参数等） |
| `document` | 文档记录（文件名、类型、大小、MinIO Key、切片数、处理状态） |
| `conversation` | 会话记录（标题、关联知识库、模型名称、消息数） |
| `message` | 消息记录（角色、内容、Token 数、图片元数据、引用信息） |

所有表支持逻辑删除（`deleted` 字段）。

## 许可证

私有项目 - 保留所有权利

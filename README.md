# 套料软件智能助手

基于 Spring Boot 3 + Spring AI + RAG + Tool Calling + MCP 协议构建的企业级套料软件智能助手系统。

## 功能特性

- **多智能体协作**: Router Agent、Technical Support Agent、System Diagnosis Agent、Knowledge Retrieval Agent、Operation Agent
- **RAG知识库**: 支持用户手册、FAQ、错误代码表、最佳实践等文档检索
- **Tool Calling**: 系统诊断工具、文件操作工具、知识检索工具、套料专用工具
- **对话记忆**: Redis存储会话上下文，支持多轮对话

## 技术栈

- Java 21
- Spring Boot 3.3.x
- Spring AI 1.0.0-M5
- PostgreSQL + PGVector
- Redis
- Maven

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- PostgreSQL 15+ (with PGVector extension)
- Redis 7+

### 本地运行

1. 克隆项目
```bash
git clone <repository-url>
cd nesting-assistant
```

2. 配置环境变量
```bash
export WENXIN_API_KEY=your-api-key
export DB_HOST=localhost
export REDIS_HOST=localhost
```

3. 运行项目
```bash
./mvnw spring-boot:run -pl nesting-assistant-starter
```

4. 访问接口
- API文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/api/v1/diagnosis/health

### Docker部署

```bash
docker-compose up -d
```

## API接口

### 聊天接口

```bash
POST /api/v1/chat
Content-Type: application/json

{
  "message": "套料利用率不高怎么办？",
  "conversationId": null,
  "userId": "user001"
}
```

### 知识库接口

```bash
# 添加FAQ
POST /api/v1/knowledge/faq
Content-Type: multipart/form-data

question=如何设置套料参数？
answer=套料参数设置建议...
tags=参数,配置
```

## 项目结构

```
nesting-assistant/
├── nesting-assistant-common/     # 公共模块
├── nesting-assistant-domain/     # 领域模块
├── nesting-assistant-memory/     # 记忆模块
├── nesting-assistant-rag/        # RAG模块
├── nesting-assistant-tools/      # 工具模块
├── nesting-assistant-mcp/        # MCP模块
├── nesting-assistant-agent/      # 智能体模块
├── nesting-assistant-api/        # API模块
└── nesting-assistant-starter/    # 启动模块
```

## 配置说明

主要配置项在 `application.yml` 中:

```yaml
nesting:
  assistant:
    system-prompt: |
      系统提示词...
    max-tool-calls: 15      # 最大工具调用次数
    max-rag-top-k: 10       # RAG检索结果数量
    conversation-timeout-minutes: 30  # 会话超时时间
```

## License

MIT License

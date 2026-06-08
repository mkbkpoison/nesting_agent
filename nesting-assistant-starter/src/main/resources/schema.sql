-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_username ON users(username);

-- ==================== 对话表 ====================
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    title VARCHAR(255),
    intent_type VARCHAR(32),
    agent_role VARCHAR(32),
    message_count INTEGER DEFAULT 0,
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_conversation_id ON conversations(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conv_user_id ON conversations(user_id);

-- ==================== 聊天消息表 ====================
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT,
    agent_role VARCHAR(32),
    tools_called TEXT,
    token_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_msg_conversation_id ON chat_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_created_at ON chat_messages(created_at);

-- ==================== 知识文档表 ====================
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(64) UNIQUE,
    title VARCHAR(255),
    content TEXT,
    doc_type VARCHAR(32),
    doc_code VARCHAR(64),
    category VARCHAR(64),
    tags VARCHAR(255),
    module VARCHAR(64),
    severity VARCHAR(16),
    solution TEXT,
    source VARCHAR(255),
    version VARCHAR(32),
    metadata TEXT,
    vector_id VARCHAR(64),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_doc_type ON knowledge_documents(doc_type);
CREATE INDEX IF NOT EXISTS idx_doc_code ON knowledge_documents(doc_code);

-- ==================== 诊断日志表 ====================
CREATE TABLE IF NOT EXISTS diagnostics_logs (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64),
    diagnosis_type VARCHAR(32),
    status VARCHAR(16),
    summary VARCHAR(500),
    details TEXT,
    recommendations TEXT,
    error_message TEXT,
    execution_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_diag_conversation_id ON diagnostics_logs(conversation_id);
CREATE INDEX IF NOT EXISTS idx_diag_type ON diagnostics_logs(diagnosis_type);

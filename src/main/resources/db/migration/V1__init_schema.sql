-- =============================================
-- TOMO Assistant 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- =============================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS tomo_assistant
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tomo_assistant;

-- =============================================
-- 1. 知识库表
-- =============================================
CREATE TABLE IF NOT EXISTS knowledge_base (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(100)    NOT NULL                COMMENT '知识库名称',
    description     VARCHAR(500)    DEFAULT NULL            COMMENT '知识库描述',
    embedding_model VARCHAR(100)    DEFAULT NULL            COMMENT '使用的Embedding模型',
    milvus_collection VARCHAR(200)  DEFAULT NULL            COMMENT 'Milvus Collection名称',
    chunk_size      INT             DEFAULT 512             COMMENT '文本切片大小',
    chunk_overlap   INT             DEFAULT 64              COMMENT '切片重叠长度',
    doc_count       INT             DEFAULT 0               COMMENT '文档数量',
    status          TINYINT         DEFAULT 1               COMMENT '状态: 1=启用, 0=禁用',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0               COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- =============================================
-- 2. 文档表
-- =============================================
CREATE TABLE IF NOT EXISTS document (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    knowledge_base_id BIGINT        NOT NULL                COMMENT '所属知识库ID',
    file_name       VARCHAR(255)    NOT NULL                COMMENT '存储文件名',
    original_name   VARCHAR(255)    NOT NULL                COMMENT '原始文件名',
    file_type       VARCHAR(50)     DEFAULT NULL            COMMENT '文件类型: PDF/DOCX/TXT/IMAGE',
    file_size       BIGINT          DEFAULT NULL            COMMENT '文件大小(字节)',
    minio_key       VARCHAR(500)    DEFAULT NULL            COMMENT 'MinIO对象Key',
    chunk_count     INT             DEFAULT 0               COMMENT '切片数量',
    status          TINYINT         DEFAULT 0               COMMENT '状态: 0=待处理, 1=处理中, 2=已完成, 3=处理失败',
    error_message   VARCHAR(1000)   DEFAULT NULL            COMMENT '错误信息',
    image_description TEXT          DEFAULT NULL            COMMENT '图片描述(多模态模型生成)',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0               COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_doc_kb_id (knowledge_base_id),
    INDEX idx_doc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- =============================================
-- 3. 会话表
-- =============================================
CREATE TABLE IF NOT EXISTS conversation (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title           VARCHAR(200)    DEFAULT NULL            COMMENT '会话标题',
    knowledge_base_id BIGINT        DEFAULT NULL            COMMENT '关联知识库ID(可为空)',
    model_name      VARCHAR(100)    DEFAULT NULL            COMMENT '使用的模型名称',
    system_prompt   TEXT            DEFAULT NULL            COMMENT '系统提示词',
    message_count   INT             DEFAULT 0               COMMENT '消息数量',
    last_message_at DATETIME        DEFAULT NULL            COMMENT '最后消息时间',
    status          TINYINT         DEFAULT 1               COMMENT '状态: 1=正常, 0=已归档',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0               COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_conv_kb_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- =============================================
-- 4. 消息表
-- =============================================
CREATE TABLE IF NOT EXISTS message (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT          NOT NULL                COMMENT '所属会话ID',
    role            VARCHAR(20)     NOT NULL                COMMENT '角色: USER/ASSISTANT/SYSTEM',
    content         TEXT            DEFAULT NULL            COMMENT '消息内容',
    token_count     INT             DEFAULT NULL            COMMENT 'Token数量',
    model_name      VARCHAR(100)    DEFAULT NULL            COMMENT '生成模型名称',
    metadata_json   JSON            DEFAULT NULL            COMMENT '元数据JSON(引用信息等)',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_msg_conv_id (conversation_id),
    INDEX idx_msg_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';


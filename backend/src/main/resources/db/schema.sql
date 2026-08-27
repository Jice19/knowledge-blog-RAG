-- =============================================================
-- AI 智识博客 · 数据库表结构（幂等，可重复执行）
-- 说明：由 spring.sql.init 在启动时自动执行
-- =============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt 加密)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名',
    `slug`        VARCHAR(50) NOT NULL COMMENT 'URL 标识',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    UNIQUE KEY `uk_slug` (`slug`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL COMMENT '标签名',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '标签表';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200) NOT NULL COMMENT '标题',
    `summary`     VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    `content`     LONGTEXT     NOT NULL COMMENT 'Markdown 正文',
    `cover`       VARCHAR(255) DEFAULT NULL COMMENT '封面 URL',
    `category_id` BIGINT       DEFAULT NULL COMMENT '分类 ID',
    `author_id`   BIGINT       NOT NULL COMMENT '作者 ID',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1已发布',
    `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `vector_status` TINYINT    NOT NULL DEFAULT 0 COMMENT '向量化状态: 0待入库 1入库中 2已入库 3失败',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_author` (`author_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章表';

-- 文章-标签关联表
CREATE TABLE IF NOT EXISTS `article_tag` (
    `article_id` BIGINT NOT NULL COMMENT '文章 ID',
    `tag_id`     BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (`article_id`, `tag_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章-标签关联表';

-- 会话窗口表
CREATE TABLE IF NOT EXISTS `conversation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '所属用户',
    `title`       VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话窗口表';

-- 会话消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id` BIGINT      NOT NULL COMMENT '所属会话',
    `role`            VARCHAR(20) NOT NULL COMMENT 'user / assistant',
    `content`         TEXT        NOT NULL COMMENT '消息内容',
    `references_json` TEXT        DEFAULT NULL COMMENT '引用来源 JSON',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`conversation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话消息表';

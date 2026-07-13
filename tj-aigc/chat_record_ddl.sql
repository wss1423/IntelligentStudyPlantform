CREATE TABLE `chat_record` (
    `id`              BIGINT NOT NULL COMMENT '数据id',
    `conversation_id` VARCHAR(64)  NOT NULL COMMENT '对话id',
    `user_id`         BIGINT NOT NULL COMMENT '用户id',
    `data`            TEXT NULL COMMENT '会话数据',
    `title`           VARCHAR(255) NULL COMMENT '对话标题',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creater`         BIGINT NULL COMMENT '创建人',
    `updater`         BIGINT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话记录';

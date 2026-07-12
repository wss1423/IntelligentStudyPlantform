package com.tianji.aigc.enums;

import lombok.Getter;

@Getter
public enum RedisKeyPrefixEnum {

    CHAT_GENERATE("chat:gen","聊天生成状态");

    private final String prefix;
    private final String description;

    RedisKeyPrefixEnum(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }

    public String key(String sessionId) {
        return prefix + sessionId;
    }
}

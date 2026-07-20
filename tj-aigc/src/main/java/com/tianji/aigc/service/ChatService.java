package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 聊天
     *
     * @param question  问题
     * @param sessionId 会话id
     * @return 回答内容
     */
    Flux<ChatEventVO> chat(String question, String sessionId);

    /**
     * 中断聊天
     * @param sessionId 会话ID
     */
    void stop(String sessionId);

    /**
     * 获取对话id，规则为用户id_会话id
     * @param sessionId
     * @return 对话id
     */
    static String getConversationId(String sessionId) {
        return UserContext.getUser() + "_" + sessionId;
    }

    /**
     * 文本对话
     *
     * @param question 问题
     * @return 回答
     */
    String chatText(String question);
}

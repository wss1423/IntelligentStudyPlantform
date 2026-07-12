package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.service.ChatRecordService;
import com.tianji.common.utils.CollUtils;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于JDBC的聊天记录存储
 */
public class JDBCChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private ChatRecordService chatRecordService;

    /**
     * 查询所有的对话id
     * @return
     */
    @Override
    public List<String> findConversationIds() {
        var conversationList = this.chatRecordService.lambdaQuery()
            .select(ChatRecord::getConversationId)
            .list();
        return CollStreamUtil.toList(conversationList,ChatRecord::getConversationId);
    }

    /**
     * 根据对话id查询某个对话的
     * @param conversationId
     * @return
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        var chatRecordList = this.chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId,conversationId)
                .list();
        return CollStreamUtil.toList(chatRecordList,chatRecord -> MessageUtil.toMessage(chatRecord.getData()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 先删掉根据conversationId的对话
        deleteByConversationId(conversationId);
        // 通过对话ID来截取用户id
        var userId = Convert.toLong(StrUtil.subBefore(conversationId, "_", true));
        // 然后保存新的对话记录
        var chatRecordList = CollStreamUtil.toList(messages, message -> ChatRecord.builder()
                .data(MessageUtil.toJson(message))
                .conversationId(conversationId)
                .userId(userId)
                .creater(userId)
                .updater(userId)
                .build()
        );
        this.chatRecordService.saveBatch(chatRecordList);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        var query = Wrappers.<ChatRecord>lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId);
        this.chatRecordService.remove(query);
    }
}

package com.tianji.aigc.memory.mongodb;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.memory.MessageUtil;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * 基于MongoDB的聊天记录存储
 */
public class MongoDBChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 查询所有的对话
     * @return
     */
    @Override
    public List<String> findConversationIds() {
        var chatRecordList = mongoTemplate.findAll(ChatRecord.class);
        return CollStreamUtil.toList(chatRecordList,ChatRecord::getConversationId);
    }

    /**
     * 根据对话id查询某个对话的
     * @param conversationId
     * @return
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));

        var chatRecord = this.mongoTemplate.findOne(null,ChatRecord.class);

        if (chatRecord == null) {
            return List.of();
        }

        return CollStreamUtil.toList(chatRecord.getMessage(), MessageUtil::toMessage);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 先删掉根据conversationId的对话
        deleteByConversationId(conversationId);
        // 构造 chatRecord对象
        var chatRecord = ChatRecord.builder()
                .conversationId(conversationId)
                .message(CollStreamUtil.toList(messages,MessageUtil::toJson))
                .build();
        // 保存到MongoDB中
        this.mongoTemplate.save(chatRecord);

    }

    @Override
    public void deleteByConversationId(String conversationId) {
        mongoTemplate.remove(Query.query(Criteria.where("conversationId").is(conversationId)), ChatRecord.class);
    }
}

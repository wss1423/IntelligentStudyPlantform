package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.MyAssistantMessage;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final SessionProperties sessionProperties;

    private final ChatMemory chatMemory;

    @Override
    public SessionVO createSession(Integer num) {
        var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));

//        // 随机生成三个热门问题
//        sessionVO.setExamples(RandomUtil.randomEleList(this.sessionProperties.getExamples(),num));

        // 随机生成sessionId
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());

        // 构建持久化对象，并持久化
        var chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        super.save(chatSession);

        return sessionVO;
    }

    @Override
    public List<SessionVO.Example> getHotSessions(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(),num);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 根据会话Id获取对话id
        String conversationId = ChatService.getConversationId(sessionId);
        // 从Redis里面获取历史消息
        List<Message> messageList = this.chatMemory.get(conversationId);
        // 过滤并且转换消息列表
        return StreamUtil.of(messageList)
                // 过滤掉非用户消息和助手消息
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                // 转换为MessageVO对象
                .map(message -> {
                    if (message instanceof MyAssistantMessage) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((MyAssistantMessage) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .toList();
    }

    @Async // 异步操作
    @Override
    public void update(String sessionId, String title, Long userId) {
        // 获取会话列表
        var chatSessionList = super.lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .list();
        // 若为空则直接结束
        if (CollUtils.isEmpty(chatSessionList)) {
            return;
        }
        // 获取第一个会话的对象数据
        var chatSession = chatSessionList.get(0);

        if (StrUtil.isEmpty(chatSession.getTitle()) && StrUtil.isNotEmpty(title)) {
            // 更新会话标题 (如果标题长度超过100个字符，则截取100个字符)
            chatSession.setTitle(StrUtil.sub(title,0,100));
        }

        // 设置更新时间
        chatSession.setUpdateTime(LocalDateTime.now());

        // 更新数据
        super.updateById(chatSession);
    }

    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        var userId = UserContext.getUser();
        // 查询历史会话，限制返回条数
        var list = super.lambdaQuery()
                .eq(ChatSession::getUserId, UserContext.getUser())
                .isNotNull(ChatSession::getTitle)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT 30")
                .list();

        if (CollUtil.isEmpty(list)) {
            log.info("No chat sessions found for user: {}", userId);
            return Map.of();
        }


        // 转换为 ChatSessionVO 列表
        var chatSessionVOS = CollStreamUtil.toList(list, chatSession ->
                ChatSessionVO.builder()
                        .sessionId(chatSession.getSessionId())
                        .title(chatSession.getTitle())
                        .updateTime(chatSession.getUpdateTime())
                        .build()
        );

        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";

        // 当前时间
        var now = LocalDateTime.now().toLocalDate();

        // 按照更新时间分组
        return CollStreamUtil.groupByKey(chatSessionVOS, vo -> {
            // 计算两个日期之间的天数差
            long between = Math.abs(ChronoUnit.DAYS.between(vo.getUpdateTime().toLocalDate(), now));
            if (between == 0) {
                return TODAY;
            } else if (between <= 30) {
                return LAST_30_DAYS;
            } else if (between <= 365) {
                return LAST_YEAR;
            } else {
                return MORE_THAN_YEAR;
            }
        });
    }

    @Override
    public void deleteHistorySession(String sessionId) {
        //删除数据库的数据
        var queryWrapper = Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser());
        super.remove(queryWrapper);

        //删除redis中的数据
        var conversationId = ChatService.getConversationId(sessionId);
        this.chatMemory.clear(conversationId);
    }

    @Override
    public void updateTitle(String sessionId, String title) {
        //更新数据
        super.lambdaUpdate()
                // 设置更新条件, 更新字段为title(最多设置前100个字符)，更新条件为sessionId和userId
                .set(ChatSession::getTitle, StrUtil.sub(title, 0, 100))
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser())
                .update();
    }
}

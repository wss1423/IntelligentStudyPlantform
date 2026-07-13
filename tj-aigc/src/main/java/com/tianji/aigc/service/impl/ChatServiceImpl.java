package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.enums.RedisKeyPrefixEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    private final SystemPromptConfig systemPromptConfig;

    // 存储大模型的生成状态，这里采用ConcurrentHashMap是确保线程安全
    // 目前的版本暂时用Map实现，如果考虑分布式环境的话，可以考虑用redis来实现
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();
    // 优化为使用Redis来实现 0710 发现存在bug，暂时回退至不使用Redis的版本 0712已修复
    private final StringRedisTemplate redisTemplate;

    private final ChatMemory chatMemory;

    private final static String GENERATE_STATE_KEY = "GENERATE_STATE";

    // 输出结束的标记
    private final static  ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 如果会话id为空，则自动生成
        boolean isNewSession = StrUtil.isBlank(sessionId);
        if (isNewSession) {
            sessionId = IdUtil.fastSimpleUUID();
        }
        // 获取对话id
        var conversationId = ChatService.getConversationId(sessionId);
        // 确保lambda中捕获的sessionId是最终变量
        var finalSessionId = sessionId;
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        // 生成请求id
        var requestId = IdUtil.fastSimpleUUID();
        // 获取用户id
        var userId = UserContext.getUser();

        var hashOps = this.redisTemplate.boundHashOps(GENERATE_STATE_KEY);
        var mainFlux = this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(this.systemPromptConfig.getChatSystemMessage().get()) // 设置系统提示语
                        .param("now", DateUtil.now()) // 设置当前时间的参数
                )
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
//                .toolContext(Map.of(Constant.REQUEST_ID, requestId))//通过工具上下文传递参数
                .toolContext(Map.of(Constant.REQUEST_ID,requestId,Constant.USER_ID,userId))
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() -> { //输出开始，标记正在输出
                    hashOps.put(finalSessionId, "true");
                })
                .doOnComplete(() -> { //输出结束，清除标记
                    hashOps.delete(finalSessionId);
                })
                .doOnError(throwable -> hashOps.delete(finalSessionId)) // 错误时清除标记
                .doOnCancel(() -> {
                    // 当输出被取消时，保存输出的内容到历史记录中
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                // 输出过程中，判断是否正在输出，如果正在输出，则继续输出，否则结束输出
                .takeWhile(s -> hashOps.get(finalSessionId) != null)
                .map(chatResponse -> {
                    // 对于响应结果进行处理，如果是最后一条数据，就把此次消息id放到内存中
                    // 主要是用于存储消息数据到Redis中，可以根据消息id获取的请求id，在通过请求id就可以获取到参数列表了
                    // 从而解决在历史聊天记录里面没有外参数的问题
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP,finishReason)) {
                        var messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId,Constant.REQUEST_ID,requestId);
                    }
                    // 获取大模型的输出的内容
                    var text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(text);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
//                .concatWith(
//                        Flux.just(ChatEventVO.builder()  // 标记输出结束
//                        .eventType(ChatEventTypeEnum.STOP.getValue())
//                        .build())
//                );
                .concatWith(
                        Flux.defer(() -> {
                            // 通过请求id获取到参数列表，如果不为空，那么就将其追加到返回结果
                            var map = ToolResultHolder.get(requestId);
                            if (CollUtils.isNotEmpty(map)) {
                                // 清除参数列表
                                ToolResultHolder.remove(requestId);

                                // 响应给前端的参数数据
                                var chatEventVO = ChatEventVO.builder()
                                        .eventData(map)
                                        .eventType(ChatEventTypeEnum.PARAM.getValue())
                                        .build();
                                return Flux.just(chatEventVO,STOP_EVENT);
                            }
                            return Flux.just(STOP_EVENT);
                        })
                );

        // 如果是新生成的会话id，则在流的最前面发送一个会话事件，方便前端获取会话id
        if (isNewSession) {
            var sessionEvent = ChatEventVO.builder()
                    .eventData(sessionId)
                    .eventType(ChatEventTypeEnum.SESSION.getValue())
                    .build();
            return Flux.concat(Flux.just(sessionEvent), mainFlux);
        }
        return mainFlux;
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

    @Override
    public void stop(String sessionId) {
        // 移除标记
        var hashOps = this.redisTemplate.boundHashOps(GENERATE_STATE_KEY);
        hashOps.delete(sessionId);
    }
}

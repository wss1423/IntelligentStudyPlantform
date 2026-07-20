package com.tianji.aigc.controller;

import com.tianji.aigc.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.annotations.NoWrapper;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.awt.*;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class chatController {

    private final ChatService chatService;

    @NoWrapper // 标记结果不进行包装 因为不能把R类型转成字符串返回
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return this.chatService.chat(chatDTO.getQuestion(),chatDTO.getSessionId());
    }

    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId) {
        this.chatService.stop(sessionId);
    }

    @PostMapping("/text")
    public String chatText(@RequestBody String question) {
        return this.chatService.chatText(question);
    }
}

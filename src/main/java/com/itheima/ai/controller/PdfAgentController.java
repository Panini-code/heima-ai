package com.itheima.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.ai.dto.AgentEvent;
import com.itheima.ai.repository.ChatHistoryRepository;
import com.itheima.ai.service.ReActAgentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf/agent")
public class PdfAgentController {

    private static final Logger log = LoggerFactory.getLogger(PdfAgentController.class);

    private final ReActAgentService reActAgentService;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String prompt,
            @RequestParam String chatId,
            @RequestParam(required = false) String fileName) {

        log.info("🔍 Agent Chat: prompt=\"{}\" chatId=\"{}\" fileName=\"{}\"", prompt, chatId, fileName);
        chatHistoryRepository.save("pdf", chatId);

        return reActAgentService.pdfAgentChat(prompt, chatId)
                .map(this::eventToJson)
                .doOnError(err -> log.error("Agent 流处理异常", err))
                .onErrorResume(err -> Flux.just(eventToJson(new AgentEvent("error", err.getMessage(), null, null, null))));
    }

    private String eventToJson(AgentEvent event) {
        try {
            return objectMapper.writeValueAsString(event) + "\n";
        } catch (Exception e) {
            log.error("序列化事件失败", e);
            return "{\"type\":\"error\",\"content\":\"序列化失败\"}\n";
        }
    }
}

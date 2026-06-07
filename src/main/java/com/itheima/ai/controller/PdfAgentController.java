package com.itheima.ai.controller;

import com.itheima.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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

    private final ChatClient pdfAgentChatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam String prompt,
            @RequestParam String chatId) {

        log.info("Agent PDF Chat: prompt='{}' chatId='{}'", prompt, chatId);
        chatHistoryRepository.save("pdf", chatId);

        return pdfAgentChatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}

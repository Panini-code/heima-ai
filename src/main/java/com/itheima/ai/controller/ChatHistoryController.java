package com.itheima.ai.controller;

import com.itheima.ai.entity.vo.MessageVO;
import com.itheima.ai.repository.ChatHistoryRepository;
import com.itheima.ai.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryController.class);

    private final ChatHistoryRepository chatHistoryRepository;

    private final ChatMemory chatMemory;

    private final FileRepository fileRepository;

    private final StringRedisTemplate redisTemplate;

    private final VectorStore vectorStore;

    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return chatHistoryRepository.getChatIds(type);
    }

    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        List<Message> messages = chatMemory.get(chatId, Integer.MAX_VALUE);
        if(messages == null) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).toList();
    }

    @DeleteMapping("/{type}/{chatId}")
    public void deleteChat(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        log.info("\u5220\u9664\u4f1a\u8bdd: type={}, chatId={}", type, chatId);
        chatMemory.clear(chatId);
        if ("pdf".equals(type)) { cleanupPdfChat(chatId); }
        redisTemplate.opsForSet().remove("chat:history:" + type, chatId);
    }
    private void cleanupPdfChat(String chatId) {
        List<String> files = fileRepository.listFiles(chatId);
        for (String filename : files) {
            deleteVectorsByFileName(filename);
            fileRepository.deleteFile(chatId, filename);
        }
        redisTemplate.delete("chat:pdf:files:" + chatId);
    }
    private void deleteVectorsByFileName(String filename) {
        String vectorKey = "chat:pdf:vectors:" + filename;
        Set<String> docIds = redisTemplate.opsForSet().members(vectorKey);
        if (docIds != null && !docIds.isEmpty()) {
            vectorStore.delete(List.copyOf(docIds));
            redisTemplate.delete(vectorKey);
        }
    }
}
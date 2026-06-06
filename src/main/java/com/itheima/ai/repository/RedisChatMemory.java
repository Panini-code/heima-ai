package com.itheima.ai.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.ai.entity.po.Msg;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        for (Message message : messages) {
            try {
                Msg msg = new Msg(message);
                String json = objectMapper.writeValueAsString(msg);
                redisTemplate.opsForList().rightPush(key, json);
            } catch (Exception e) {
                log.error("序列化消息失败, conversationId: {}", conversationId, e);
                throw new RuntimeException("序列化消息失败", e);
            }
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return List.of();
        }
        long start;
        long end = -1;
        if (lastN > 0 && lastN < size) {
            start = size - lastN;
        } else {
            start = 0;
        }
        List<String> jsonList = redisTemplate.opsForList().range(key, start, end);
        if (jsonList == null || jsonList.isEmpty()) {
            return List.of();
        }
        return jsonList.stream()
                .map(json -> {
                    try {
                        Msg msg = objectMapper.readValue(json, Msg.class);
                        return msg.toMessage();
                    } catch (Exception e) {
                        log.error("反序列化消息失败, conversationId: {}", conversationId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
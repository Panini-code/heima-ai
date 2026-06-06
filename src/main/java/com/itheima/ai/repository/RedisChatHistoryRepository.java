package com.itheima.ai.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisChatHistoryRepository implements ChatHistoryRepository {

    private static final String KEY_PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String type, String chatId) {
        redisTemplate.opsForSet().add(KEY_PREFIX + type, chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
        Set<String> chatIds = redisTemplate.opsForSet().members(KEY_PREFIX + type);
        return chatIds == null ? List.of() : new ArrayList<>(chatIds);
    }
}
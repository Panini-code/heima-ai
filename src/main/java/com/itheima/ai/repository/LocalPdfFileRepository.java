package com.itheima.ai.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPdfFileRepository implements FileRepository {

    private static final String KEY_PREFIX = "chat:pdf:file:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean save(String chatId, Resource resource) {
        // 1.保存PDF文件到本地磁盘
        String filename = resource.getFilename();
        try {
            File target = new File(Objects.requireNonNull(filename));
            if (!target.exists()) {
                Files.copy(resource.getInputStream(), target.toPath());
            }
            // 2.保存 chatId 与文件名的映射关系到 Redis
            redisTemplate.opsForValue().set(KEY_PREFIX + chatId, filename);
            return true;
        } catch (IOException e) {
            log.error("保存PDF文件失败", e);
            return false;
        }
    }

    @Override
    public Resource getFile(String chatId) {
        String filename = redisTemplate.opsForValue().get(KEY_PREFIX + chatId);
        return filename == null ? new FileSystemResource("") : new FileSystemResource(filename);
    }
}
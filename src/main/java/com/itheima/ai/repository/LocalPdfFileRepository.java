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
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPdfFileRepository implements FileRepository {

    private static final String KEY_PREFIX = "chat:pdf:files:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean save(String chatId, Resource resource) {
        String filename = resource.getFilename();
        try {
            File target = new File(Objects.requireNonNull(filename));
            if (!target.exists()) {
                Files.copy(resource.getInputStream(), target.toPath());
            }
            // 将文件名添加到会话的文件列表（Redis List，自动去重检查）
            String key = KEY_PREFIX + chatId;
            if (!redisTemplate.opsForList().range(key, 0, -1).contains(filename)) {
                redisTemplate.opsForList().rightPush(key, filename);
            }
            return true;
        } catch (IOException e) {
            log.error("保存PDF文件失败", e);
            return false;
        }
    }

    @Override
    public Resource getFile(String chatId) {
        List<String> files = listFiles(chatId);
        if (files.isEmpty()) {
            return new FileSystemResource("");
        }
        return new FileSystemResource(files.get(0));
    }

    @Override
    public Resource getFile(String chatId, String filename) {
        return new FileSystemResource(filename);
    }

    @Override
    public List<String> listFiles(String chatId) {
        String key = KEY_PREFIX + chatId;
        List<String> files = redisTemplate.opsForList().range(key, 0, -1);
        return files == null ? List.of() : files;
    }

    @Override
    public boolean deleteFile(String chatId, String filename) {
        String key = KEY_PREFIX + chatId;
        // 从 Redis 列表中移除文件名
        redisTemplate.opsForList().remove(key, 0, filename);
        // 删除本地文件
        File file = new File(filename);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                log.warn("删除本地文件失败: {}", filename, e);
            }
        }
        log.info("已删除文件: chatId={}, filename={}", chatId, filename);
        return true;
    }
}

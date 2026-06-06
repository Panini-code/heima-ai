package com.itheima.ai.repository;

import org.springframework.core.io.Resource;

import java.util.List;

public interface FileRepository {

    boolean save(String chatId, Resource resource);

    Resource getFile(String chatId);

    /** 根据会话和文件名获取文件资源 */
    Resource getFile(String chatId, String filename);

    List<String> listFiles(String chatId);

    boolean deleteFile(String chatId, String filename);
}
package com.nesting.assistant.memory.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话记忆服务 - 基于Redis实现
 * 按 userId 隔离存储，key 格式: nesting:memory:{userId}:{conversationId}
 */
@Slf4j
@Service
public class ConversationMemoryService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String MEMORY_KEY_PREFIX = "nesting:memory:";
    private static final Duration MEMORY_TTL = Duration.ofHours(24);
    private static final int MAX_HISTORY_SIZE = 50;

    private String buildKey(String userId, String conversationId) {
        String uid = (userId != null && !userId.isEmpty()) ? userId : "default";
        return MEMORY_KEY_PREFIX + uid + ":" + conversationId;
    }

    /**
     * 添加消息到对话历史
     */
    public void addMessage(String userId, String conversationId, Message message) {
        String key = buildKey(userId, conversationId);
        MessageEntry entry = toEntry(message);
        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.expire(key, MEMORY_TTL);
        trimHistory(userId, conversationId);
        log.debug("Added message to conversation: userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 获取对话历史
     */
    public List<Message> getHistory(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        List<Object> entries = redisTemplate.opsForList().range(key, 0, -1);
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        return entries.stream()
                .map(obj -> (MessageEntry) obj)
                .map(this::toMessage)
                .toList();
    }

    /**
     * 获取最近N条消息
     */
    public List<Message> getRecentMessages(String userId, String conversationId, int limit) {
        String key = buildKey(userId, conversationId);
        long size = redisTemplate.opsForList().size(key);
        long start = Math.max(0, size - limit);
        List<Object> entries = redisTemplate.opsForList().range(key, start, -1);
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        return entries.stream()
                .map(obj -> (MessageEntry) obj)
                .map(this::toMessage)
                .toList();
    }

    /**
     * 清除对话历史
     */
    public void clearHistory(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        redisTemplate.delete(key);
        log.info("Cleared conversation history: userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 检查对话是否存在
     */
    public boolean exists(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }

    /**
     * 获取对话消息数量
     */
    public long getMessageCount(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    /**
     * 更新对话TTL
     */
    public void refreshTtl(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        redisTemplate.expire(key, MEMORY_TTL);
    }

    private void trimHistory(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY_SIZE) {
            long trimCount = size - MAX_HISTORY_SIZE;
            for (int i = 0; i < trimCount; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
            log.debug("Trimmed {} messages from conversation: userId={}, conversationId={}",
                    trimCount, userId, conversationId);
        }
    }

    private MessageEntry toEntry(Message message) {
        MessageEntry entry = new MessageEntry();
        entry.setRole(message.getMessageType().getValue());
        entry.setContent(message.getText());
        return entry;
    }

    private Message toMessage(MessageEntry entry) {
        return switch (entry.getRole().toLowerCase()) {
            case "user" -> new UserMessage(entry.getContent());
            case "assistant" -> new AssistantMessage(entry.getContent());
            case "system" -> new SystemMessage(entry.getContent());
            default -> new UserMessage(entry.getContent());
        };
    }

    /**
     * 消息条目（用于Redis序列化）
     */
    @lombok.Data
    public static class MessageEntry {
        private String role;
        private String content;
    }
}

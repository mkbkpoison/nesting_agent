package com.nesting.assistant.memory.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nesting.assistant.common.util.IdGenerator;
import com.nesting.assistant.domain.entity.ChatMessage;
import com.nesting.assistant.domain.entity.Conversation;
import com.nesting.assistant.domain.repository.ChatMessageMapper;
import com.nesting.assistant.domain.repository.ConversationMapper;
import com.nesting.assistant.memory.service.ConversationMemoryService;
import com.nesting.assistant.memory.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 多用户对话管理器
 * 协调 MyBatis-Plus（持久化）和 Redis（快速访问）之间的对话生命周期
 *
 * 写入策略：DB + Redis 双写
 * 读取策略：优先 Redis，Redis 无数据则从 DB 恢复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationManager {

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ConversationMemoryService memoryService;
    private final ConversationStateService stateService;

    // ==================== 会话生命周期 ====================

    @Transactional
    public String createOrGetConversation(String userId, String conversationId, String title) {
        if (conversationId != null && !conversationId.isEmpty()) {
            Conversation existing = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
            if (existing != null) {
                return conversationId;
            }
            log.warn("Conversation {} does not belong to user {}, creating new", conversationId, userId);
        }

        String newId = IdGenerator.conversationId();
        Conversation conv = Conversation.builder()
                .conversationId(newId)
                .userId(userId != null ? userId : "default")
                .title(title != null ? title.substring(0, Math.min(title.length(), 200)) : "新对话")
                .messageCount(0)
                .status("active")
                .build();
        conversationMapper.insert(conv);
        log.info("Created new conversation: userId={}, conversationId={}", userId, newId);
        return newId;
    }

    // ==================== 消息写入（双写：DB + Redis） ====================

    @Transactional
    public void addUserMessage(String userId, String conversationId, String content) {
        // 1. DB 持久化
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId)
                .role(ChatMessage.Role.USER.name().toLowerCase())
                .content(content)
                .build();
        chatMessageMapper.insert(msg);

        // 2. Redis 缓存
        memoryService.addMessage(userId, conversationId,
                new org.springframework.ai.chat.messages.UserMessage(content));

        // 3. 更新会话元数据
        Conversation conv = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
        if (conv != null) {
            conv.setMessageCount(conv.getMessageCount() != null ? conv.getMessageCount() + 1 : 1);
            if (conv.getMessageCount() == 1 && "新对话".equals(conv.getTitle())) {
                String t = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                conv.setTitle(t);
            }
            conversationMapper.updateById(conv);
        }
    }

    @Transactional
    public void addAssistantMessage(String userId, String conversationId, String content,
                                     String agentRole, String intentType) {
        // 1. DB 持久化
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId)
                .role(ChatMessage.Role.ASSISTANT.name().toLowerCase())
                .content(content)
                .agentRole(agentRole)
                .build();
        chatMessageMapper.insert(msg);

        // 2. Redis 缓存
        memoryService.addMessage(userId, conversationId,
                new org.springframework.ai.chat.messages.AssistantMessage(content));

        // 3. 更新会话元数据
        Conversation conv = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
        if (conv != null) {
            conv.setMessageCount(conv.getMessageCount() != null ? conv.getMessageCount() + 1 : 1);
            conv.setAgentRole(agentRole);
            conv.setIntentType(intentType);
            conversationMapper.updateById(conv);
        }
    }

    // ==================== 消息读取（Redis → DB 降级） ====================

    public List<Message> getConversationHistory(String userId, String conversationId) {
        // 验证会话属于该用户
        Conversation conv = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
        if (conv == null) {
            log.warn("Conversation not found: userId={}, conversationId={}", userId, conversationId);
            return List.of();
        }

        // 优先从 Redis 读取（快）
        List<Message> redisMessages = memoryService.getHistory(userId, conversationId);
        if (!redisMessages.isEmpty()) {
            return redisMessages;
        }

        // Redis 无数据（TTL 过期），从 DB 恢复
        log.info("Redis cache miss, loading from DB: conversationId={}", conversationId);
        List<ChatMessage> dbMessages = chatMessageMapper.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> messages = new ArrayList<>();
        for (ChatMessage cm : dbMessages) {
            Message msg = switch (cm.getRole().toLowerCase()) {
                case "user" -> new org.springframework.ai.chat.messages.UserMessage(cm.getContent());
                case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(cm.getContent());
                case "system" -> new org.springframework.ai.chat.messages.SystemMessage(cm.getContent());
                default -> new org.springframework.ai.chat.messages.UserMessage(cm.getContent());
            };
            messages.add(msg);
            memoryService.addMessage(userId, conversationId, msg);
        }

        log.info("Restored {} messages from DB to Redis for conversationId={}",
                messages.size(), conversationId);
        return messages;
    }

    // ==================== 对话管理 ====================

    public List<Conversation> getUserConversations(String userId) {
        return conversationMapper.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public void deleteConversation(String userId, String conversationId) {
        Conversation conv = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
        if (conv != null) {
            chatMessageMapper.deleteByConversationId(conversationId);
            memoryService.clearHistory(userId, conversationId);
            stateService.clearState(userId, conversationId);
            conversationMapper.deleteById(conv.getId());
            log.info("Deleted conversation: userId={}, conversationId={}", userId, conversationId);
        }
    }

    @Transactional
    public void renameConversation(String userId, String conversationId, String newTitle) {
        Conversation conv = conversationMapper.findByConversationIdAndUserId(conversationId, userId);
        if (conv != null) {
            conv.setTitle(newTitle);
            conversationMapper.updateById(conv);
            log.info("Renamed conversation: userId={}, conversationId={}", userId, conversationId);
        }
    }

    public void refreshTtl(String userId, String conversationId) {
        memoryService.refreshTtl(userId, conversationId);
        stateService.refreshTtl(userId, conversationId);
    }

    public String getOrCreateDefaultConversation(String userId) {
        if (userId == null || userId.isEmpty()) {
            userId = "default";
        }
        List<Conversation> convs = conversationMapper.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "active");
        if (!convs.isEmpty()) {
            return convs.get(0).getConversationId();
        }
        return createOrGetConversation(userId, null, "新对话");
    }
}

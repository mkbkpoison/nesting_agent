package com.nesting.assistant.memory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nesting.assistant.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话状态服务 - 管理智能体间的上下文传递
 * 按 userId 隔离存储
 */
@Slf4j
@Service
public class ConversationStateService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final String STATE_KEY_PREFIX = "nesting:state:";
    private static final String CONTEXT_KEY_PREFIX = "nesting:context:";
    private static final Duration STATE_TTL = Duration.ofHours(24);

    private String stateKey(String userId, String conversationId, String agentRole) {
        return STATE_KEY_PREFIX + userId + ":" + conversationId + ":" + agentRole;
    }

    private String contextKey(String userId, String conversationId) {
        return CONTEXT_KEY_PREFIX + userId + ":" + conversationId;
    }

    private String intentKey(String userId, String conversationId) {
        return CONTEXT_KEY_PREFIX + userId + ":" + conversationId + ":intent";
    }

    /**
     * 保存智能体状态
     */
    public void saveAgentState(String userId, String conversationId, String agentRole, Map<String, Object> state) {
        String key = stateKey(userId, conversationId, agentRole);
        String json = JsonUtils.toJson(state);
        redisTemplate.opsForValue().set(key, json, STATE_TTL);
        log.debug("Saved agent state: userId={}, conversationId={}, agent={}", userId, conversationId, agentRole);
    }

    /**
     * 获取智能体状态
     */
    public Map<String, Object> getAgentState(String userId, String conversationId, String agentRole) {
        String key = stateKey(userId, conversationId, agentRole);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return new HashMap<>();
        }
        return JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 设置对话上下文
     */
    public void setConversationContext(String userId, String conversationId, String context) {
        String key = contextKey(userId, conversationId);
        redisTemplate.opsForValue().set(key, context, STATE_TTL);
        log.debug("Set conversation context: userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 获取对话上下文
     */
    public String getConversationContext(String userId, String conversationId) {
        String key = contextKey(userId, conversationId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 保存当前意图
     */
    public void setCurrentIntent(String userId, String conversationId, String intent) {
        String key = intentKey(userId, conversationId);
        redisTemplate.opsForValue().set(key, intent, STATE_TTL);
    }

    /**
     * 获取当前意图
     */
    public String getCurrentIntent(String userId, String conversationId) {
        String key = intentKey(userId, conversationId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 清除对话状态
     */
    public void clearState(String userId, String conversationId) {
        String pattern = STATE_KEY_PREFIX + userId + ":" + conversationId + "*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        String contextPattern = CONTEXT_KEY_PREFIX + userId + ":" + conversationId + "*";
        var contextKeys = redisTemplate.keys(contextPattern);
        if (contextKeys != null && !contextKeys.isEmpty()) {
            redisTemplate.delete(contextKeys);
        }
        log.info("Cleared conversation state: userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 刷新状态TTL
     */
    public void refreshTtl(String userId, String conversationId) {
        String pattern = STATE_KEY_PREFIX + userId + ":" + conversationId + "*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null) {
            keys.forEach(key -> redisTemplate.expire(key, STATE_TTL));
        }
    }
}

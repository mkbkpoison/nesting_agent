package com.nesting.assistant.agent.orchestration;

import com.nesting.assistant.agent.core.AgentContext;
import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.impl.RouterAgent;
import com.nesting.assistant.memory.service.ConversationMemoryService;
import com.nesting.assistant.memory.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 智能体编排器
 * 负责协调多个智能体之间的协作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final RouterAgent routerAgent;
    private final ConversationMemoryService memoryService;
    private final ConversationStateService stateService;

    /**
     * 编排处理用户请求
     */
    public AgentResponse orchestrate(String conversationId, String userId, String userMessage) {
        log.info("Orchestrating request: userId={}, conversationId={}, message={}",
                userId, conversationId, userMessage);

        String uid = (userId != null && !userId.isEmpty()) ? userId : "default";

        // 构建上下文
        AgentContext context = AgentContext.builder()
                .conversationId(conversationId)
                .userId(uid)
                .userMessage(userMessage)
                .history(memoryService.getHistory(uid, conversationId))
                .sharedData(new HashMap<>())
                .toolCallResults(new ArrayList<>())
                .build();

        // 恢复之前的对话状态
        String savedIntent = stateService.getCurrentIntent(uid, conversationId);
        if (savedIntent != null) {
            context.addSharedData("previousIntent", savedIntent);
        }

        // 通过路由智能体处理
        AgentResponse response = routerAgent.process(context);

        // 保存对话状态
        if (response.getIntentType() != null) {
            stateService.setCurrentIntent(uid, conversationId, response.getIntentType());
        }

        // 更新响应信息
        response.setConversationId(conversationId);

        return response;
    }

    /**
     * 清除会话状态
     */
    public void clearConversation(String userId, String conversationId) {
        String uid = (userId != null && !userId.isEmpty()) ? userId : "default";
        memoryService.clearHistory(uid, conversationId);
        stateService.clearState(uid, conversationId);
        log.info("Cleared conversation: userId={}, conversationId={}", uid, conversationId);
    }

    /**
     * 刷新会话TTL
     */
    public void refreshConversation(String userId, String conversationId) {
        String uid = (userId != null && !userId.isEmpty()) ? userId : "default";
        memoryService.refreshTtl(uid, conversationId);
        stateService.refreshTtl(uid, conversationId);
    }
}

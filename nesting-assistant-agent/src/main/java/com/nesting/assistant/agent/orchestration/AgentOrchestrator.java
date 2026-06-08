package com.nesting.assistant.agent.orchestration;

import com.nesting.assistant.agent.core.AgentContext;
import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.impl.RouterAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 智能体编排器
 * 构建上下文并交给 RouterAgent（监督者）处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final RouterAgent routerAgent;

    /**
     * 编排处理用户请求
     */
    public AgentResponse orchestrate(String conversationId, String userId, String userMessage) {
        log.info("Orchestrating request: userId={}, conversationId={}, message={}",
                userId, conversationId, userMessage);

        AgentContext context = AgentContext.builder()
                .conversationId(conversationId)
                .userId(userId != null ? userId : "default")
                .userMessage(userMessage)
                .sharedData(new HashMap<>())
                .toolCallResults(new ArrayList<>())
                .build();

        AgentResponse response = routerAgent.process(context);
        response.setConversationId(conversationId);
        return response;
    }
}

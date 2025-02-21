package com.twelvet.ai.server.service;

import com.twelvet.ai.server.model.ChatSession;

public interface ChatSessionService {
    ChatSession createSession(String userId);
    ChatSession getSessionById(String sessionId);
    boolean deleteSession(String sessionId);
    boolean updateSessionState(String sessionId, String newState);
    boolean clearSessionChatMemory(String sessionId);
}

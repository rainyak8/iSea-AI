package com.twelvet.ai.server.service.impl;

import com.twelvet.ai.server.model.ChatSession;
import com.twelvet.ai.server.service.ChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {
    // 简单的存储方式
    // 后面可以持久化
    private final Map<String, ChatSession> activeSessions = new HashMap<>();

    @Override
    public ChatSession createSession(String userId) {
        try{
            // 生成一个唯一的 sessionId
            String sessionId = UUID.randomUUID().toString();
            // 创建一个新的会话
            ChatSession session = new ChatSession(sessionId, userId);
            activeSessions.put(sessionId, session);  // 存储会话
            return session;
        } catch (Exception e){
            log.error("ChatSessionServiceImpl.createSession error: userId {}", userId, e);
            return null;
        }
    }

    @Override
    public ChatSession getSessionById(String sessionId) {
        try{
            // 删除会话
            activeSessions.remove(sessionId);
            return activeSessions.get(sessionId);
        } catch (Exception e){
            log.error("ChatSessionServiceImpl.getSessionById error: sessionId {}", sessionId, e);
            return null;
        }
    }

    @Override
    public boolean deleteSession(String sessionId) {
        try{
            // 删除会话
            activeSessions.remove(sessionId);
            return true;
        } catch (Exception e){
            log.error("ChatSessionServiceImpl.deleteSession error: sessionId {}", sessionId, e);
            return false;
        }
    }

    @Override
    public boolean updateSessionState(String sessionId, String newState) {
        try{
            ChatSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.setSessionState(newState);  // 更新会话状态
            }
            return true;
        } catch (Exception e){
            log.error("ChatSessionServiceImpl.updateSessionState error: sessionId {} newState {}", sessionId, newState, e);
            return false;
        }
    }

    @Override
    public boolean clearSessionChatMemory(String sessionId){
        try{
            ChatSession session = activeSessions.get(sessionId);
            ChatMemory chatMemory = new InMemoryChatMemory();
            session.setChatMemory(chatMemory);
            return true;
        } catch (Exception e){
            log.error("ChatSessionServiceImpl.clearSessionChatMemory error: sessionId {}", sessionId, e);
            return false;
        }
    }
}

package com.twelvet.ai.server.controller;

import com.twelvet.ai.server.model.ChatSession;
import com.twelvet.ai.server.service.ChatSessionService;
import com.twelvet.framework.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/session")
@Tag(name = "ChatSessionController", description = "聊天会话管理")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Operation(summary = "创建聊天会话")
    @PostMapping("/createSession")
    public ResponseEntity<ChatSession> createSession() {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        ChatSession session = chatSessionService.createSession(userId);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "根据会话ID获取聊天会话")
    @GetMapping("/getSession")
    public ResponseEntity<ChatSession> getSession(@RequestParam String sessionId) {
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (session != null) {
            return ResponseEntity.ok(session);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "删除聊天会话")
    @DeleteMapping("/deleteSession")
    public ResponseEntity<Void> deleteSession(@RequestParam String sessionId) {
        boolean deleted = chatSessionService.deleteSession(sessionId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "更新聊天会话状态")
    @PutMapping("/updateSessionState")
    public ResponseEntity<Void> updateSessionState(@RequestParam String sessionId, @RequestParam String newState) {
        boolean updated = chatSessionService.updateSessionState(sessionId, newState);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "清除聊天会话内存")
    @DeleteMapping("/cleanSessionMemory")
    public ResponseEntity<Void> clearSessionMemory(@RequestParam String sessionId) {
        boolean cleared = chatSessionService.clearSessionChatMemory(sessionId);
        return cleared ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}


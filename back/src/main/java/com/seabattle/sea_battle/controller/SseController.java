// back\src\main\java\com\seabattle\sea_battle\controller\SseController.java
package com.seabattle.sea_battle.controller;

import com.seabattle.sea_battle.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*") // Для тестирования из браузера
public class SseController {
    
    private final SseNotificationService sseService;
    
    /**
     * Подписка на SSE события игры
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToGame(
            @RequestParam UUID sessionId,
            @RequestParam String playerName) {
        
        log.info("SSE subscription request: session={}, player={}", sessionId, playerName);
        
        return sseService.registerPlayer(playerName, sessionId);
    }
    
    /**
     * Heartbeat endpoint для поддержания соединения
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestParam UUID sessionId,
            @RequestParam String playerName) {
        
        sseService.sendHeartbeat(playerName, sessionId);
        
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Heartbeat sent"
        ));
    }
    
    /**
     * Проверка подключений
     */
    @GetMapping("/connections")
    public ResponseEntity<?> getActiveConnections() {
        return ResponseEntity.ok(Map.of(
            "activeConnections", "check via logs"
        ));
    }
}
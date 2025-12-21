// back\src\main\java\com\seabattle\sea_battle\controller\GameController.java
package com.seabattle.sea_battle.controller;

import com.seabattle.sea_battle.dto.CreateGameRequest;
import com.seabattle.sea_battle.dto.CreateGameResponse;
import com.seabattle.sea_battle.dto.JoinGameRequest;
import com.seabattle.sea_battle.dto.JoinGameResponse;
import com.seabattle.sea_battle.model.GameSession;
import com.seabattle.sea_battle.service.GameSessionManager;
import com.seabattle.sea_battle.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {
    
    private final GameService gameService;
    private final GameSessionManager gameSessionManager; // Добавляем менеджер
    
    public GameController(GameService gameService, GameSessionManager gameSessionManager) {
        this.gameService = gameService;
        this.gameSessionManager = gameSessionManager;
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createGame(@RequestBody CreateGameRequest request) {
        try {
            CreateGameResponse response = gameService.createGameSession(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to create game session")
            );
        }
    }
    
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<?> joinGame(@PathVariable UUID sessionId, 
                                      @RequestBody JoinGameRequest request) {
        try {
            JoinGameResponse response = gameService.joinGameSession(sessionId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to join game session")
            );
        }
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionInfo(@PathVariable UUID sessionId) {
        GameSession session = gameService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(session);
    }
    
    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions() {
        Map<UUID, GameSession> sessions = gameSessionManager.getAllActiveSessions();
        
        Map<UUID, Map<String, Object>> sessionInfo = new java.util.HashMap<>();
        
        sessions.forEach((id, session) -> {
            sessionInfo.put(id, Map.of(
                "player1", session.getPlayer1().getUsername(),
                "player2", session.getPlayer2() != null ? 
                          session.getPlayer2().getUsername() : "Waiting...",
                "gameType", session.getGameType(),
                "status", session.getStatus(),
                "createdAt", session.getCreatedAt(),
                "canJoin", session.getStatus().toString().equals("WAITING_FOR_PLAYER") && 
                          session.getGameType().toString().equals("PVP")
            ));
        });
        
        return ResponseEntity.ok(Map.of(
            "activeSessionsCount", sessions.size(),
            "sessions", sessionInfo
        ));
    }
    
    @GetMapping("/waiting-sessions")
    public ResponseEntity<?> getWaitingSessions() {
        var waitingSessions = gameSessionManager.getWaitingSessions()
            .stream()
            .map(session -> Map.of(
                "sessionId", session.getSessionId(),
                "player1", session.getPlayer1().getUsername(),
                "createdAt", session.getCreatedAt(),
                "waitingTimeSeconds", java.time.Duration.between(
                    session.getCreatedAt(), java.time.LocalDateTime.now()
                ).getSeconds()
            ))
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "waitingSessionsCount", waitingSessions.size(),
            "sessions", waitingSessions
        ));
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getServiceStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "activeSessions", gameSessionManager.getActiveSessionsCount(),
            "maxSupportedSessions", gameSessionManager.getGameProperties().getMaxActiveSessions(),
            "minRequiredSessions", gameSessionManager.getGameProperties().getMinActiveSessions(),
            "meetsMinimumRequirement", gameSessionManager.meetsMinimumSessionsRequirement(),
            "waitingSessions", gameSessionManager.getWaitingSessions().size(),
            "activePlayers", gameSessionManager.getActivePlayerSessions().size(),
            "supportedGameTypes", new String[]{"PVP", "PVE"}
        ));
    }
    
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<?> cancelGame(@PathVariable UUID sessionId) {
        try {
            gameService.cancelGame(sessionId);
            return ResponseEntity.ok(Map.of(
                "message", "Game session cancelled",
                "sessionId", sessionId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
    }

}
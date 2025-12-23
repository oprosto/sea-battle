// back\src\main\java\com\seabattle\sea_battle\service\SseNotificationService.java
package com.seabattle.sea_battle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.seabattle.sea_battle.model.enums.GameStatus;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SseNotificationService {
    
    private final Map<String, SseEmitter> playerEmitters = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, SseEmitter>> gameEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public SseNotificationService() {
        // Регулярная очистка "мертвых" соединений
        scheduler.scheduleAtFixedRate(this::cleanupDeadConnections, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Регистрация SSE соединения для игрока
     */
    public SseEmitter registerPlayer(String playerName, UUID sessionId) {
        String emitterId = playerName + "_" + sessionId;
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30 минут timeout
        
        // Удаляем старый emitter если был
        if (playerEmitters.containsKey(emitterId)) {
            playerEmitters.get(emitterId).complete();
        }
        
        // Сохраняем новый
        playerEmitters.put(emitterId, emitter);
        
        // Также сохраняем в структуре по игре
        gameEmitters.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                   .put(playerName, emitter);
        
        // Настройка обработчиков
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for {}", emitterId);
            playerEmitters.remove(emitterId);
            cleanupGameEmitter(sessionId, playerName);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout for {}", emitterId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("SSE error for {}: {}", emitterId, ex.getMessage());
            playerEmitters.remove(emitterId);
            cleanupGameEmitter(sessionId, playerName);
        });
        
        // Отправляем начальное сообщение
        try {
            emitter.send(SseEmitter.event()
                .name("CONNECTED")
                .data(Map.of(
                    "type", "CONNECTED",
                    "player", playerName,
                    "sessionId", sessionId.toString(),
                    "timestamp", System.currentTimeMillis()
                )));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message", e);
        }
        
        log.info("SSE connection registered for {}", emitterId);
        return emitter;
    }
    
    /**
     * Отправка уведомления конкретному игроку
     */
    public void sendToPlayer(String playerName, UUID sessionId, String eventName, Object data) {
        String emitterId = playerName + "_" + sessionId;
        SseEmitter emitter = playerEmitters.get(emitterId);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(Map.of(
                        "type", eventName,
                        "data", data,
                        "timestamp", System.currentTimeMillis()
                    )));
                log.debug("SSE event sent to {}: {}", emitterId, eventName);
            } catch (IOException e) {
                log.error("Failed to send SSE to {}, removing connection", emitterId, e);
                playerEmitters.remove(emitterId);
                cleanupGameEmitter(sessionId, playerName);
            }
        } else {
            log.debug("No SSE connection found for {}", emitterId);
        }
    }
    
    /**
     * Отправка уведомления всем игрокам в сессии
     */
    public void sendToAllInGame(UUID sessionId, String eventName, Object data) {
        Map<String, SseEmitter> gamePlayers = gameEmitters.get(sessionId);
        
        if (gamePlayers != null) {
            gamePlayers.forEach((playerName, emitter) -> {
                sendToPlayer(playerName, sessionId, eventName, data);
            });
        }
    }
    
    /**
     * Уведомление об обновлении доски
     */
    public void notifyBoardUpdate(UUID sessionId, String playerName) {
        sendToPlayer(playerName, sessionId, "BOARD_UPDATE", Map.of(
            "message", "Board updated, please refresh",
            "sessionId", sessionId.toString()
        ));
    }
    
    /**
     * Уведомление о результате выстрела
     */
    public void notifyFireResult(UUID sessionId, String shooterName, Map<String, Object> fireResult) {
        sendToAllInGame(sessionId, "FIRE_RESULT", Map.of(
            "shooter", shooterName,
            "result", fireResult
        ));
    }
    
    /**
 * Отправка полного обновления доски игроку
 */
// public void sendBoardUpdate(UUID sessionId, String playerName, String[][] boardState, boolean isOwner) {
//     sendToPlayer(playerName, sessionId, "BOARD_FULL_UPDATE", Map.of(
//         "board", boardState,
//         "isOwner", isOwner,
//         "playerName", playerName,
//         "timestamp", System.currentTimeMillis()
//     ));
// }

    /**
     * Отправка обновления конкретной клетки
     */
    public void sendCellUpdate(UUID sessionId, String playerName, int x, int y, String status) {
        sendToPlayer(playerName, sessionId, "CELL_UPDATE", Map.of(
            "x", x,
            "y", y,
            "status", status,
            "playerName", playerName,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Отправка обновления доски противника (вид для стреляющего)
     */
    public void sendOpponentBoardUpdate(UUID sessionId, String playerName, String[][] BoardState) {
        sendToPlayer(playerName, sessionId, "OPPONENT_BOARD_UPDATE", Map.of(
            "board", BoardState,
            "playerName", playerName,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Отправка обновления доски владельца (вид для стреляющего)
     */
    public void sendOwnerBoardUpdate(UUID sessionId, String playerName, String[][] BoardState) {
        sendToPlayer(playerName, sessionId, "OWNER_BOARD_UPDATE", Map.of(
            "board", BoardState,
            "playerName", playerName,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Уведомление о смене хода
     */
    public void notifyTurnChange(UUID sessionId, String currentTurn) {
        sendToAllInGame(sessionId, "TURN_CHANGE", Map.of(
            "currentTurn", currentTurn
        ));
    }
    
    /**
 * Уведомление о завершении игры
 */
public void notifyGameOver(UUID sessionId, String winner, GameStatus status) {
    sendToAllInGame(sessionId, "GAME_OVER", Map.of(
        "status", status.toString()
    ));
}

/**
 * Уведомление о подключении игрока
 */
public void notifyPlayerJoined(UUID sessionId, String playerName) {
    sendToAllInGame(sessionId, "PLAYER_JOINED", Map.of(
        "player", playerName,
        "message", playerName + " joined the game"
    ));
}

/**
 * Уведомление о начале игры
 */
public void notifyGameStarted(UUID sessionId) {
    sendToAllInGame(sessionId, "GAME_STARTED", Map.of(
        "message", "Game started! Place your ships."
    ));
}

    /**
     * Heartbeat для поддержания соединения
     */
    public void sendHeartbeat(String playerName, UUID sessionId) {
        sendToPlayer(playerName, sessionId, "HEARTBEAT", Map.of("status", "alive"));
    }
    
    private void cleanupGameEmitter(UUID sessionId, String playerName) {
        Map<String, SseEmitter> gamePlayers = gameEmitters.get(sessionId);
        if (gamePlayers != null) {
            gamePlayers.remove(playerName);
            if (gamePlayers.isEmpty()) {
                gameEmitters.remove(sessionId);
            }
        }
    }
    
    private void cleanupDeadConnections() {
        playerEmitters.entrySet().removeIf(entry -> {
            SseEmitter emitter = entry.getValue();
            if (emitter == null) {
                return true;
            }
            // Можно добавить дополнительную логику проверки
            return false;
        });
    }
}
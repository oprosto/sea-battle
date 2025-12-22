// back\src\main\java\com\seabattle\sea_battle\service\GameNotificationService.java
package com.seabattle.sea_battle.service;

import com.seabattle.sea_battle.dto.GameUpdate;
import com.seabattle.sea_battle.model.enums.GameStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PreDestroy;

@Service
public class GameNotificationService {
    
    private final Map<String, BlockingQueue<GameUpdate>> playerQueues = new ConcurrentHashMap<>();
    private final Map<String, Long> playerLastSeen = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final long TIMEOUT_MS = 30000; // 30 секунд
    private final AtomicLong updateCounter = new AtomicLong(0);
    
    public GameNotificationService() {
        // Запускаем очистку устаревших подписок каждую минуту
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleSubscriptions, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Подписаться на обновления игры
     */
    public CompletableFuture<GameUpdate> subscribe(String playerName, UUID sessionId, long lastUpdateId) {
        CompletableFuture<GameUpdate> future = new CompletableFuture<>();
        
        // Создаем или получаем очередь игрока
        BlockingQueue<GameUpdate> queue = playerQueues.computeIfAbsent(
            playerName, k -> new LinkedBlockingQueue<>()
        );
        
        // Обновляем время последней активности
        playerLastSeen.put(playerName, System.currentTimeMillis());
        
        // Если уже есть ожидающие обновления, возвращаем их
        if (!queue.isEmpty() && getLastUpdateId(queue.peek()) > lastUpdateId) {
            GameUpdate update = queue.poll();
            if (update != null) {
                future.complete(update);
            }
        }
        
        return future;
    }
    
    /**
     * Отправить обновление игры всем участникам
     */
    public void notifyGameUpdate(UUID sessionId, String eventType, Object data, String... recipientNames) {
        long updateId = updateCounter.incrementAndGet();
        GameUpdate update = new GameUpdate(updateId, sessionId, eventType, data);
        
        if (recipientNames.length == 0) {
            // Отправляем всем игрокам в сессии
            playerQueues.keySet().forEach(playerName -> {
                if (playerQueues.containsKey(playerName)) {
                    playerQueues.get(playerName).offer(update);
                }
            });
        } else {
            // Отправляем только указанным игрокам
            for (String playerName : recipientNames) {
                if (playerQueues.containsKey(playerName)) {
                    playerQueues.get(playerName).offer(update);
                }
            }
        }
    }
    
    /**
     * Отправить уведомление о попадании
     */
    public void notifyHit(UUID sessionId, String shooterName, String targetName, 
                         int x, int y, boolean hit, boolean sunk, boolean gameOver) {
        Map<String, Object> hitData = Map.of(
            "shooter", shooterName,
            "target", targetName,
            "x", x,
            "y", y,
            "hit", hit,
            "sunk", sunk,
            "gameOver", gameOver,
            "timestamp", System.currentTimeMillis()
        );
        
        // Уведомляем обоих игроков
        notifyGameUpdate(sessionId, "HIT", hitData, shooterName, targetName);
    }
    
    /**
     * Отправить уведомление о смене хода
     */
    public void notifyTurnChange(UUID sessionId, String currentPlayer) {
        Map<String, Object> turnData = Map.of(
            "currentPlayer", currentPlayer,
            "timestamp", System.currentTimeMillis()
        );
        
        notifyGameUpdate(sessionId, "TURN_CHANGE", turnData);
    }
    
    /**
     * Отправить уведомление о готовности игрока
     */
    public void notifyPlayerReady(UUID sessionId, String playerName, boolean ready) {
        Map<String, Object> readyData = Map.of(
            "player", playerName,
            "ready", ready,
            "timestamp", System.currentTimeMillis()
        );
        
        notifyGameUpdate(sessionId, "PLAYER_READY", readyData);
    }
    
    /**
     * Отправить уведомление о начале игры
     */
    public void notifyGameStart(UUID sessionId) {
        notifyGameUpdate(sessionId, "GAME_START", Map.of("timestamp", System.currentTimeMillis()));
    }
    
    /**
     * Отправить уведомление о завершении игры
     */
    public void notifyGameEnd(UUID sessionId, String winner, GameStatus status) {
        Map<String, Object> endData = Map.of(
            "winner", winner,
            "status", status,
            "timestamp", System.currentTimeMillis()
        );
        
        notifyGameUpdate(sessionId, "GAME_END", endData);
    }
    
    /**
     * Получить ID последнего обновления в очереди
     */
    private long getLastUpdateId(GameUpdate update) {
        return update != null ? update.getUpdateId() : 0;
    }
    
    /**
     * Очистка устаревших подписок
     */
    private void cleanupStaleSubscriptions() {
        long now = System.currentTimeMillis();
        playerLastSeen.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > TIMEOUT_MS) {
                playerQueues.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Удалить подписку игрока
     */
    public void unsubscribe(String playerName) {
        playerQueues.remove(playerName);
        playerLastSeen.remove(playerName);
    }
    
    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
// back\src\main\java\com\seabattle\sea_battle\service\GameSessionManager.java
package com.seabattle.sea_battle.service;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.seabattle.sea_battle.config.GameProperties;
import com.seabattle.sea_battle.model.GameSession;
import com.seabattle.sea_battle.model.Player;
import com.seabattle.sea_battle.model.PlayerSession;
import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;

import lombok.Getter;


@Getter
@Component
public class GameSessionManager {
    
    private final Map<UUID, GameSession> activeGameSessions = new ConcurrentHashMap<>();
    private final Map<String, PlayerSession> activePlayerSessions = new ConcurrentHashMap<>();
    private final GameProperties gameProperties;
    
    public GameSessionManager(GameProperties gameProperties) {
        this.gameProperties = gameProperties;
    }
    
    /**
     * Создание новой игровой сессии с проверкой лимитов
     */
    public synchronized GameSession createGameSession(GameSession session) {
        // Проверяем максимальное количество активных сессий
        if (activeGameSessions.size() >= gameProperties.getMaxActiveSessions()) {
            throw new IllegalStateException(
                String.format("Maximum active sessions reached (%d)", 
                gameProperties.getMaxActiveSessions())
            );
        }
        
        // Проверяем, что игрок не участвует уже в другой игре
        String playerName = session.getPlayer1().getUsername();
        if (isPlayerInActiveGame(playerName)) {
            throw new IllegalArgumentException(
                String.format("Player %s is already in an active game", playerName)
            );
        }
        
        // Регистрируем игрока
        PlayerSession playerSession = new PlayerSession(playerName, session.getSessionId());
        activePlayerSessions.put(playerName, playerSession);
        
        // Сохраняем игровую сессию
        activeGameSessions.put(session.getSessionId(), session);
        
        return session;
    }
    
    /**
     * Подключение игрока к существующей сессии
     */
    public synchronized GameSession joinGameSession(UUID sessionId, String playerName) {
        GameSession session = activeGameSessions.get(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
    
        synchronized (session) {
            if (session.getStatus() != GameStatus.WAITING_FOR_PLAYER) {
                throw new IllegalStateException("Game session is not waiting for players");
            }
        
            if (session.getGameType() != GameType.PVP) {
                throw new IllegalStateException("Only PVP games can be joined");
            }
        
            // Проверяем, что игрок не участвует уже в другой игре
            if (isPlayerInActiveGame(playerName)) {
                throw new IllegalArgumentException(
                    String.format("Player %s is already in an active game", playerName)
                );
            }
        
            // Создаем и подключаем игрока
            Player player2 = new Player(playerName);
            session.connectSecondPlayer(player2);
        
            // Регистрируем игрока
            PlayerSession playerSession = new PlayerSession(playerName, sessionId);
            activePlayerSessions.put(playerName, playerSession);
        }

        return session;
    }
    
    /*
     * Проверка участия игрока в активной игре
     */
    public boolean isPlayerInActiveGame(String playerName) {
        PlayerSession playerSession = activePlayerSessions.get(playerName);
        
        if (playerSession == null) {
            return false;
        }
        
        GameSession gameSession = activeGameSessions.get(playerSession.getGameSessionId());
        
        if (gameSession == null) {
            // Игровая сессия не найдена, удаляем запись игрока
            activePlayerSessions.remove(playerName);
            return false;
        }
        
        // Проверяем статус игры
        GameStatus status = gameSession.getStatus();
        return status == GameStatus.WAITING_FOR_PLAYER || 
               status == GameStatus.IN_PROGRESS;
    }
    
    /**
     * Завершение игровой сессии
     */
    public synchronized void finishGameSession(UUID sessionId, GameStatus finalStatus) {
        GameSession session = activeGameSessions.get(sessionId);
        
        if (session != null) {
            session.finishGame(finalStatus);
            
            // Освобождаем игроков
            if (session.getPlayer1() != null) {
                activePlayerSessions.remove(session.getPlayer1().getUsername());
            }
            if (session.getPlayer2() != null && !session.getPlayer2().isAI()) {
                activePlayerSessions.remove(session.getPlayer2().getUsername());
            }
            
            // TODO: Сохранить результат в БД
            
            // Удаляем из активных через некоторое время
            // (в реальном приложении можно переместить в архив)
            scheduleSessionRemoval(sessionId);
        }
    }
    
    /**
     * Отмена игровой сессии (например, при таймауте)
     */
    public synchronized void cancelGameSession(UUID sessionId) {
        GameSession session = activeGameSessions.get(sessionId);
        
        if (session != null) {
            session.finishGame(GameStatus.CANCELLED);
            
            // Освобождаем игроков
            if (session.getPlayer1() != null) {
                activePlayerSessions.remove(session.getPlayer1().getUsername());
            }
            if (session.getPlayer2() != null && !session.getPlayer2().isAI()) {
                activePlayerSessions.remove(session.getPlayer2().getUsername());
            }
            
            activeGameSessions.remove(sessionId);
        }
    }
    
    /**
     * Получение активной сессии по ID
     */
    public GameSession getGameSession(UUID sessionId) {
        return activeGameSessions.get(sessionId);
    }
    
    /**
     * Получение всех активных сессий
     */
    public Map<UUID, GameSession> getAllActiveSessions() {
        return new ConcurrentHashMap<>(activeGameSessions);
    }
    
    /**
     * Получение количества активных сессий
     */
    public int getActiveSessionsCount() {
        return activeGameSessions.size();
    }
    
    /**
     * Проверка минимального требования по сессиям
     */
    public boolean meetsMinimumSessionsRequirement() {
        return getActiveSessionsCount() >= gameProperties.getMinActiveSessions();
    }
    
    /**
     * Получение сессий, ожидающих второго игрока
     */
    public List<GameSession> getWaitingSessions() {
        return activeGameSessions.values().stream()
                .filter(session -> session.getStatus() == GameStatus.WAITING_FOR_PLAYER)
                .collect(Collectors.toList());
    }
    
    /**
     * Получение статистики по сессиям
     */
    public Map<String, Object> getSessionStatistics() {
        long waitingCount = activeGameSessions.values().stream()
                .filter(s -> s.getStatus() == GameStatus.WAITING_FOR_PLAYER)
                .count();
        
        long inProgressCount = activeGameSessions.values().stream()
                .filter(s -> s.getStatus() == GameStatus.IN_PROGRESS)
                .count();
        
        long pvpCount = activeGameSessions.values().stream()
                .filter(s -> s.getGameType() == GameType.PVP)
                .count();
        
        long pveCount = activeGameSessions.values().stream()
                .filter(s -> s.getGameType() == GameType.PVE)
                .count();
        
        return Map.of(
            "totalSessions", getActiveSessionsCount(),
            "waitingSessions", waitingCount,
            "inProgressSessions", inProgressCount,
            "pvpSessions", pvpCount,
            "pveSessions", pveCount,
            "activePlayers", activePlayerSessions.size(),
            "maxSessions", gameProperties.getMaxActiveSessions(),
            "minSessionsRequired", gameProperties.getMinActiveSessions(),
            "meetsMinimumRequirement", meetsMinimumSessionsRequirement()
        );
    }
    
    private void scheduleSessionRemoval(UUID sessionId) {
        // В реальном приложении можно использовать ScheduledExecutorService
        // Для простоты удалим сразу, но лучше дать время на получение результата
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                activeGameSessions.remove(sessionId);
            }
        }, 60000); // Удалить через 1 минуту после завершения
    }
}
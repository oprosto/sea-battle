// C:\sea-battle\src\main\java\com\seabattle\sea_battle\service\GameService.java
package com.seabattle.sea_battle.service;

import com.seabattle.sea_battle.dto.*;
import com.seabattle.sea_battle.model.*;
import com.seabattle.sea_battle.model.enums.*;
import com.seabattle.sea_battle.repository.GameRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GameService {
    
    private final GameSessionManager sessionManager;
    private final AIService aiService;
    private final GameRepository gameRepository; 
    
    // Обновить конструктор:
    public GameService(GameSessionManager sessionManager, AIService aiService, 
                       GameRepository gameRepository) { // Добавляем параметр
        this.sessionManager = sessionManager;
        this.aiService = aiService;
        this.gameRepository = gameRepository; // Инициализируем
    }
    
    public List<GameHistoryResponse> getAllGameHistory() {
            List<Game> games = gameRepository.findAll();
        
        return games.stream()
                .map(this::convertToHistoryResponse)
                .sorted((g1, g2) -> g2.getFinishedAt().compareTo(g1.getFinishedAt())) // Сортировка по дате (новые сначала)
                .collect(Collectors.toList());
    }
    
    private GameHistoryResponse convertToHistoryResponse(Game game) {
        return new GameHistoryResponse(
            game.getId(),
            game.getPlayer1Name(),
            game.getPlayer2Name(),
            game.getGameType(),
            game.getResult(),
            game.getStartedAt(),
            game.getFinishedAt()
        );
    }

    public void saveCompletedGame(GameSession session, GameStatus finalStatus) {
        Game game = new Game(
            session.getPlayer1().getUsername(),
            session.getPlayer2() != null ? session.getPlayer2().getUsername() : null,
            session.getGameType(),
            finalStatus,
            session.getStartedAt() != null ? session.getStartedAt() : LocalDateTime.now(),
            session.getFinishedAt() != null ? session.getFinishedAt() : LocalDateTime.now()
        );
        
        gameRepository.save(game);
    }

    public CreateGameResponse createGameSession(CreateGameRequest request) {
        // Проверяем можно ли создать новую сессию
        if (!canCreateNewSession()) {
            throw new IllegalStateException(
                "Cannot create new game session. Maximum limit reached: " + 
                sessionManager.getGameProperties().getMaxActiveSessions()
            );
        }
        
        // Создаем первого игрока
        Player player1 = new Player(request.getPlayerName());
        
        // Создаем игровую сессию
        GameSession session = new GameSession(player1, request.getGameType());
        
        // Если это игра против AI, сразу создаем AI игрока
        if (request.getGameType() == GameType.PVE) {
            Player aiPlayer = aiService.createAIPlayer();
            session.connectSecondPlayer(aiPlayer);
        }
        
        // Регистрируем сессию в менеджере
        sessionManager.createGameSession(session);
        
        // Формируем ответ
        String joinUrl = null;
        String message;
        
        if (request.getGameType() == GameType.PVP) {
            joinUrl = "/api/game/" + session.getSessionId() + "/join";
            message = "Game created. Waiting for second player. Share this URL: " + joinUrl;
        } else {
            message = "Game against AI created. Game started! Session ID: " + session.getSessionId();
        }
        
        return new CreateGameResponse(
            session.getSessionId(),
            message,
            joinUrl
            // getAvailableSessionsInfo()
        );
    }
    
    public JoinGameResponse joinGameSession(UUID sessionId, JoinGameRequest request) {
        // Используем менеджер для атомарной операции
        GameSession session = sessionManager.joinGameSession(sessionId, request.getPlayerName());
    
        return new JoinGameResponse(
            session.getSessionId(),
            "Successfully joined the game. Game started!",
            session.getPlayer1().getUsername()
        );
    }
    
    public GameSession getSession(UUID sessionId) {
        return sessionManager.getGameSession(sessionId);
    }
    
    public void finishGame(UUID sessionId, GameStatus finalStatus) {
        sessionManager.finishGameSession(sessionId, finalStatus);
    }
    
    public void cancelGame(UUID sessionId) {
        sessionManager.cancelGameSession(sessionId);
    }
    
    public SessionInfoResponse getSessionInfo(UUID sessionId) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            return null;
        }
        
        return new SessionInfoResponse(
            session.getSessionId(),
            session.getPlayer1().getUsername(),
            session.getPlayer2() != null ? session.getPlayer2().getUsername() : null,
            session.getGameType(),
            session.getStatus(),
            session.getCreatedAt(),
            session.getStartedAt(),
            session.getFinishedAt()
        );
    }
    
    // public AvailableSessionsResponse getAvailableSessionsInfo() {
    //     return new AvailableSessionsResponse(
    //         sessionManager.getActiveSessionsCount(),
    //         sessionManager.getWaitingSessions().size(),
    //         sessionManager.getSessionStatistics()
    //     );
    // }
    
    public boolean canCreateNewSession() {
        return sessionManager.getActiveSessionsCount() < 
               sessionManager.getGameProperties().getMaxActiveSessions();
    }
    
}
// back\src\main\java\com\seabattle\sea_battle\service\GamePlayService.java
package com.seabattle.sea_battle.service;

import com.seabattle.sea_battle.dto.*;
import com.seabattle.sea_battle.model.*;
import com.seabattle.sea_battle.model.enums.CellStatus;
import com.seabattle.sea_battle.model.enums.GameStatus;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamePlayService {
    
    private final GameSessionManager sessionManager;
    private final AIService aiService;
    private final SseNotificationService sseService; // Добавляем это поле
    
    // Обновляем конструктор:
    public GamePlayService(GameSessionManager sessionManager, AIService aiService,
                          SseNotificationService sseService) { // Добавляем параметр
        this.sessionManager = sessionManager;
        this.aiService = aiService;
        this.sseService = sseService; // Инициализируем
    }
    
    /**
     * Расстановка кораблей игроком
     */
    public boolean placeShips(UUID sessionId, PlaceShipsRequest request) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        if (session.getStatus().equals(GameStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Cannot place ships, game already started");
        }
        
        // Конвертируем DTO в модели кораблей
        List<Ship> ships = request.getShips().stream()
                .map(shipReq -> new Ship(
                    shipReq.getLength(),
                    shipReq.isHorizontal(),
                    shipReq.getX(),
                    shipReq.getY()
                ))
                .collect(Collectors.toList());
        
        Board playerBoard = session.getPlayerBoard(request.getPlayerName());
        boolean success = playerBoard.placeShips(ships);
        
        if (success) {
        session.playerReady(request.getPlayerName());
        
        // Уведомляем через SSE о готовности игрока
        sseService.sendToAllInGame(sessionId, "PLAYER_READY", Map.of(
            "player", request.getPlayerName(),
            "ready", true
        ));
        
        // Если оба игрока готовы
        if (session.isPlayer1Ready() && session.isPlayer2Ready()) {
            sseService.notifyGameStarted(sessionId);
        }
    }
        
        return success;
    }
    
    /**
     * Автоматическая расстановка кораблей
     */
    public void autoPlaceShips(UUID sessionId, String playerName) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        Board playerBoard = session.getPlayerBoard(playerName);
        playerBoard.placeShipsAutomatically();
        session.playerReady(playerName);

        // Уведомляем через SSE
        sseService.sendToAllInGame(sessionId, "PLAYER_READY", Map.of(
            "player", playerName,
            "ready", true,
            "autoPlaced", true
        ));
        
        if (session.isPlayer1Ready() && session.isPlayer2Ready()) {
            sseService.notifyGameStarted(sessionId);
        }
    }
    
    /**
     * Получение состояния доски
     */
    public BoardStateResponse getBoardState(UUID sessionId, String playerName, boolean isOwner) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        Board board;
        if (isOwner) {
            board = session.getPlayerBoard(playerName);
        } else {
            board = session.getOpponentBoard(playerName);
        }
        
        String[][] boardState = board.getBoardState(isOwner);
        return new BoardStateResponse(playerName, boardState, isOwner);
    }
    
    /**
     * Выстрел по противнику
     */
    @Transactional
    public FireResponse fire(UUID sessionId, FireRequest request) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        if (!session.getStatus().equals(GameStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Game is not in progress");
        }
        
        // Выполняем выстрел
        GameSession.FireResult result = session.fireAtOpponent(request.getPlayerName(), request.getX(), request.getY());
        
        // Получаем обновленные доски
        String opponentName = session.getOpponentName(request.getPlayerName());
    
        // Доска игрока, получившего выстрел (с обновленным статусом клетки)
        Board targetBoard = session.getPlayerBoard(opponentName);
        String[][] updatedTargetBoard = targetBoard.getBoardState(true);
        
        // Доска противника для стреляющего (с видимыми результатами)
        String[][] opponentViewForShooter = targetBoard.getBoardState(false);
        
        // 1. Отправляем обновление конкретной клетки игроку, получившему выстрел
        String cellStatusStr = result.isHit() ? "HIT" : "MISS";
        sseService.sendCellUpdate(sessionId, opponentName, request.getX(), request.getY(), cellStatusStr);
        
        // 2. Отправляем обновление доски противника стреляющему игроку
        sseService.sendOwnerBoardUpdate(sessionId, opponentName, updatedTargetBoard);
        sseService.sendOpponentBoardUpdate(sessionId, request.getPlayerName(), opponentViewForShooter);
        

        // Формируем результат для SSE
        Map<String, Object> fireResult = new HashMap<>();
        fireResult.put("hit", result.isHit());
        fireResult.put("sunk", result.isSunk());
        fireResult.put("x", request.getX());
        fireResult.put("y", request.getY());
        fireResult.put("shooter", request.getPlayerName());
        
        // 1. Уведомляем всех о результате выстрела
        sseService.notifyFireResult(sessionId, request.getPlayerName(), fireResult);

        // // 2. Уведомляем игрока о необходимости обновить доску
        // String opponentName = session.getOpponentName(request.getPlayerName());
        // sseService.notifyBoardUpdate(sessionId, opponentName);
        
        // 3. При промахе уведомляем о смене хода
        if (!result.isHit()) {
            sseService.notifyTurnChange(sessionId, session.getCurrentTurn());
        }

        // Если игра против AI и был промах - AI делает ход
        if (session.getGameType().equals(com.seabattle.sea_battle.model.enums.GameType.PVE) && 
            session.getPlayer2().isAI() && !result.isHit()) {
            aiService.makeAIMove(session);

            // Уведомляем о ходе AI
            Map<String, Object> aiFireResult = new HashMap<>();
            aiFireResult.put("shooter", session.getPlayer2().getUsername());
            aiFireResult.put("isAI", true);
            sseService.notifyFireResult(sessionId, session.getPlayer2().getUsername(), aiFireResult);
            sseService.notifyBoardUpdate(sessionId, session.getPlayer1().getUsername());
            
            // Если AI потопил все корабли игрока
            if (session.getPlayer1Board().allShipsSunk()) {
                GameStatus aiWinStatus = GameStatus.AI_WON;
                sessionManager.finishGameSession(sessionId, aiWinStatus);
                sseService.notifyGameOver(sessionId, session.getPlayer2().getUsername(), aiWinStatus);
            }
        }
        
        // Формируем ответ
        CellStatus cellStatus = result.isHit() ? CellStatus.HIT : CellStatus.MISS;
        String message = result.isHit() ? 
            (result.isSunk() ? "Hit! Ship sunk!" : "Hit!") : 
            "Miss!";
        
        if (result.isGameOver()) {
            message += " Game over!";
            sessionManager.finishGameSession(sessionId, session.getStatus());
        }
        
        return new FireResponse(
            result.isHit(),
            result.isSunk(),
            result.isGameOver(),
            message,
            cellStatus
        );
    }
    
    /**
     * Получение текущего хода
     */
    public String getCurrentTurn(UUID sessionId) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        return session.getCurrentTurn();
    }
    
    /**
     * Проверка готовности игрока
     */
    public boolean isPlayerReady(UUID sessionId, String playerName) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            return false;
        }
        
        if (playerName.equals(session.getPlayer1().getUsername())) {
            return session.isPlayer1Ready();
        } else if (session.getPlayer2() != null && playerName.equals(session.getPlayer2().getUsername())) {
            return session.isPlayer2Ready();
        }
        
        return false;
    }
    
    /**
     * Получение статуса готовности всех игроков
     */
    public Map<String, Boolean> getPlayersReadyStatus(UUID sessionId) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        Map<String, Boolean> status = new HashMap<>();
        status.put(session.getPlayer1().getUsername(), session.isPlayer1Ready());
        
        if (session.getPlayer2() != null) {
            status.put(session.getPlayer2().getUsername(), session.isPlayer2Ready());
        }
        
        return status;
    }
    
    /**
     * Получение полного состояния игры
     */
    public Map<String, Object> getGameState(UUID sessionId, String playerName) {
        GameSession session = sessionManager.getGameSession(sessionId);
        
        if (session == null) {
            throw new IllegalArgumentException("Game session not found");
        }
        
        Map<String, Object> state = new HashMap<>();
        state.put("sessionId", sessionId);
        state.put("status", session.getStatus());
        state.put("gameType", session.getGameType());
        state.put("currentTurn", session.getCurrentTurn());
        state.put("createdAt", session.getCreatedAt());
        state.put("startedAt", session.getStartedAt());
        state.put("finishedAt", session.getFinishedAt());
        
        // Информация о игроках
        state.put("player1", Map.of(
            "name", session.getPlayer1().getUsername(),
            "ready", session.isPlayer1Ready(),
            "isAI", session.getPlayer1().isAI()
        ));
        
        if (session.getPlayer2() != null) {
            state.put("player2", Map.of(
                "name", session.getPlayer2().getUsername(),
                "ready", session.isPlayer2Ready(),
                "isAI", session.getPlayer2().isAI()
            ));
        }
        
        // Состояние досок
        state.put("playerBoard", getBoardState(sessionId, playerName, true));
        state.put("opponentBoard", getBoardState(sessionId, playerName, false));
        
        return state;
    }
    
    // private GameService getGameService() {
    //     // Этот метод нужен для доступа к GameService для сохранения истории
    //     // В реальном приложении лучше использовать Dependency Injection
    //     return null;
    // }
}
// C:\sea-battle\src\main\java\com\seabattle\sea_battle\dto\GameHistoryResponse.java
package com.seabattle.sea_battle.dto;

import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;

import java.time.LocalDateTime;
import java.util.UUID;

public class GameHistoryResponse {
    private UUID gameId;
    private String player1Name;
    private String player2Name;
    private GameType gameType;
    private GameStatus result;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long durationSeconds;
    private String winnerName;
    
    public GameHistoryResponse(UUID gameId, String player1Name, String player2Name, 
                               GameType gameType, GameStatus result,
                               LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.gameId = gameId;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.gameType = gameType;
        this.result = result;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        
        // Рассчитываем продолжительность игры
        if (startedAt != null && finishedAt != null) {
            this.durationSeconds = java.time.Duration.between(startedAt, finishedAt).getSeconds();
        } else {
            this.durationSeconds = 0;
        }
        
        // Определяем имя победителя
        this.winnerName = determineWinnerName();
    }
    
    private String determineWinnerName() {
        switch (result) {
            case PLAYER1_WON:
                return player1Name;
            case PLAYER2_WON:
                return player2Name != null ? player2Name : "AI";
            case PLAYER_WON: // Для PVE игр
                return player1Name;
            case AI_WON: // Для PVE игр
                return player2Name != null ? player2Name : "AI";
            case CANCELLED:
                return "Game was cancelled";
            default:
                return "Unknown";
        }
    }
    
    // Getters
    public UUID getGameId() { return gameId; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public GameType getGameType() { return gameType; }
    public GameStatus getResult() { return result; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public long getDurationSeconds() { return durationSeconds; }
    public String getWinnerName() { return winnerName; }
}
// C:\sea-battle\src\main\java\com\seabattle\sea_battle\dto\SessionInfoResponse.java
package com.seabattle.sea_battle.dto;

import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;

import java.time.LocalDateTime;
import java.util.UUID;

public class SessionInfoResponse {
    private UUID sessionId;
    private String player1Name;
    private String player2Name;
    private GameType gameType;
    private GameStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    
    public SessionInfoResponse(UUID sessionId, String player1Name, String player2Name, 
                               GameType gameType, GameStatus status,
                               LocalDateTime createdAt, LocalDateTime startedAt, 
                               LocalDateTime finishedAt) {
        this.sessionId = sessionId;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.gameType = gameType;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }
    
    // Getters
    public UUID getSessionId() { return sessionId; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public GameType getGameType() { return gameType; }
    public GameStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
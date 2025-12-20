// C:\sea-battle\src\main\java\com\seabattle\sea_battle\model\GameSession.java
package com.seabattle.sea_battle.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class GameSession {
    private UUID sessionId;
    private Player player1;
    private Player player2;
    private GameType gameType;
    private GameStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    
    public GameSession(Player player1, GameType gameType) {
        this.sessionId = UUID.randomUUID();
        this.player1 = player1;
        this.gameType = gameType;
        this.status = GameStatus.WAITING_FOR_PLAYER;
        this.createdAt = LocalDateTime.now();
    }
    
    // Метод для подключения второго игрока
    public void connectSecondPlayer(Player player2) {
        if (this.player2 != null) {
            throw new IllegalStateException("Second player already connected");
        }
        
        this.player2 = player2;
        
        if (gameType == GameType.PVE && player2.isAI()) {
            // Для PVE начинаем сразу, если второй игрок - AI
            this.status = GameStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        } else if (gameType == GameType.PVP && !player2.isAI()) {
            // Для PVP начинаем когда подключился реальный игрок
            this.status = GameStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }
    
    // Метод для завершения игры
    public void finishGame(GameStatus finalStatus) {
        this.status = finalStatus;
        this.finishedAt = LocalDateTime.now();
    }
}
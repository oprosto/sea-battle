// C:\sea-battle\src\main\java\com\seabattle\sea_battle\model\PlayerSession.java
package com.seabattle.sea_battle.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlayerSession {
    private String playerId; // username или уникальный идентификатор
    private UUID gameSessionId;
    private LocalDateTime joinedAt;
    private boolean isActive;
    
    public PlayerSession(String playerId, UUID gameSessionId) {
        this.playerId = playerId;
        this.gameSessionId = gameSessionId;
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;
    }
}
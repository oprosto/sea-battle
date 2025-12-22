// back\src\main\java\com\seabattle\sea_battle\dto\GameUpdate.java
package com.seabattle.sea_battle.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class GameUpdate {
    private final long updateId;
    private final UUID sessionId;
    private final String eventType;
    private final Object data;
    private final long timestamp;
    
    public GameUpdate(long updateId, UUID sessionId, String eventType, Object data) {
        this.updateId = updateId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
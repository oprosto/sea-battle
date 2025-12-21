// back\src\main\java\com\seabattle\sea_battle\dto\JoinGameResponse.java
package com.seabattle.sea_battle.dto;

import java.util.UUID;

public class JoinGameResponse {
    private UUID sessionId;
    private String message;
    private String opponentName;
    
    public JoinGameResponse(UUID sessionId, String message, String opponentName) {
        this.sessionId = sessionId;
        this.message = message;
        this.opponentName = opponentName;
    }
    
    // Getters
    public UUID getSessionId() { return sessionId; }
    public String getMessage() { return message; }
    public String getOpponentName() { return opponentName; }
}
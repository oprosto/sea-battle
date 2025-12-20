// C:\sea-battle\src\main\java\com\seabattle\sea_battle\dto\CreateGameResponse.java
package com.seabattle.sea_battle.dto;

import java.util.UUID;

public class CreateGameResponse {
    private UUID sessionId;
    private String message;
    private String joinUrl;
    //private AvailableSessionsResponse availableSessions;
    
    public CreateGameResponse(UUID sessionId, String message, String joinUrl
                             /*AvailableSessionsResponse availableSessions*/) {
        this.sessionId = sessionId;
        this.message = message;
        this.joinUrl = joinUrl;
    }
    
    // Getters
    public UUID getSessionId() { return sessionId; }
    public String getMessage() { return message; }
    public String getJoinUrl() { return joinUrl; }
    //public AvailableSessionsResponse getAvailableSessions() { return availableSessions; }
}
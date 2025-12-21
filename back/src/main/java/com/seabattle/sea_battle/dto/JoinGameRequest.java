// back\src\main\java\com\seabattle\sea_battle\dto\JoinGameRequest.java
package com.seabattle.sea_battle.dto;

public class JoinGameRequest {
    private String playerName;
    
    public JoinGameRequest() {}
    
    public JoinGameRequest(String playerName) {
        this.playerName = playerName;
    }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}
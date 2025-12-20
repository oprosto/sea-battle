// C:\sea-battle\src\main\java\com\seabattle\sea_battle\model\Player.java
package com.seabattle.sea_battle.model;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Player {
    private UUID id;
    private String username;
    private boolean isAI;
    
    public Player(String username) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.isAI = false;
    }
    
    public Player(String username, boolean isAI) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.isAI = isAI;
    }
}
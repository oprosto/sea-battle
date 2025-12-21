// back\src\main\java\com\seabattle\sea_battle\dto\CreateGameRequest.java
package com.seabattle.sea_battle.dto;

import com.seabattle.sea_battle.model.enums.GameType;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class CreateGameRequest {
    private String playerName;
    private GameType gameType;
    
    // Конструкторы, геттеры и сеттеры
    public CreateGameRequest() {}
    
    public CreateGameRequest(String playerName, GameType gameType) {
        this.playerName = playerName;
        this.gameType = gameType;
    }
}
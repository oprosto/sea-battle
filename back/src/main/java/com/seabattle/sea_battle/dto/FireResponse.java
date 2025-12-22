// back\src\main\java\com\seabattle\sea_battle\dto\FireResponse.java
package com.seabattle.sea_battle.dto;

import com.seabattle.sea_battle.model.enums.CellStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FireResponse {
    private boolean hit;
    private boolean sunk;
    private boolean gameOver;
    private String message;
    private CellStatus cellStatus;
    
    public FireResponse(boolean hit, boolean sunk, boolean gameOver, String message, CellStatus cellStatus) {
        this.hit = hit;
        this.sunk = sunk;
        this.gameOver = gameOver;
        this.message = message;
        this.cellStatus = cellStatus;
    }
}
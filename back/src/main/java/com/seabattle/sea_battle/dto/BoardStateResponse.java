// back\src\main\java\com\seabattle\sea_battle\dto\BoardStateResponse.java
package com.seabattle.sea_battle.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardStateResponse {
    private String playerName;
    private String[][] board;
    private boolean isOwner;
    
    public BoardStateResponse(String playerName, String[][] board, boolean isOwner) {
        this.playerName = playerName;
        this.board = board;
        this.isOwner = isOwner;
    }
}

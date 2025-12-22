// back\src\main\java\com\seabattle\sea_battle\dto\PlaceShipsRequest.java
package com.seabattle.sea_battle.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PlaceShipsRequest {
    private String playerName;
    private List<PlaceShipRequest> ships;
}

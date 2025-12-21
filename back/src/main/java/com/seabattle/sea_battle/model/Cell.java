// back\src\main\java\com\seabattle\sea_battle\model\Cell.java
package com.seabattle.sea_battle.model;

import com.seabattle.sea_battle.model.enums.CellStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cell {
    private final int x;
    private final int y;
    private CellStatus status;
    private Ship ship;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.status = CellStatus.EMPTY;
        this.ship = null;
    }
}
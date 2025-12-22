// back\src\main\java\com\seabattle\sea_battle\model\Ship.java
package com.seabattle.sea_battle.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ship {
    private final int length;
    private final boolean horizontal;
    private final int x;
    private final int y;
    private int hits;
    private boolean sunk;

    public Ship(int length, boolean horizontal, int x, int y) {
        this.length = length;
        this.horizontal = horizontal;
        this.x = x;
        this.y = y;
        this.hits = 0;
        this.sunk = false;
    }

    public void hit() {
        hits++;
        if (hits >= length) {
            sunk = true;
        }
    }

    public boolean isSunk() {
        return sunk;
    }
}
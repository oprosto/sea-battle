// back\src\main\java\com\seabattle\sea_battle\model\Board.java
package com.seabattle.sea_battle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import com.seabattle.sea_battle.model.enums.CellStatus;

import lombok.Getter;

@Getter
public class Board {
    private static final int BOARD_SIZE = 10;
    private final Cell[][] cells;
    private final List<Ship> ships;
    private final String playerName;

    public Board(String playerName) {
        this.playerName = playerName;
        this.cells = new Cell[BOARD_SIZE][BOARD_SIZE];
        this.ships = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                cells[x][y] = new Cell(x, y);
            }
        }
    }

    /**
     * Расстановка кораблей на доске
     * Правила: 1x4, 2x3, 3x2, 4x1
     */
    public boolean placeShips(List<Ship> shipsToPlace) {
        // Очищаем текущие корабли
        ships.clear();
        clearBoard();

        for (Ship ship : shipsToPlace) {
            if (!canPlaceShip(ship)) {
                return false;
            }
            placeShip(ship);
        }

        // Проверяем правильное количество кораблей
        return validateShipCount();
    }

    /**
     * Автоматическая расстановка кораблей
     */
    public void placeShipsAutomatically() {
        ships.clear();
        clearBoard();

        // Расставляем корабли согласно правилам
        int[] shipLengths = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        
        for (int length : shipLengths) {
            boolean placed = false;
            int attempts = 0;
            
            while (!placed && attempts < 1000) {
                attempts++;
                
                boolean horizontal = Math.random() > 0.5;
                int x = (int) (Math.random() * BOARD_SIZE);
                int y = (int) (Math.random() * BOARD_SIZE);
                
                Ship ship = new Ship(length, horizontal, x, y);
                
                if (canPlaceShip(ship)) {
                    placeShip(ship);
                    placed = true;
                }
            }
            
            if (!placed) {
                //throw new IllegalStateException("Failed to place ship automatically");
                ships.clear();
                clearBoard();
                placeShipsAutomatically(); // Рекурсивный вызов
                return;
            }
        }
    }

    private boolean canPlaceShip(Ship ship) {
        int x = ship.getX();
        int y = ship.getY();
        int length = ship.getLength();

        // Проверка выхода за границы
        if (ship.isHorizontal()) {
            if (x + length >= BOARD_SIZE) return false;
        } else {
            if (y + length >= BOARD_SIZE) return false;
        }

        // Проверка наложения и соседних клеток
        for (int i = 0; i < length; i++) {
            int checkX = ship.isHorizontal() ? x + i : x;
            int checkY = ship.isHorizontal() ? y : y + i;

            // Проверяем саму клетку
            if (cells[checkX][checkY].getStatus() != CellStatus.EMPTY) {
                return false;
            }

            // Проверяем соседние клетки
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int neighborX = checkX + dx;
                    int neighborY = checkY + dy;
                    
                    if (neighborX >= 0 && neighborX < BOARD_SIZE && 
                        neighborY >= 0 && neighborY < BOARD_SIZE) {
                        if (cells[neighborX][neighborY].getStatus() == CellStatus.SHIP) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void placeShip(Ship ship) {
        int x = ship.getX();
        int y = ship.getY();
        int length = ship.getLength();

        for (int i = 0; i < length; i++) {
            int cellX = ship.isHorizontal() ? x + i : x;
            int cellY = ship.isHorizontal() ? y : y + i;
            
            cells[cellX][cellY].setStatus(CellStatus.SHIP);
            cells[cellX][cellY].setShip(ship);
        }

        ships.add(ship);
    }

    private void clearBoard() {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                cells[x][y].setStatus(CellStatus.EMPTY);
                cells[x][y].setShip(null);
            }
        }
    }

    private boolean validateShipCount() {
        Map<Integer, Long> shipsByLength = ships.stream()
        .collect(Collectors.groupingBy(Ship::getLength, Collectors.counting()));
    
        // Стандартная расстановка: 1x4, 2x3, 3x2, 4x1
        Map<Integer, Integer> requiredShips = Map.of(
            4, 1,
            3, 2,
            2, 3,
            1, 4
        );
            
        for (Map.Entry<Integer, Integer> entry : requiredShips.entrySet()) {
            int length = entry.getKey();
            int requiredCount = entry.getValue();
            long actualCount = shipsByLength.getOrDefault(length, 0L);
                
            if (actualCount != requiredCount) {
                return false;
            }
        }
            
        return true;
        // // Проверяем по количеству клеток кораблей
        // long totalShipCells = ships.stream()
        //         .mapToInt(Ship::getLength)
        //         .sum();
        
        // return totalShipCells == 20; // 4+3+3+2+2+2+1+1+1+1 = 20
    }

        

    /**
     * Выстрел по доске
     * @return true если попал, false если промах
     */
    public boolean takeShot(int x, int y) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid coordinates");
        }

        Cell cell = cells[x][y];
        
        if (cell.getStatus() == CellStatus.SHIP) {
            cell.setStatus(CellStatus.HIT);
            if (cell.getShip() != null) {
                cell.getShip().hit();
            }
            return true;
        } else if (cell.getStatus() == CellStatus.EMPTY) {
            cell.setStatus(CellStatus.MISS);
            return false;
        }
        
        // Если уже стреляли в эту клетку
        return false;
    }

    /**
     * Проверка, все ли корабли потоплены
     */
    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    /**
     * Получение копии доски для противника (без информации о кораблях)
     */
    public Cell[][] getOpponentView() {
        Cell[][] opponentView = new Cell[BOARD_SIZE][BOARD_SIZE];
        
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                Cell original = cells[x][y];
                Cell copy = new Cell(x, y);
                
                // Скрываем не потопленные корабли
                if (original.getStatus() == CellStatus.SHIP) {
                    copy.setStatus(CellStatus.EMPTY);
                } else {
                    copy.setStatus(original.getStatus());
                }
                
                opponentView[x][y] = copy;
            }
        }
        
        return opponentView;
    }

    /**
     * Получение текущего состояния доски
     */
    public String[][] getBoardState(boolean isOwner) {
        String[][] state = new String[BOARD_SIZE][BOARD_SIZE];
        
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                Cell cell = cells[x][y];
                if (isOwner) {
                    state[x][y] = cell.getStatus().toString();
                } else {
                    // Для противника скрываем не потопленные корабли
                    if (cell.getStatus() == CellStatus.SHIP) {
                        state[x][y] = CellStatus.EMPTY.toString();
                    } else {
                        state[x][y] = cell.getStatus().toString();
                    }
                }
            }
        }
        
        return state;
    }
}
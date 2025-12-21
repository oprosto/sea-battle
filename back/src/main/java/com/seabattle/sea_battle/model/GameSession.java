// back\src\main\java\com\seabattle\sea_battle\model\GameSession.java
package com.seabattle.sea_battle.model;

import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
public class GameSession {
    private UUID sessionId;
    private Player player1;
    private Player player2;
    private Board player1Board;
    private Board player2Board;
    private GameType gameType;
    private GameStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String currentTurn; // username игрока, чей сейчас ход
    private boolean player1Ready;
    private boolean player2Ready;
    
    public GameSession(Player player1, GameType gameType) {
        this.sessionId = UUID.randomUUID();
        this.player1 = player1;
        this.gameType = gameType;
        this.status = GameStatus.WAITING_FOR_PLAYER;
        this.createdAt = LocalDateTime.now();
        this.player1Board = new Board(player1.getUsername());
        this.currentTurn = player1.getUsername();
        this.player1Ready = false;
        this.player2Ready = false;
    }
    
    // Метод для подключения второго игрока
    public void connectSecondPlayer(Player player2) {
        if (this.player2 != null) {
            throw new IllegalStateException("Second player already connected");
        }
        
        this.player2 = player2;
        this.player2Board = new Board(player2.getUsername());
        
        if (gameType == GameType.PVE && player2.isAI()) {
            // Для PVE начинаем сразу, если второй игрок - AI
            this.status = GameStatus.PLACING_SHIPS;
            // AI автоматически расставляет корабли
            player2Board.placeShipsAutomatically();
            player2Ready = true;
        } else if (gameType == GameType.PVP && !player2.isAI()) {
            // Для PVP начинаем когда подключился реальный игрок
            this.status = GameStatus.PLACING_SHIPS;
        }
    }
    
    /**
     * Игрок готов к игре (расставил корабли)
     */
    public synchronized void playerReady(String playerName) {
        if (playerName.equals(player1.getUsername())) {
            player1Ready = true;
        } else if (player2 != null && playerName.equals(player2.getUsername())) {
            player2Ready = true;
        }
        
        // Если оба игрока готовы, начинаем игру
        if (player1Ready && player2Ready) {
            status = GameStatus.IN_PROGRESS;
            startedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Выстрел по противнику
     */
    public FireResult fireAtOpponent(String shooterName, int x, int y) {
        if (!isPlayerTurn(shooterName)) {
            throw new IllegalStateException("Not your turn");
        }
        
        boolean hit;
        boolean sunk = false;
        boolean gameOver = false;
        
        if (shooterName.equals(player1.getUsername())) {
            // Игрок 1 стреляет по игроку 2
            hit = player2Board.takeShot(x, y);
            if (hit) {
                // Проверяем, потоплен ли корабль
                Cell cell = player2Board.getCells()[x][y];
                if (cell.getShip() != null && cell.getShip().isSunk()) {
                    sunk = true;
                }
                // Проверяем конец игры
                if (player2Board.allShipsSunk()) {
                    gameOver = true;
                    finishGame(GameStatus.PLAYER1_WON);
                }
            }
        } else {
            // Игрок 2 стреляет по игроку 1
            hit = player1Board.takeShot(x, y);
            if (hit) {
                Cell cell = player1Board.getCells()[x][y];
                if (cell.getShip() != null && cell.getShip().isSunk()) {
                    sunk = true;
                }
                if (player1Board.allShipsSunk()) {
                    gameOver = true;
                    if (player2.isAI()) {
                        finishGame(GameStatus.AI_WON);
                    } else {
                        finishGame(GameStatus.PLAYER2_WON);
                    }
                }
            }
        }
        
        // Если был промах - передаем ход
        if (!hit) {
            switchTurn();
        }
        
        return new FireResult(hit, sunk, gameOver);
    }
    
    /**
     * Проверка, чей сейчас ход
     */
    public boolean isPlayerTurn(String playerName) {
        return currentTurn.equals(playerName);
    }
    
    /**
     * Смена хода
     */
    private void switchTurn() {
        if (currentTurn.equals(player1.getUsername())) {
            currentTurn = player2.getUsername();
        } else {
            currentTurn = player1.getUsername();
        }
    }
    
    /**
     * Получение доски игрока
     */
    public Board getPlayerBoard(String playerName) {
        if (playerName.equals(player1.getUsername())) {
            return player1Board;
        } else if (player2 != null && playerName.equals(player2.getUsername())) {
            return player2Board;
        }
        throw new IllegalArgumentException("Player not found in this session");
    }
    
    /**
     * Получение доски противника для просмотра
     */
    public Board getOpponentBoard(String playerName) {
        if (playerName.equals(player1.getUsername())) {
            return player2Board;
        } else if (player2 != null && playerName.equals(player2.getUsername())) {
            return player1Board;
        }
        throw new IllegalArgumentException("Player not found in this session");
    }
    
    // Метод для завершения игры
    public void finishGame(GameStatus finalStatus) {
        this.status = finalStatus;
        this.finishedAt = LocalDateTime.now();
    }
    
    /**
     * Внутренний класс для результата выстрела
     */
    @Getter
    public static class FireResult {
        private final boolean hit;
        private final boolean sunk;
        private final boolean gameOver;
        
        public FireResult(boolean hit, boolean sunk, boolean gameOver) {
            this.hit = hit;
            this.sunk = sunk;
            this.gameOver = gameOver;
        }
    }
}
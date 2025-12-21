// back\src\main\java\com\seabattle\sea_battle\service\AIService.java
package com.seabattle.sea_battle.service;

import com.seabattle.sea_battle.model.*;
import com.seabattle.sea_battle.model.enums.CellStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AIService {
    
    public Player createAIPlayer() {
        return new Player("AI_Player_" + UUID.randomUUID().toString().substring(0, 8), true);
    }
    
    /**
     * AI делает ход
     */
    public void makeAIMove(GameSession session) {
        if (!session.getPlayer2().isAI()) {
            return; // Не AI игрок
        }
        
        // Простая логика AI: сначала случайные выстрелы, после попадания - вокруг
        Board opponentBoard = session.getOpponentBoard(session.getPlayer2().getUsername());
        int[] target = findTarget(opponentBoard);
        
        if (target != null) {
            session.fireAtOpponent(session.getPlayer2().getUsername(), target[0], target[1]);
        }
    }
    
    private int[] findTarget(Board board) {
        Cell[][] cells = board.getCells();
        List<int[]> possibleTargets = new ArrayList<>();
        
        // Сначала ищем клетки для "умного" выстрела (вокруг попаданий)
        List<int[]> hitCells = new ArrayList<>();
        
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                if (cells[x][y].getStatus() == CellStatus.HIT) {
                    hitCells.add(new int[]{x, y});
                }
            }
        }
        
        // Если есть попадания, ищем клетки вокруг них
        for (int[] hit : hitCells) {
            int x = hit[0];
            int y = hit[1];
            
            // Проверяем соседние клетки
            addPossibleTarget(possibleTargets, cells, x-1, y);
            addPossibleTarget(possibleTargets, cells, x+1, y);
            addPossibleTarget(possibleTargets, cells, x, y-1);
            addPossibleTarget(possibleTargets, cells, x, y+1);
        }
        
        // Если есть возможные цели для "умного" выстрела
        if (!possibleTargets.isEmpty()) {
            return possibleTargets.get(new Random().nextInt(possibleTargets.size()));
        }
        
        // Иначе случайный выстрел
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                if (cells[x][y].getStatus() == CellStatus.EMPTY) {
                    possibleTargets.add(new int[]{x, y});
                }
            }
        }
        
        if (!possibleTargets.isEmpty()) {
            return possibleTargets.get(new Random().nextInt(possibleTargets.size()));
        }
        
        return null;
    }
    
    private void addPossibleTarget(List<int[]> targets, Cell[][] cells, int x, int y) {
        if (x >= 0 && x < cells.length && y >= 0 && y < cells[0].length) {
            if (cells[x][y].getStatus() == CellStatus.EMPTY) {
                targets.add(new int[]{x, y});
            }
        }
    }
}
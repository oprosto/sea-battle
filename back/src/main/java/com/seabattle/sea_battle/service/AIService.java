// C:\sea-battle\src\main\java\com\seabattle\sea_battle\service\AIService.java
package com.seabattle.sea_battle.service;

import com.seabattle.sea_battle.model.GameSession;
import com.seabattle.sea_battle.model.Player;

import org.springframework.stereotype.Service;

@Service
public class AIService {
    
    private int aiCounter = 1;
    
    public Player createAIPlayer() {
        String aiName = "AI_Player_" + aiCounter++;
        return new Player(aiName, true);
    }
    
    // В дальнейшем здесь будет логика ходов AI
    public void makeAIMove(GameSession session) {
        // TODO: Реализовать логику хода AI
    }
}
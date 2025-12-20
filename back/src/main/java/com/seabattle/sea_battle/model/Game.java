// C:\sea-battle\src\main\java\com\seabattle\sea_battle\model\Game.java
package com.seabattle.sea_battle.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String player1Name;
    
    private String player2Name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus result;
    
    @Column(nullable = false)
    private LocalDateTime startedAt;
    
    @Column(nullable = false)
    private LocalDateTime finishedAt;
    
    public Game() {}
    
    public Game(String player1Name, String player2Name, GameType gameType, 
                GameStatus result, LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.gameType = gameType;
        this.result = result;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }
}
// back\src\main\java\com\seabattle\sea_battle\repository\GameRepository.java
package com.seabattle.sea_battle.repository;

import com.seabattle.sea_battle.model.Game;
import com.seabattle.sea_battle.model.enums.GameStatus;
import com.seabattle.sea_battle.model.enums.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    
    /**
     * Найти все завершенные игры
     */
    List<Game> findByResultIn(List<GameStatus> statuses);
    
    /**
     * Найти все игры по типу (PvP или PvE)
     */
    List<Game> findByGameType(GameType gameType);
    
    /**
     * Найти игры по имени игрока (player1 или player2)
     */
    List<Game> findByPlayer1NameOrPlayer2Name(String player1Name, String player2Name);
    
    /**
     * Найти игры, в которых участвовал конкретный игрок
     */
    @Query("SELECT g FROM Game g WHERE g.player1Name = :playerName OR g.player2Name = :playerName")
    List<Game> findGamesByPlayerName(@Param("playerName") String playerName);
    
    /**
     * Найти игры по статусу
     */
    List<Game> findByResult(GameStatus result);
    
    /**
     * Найти игры, завершенные после указанной даты
     */
    List<Game> findByFinishedAtAfter(LocalDateTime date);
    
    /**
     * Найти игры, завершенные до указанной даты
     */
    List<Game> findByFinishedAtBefore(LocalDateTime date);
    
    /**
     * Найти игры за определенный период
     */
    List<Game> findByFinishedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Найти игры по победителю (на основе статуса)
     * Для PvP игр можно определить победителя по статусу
     */
    @Query("SELECT g FROM Game g WHERE " +
           "(g.gameType = 'PVP' AND " +
           "((g.result = 'PLAYER1_WON' AND g.player1Name = :playerName) OR " +
           "(g.result = 'PLAYER2_WON' AND g.player2Name = :playerName))) OR " +
           "(g.gameType = 'PVE' AND g.result = 'PLAYER_WON' AND g.player1Name = :playerName)")
    List<Game> findWonGamesByPlayer(@Param("playerName") String playerName);
    
    /**
     * Посчитать количество игр по типу
     */
    long countByGameType(GameType gameType);
    
    /**
     * Посчитать количество побед игрока
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE " +
           "(g.gameType = 'PVP' AND " +
           "((g.result = 'PLAYER1_WON' AND g.player1Name = :playerName) OR " +
           "(g.result = 'PLAYER2_WON' AND g.player2Name = :playerName))) OR " +
           "(g.gameType = 'PVE' AND g.result = 'PLAYER_WON' AND g.player1Name = :playerName)")
    long countWinsByPlayer(@Param("playerName") String playerName);
}
// back\src\main\java\com\seabattle\sea_battle\controller\GamePlayController.java
package com.seabattle.sea_battle.controller;

import com.seabattle.sea_battle.dto.*;
import com.seabattle.sea_battle.service.GamePlayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gameplay")
public class GamePlayController {
    
    private final GamePlayService gamePlayService;
    
    public GamePlayController(GamePlayService gamePlayService) {
        this.gamePlayService = gamePlayService;
    }
    
    /**
     * Расстановка кораблей игроком
     */
    @PostMapping("/{sessionId}/place-ships")
    public ResponseEntity<?> placeShips(@PathVariable UUID sessionId,
                                         @RequestBody PlaceShipsRequest request) {
        try {
            boolean success = gamePlayService.placeShips(sessionId, request);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "Ships placed successfully",
                    "sessionId", sessionId,
                    "player", request.getPlayerName(),
                    "ready", gamePlayService.isPlayerReady(sessionId, request.getPlayerName())
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid ship placement"
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to place ships"
            ));
        }
    }
    
    /**
     * Автоматическая расстановка кораблей
     */
    @PostMapping("/{sessionId}/auto-place-ships")
    public ResponseEntity<?> autoPlaceShips(@PathVariable UUID sessionId,
                                            @RequestBody Map<String, String> request) {
        try {
            String playerName = request.get("playerName");
            gamePlayService.autoPlaceShips(sessionId, playerName);
            
            return ResponseEntity.ok(Map.of(
                "message", "Ships placed automatically",
                "sessionId", sessionId,
                "player", playerName,
                "ready", gamePlayService.isPlayerReady(sessionId, playerName)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    // /**
    //  * Получение состояния доски
    //  */
    // @GetMapping("/{sessionId}/board")
    // public ResponseEntity<?> getBoardState(@PathVariable UUID sessionId,
    //                                        @RequestParam String playerName,
    //                                        @RequestParam(required = false, defaultValue = "true") boolean isOwner) {
    //     try {
    //         BoardStateResponse response = gamePlayService.getBoardState(sessionId, playerName, isOwner);
    //         return ResponseEntity.ok(response);
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "error", e.getMessage()
    //         ));
    //     }
    // }
    
    // /**
    //  * Выстрел по противнику
    //  */
    // @PostMapping("/{sessionId}/fire")
    // public ResponseEntity<?> fire(@PathVariable UUID sessionId,
    //                                @RequestBody FireRequest request) {
    //     try {
    //         FireResponse response = gamePlayService.fire(sessionId, request);
    //         return ResponseEntity.ok(response);
    //     } catch (IllegalStateException e) {
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "error", e.getMessage()
    //         ));
    //     } catch (Exception e) {
    //         return ResponseEntity.internalServerError().body(Map.of(
    //             "error", "Failed to process fire"
    //         ));
    //     }
    // }
    
    // /**
    //  * Получение информации о текущем ходе
    //  */
    // @GetMapping("/{sessionId}/turn")
    // public ResponseEntity<?> getCurrentTurn(@PathVariable UUID sessionId) {
    //     try {
    //         String currentPlayer = gamePlayService.getCurrentTurn(sessionId);
    //         return ResponseEntity.ok(Map.of(
    //             "currentTurn", currentPlayer
    //         ));
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "error", e.getMessage()
    //         ));
    //     }
    // }
    
    // /**
    //  * Проверка готовности игроков
    //  */
    // @GetMapping("/{sessionId}/ready-status")
    // public ResponseEntity<?> getReadyStatus(@PathVariable UUID sessionId) {
    //     try {
    //         Map<String, Boolean> readyStatus = gamePlayService.getPlayersReadyStatus(sessionId);
    //         return ResponseEntity.ok(readyStatus);
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "error", e.getMessage()
    //         ));
    //     }
    // }
    
    // /**
    //  * Получение информации о состоянии игры
    //  */
    // @GetMapping("/{sessionId}/game-state")
    // public ResponseEntity<?> getGameState(@PathVariable UUID sessionId,
    //                                       @RequestParam String playerName) {
    //     try {
    //         Map<String, Object> gameState = gamePlayService.getGameState(sessionId, playerName);
    //         return ResponseEntity.ok(gameState);
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "error", e.getMessage()
    //         ));
    //     }
    // }
}
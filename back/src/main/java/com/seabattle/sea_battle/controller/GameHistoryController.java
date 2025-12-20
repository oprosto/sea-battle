// // C:\sea-battle\src\main\java\com\seabattle\sea_battle\controller\GameHistoryController.java
// package com.seabattle.sea_battle.controller;

// import com.seabattle.sea_battle.dto.GameHistoryResponse;
// import com.seabattle.sea_battle.model.enums.GameType;
// import com.seabattle.sea_battle.service.GameService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/history")
// public class GameHistoryController {
    
//     private final GameService gameService;
    
//     public GameHistoryController(GameService gameService) {
//         this.gameService = gameService;
//     }
    
//     /**
//      * Получение полной истории игр
//      */
//     @GetMapping("/games")
//     public ResponseEntity<?> getAllGamesHistory() {
//         try {
//             List<GameHistoryResponse> history = gameService.getAllGameHistory();
            
//             return ResponseEntity.ok(Map.of(
//                 "totalGames", history.size(),
//                 "games", history
//             ));
//         } catch (Exception e) {
//             return ResponseEntity.internalServerError().body(
//                 Map.of("error", "Failed to retrieve game history")
//             );
//         }
//     }
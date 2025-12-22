// back\src\main\java\com\seabattle\sea_battle\controller\GameUpdateController.java
package com.seabattle.sea_battle.controller;

import com.seabattle.sea_battle.dto.GameUpdate;
import com.seabattle.sea_battle.service.GameNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/gameupdates")
public class GameUpdateController {
    
    private final GameNotificationService notificationService;
    private static final long LONG_POLLING_TIMEOUT = 30000; // 30 секунд
    
    public GameUpdateController(GameNotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Long polling эндпоинт для получения обновлений игры
     */
    @GetMapping("/subscribe")
    public DeferredResult<ResponseEntity<?>> subscribe(
            @RequestParam String playerName,
            @RequestParam UUID sessionId,
            @RequestParam(defaultValue = "0") long lastUpdateId) {
        
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(
            LONG_POLLING_TIMEOUT,
            ResponseEntity.noContent().build()
        );
        
        // Подписываемся на обновления
        CompletableFuture<GameUpdate> updateFuture = 
            notificationService.subscribe(playerName, sessionId, lastUpdateId);
        
        // Настраиваем таймауты
        updateFuture.thenAccept(update -> {
            if (update != null) {
                deferredResult.setResult(ResponseEntity.ok(update));
            }
        });
        
        deferredResult.onTimeout(() -> {
            deferredResult.setResult(ResponseEntity.noContent().build());
        });
        
        deferredResult.onError(throwable -> {
            deferredResult.setErrorResult(
                ResponseEntity.internalServerError().body(
                    Map.of("error", "Subscription error")
                )
            );
        });
        
        return deferredResult;
    }
    
    /**
     * Отписка от обновлений
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> request) {
        String playerName = request.get("playerName");
        if (playerName != null) {
            notificationService.unsubscribe(playerName);
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Successfully unsubscribed",
            "player", playerName
        ));
    }
    
    /**
     * Получение последних обновлений без ожидания
     */
    @GetMapping("/poll")
    public ResponseEntity<?> pollUpdates(
            @RequestParam String playerName,
            @RequestParam UUID sessionId,
            @RequestParam(defaultValue = "0") long lastUpdateId) {
        
        try {
            // Блокирующий вызов с коротким таймаутом
            GameUpdate update = notificationService.subscribe(playerName, sessionId, lastUpdateId)
                    .get(1, TimeUnit.SECONDS);
            
            if (update != null) {
                return ResponseEntity.ok(update);
            }
        } catch (Exception e) {
            // Таймаут - нормальная ситуация
        }
        
        return ResponseEntity.noContent().build();
    }
}
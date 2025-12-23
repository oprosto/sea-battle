// back\src\main\java\com\seabattle\sea_battle\config\GameProperties.java
package com.seabattle.sea_battle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "game")
public class GameProperties {
    private int maxActiveSessions = 10;    // Максимум 10 активных сессий
    private long sessionTimeout = 300000;  // 5 минут таймаут для ожидания игрока (в мс)
    private long cleanupInterval = 60000;  // Интервал очистки (1 минута)
    
    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }
    
    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }
    
    public long getSessionTimeout() {
        return sessionTimeout;
    }
    
    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    
    public long getCleanupInterval() {
        return cleanupInterval;
    }
    
    public void setCleanupInterval(long cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
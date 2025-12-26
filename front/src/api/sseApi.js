// api/sseApi.js
import { api } from './http'

export const sseApi = {
    // Подключение к SSE потоку
    connectToGameEvents(sessionId, playerName, onMessage) {
        const eventSource = new EventSource(
            `http://localhost:8080/api/sse/game/${sessionId}?playerName=${encodeURIComponent(playerName)}`
        )

        eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data)
                onMessage(data)
            } catch (e) {
                console.error('Ошибка парсинга SSE сообщения:', e)
            }
        }

        eventSource.onerror = (error) => {
            console.error('SSE ошибка:', error)
            eventSource.close()
        }

        return eventSource
    },

    heartbeat() {
        return api.post('/sse/heartbeat')
    },

    getConnections() {
        return api.get('/sse/connections')
    }
}
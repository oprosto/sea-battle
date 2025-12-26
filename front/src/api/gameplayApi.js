// api/gameplayApi.js
import { api } from './http'

export const gameplayApi = {
    placeShips(sessionId, playerName, shipsData) {
        console.log('Отправка кораблей в формате бэкенда:', {
            sessionId,
            playerName,
            shipsCount: shipsData.ships?.length
        })

        // shipsData должен содержать { playerName, ships: [...] }
        const payload = {
            playerName: playerName,
            ships: shipsData.ships // массив объектов {x, y, length, horizontal}
        }

        console.log('Payload для бэкенда:', payload)
        return api.post(`/gameplay/${sessionId}/place-ships`, payload)
    },

    autoPlaceShips(sessionId, playerName) {
        return api.post(`/gameplay/${sessionId}/auto-place-ships`, {
            playerName
        })
    },

    autoPlaceShips(sessionId, playerName) {
        return api.post(`/gameplay/${sessionId}/auto-place-ships`, {
            playerName // playerName в теле запроса
        })
    },

    getBoard(sessionId, playerName, isOwner = true) {
        if (!sessionId) throw new Error('sessionId не передан в getBoard')
        if (!playerName) throw new Error('playerName не передан в getBoard')

        return api.get(`/gameplay/${sessionId}/board`, {
            params: {
                playerName,
                isOwner
            }
        })
    },

    fire(sessionId, playerName, row, col) {
        // row = y (строка), col = x (столбец)
        // Бэкенд ожидает: x = col, y = row
        console.log('API fire: отправка выстрела', {
            x: col,     // столбец -> x
            y: row,     // строка -> y
            playerName: playerName
        })

        return api.post(`/gameplay/${sessionId}/fire`, {
            x: col,     // столбец
            y: row,     // строка
            playerName: playerName
        })
    },

    getTurn(sessionId) {
        return api.get(`/gameplay/${sessionId}/turn`)
    },

    getReadyStatus(sessionId, playerName) {
        return api.get(`/gameplay/${sessionId}/ready-status`, {
            params: { playerName }
        })
    },

    getGameState(sessionId, playerName) {
        return api.get(`/gameplay/${sessionId}/game-state`, {
            params: { playerName }
        })
    },

    markReady(sessionId, playerName) {
        return api.post(`/gameplay/${sessionId}/ready`, {
            playerName
        })
    },
}
import axios from 'axios'

// Базовый URL Spring бэкенда
const API_BASE_URL = 'http://localhost:8080/api'

// Создаём экземпляр axios
const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }
})

// Интерцептор для обработки ошибок
api.interceptors.response.use(
    response => response.data,
    error => {
        const message = error.response?.data?.message || error.message || 'Ошибка сервера'
        console.error('API Error:', message)
        return Promise.reject({ message, status: error.response?.status })
    }
)

// API методы для игры
export const gameApi = {
    // 1. Создание игровой сессии
    async createGame(mode = 'PVE') {
        return api.post('/games', { mode })
    },

    // 2. Присоединение к игре (для PvP)
    async joinGame(gameId, playerName) {
        return api.post(`/games/${gameId}/join`, { playerName })
    },

    // 3. Получение состояния игры
    async getGameState(gameId) {
        return api.get(`/games/${gameId}`)
    },

    // 4. Расстановка кораблей
    async placeShips(gameId, playerId, ships) {
        return api.post(`/games/${gameId}/ships`, { playerId, ships })
    },

    // 5. Ход игрока
    async makeMove(gameId, playerId, row, col) {
        return api.post(`/games/${gameId}/move`, { playerId, row, col })
    },

    // 6. Пропуск хода
    async skipTurn(gameId, playerId) {
        return api.post(`/games/${gameId}/skip`, { playerId })
    },

    // 7. Сдача
    async surrender(gameId, playerId) {
        return api.post(`/games/${gameId}/surrender`, { playerId })
    },

    // 8. Получение истории игры
    async getGameHistory(gameId) {
        return api.get(`/games/${gameId}/history`)
    },

    // 9. Просмотр истории игр (все завершённые)
    async getCompletedGames(page = 0, size = 10) {
        return api.get('/games/completed', { params: { page, size } })
    },

    // 10. Получение активных игр пользователя
    async getActiveGames(playerId) {
        return api.get(`/players/${playerId}/games/active`)
    },

    // 11. Тестовый эндпоинт для проверки связи
    async healthCheck() {
        return api.get('/health')
    }
}

// Экспортируем базовый клиент для кастомных запросов
export default api
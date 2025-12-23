import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { gameApi } from '../api/gameapi'

export const useGameStore = defineStore('game', () => {
    // UI состояние (не игровая логика!)
    const currentGameId = ref(null)
    const playerId = ref(null)
    const gameMode = ref('PVE')
    const isLoading = ref(false)
    const error = ref(null)
    const notifications = ref([])

    // История игр (кешированная)
    const gameHistory = ref([])
    const playerStats = ref({
        totalGames: 0,
        wins: 0,
        losses: 0,
        winRate: 0
    })

    // Действия
    async function createNewGame(mode) {
        isLoading.value = true
        error.value = null

        try {
            const game = await gameApi.createGame(mode)
            currentGameId.value = game.id
            playerId.value = game.playerId
            gameMode.value = mode

            addNotification('Игра создана! ID: ' + game.id, 'success')
            return game
        } catch (err) {
            error.value = err.message
            addNotification('Ошибка создания игры: ' + err.message, 'error')
            throw err
        } finally {
            isLoading.value = false
        }
    }

    async function joinGame(gameId, playerName) {
        isLoading.value = true

        try {
            const result = await gameApi.joinGame(gameId, playerName)
            currentGameId.value = gameId
            playerId.value = result.playerId

            addNotification('Присоединились к игре ' + gameId, 'success')
            return result
        } catch (err) {
            error.value = err.message
            addNotification('Ошибка присоединения: ' + err.message, 'error')
            throw err
        } finally {
            isLoading.value = false
        }
    }

    async function loadGameHistory() {
        try {
            const history = await gameApi.getCompletedGames()
            gameHistory.value = history.content || history
        } catch (err) {
            console.error('Ошибка загрузки истории:', err)
        }
    }

    async function loadPlayerStats() {
        if (!playerId.value) return

        try {
            // Загружаем статистику с бэкенда
            const stats = await gameApi.getCompletedGames()
            const playerGames = stats.filter(game =>
                game.players.some(p => p.id === playerId.value)
            )

            playerStats.value = {
                totalGames: playerGames.length,
                wins: playerGames.filter(g => g.winnerId === playerId.value).length,
                losses: playerGames.filter(g => g.winnerId && g.winnerId !== playerId.value).length,
                winRate: playerGames.length > 0
                    ? Math.round((playerGames.filter(g => g.winnerId === playerId.value).length / playerGames.length) * 100)
                    : 0
            }
        } catch (err) {
            console.error('Ошибка загрузки статистики:', err)
        }
    }

    function addNotification(message, type = 'info') {
        notifications.value.push({
            id: Date.now(),
            message,
            type,
            timestamp: new Date()
        })

        // Автоудаление через 5 секунд
        setTimeout(() => {
            notifications.value = notifications.value.filter(n => n.id !== notifications.value[0]?.id)
        }, 5000)
    }

    function clearError() {
        error.value = null
    }

    function clearNotifications() {
        notifications.value = []
    }

    function resetGame() {
        currentGameId.value = null
        playerId.value = null
        gameMode.value = 'PVE'
        error.value = null
    }

    return {
        // Состояние
        currentGameId,
        playerId,
        gameMode,
        isLoading,
        error,
        notifications,
        gameHistory,
        playerStats,

        // Геттеры
        isInGame: computed(() => !!currentGameId.value),
        hasError: computed(() => !!error.value),

        // Действия
        createNewGame,
        joinGame,
        loadGameHistory,
        loadPlayerStats,
        addNotification,
        clearError,
        clearNotifications,
        resetGame
    }
})
// gamestore.js
import { defineStore } from 'pinia'
import { gameApi } from '../api/gameApi'
import { gameplayApi } from '../api/gameplayApi'
import { historyApi } from '../api/historyApi'

let notificationId = 0

export const useGameStore = defineStore('game', {
    state: () => ({
        // Общие состояния
        isLoading: false,
        isInGame: false,

        // Игра
        playerName: '',
        playerId: null,
        currentGameId: null,
        gameMode: null,

        gameState: null,
        playerBoard: [],
        enemyBoard: [],
        moves: [],

        // История
        gameHistory: [],

        // UI
        notifications: [],
        sessions: [],

        eventSource: null
    }),

    actions: {
        /* =======================
           UI helpers
        ======================= */
        addNotification(message, type = 'info') {
            const id = ++notificationId
            this.notifications.push({ id, message, type })

            setTimeout(() => {
                this.notifications = this.notifications.filter(n => n.id !== id)
            }, 4000)
        },

        setLoading(value) {
            this.isLoading = value
        },

        resetGame() {
            this.disconnectSSE()
            this.isInGame = false
            this.currentGameId = null
            this.gameMode = null
            this.gameState = null
            this.playerBoard = []
            this.enemyBoard = []
            this.moves = []
            this.playerName = ''
            this.playerId = null
        },

        /* =======================
           Server / GameController
        ======================= */
        async checkServer() {
            try {
                await gameApi.getStatus()
            } catch (e) {
                this.addNotification('Сервер недоступен', 'error')
                throw e
            }
        },

        async createNewGame(mode) {
            try {
                this.setLoading(true)

                if (!this.playerName) {
                    this.addNotification('Имя игрока не установлено', 'error')
                    return
                }

                console.log('Создание игры:', {
                    playerName: this.playerName,
                    gameType: mode
                })

                // 1. Создаем игру
                const { data } = await gameApi.createSession({
                    playerName: this.playerName,
                    gameType: mode
                })

                console.log('Игра создана:', data)

                this.currentGameId = data.sessionId || data.id
                this.playerId = data.playerId || this.playerName
                this.gameMode = mode
                this.isInGame = true

                this.addNotification('Игра создана. Расставьте корабли.', 'success')

                // 2. Инициализируем пустые доски
                this.playerBoard = Array(10).fill().map(() => Array(10).fill('empty'))
                this.enemyBoard = Array(10).fill().map(() => Array(10).fill('empty'))

                // 3. Устанавливаем начальное состояние
                this.gameState = {
                    gameStarted: false,
                    currentPlayer: null,
                    winner: null,
                    status: 'WAITING_FOR_PLAYERS',
                    message: 'Расставьте корабли'
                }

            } catch (e) {
                console.error('Ошибка создания игры:', e.response?.data || e.message)
                this.addNotification(e.response?.data?.error || e.message || 'Ошибка сервера', 'error')
                throw e
            } finally {
                this.setLoading(false)
            }
        },

        async joinGame(sessionId, playerName) {
            try {
                this.setLoading(true)
                this.playerName = playerName

                // 1. Подключаемся к игре
                const { data } = await gameApi.joinSession(sessionId, { playerName })

                this.currentGameId = data.sessionId || sessionId
                this.playerId = data.playerId || playerName
                this.gameMode = 'PVP'
                this.isInGame = true

                this.addNotification('Вы подключились к игре', 'success')

                // 2. Сразу расставляем корабли автоматически
                await this.autoPlaceShips()

                // 3. Загружаем состояние
                await this.loadGameState()
                await this.loadBoards()

            } catch (e) {
                console.error('Ошибка подключения:', e.response?.data || e.message)
                this.addNotification(e.response?.data?.error || e.message || 'Ошибка сервера', 'error')
                throw e
            } finally {
                this.setLoading(false)
            }
        },

        /* =======================
           GamePlayController
        ======================= */
        async loadGameState() {
            if (!this.currentGameId || !this.playerName) {
                console.warn('Не хватает данных для загрузки состояния')
                return
            }

            try {
                console.log('Загрузка состояния игры:', {
                    sessionId: this.currentGameId,
                    playerName: this.playerName
                })

                const { data } = await gameplayApi.getGameState(this.currentGameId, this.playerName)

                console.log('Данные состояния от сервера:', data)

                // Если board is null, значит игра еще не началась
                if (data.status === "WAITING_FOR_PLAYERS" || !data.playerBoard) {
                    this.gameState = {
                        gameStarted: false,
                        currentPlayer: null,
                        winner: null,
                        status: 'WAITING_FOR_PLAYERS',
                        message: 'Расставьте корабли'
                    }
                    return
                }

                // Игра активна
                this.gameState = {
                    gameStarted: data.status === "IN_PROGRESS",
                    currentPlayer: data.currentTurn,
                    winner: data.winner || null,
                    status: data.status,
                    players: {
                        player1: data.player1,
                        player2: data.player2
                    },
                    rawData: data
                }

                console.log('Состояние игры установлено:', this.gameState)

            } catch (e) {
                console.error('Ошибка загрузки состояния:', e.response?.data || e.message)

                // Если ошибка "board is null", значит игра еще не началась
                if (e.response?.status === 400 &&
                    e.response?.data?.error?.includes('board is null')) {
                    console.log('Игра еще не началась, ждем расстановки кораблей')
                    this.gameState = {
                        gameStarted: false,
                        currentPlayer: null,
                        winner: null,
                        status: 'WAITING_FOR_PLAYERS',
                        message: 'Расставьте корабли'
                    }
                    return
                }

                this.gameState = {
                    gameStarted: false,
                    currentPlayer: null,
                    winner: null,
                    status: 'ERROR',
                    message: 'Ошибка загрузки'
                }
            }
        },

        async loadBoards() {
            if (!this.currentGameId || !this.playerName) {
                console.warn('Не хватает данных для загрузки досок')
                return
            }

            // Если игра еще не началась, НЕ загружаем доски с сервера
            if (this.gameState?.status === 'WAITING_FOR_PLAYERS') {
                console.log('Игра не началась, пропускаем загрузку досок')
                return
            }

            try {
                console.log('Загрузка досок с сервера:', {
                    sessionId: this.currentGameId,
                    playerName: this.playerName
                })

                const { data } = await gameplayApi.getBoard(this.currentGameId, this.playerName, true)

                console.log('Данные доски от сервера:', data)

                // Только если есть данные, обновляем
                if (data.board && Array.isArray(data.board) && data.board.length > 0) {
                    this.playerBoard = data.board.map(row =>
                        row.map(cell => {
                            if (cell === "SHIP") return "ship"
                            if (cell === "HIT") return "hit"
                            if (cell === "MISS") return "miss"
                            return "empty"
                        })
                    )
                }

                // Для вражеской доски - только если игра началась
                if (this.gameState?.gameStarted && this.enemyBoard.length === 0) {
                    this.enemyBoard = Array(10).fill().map(() => Array(10).fill('empty'))
                }

                console.log('Доски обновлены. Игрок:', this.playerBoard.length)

            } catch (e) {
                console.error('Ошибка загрузки досок:', e.response?.data || e.message)

                // Если игра не началась - это нормально, не создаем пустые доски
                if (e.response?.status === 400 &&
                    e.response?.data?.error?.includes('board is null')) {
                    console.log('Доска на сервере еще не создана - это нормально для этапа расстановки')
                    return
                }
            }
        },

        async fire(row, col) {
            if (!this.currentGameId || !this.playerName) {
                this.addNotification('Нет активной игры', 'error')
                throw new Error('Нет активной игры')
            }

            try {
                console.log('Выстрел игрока:', {
                    player: this.playerName,
                    row: row,  // строка
                    col: col   // столбец
                })

                // row = y (строка), col = x (столбец)
                const response = await gameplayApi.fire(this.currentGameId, this.playerName, row, col)
                console.log('Результат выстрела:', response.data)

                // Добавляем ход в историю
                this.moves.push({
                    playerName: this.playerName,
                    row: row,
                    col: col,
                    result: response.data.hit ? 'Попадание!' : 'Промах',
                    timestamp: new Date().toISOString(),
                    description: `Выстрел по (${row}, ${col}) - ${response.data.hit ? 'Попадание' : 'Промах'}`
                })

                // Обновляем вражескую доску на основе ответа
                if (response.data.hit) {
                    this.updateEnemyBoardCell(row, col, 'hit')
                } else {
                    this.updateEnemyBoardCell(row, col, 'miss')
                }

                // Если в ответе есть обновленная доска
                if (response.data.opponentBoard) {
                    this.enemyBoard = this.convertBoard(response.data.opponentBoard, false)
                }

                this.addNotification(response.data.message || `Выстрел по ${String.fromCharCode(65 + col)}${row + 1}`, 'info')

                // Обновляем состояние игры
                await this.loadGameState()

                return response.data

            } catch (e) {
                console.error('Ошибка выстрела:', {
                    error: e.response?.data || e.message,
                    row,
                    col
                })

                if (e.response?.status === 400) {
                    if (e.response?.data?.error?.includes('Already fired')) {
                        this.addNotification('Вы уже стреляли в эту клетку', 'error')
                    } else if (e.response?.data?.error?.includes('Not your turn')) {
                        this.addNotification('Сейчас не ваш ход', 'error')
                    } else {
                        this.addNotification(e.response?.data?.error || 'Ошибка выстрела', 'error')
                    }
                } else {
                    this.addNotification('Не удалось сделать выстрел', 'error')
                }

                throw e
            }
        },

        // Новый метод для обновления доски после выстрела
        updateBoardAfterShot(row, col, shotResult) {
            console.log('Обновляем доску после выстрела:', shotResult)

            if (shotResult.hit === true) {
                this.updateEnemyBoardCell(row, col, 'hit')
            } else if (shotResult.hit === false) {
                this.updateEnemyBoardCell(row, col, 'miss')
            }

            // Если сервер вернул обновленную доску
            if (shotResult.board) {
                this.enemyBoard = this.convertBoard(shotResult.board, false)
            }
        },

        // Метод обновления конкретной клетки
        updateEnemyBoardCell(row, col, state) {
            if (!this.enemyBoard[row]) {
                this.enemyBoard[row] = Array(10).fill('empty')
            }
            this.enemyBoard[row][col] = state
            console.log(`Клетка [${row}][${col}] обновлена на: ${state}`)
        },

        // Конвертер доски из формата бэкенда
        convertBoard(backendBoard, showShips = false) {
            if (!backendBoard || !Array.isArray(backendBoard)) {
                return Array(10).fill().map(() => Array(10).fill('empty'))
            }

            return backendBoard.map(row =>
                row.map(cell => {
                    if (cell === 'HIT') return 'hit'
                    if (cell === 'MISS') return 'miss'
                    if (cell === 'SHIP' && showShips) return 'ship'
                    return 'empty'
                })
            )
        },

        async autoPlaceShips() {
            if (!this.currentGameId || !this.playerName) {
                this.addNotification('Нет активной игры', 'error')
                throw new Error('Нет активной игры для расстановки кораблей')
            }

            try {
                console.log('Авторасстановка кораблей...')

                const response = await gameplayApi.autoPlaceShips(this.currentGameId, this.playerName)
                console.log('Корабли расставлены автоматически:', response.data)

                this.addNotification('Корабли расставлены автоматически', 'success')

                // Сохраняем доску из ответа
                if (response.data.board) {
                    this.playerBoard = response.data.board.map(row =>
                        row.map(cell => {
                            if (cell === "SHIP") return "ship"
                            return "empty"
                        })
                    )
                }

                return response.data
            } catch (e) {
                console.error('Ошибка автоматической расстановки:', e.response?.data || e.message)
                this.addNotification(e.response?.data?.error || 'Ошибка автоматической расстановки', 'error')
                throw e
            }
        },

        async placeShipsManually(shipsData) {
            if (!this.currentGameId || !this.playerName) {
                this.addNotification('Нет активной игры', 'error')
                throw new Error('Нет активной игры')
            }

            try {
                console.log('Ручная расстановка кораблей - данные:', {
                    sessionId: this.currentGameId,
                    playerName: this.playerName,
                    shipsData: shipsData,
                    hasShips: !!shipsData.ships,
                    shipCount: shipsData.ships?.length
                })

                // Проверяем формат - должен быть ships массив
                if (!shipsData.ships || !Array.isArray(shipsData.ships)) {
                    console.error('Неверный формат кораблей:', shipsData)
                    throw new Error('Неверный формат кораблей. Ожидается {ships: [...]}')
                }

                const response = await gameplayApi.placeShips(this.currentGameId, this.playerName, shipsData)
                console.log('Корабли расставлены вручную:', response.data)

                this.addNotification('Корабли сохранены на сервере', 'success')

                // Обновляем доску на основе сохраненных кораблей
                this.updateBoardFromShips(shipsData.ships)

                return response.data
            } catch (e) {
                console.error('Ошибка ручной расстановки:', e.message, e.response?.data)
                this.addNotification(e.response?.data?.error || e.message || 'Ошибка расстановки кораблей', 'error')
                throw e
            }
        },

        updateBoardFromShips(ships) {
            // Создаем пустую доску
            const newBoard = Array(10).fill().map(() => Array(10).fill('empty'))

            // Размещаем корабли: ships[i].x = столбец, ships[i].y = строка
            ships.forEach(ship => {
                console.log('Размещаем корабль:', ship)
                for (let i = 0; i < ship.length; i++) {
                    const col = ship.horizontal ? ship.x + i : ship.x  // x = столбец
                    const row = ship.horizontal ? ship.y : ship.y + i  // y = строка

                    if (col >= 0 && col < 10 && row >= 0 && row < 10) {
                        newBoard[row][col] = 'ship'
                        console.log(`  Клетка [${row}][${col}] = ship`)
                    }
                }
            })

            this.playerBoard = newBoard
            console.log('Доска обновлена:', newBoard)
        },

        async markReady() {
            if (!this.currentGameId || !this.playerName) {
                throw new Error('Нет активной игры')
            }

            try {
                console.log('Отметка готовности:', {
                    sessionId: this.currentGameId,
                    playerName: this.playerName
                })

                // Если бэкенд имеет endpoint для готовности
                const response = await gameplayApi.markReady(this.currentGameId, this.playerName)
                console.log('Готовность подтверждена:', response.data)

                this.addNotification('Вы готовы к игре!', 'success')
                return response.data

            } catch (e) {
                console.error('Ошибка отметки готовности:', e.response?.data || e.message)

                // Если endpoint не существует, просто обновляем состояние
                if (this.gameState) {
                    this.gameState.status = 'IN_PROGRESS'
                    this.gameState.gameStarted = true
                }

                this.addNotification('Готовность отмечена локально', 'info')
                return { success: true }
            }
        },

        async checkReadyStatus() {
            if (!this.currentGameId || !this.playerName) {
                return null
            }

            try {
                const response = await gameplayApi.getReadyStatus(this.currentGameId, this.playerName)
                console.log('Статус готовности:', response.data)
                return response.data
            } catch (e) {
                console.error('Ошибка проверки готовности:', e)
                return null
            }
        },

        /* =======================
           История и сессии
        ======================= */
        async loadGameHistory() {
            try {
                const response = await historyApi.getCompletedGames()
                this.gameHistory = response.data?.games || response.data || []
                console.log('История загружена:', this.gameHistory.length, 'игр')
            } catch (e) {
                console.error('Ошибка загрузки истории:', e)
                this.gameHistory = []
            }
        },

        async loadActiveSessions() {
            try {
                const response = await gameApi.getActiveSessions()
                this.sessions = response.data?.sessions || response.data || []
                console.log('Активные сессии загружены:', this.sessions.length)
                return this.sessions
            } catch (e) {
                console.error('Ошибка загрузки сессий:', e)
                this.sessions = []
                return []
            }
        },

        async closeMyGames() {
            try {
                const sessions = await this.loadActiveSessions()
                let closedCount = 0

                for (const session of sessions) {
                    if (session.player1 === this.playerName ||
                        (session.player2 && session.player2 === this.playerName)) {
                        try {
                            await gameApi.cancelSession(session.sessionId || session.id)
                            closedCount++
                            console.log(`Сессия ${session.sessionId || session.id} отменена`)
                        } catch (e) {
                            console.error(`Не удалось закрыть сессию:`, e)
                        }
                    }
                }

                if (closedCount > 0) {
                    this.addNotification(`Закрыто ${closedCount} ваших игр`, 'info')
                }
                return closedCount
            } catch (e) {
                console.error('Ошибка закрытия игр:', e)
                return 0
            }
        },

        async surrender() {
            if (!this.currentGameId) {
                return
            }

            try {
                // TODO: Добавить endpoint для сдачи
                this.addNotification('Вы сдались', 'info')
                this.resetGame()
            } catch (e) {
                console.error('Ошибка сдачи:', e)
            }
        },

        async connectSSE() {
            if (!this.currentGameId || !this.playerName) {
                return
            }

            try {
                // Закрываем старое соединение
                if (this.eventSource) {
                    this.eventSource.close()
                }

                // Создаем новое
                this.eventSource = sseApi.connectToGameEvents(
                    this.currentGameId,
                    this.playerName,
                    (event) => {
                        this.handleSSEEvent(event)
                    }
                )

                console.log('SSE подключен')
            } catch (e) {
                console.error('Ошибка подключения SSE:', e)
            }
        },

        disconnectSSE() {
            if (this.eventSource) {
                this.eventSource.close()
                this.eventSource = null
                console.log('SSE отключен')
            }
        },

        handleSSEEvent(event) {
            console.log('SSE событие:', event)

            switch (event.type) {
                case 'GAME_STATE_UPDATE':
                    this.gameState = event.data
                    break

                case 'BOARD_UPDATE':
                    if (event.data.playerName === this.playerName) {
                        this.playerBoard = this.convertBoard(event.data.board, true)
                    } else {
                        this.enemyBoard = this.convertBoard(event.data.board, false)
                    }
                    break

                case 'SHOT_RESULT':
                    this.moves.push({
                        playerName: event.data.player,
                        row: event.data.row,
                        col: event.data.col,
                        result: event.data.hit ? 'Попадание' : 'Промах',
                        timestamp: new Date().toISOString()
                    })
                    break

                case 'TURN_CHANGE':
                    this.addNotification(`Сейчас ход: ${event.data.player}`, 'info')
                    if (this.gameState) {
                        this.gameState.currentPlayer = event.data.player
                    }
                    break

                case 'GAME_END':
                    this.addNotification(`Игра окончена! Победитель: ${event.data.winner}`, 'success')
                    if (this.gameState) {
                        this.gameState.winner = event.data.winner
                        this.gameState.status = 'FINISHED'
                    }
                    break
            }
        },
    }
})
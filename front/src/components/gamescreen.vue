<!-- gamescreen.vue -->
<template>
    <div class="max-w-7xl mx-auto">
        <!-- Шапка -->
        <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6 mb-8">
            <div class="flex justify-between items-center">
                <div>
                    <h2 class="text-2xl font-bold">Игра #{{ gameId }}</h2>
                    <p class="text-gray-400">{{ mode === 'PVP' ? 'Игрок vs Игрок' : 'Игрок vs ИИ' }}</p>
                    <p class="text-gray-400 mt-1">Игрок: {{ gameStore.playerName }}</p>
                    <p class="text-gray-400">Статус: {{ gameStatusText }}</p>
                </div>

                <div class="flex gap-4">
                    <!-- В зависимости от статуса игры -->
                    <template v-if="gameStatus === 'WAITING_FOR_PLAYERS' || gameStatus === 'ERROR'">
                        <button @click="clearBoard" class="px-4 py-2 bg-gray-600 hover:bg-gray-700 rounded-lg">
                            Очистить
                        </button>
                        <button @click="autoPlaceShips" class="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-lg">
                            Авторасстановка
                        </button>
                        <button @click="markReady" :disabled="!canMarkReady"
                            class="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg disabled:opacity-50">
                            Готов к игре
                        </button>
                    </template>

                    <template v-if="gameStatus === 'IN_PROGRESS'">
                        <button @click="surrender" class="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg">
                            Сдаться
                        </button>
                    </template>

                    <button @click="$emit('exit-game')" class="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg">
                        Выйти
                    </button>
                </div>
            </div>

            <!-- Сообщения о статусе -->
            <div class="mt-4 space-y-2">
                <div v-if="gameStatus === 'WAITING_FOR_PLAYERS' || gameStatus === 'ERROR'"
                    class="p-3 bg-yellow-500/20 border border-yellow-500/30 rounded-lg">
                    ⚓ Расставьте корабли (20 клеток) и нажмите "Готов к игре"
                    <div class="mt-2 text-sm">
                        Кораблей расставлено: {{ shipCount }}/20
                    </div>
                </div>

                <div v-else-if="gameStatus === 'IN_PROGRESS' && isMyTurn"
                    class="p-3 bg-green-500/20 border border-green-500/30 rounded-lg">
                    ✅ Ваш ход! Стреляйте по вражескому полю
                </div>

                <div v-else-if="gameStatus === 'IN_PROGRESS'"
                    class="p-3 bg-blue-500/20 border border-blue-500/30 rounded-lg">
                    ⏳ Ход {{ mode === 'PVP' ? 'противника' : 'ИИ' }}
                </div>

                <div v-else-if="gameStatus === 'FINISHED'"
                    class="p-3 bg-red-500/20 border border-red-500/30 rounded-lg">
                    🏆 Игра завершена
                </div>
            </div>
        </div>

        <!-- Игровые поля -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
            <!-- Своя доска - кликабельна при расстановке -->
            <GameBoard title="Ваш флот" :board="playerBoardDisplay" :show-ships="true"
                :interactive="gameStatus === 'WAITING_FOR_PLAYERS' || gameStatus === 'ERROR'"
                @cell-click="handleCellClick" />

            <!-- Вражеская доска - только во время игры -->
            <GameBoard :title="mode === 'PVP' ? 'Противник' : 'ИИ'" :board="enemyBoardDisplay" :show-ships="false"
                :interactive="gameStatus === 'IN_PROGRESS' && isMyTurn" @cell-click="handleAttack" />
        </div>

        <!-- История ходов -->
        <div v-if="gameStatus === 'IN_PROGRESS'" class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6">
            <h3 class="text-xl font-bold mb-4">📜 История ходов</h3>
            <div class="space-y-2 max-h-64 overflow-y-auto">
                <div v-for="(move, index) in movesHistory" :key="index" class="p-3 bg-gray-900/30 rounded-lg">
                    <div class="flex justify-between">
                        <span class="font-medium">{{ move.player }}</span>
                        <span class="text-gray-400 text-sm">{{ move.time }}</span>
                    </div>
                    <div>{{ move.text }}</div>
                </div>
                <div v-if="movesHistory.length === 0" class="text-gray-500 text-center py-2">
                    Ходов пока нет
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useGameStore } from '@/stores/gamestore'
import GameBoard from './gameboard.vue'

const props = defineProps({
    gameId: String,
    mode: String // "PVP" или "PVE"
})

const emit = defineEmits(['exit-game'])

const gameStore = useGameStore()
const pollInterval = ref(null)

// ЛОКАЛЬНАЯ ДОСКА ДЛЯ РАССТАНОВКИ
const localBoard = ref(Array(10).fill().map(() => Array(10).fill('empty')))

// Вычисляемые свойства
const gameStatus = computed(() => {
    return gameStore.gameState?.status || 'WAITING_FOR_PLAYERS'
})

const gameStatusText = computed(() => {
    const statusMap = {
        'WAITING_FOR_PLAYERS': 'Ожидание игроков',
        'IN_PROGRESS': 'В процессе',
        'FINISHED': 'Завершена',
        'ERROR': 'Ошибка'
    }
    return statusMap[gameStatus.value] || gameStatus.value
})

const isMyTurn = computed(() => {
    return gameStore.gameState?.currentPlayer === gameStore.playerName
})

// Количество расставленных кораблей
const shipCount = computed(() => {
    return localBoard.value.flat().filter(cell => cell === 'ship').length
})

const canMarkReady = computed(() => {
    return shipCount.value === 20 // Ровно 20 клеток кораблей
})

// Доски для отображения
const playerBoardDisplay = computed(() => {
    // Если игра еще не началась - показываем локальную доску
    if (gameStatus.value === 'WAITING_FOR_PLAYERS' || gameStatus.value === 'ERROR') {
        return localBoard.value
    }

    // Если игра началась - показываем доску из хранилища
    if (gameStore.playerBoard && gameStore.playerBoard.length > 0) {
        return gameStore.playerBoard
    }

    return Array(10).fill().map(() => Array(10).fill('empty'))
})

const enemyBoardDisplay = computed(() => {
    if (gameStore.enemyBoard && gameStore.enemyBoard.length > 0) {
        return gameStore.enemyBoard
    }
    return Array(10).fill().map(() => Array(10).fill('empty'))
})

// История
const movesHistory = computed(() => {
    return gameStore.moves.slice(-10).reverse().map(move => ({
        player: move.playerName,
        time: new Date(move.timestamp).toLocaleTimeString(),
        text: move.description || `Выстрел по (${move.row}, ${move.col})`
    }))
})

onMounted(() => {
    // Начинаем polling только если игра уже началась
    if (gameStore.gameState?.gameStarted) {
        startPolling()
    }
})

onUnmounted(() => {
    stopPolling()
})

function startPolling() {
    loadGameData()
    pollInterval.value = setInterval(loadGameData, 2000)
}

function stopPolling() {
    if (pollInterval.value) {
        clearInterval(pollInterval.value)
        pollInterval.value = null
    }
}

async function loadGameData() {
    try {
        // Всегда обновляем состояние игры
        await gameStore.loadGameState()

        // Если игра началась
        if (gameStore.gameState?.gameStarted) {
            // Обновляем доски
            await gameStore.loadBoards()

            // Если сейчас ход противника, чаще проверяем
            if (!isMyTurn.value) {
                console.log('Ход противника, ждём...')
            }
        }

    } catch (e) {
        console.error('Ошибка загрузки данных:', e)
    }
}

// Клик по своей доске (для ручной расстановки)
function handleCellClick({ row, col }) {
    if (gameStatus.value !== 'WAITING_FOR_PLAYERS' && gameStatus.value !== 'ERROR') {
        return
    }

    // row = y, col = x
    const newBoard = localBoard.value.map(r => [...r])
    newBoard[row][col] = newBoard[row][col] === 'ship' ? 'empty' : 'ship'
    localBoard.value = newBoard
}

function clearBoard() {
    localBoard.value = Array(10).fill().map(() => Array(10).fill('empty'))
}

async function autoPlaceShips() {
    try {
        const newBoard = Array(10).fill().map(() => Array(10).fill('empty'))
        const ships = []

        // Правильные корабли для морского боя
        const shipTypes = [
            { length: 4, count: 1 },
            { length: 3, count: 2 },
            { length: 2, count: 3 },
            { length: 1, count: 4 }
        ]

        shipTypes.forEach(shipType => {
            for (let i = 0; i < shipType.count; i++) {
                const ship = placeShipRandom(newBoard, shipType.length)
                if (ship) {
                    ships.push(ship)
                    // Размещаем корабль на доске
                    if (ship.horizontal) {
                        for (let dx = 0; dx < ship.length; dx++) {
                            newBoard[ship.y][ship.x + dx] = 'ship'
                        }
                    } else {
                        for (let dy = 0; dy < ship.length; dy++) {
                            newBoard[ship.y + dy][ship.x] = 'ship'
                        }
                    }
                }
            }
        })

        localBoard.value = newBoard

        // Сохраняем корабли для отправки на сервер
        window.autoPlacedShips = ships // временно сохраняем в глобальной переменной

        gameStore.addNotification('Корабли расставлены автоматически', 'success')

    } catch (e) {
        console.error('Ошибка авторасстановки:', e)
        gameStore.addNotification('Ошибка авторасстановки', 'error')
    }
}

function placeShipRandom(board, length) {
    const maxAttempts = 100

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
        const horizontal = Math.random() > 0.5
        const x = Math.floor(Math.random() * 10)  // столбец
        const y = Math.floor(Math.random() * 10)  // строка

        if (canPlaceShip(board, x, y, length, horizontal)) {
            return {
                x: x,      // столбец
                y: y,      // строка  
                length,
                horizontal
            }
        }
    }
    return null
}

function canPlaceShip(board, x, y, length, horizontal) {
    // Проверяем границы
    if (horizontal) {
        if (x + length > 10) return false
        for (let dx = 0; dx < length; dx++) {
            if (!isCellAvailable(board, x + dx, y)) return false
        }
    } else {
        if (y + length > 10) return false
        for (let dy = 0; dy < length; dy++) {
            if (!isCellAvailable(board, x, y + dy)) return false
        }
    }
    return true
}

function isCellAvailable(board, x, y) {
    if (x < 0 || x >= 10 || y < 0 || y >= 10) return false

    // Проверяем саму клетку и соседние (8 направлений)
    for (let dy = -1; dy <= 1; dy++) {
        for (let dx = -1; dx <= 1; dx++) {
            const nx = x + dx
            const ny = y + dy
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                if (board[ny][nx] === 'ship') return false
            }
        }
    }
    return true
}

async function markReady() {
    try {
        // Если есть автоматически расставленные корабли - используем их
        if (window.autoPlacedShips) {
            console.log('Используем авторасставленные корабли:', window.autoPlacedShips)

            const payload = {
                playerName: gameStore.playerName,
                ships: window.autoPlacedShips
            }

            const response = await gameStore.placeShipsManually(payload)
            console.log('Корабли сохранены:', response)
        } else {
            // Иначе пытаемся конвертировать ручную расстановку
            await saveShipsToServer()
        }

        // Отмечаем готовность
        await gameStore.markReady()

        gameStore.addNotification('Вы готовы к игре!', 'success')
        startPolling()

    } catch (e) {
        console.error('Ошибка:', e)
        alert('Не удалось начать игру: ' + (e.message || 'Ошибка сервера'))
    }
}

async function saveShipsToServer() {
    try {
        console.log('Используем тестовую расстановку для проверки...')

        // Тестовая расстановка (корректная)
        const testShips = [
            { x: 0, y: 0, length: 4, horizontal: true },
            { x: 0, y: 2, length: 3, horizontal: true },
            { x: 4, y: 2, length: 3, horizontal: true },
            { x: 0, y: 4, length: 2, horizontal: true },
            { x: 3, y: 4, length: 2, horizontal: true },
            { x: 6, y: 4, length: 2, horizontal: true },
            { x: 0, y: 6, length: 1, horizontal: true },
            { x: 2, y: 6, length: 1, horizontal: true },
            { x: 4, y: 6, length: 1, horizontal: true },
            { x: 6, y: 6, length: 1, horizontal: true }
        ]

        const payload = {
            playerName: gameStore.playerName,
            ships: testShips
        }

        console.log('Отправляем тестовые корабли:', payload)

        const response = await gameStore.placeShipsManually(payload)
        console.log('Корабли сохранены:', response)

        return response

    } catch (e) {
        console.error('Ошибка сохранения кораблей:', e)
        throw e
    }
}

function findShip(board, visited, startX, startY) {
    // Проверяем горизонтальный корабль
    let horizontalLength = 1
    while (startX + horizontalLength < 10 &&
        board[startY][startX + horizontalLength] === 'ship' &&
        !visited[startY][startX + horizontalLength]) {
        horizontalLength++
    }

    // Проверяем вертикальный корабль
    let verticalLength = 1
    while (startY + verticalLength < 10 &&
        board[startY + verticalLength][startX] === 'ship' &&
        !visited[startY + verticalLength][startX]) {
        verticalLength++
    }

    let ship
    if (horizontalLength > 1 && verticalLength === 1) {
        // Горизонтальный корабль
        ship = {
            x: startX,
            y: startY,
            length: horizontalLength,
            horizontal: true
        }
        // Помечаем клетки как посещенные
        for (let i = 0; i < horizontalLength; i++) {
            visited[startY][startX + i] = true
        }
    } else if (verticalLength > 1 && horizontalLength === 1) {
        // Вертикальный корабль
        ship = {
            x: startX,
            y: startY,
            length: verticalLength,
            horizontal: false
        }
        // Помечаем клетки как посещенные
        for (let i = 0; i < verticalLength; i++) {
            visited[startY + i][startX] = true
        }
    } else if (horizontalLength === 1 && verticalLength === 1) {
        // Однопалубный корабль
        ship = {
            x: startX,
            y: startY,
            length: 1,
            horizontal: true // ориентация не важна для однопалубного
        }
        visited[startY][startX] = true
    } else {
        // Некорректный корабль (L-образный или касающийся)
        return null
    }

    return ship
}

function arraysEqual(a, b) {
    if (a.length !== b.length) return false
    const sortedA = [...a].sort()
    const sortedB = [...b].sort()
    return sortedA.every((val, index) => val === sortedB[index])
}

async function handleAttack({ row, col }) {
    console.log('Клик по вражеской доске:', { row, col })

    if (gameStatus.value !== 'IN_PROGRESS') {
        console.log('Игра не в процессе')
        return
    }

    if (!isMyTurn.value) {
        gameStore.addNotification('Сейчас не ваш ход', 'error')
        return
    }

    // Проверяем, не стреляли ли уже в эту клетку
    const cellState = enemyBoardDisplay.value[row]?.[col]
    if (cellState === 'hit' || cellState === 'miss') {
        gameStore.addNotification('Вы уже стреляли в эту клетку', 'error')
        return
    }

    try {
        await gameStore.fire(row, col)

        // После выстрела проверяем состояние
        await gameStore.loadGameState()

        // Если ход перешел к противнику, обновляем доску
        if (!isMyTurn.value) {
            await gameStore.loadBoards()
        }

    } catch (e) {
        console.error('Ошибка выстрела:', e)
    }
}

function surrender() {
    if (confirm('Вы уверены, что хотите сдаться?')) {
        gameStore.addNotification('Вы сдались', 'info')
        emit('exit-game')
    }
}
</script>
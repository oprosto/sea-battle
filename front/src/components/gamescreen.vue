<template>
    <div class="max-w-7xl mx-auto">
        <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6 mb-8">
            <div class="flex justify-between items-center">
                <div>
                    <h2 class="text-2xl font-bold">Игра #{{ gameId }}</h2>
                    <p class="text-gray-400">{{ mode === 'PVP' ? 'Игрок vs Игрок' : 'Игрок vs ИИ' }}</p>
                </div>
                <div class="flex gap-4">
                    <button @click="surrender"
                        class="px-4 py-2 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg">
                        Сдаться
                    </button>
                    <button @click="$emit('exit-game')" class="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg">
                        Выйти
                    </button>
                </div>
            </div>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
            <GameBoard title="Ваш флот" :board="playerBoard" :show-ships="true" :interactive="false" />
            <GameBoard :title="mode === 'PVP' ? 'Противник' : 'ИИ'" :board="enemyBoard" :show-ships="false"
                :interactive="gameState.currentPlayer === playerId" @cell-click="handleAttack" />
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 justify-center">
            <div class="lg:col-span-2 bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6">
                <h3 class="text-xl font-bold mb-4">📜 История ходов</h3>
                <div class="space-y-2 max-h-64 overflow-y-auto">
                    <div v-for="move in moves" :key="move.id" class="p-3 bg-gray-900/30 rounded-lg">
                        <div class="flex justify-between">
                            <span class="font-medium">{{ move.playerName }}</span>
                            <span class="text-gray-400 text-sm">{{ formatTime(move.timestamp) }}</span>
                        </div>
                        <div>{{ move.description }}</div>
                    </div>
                </div>
            </div>
            <div class="space-y-4">
                <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6">
                    <h3 class="text-xl font-bold mb-4">Ход игрока</h3>
                    <div v-if="gameState.currentPlayer === playerId" class="text-center p-4 bg-green-500/10 rounded-lg">
                        <div class="text-2xl font-bold text-green-400">⭐ Ваш ход!</div>
                        <p class="text-gray-300 mt-2">Кликните по клетке противника</p>
                    </div>
                    <div v-else class="text-center p-4 bg-yellow-500/10 rounded-lg">
                        <div class="text-2xl font-bold text-yellow-400">⏳ Ожидание</div>
                        <p class="text-gray-300 mt-2">Ход {{ mode === 'PVP' ? 'противника' : 'ИИ' }}</p>
                    </div>
                </div>

                <button @click="skipTurn" :disabled="gameState.currentPlayer === playerId"
                    class="w-full py-3 bg-gray-700 hover:bg-gray-600 rounded-lg disabled:opacity-50">
                    Пропустить ход
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { gameApi } from '../api/gameapi'
import GameBoard from './gameboard.vue'

const props = defineProps({
    gameId: String,
    playerId: String,
    mode: String
})

const emit = defineEmits(['exit-game'])

// Состояние игры (с бэкенда)
const gameState = ref({
    phase: 'setup', // setup, playing, finished
    currentPlayer: null,
    players: []
})

const playerBoard = ref([])
const enemyBoard = ref([])
const moves = ref([])

let pollInterval

onMounted(() => {
    loadGameState()
    startPolling()
})

onUnmounted(() => {
    stopPolling()
})

async function loadGameState() {
    try {
        const state = await gameApi.getGameState(props.gameId)
        gameState.value = state

        // Здесь бэкенд должен возвращать доски и ходы
        // playerBoard.value = state.playerBoard
        // enemyBoard.value = state.enemyBoard
        // moves.value = state.moves

    } catch (error) {
        console.error('Ошибка загрузки состояния:', error)
    }
}

function startPolling() {
    pollInterval = setInterval(loadGameState, 2000)
}

function stopPolling() {
    if (pollInterval) {
        clearInterval(pollInterval)
    }
}

async function handleAttack(row, col) {
    if (gameState.value.currentPlayer !== props.playerId) return

    try {
        await gameApi.makeMove(props.gameId, props.playerId, row, col)
        // Обновление произойдёт при следующем polling
    } catch (error) {
        console.error('Ошибка хода:', error)
        alert(error.message)
    }
}

async function skipTurn() {
    try {
        await gameApi.skipTurn(props.gameId, props.playerId)
    } catch (error) {
        console.error('Ошибка пропуска хода:', error)
    }
}

async function surrender() {
    if (confirm('Вы уверены, что хотите сдаться?')) {
        try {
            await gameApi.surrender(props.gameId, props.playerId)
            emit('exit-game')
        } catch (error) {
            console.error('Ошибка сдачи:', error)
        }
    }
}

function formatTime(timestamp) {
    return new Date(timestamp).toLocaleTimeString('ru-RU', {
        hour: '2-digit',
        minute: '2-digit'
    })
}
</script>
<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-900 to-blue-950 text-white">
        <div class="fixed top-4 right-4 z-50 space-y-2 max-w-md">
            <div v-for="notification in gameStore.notifications" :key="notification.id"
                class="p-4 rounded-lg shadow-lg transition-all duration-300 animate-fade-in" :class="notification.type === 'error' ? 'bg-red-500' :
                    notification.type === 'success' ? 'bg-green-500' : 'bg-blue-500'">
                {{ notification.message }}
            </div>
        </div>
        <div v-if="gameStore.isLoading" class="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
            <div class="text-center">
                <div
                    class="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4">
                </div>
                <p class="text-white text-lg">Загрузка...</p>
            </div>
        </div>
        <div v-if="!gameStore.isInGame" class="min-h-screen flex items-center justify-center px-4">
            <HomeScreen @create-game="handleCreateGame" @join-game="handleJoinGame" />
        </div>
        <div v-else class="container mx-auto px-4 py-8">
            <GameScreen :game-id="gameStore.currentGameId" :player-id="gameStore.playerId" :mode="gameStore.gameMode"
                @exit-game="handleExitGame" />
        </div>
        <button @click="showHistory = true"
            class="fixed bottom-4 right-4 bg-gray-800 hover:bg-gray-700 p-3 rounded-full shadow-lg">
            📜
        </button>
        <HistoryModal v-if="showHistory" @close="showHistory = false" />
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useGameStore } from './stores/gamestore'
import HomeScreen from './components/homescreen.vue'
import GameScreen from './components/gamescreen.vue'
import HistoryModal from './components/historymodal.vue'
import { gameApi } from './api/gameapi'

const gameStore = useGameStore()
const showHistory = ref(false)

onMounted(() => {
    checkConnection()
})

async function checkConnection() {
    try {
        await gameApi.healthCheck()
        gameStore.addNotification('✅ Соединение с сервером установлено', 'success')
    } catch (error) {
        gameStore.addNotification('❌ Не удалось подключиться к серверу', 'error')
    }
}

async function handleCreateGame(mode) {
    try {
        await gameStore.createNewGame(mode)
    } catch (error) {
        console.error('Ошибка создания игры:', error)
    }
}

async function handleJoinGame(gameId, playerName) {
    try {
        await gameStore.joinGame(gameId, playerName)
    } catch (error) {
        console.error('Ошибка присоединения:', error)
    }
}

function handleExitGame() {
    if (confirm('Выйти из игры? Текущий прогресс будет сохранён.')) {
        gameStore.resetGame()
    }
}
</script>

<style>
@keyframes fade-in {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-fade-in {
    animation: fade-in 0.3s ease-out;
}
</style>
<template>
    <div class="max-w-4xl mx-auto">
        <header class="text-center mb-12">
            <h1 class="text-5xl md:text-6xl font-bold mb-4">
                <span class="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-cyan-300">
                    🚢 Морской бой
                </span>
            </h1>
            <p class="text-xl text-gray-400">Подключение к Spring REST API</p>
        </header>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
            <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-8">
                <div class="text-4xl mb-4">🤖</div>
                <h3 class="text-2xl font-bold mb-4">Игра против ИИ</h3>
                <p class="text-gray-400 mb-6">Сразитесь с компьютерным противником</p>
                <button @click="$emit('create-game', 'PVE')"
                    class="w-full py-4 bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 rounded-xl font-semibold text-lg">
                    Начать игру
                </button>
            </div>
            <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-8">
                <div class="text-4xl mb-4">👥</div>
                <h3 class="text-2xl font-bold mb-4">Игра против игрока</h3>
                <p class="text-gray-400 mb-6">Создайте комнату для игры с другом</p>
                <button @click="startPvP"
                    class="w-full py-4 bg-gradient-to-r from-purple-600 to-pink-500 hover:from-purple-700 hover:to-pink-600 rounded-xl font-semibold text-lg">
                    Создать комнату
                </button>
            </div>
        </div>
        <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-8 mb-12">
            <h3 class="text-2xl font-bold mb-4">Присоединиться к игре</h3>
            <div class="flex flex-col md:flex-row gap-4">
                <input v-model="joinGameId" placeholder="ID игры"
                    class="flex-grow px-4 py-3 bg-gray-900/50 border border-gray-700 rounded-lg focus:outline-none focus:border-blue-500">
                <input v-model="playerName" placeholder="Ваше имя"
                    class="flex-grow px-4 py-3 bg-gray-900/50 border border-gray-700 rounded-lg focus:outline-none focus:border-blue-500">
                <button @click="handleJoin"
                    class="px-8 py-3 bg-gradient-to-r from-green-600 to-emerald-500 hover:from-green-700 hover:to-emerald-600 rounded-lg font-semibold">
                    Присоединиться
                </button>
            </div>
        </div>
        <!-- <div class="bg-gray-800/50 backdrop-blur-sm rounded-2xl p-8">
            <h3 class="text-2xl font-bold mb-6">📊 Статистика</h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
                <div class="text-center">
                    <div class="text-3xl font-bold text-blue-400">{{ stats.totalGames }}</div>
                    <div class="text-gray-400">Всего игр</div>
                </div>
                <div class="text-center">
                    <div class="text-3xl font-bold text-green-400">{{ stats.wins }}</div>
                    <div class="text-gray-400">Побед</div>
                </div>
                <div class="text-center">
                    <div class="text-3xl font-bold text-red-400">{{ stats.losses }}</div>
                    <div class="text-gray-400">Поражений</div>
                </div>
                <div class="text-center">
                    <div class="text-3xl font-bold text-purple-400">{{ stats.winRate }}%</div>
                    <div class="text-gray-400">Процент побед</div>
                </div>
            </div>
        </div> -->
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useGameStore } from '../stores/gamestore'

const gameStore = useGameStore()
const joinGameId = ref('')
const playerName = ref('Игрок')

const stats = computed(() => gameStore.playerStats)

function startPvP() {
    const name = prompt('Введите ваше имя:', 'Игрок 1')
    if (name) {
        playerName.value = name
        emit('create-game', 'PVP')
    }
}

function handleJoin() {
    if (!joinGameId.value.trim()) {
        alert('Введите ID игры')
        return
    }

    if (!playerName.value.trim()) {
        alert('Введите ваше имя')
        return
    }

    emit('join-game', joinGameId.value, playerName.value)
}

onMounted(() => {
    gameStore.loadGameHistory()
    gameStore.loadPlayerStats()
})
</script>
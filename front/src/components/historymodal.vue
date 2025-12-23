<template>
    <div class="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
        <div class="bg-gray-800 rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            <div class="flex justify-between items-center mb-6">
                <h2 class="text-2xl font-bold">📋 История игр</h2>
                <button @click="$emit('close')" class="text-gray-400 hover:text-white">
                    ✕
                </button>
            </div>

            <div v-if="loading" class="text-center py-8">
                <div class="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto">
                </div>
                <p class="mt-4 text-gray-400">Загрузка истории...</p>
            </div>

            <div v-else class="space-y-3">
                <div v-for="game in history" :key="game.id"
                    class="p-4 bg-gray-900/50 rounded-lg hover:bg-gray-900 transition-colors">
                    <div class="flex justify-between items-center mb-2">
                        <span class="font-medium">{{ formatDate(game.endTime) }}</span>
                        <span :class="[
                            'px-2 py-1 rounded text-sm',
                            game.winner ? 'bg-green-500/20 text-green-400' : 'bg-gray-500/20 text-gray-400'
                        ]">
                            {{ game.winner ? 'Завершена' : 'В процессе' }}
                        </span>
                    </div>
                    <div class="text-sm text-gray-400">
                        <div>Режим: {{ game.mode === 'PVP' ? 'Игрок vs Игрок' : 'Игрок vs ИИ' }}</div>
                        <div>Игроки: {{game.players?.map(p => p.name).join(' vs ') || 'Неизвестно'}}</div>
                        <div v-if="game.winner">Победитель: {{ game.winner.name }}</div>
                    </div>
                </div>

                <div v-if="history.length === 0" class="text-center py-8 text-gray-500">
                    Нет завершённых игр
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { gameApi } from '../api/gameapi'

const loading = ref(false)
const history = ref([])

onMounted(async () => {
    await loadHistory()
})

async function loadHistory() {
    loading.value = true
    try {
        const data = await gameApi.getCompletedGames()
        history.value = data.content || data
    } catch (error) {
        console.error('Ошибка загрузки истории:', error)
    } finally {
        loading.value = false
    }
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    })
}
</script>
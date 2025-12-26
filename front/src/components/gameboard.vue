<template>
    <div class="bg-gray-800/30 rounded-2xl p-6">
        <h3 class="text-xl font-bold mb-4">{{ title }}</h3>
        <div class="grid grid-cols-10 gap-1 bg-gray-900/50 p-2 rounded-lg">
            <div v-for="row in 10" :key="row" class="contents">
                <button v-for="col in 10" :key="col"
                    class="w-10 h-10 border border-gray-700 rounded flex items-center justify-center transition-all hover:bg-white/5 disabled:cursor-not-allowed"
                    :class="getCellClass(row - 1, col - 1)"
                    :disabled="!interactive"
                    @click="handleClick(row - 1, col - 1)">
                    
                    <template v-if="showShips && getCell(row - 1, col - 1) === 'ship'">
                        🚢
                    </template>
                    <template v-else-if="getCell(row - 1, col - 1) === 'hit'">
                        💥
                    </template>
                    <template v-else-if="getCell(row - 1, col - 1) === 'miss'">
                        ❌
                    </template>
                    <!-- Для ручной расстановки показываем точку на пустой клетке -->
                    <template v-else-if="interactive && showShips">
                        <div class="w-2 h-2 rounded-full bg-gray-500/50"></div>
                    </template>
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
const props = defineProps({
    title: String,
    board: {
        type: Array,
        default: () => Array(10).fill().map(() => Array(10).fill('empty'))
    },
    showShips: Boolean,
    interactive: Boolean
})

const emit = defineEmits(['cell-click'])

function getCell(row, col) {
    return props.board[row]?.[col] || 'empty'
}

function getCellClass(row, col) {
    const cell = getCell(row, col)
    
    if (cell === 'hit') return 'bg-red-900/60'
    if (cell === 'miss') return 'bg-gray-900/70'
    if (cell === 'ship' && props.showShips) return 'bg-blue-900/40'
    return 'bg-gray-800/30'
}

function handleClick(row, col) {
    if (!props.interactive) return
    emit('cell-click', { row, col })
}
</script>
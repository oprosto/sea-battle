<template>
    <div class="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
        <div class="bg-gray-800 rounded-2xl p-6 max-w-2xl w-full">
            <div class="flex justify-between items-center mb-6">
                <h3 class="text-2xl font-bold">Ручная расстановка кораблей</h3>
                <button @click="$emit('close')" class="text-gray-400 hover:text-white">
                    ✕
                </button>
            </div>
            
            <div class="mb-6">
                <p class="text-gray-400">Кликайте по клеткам для размещения кораблей</p>
                <div class="flex gap-4 mt-4">
                    <div v-for="ship in ships" :key="ship.size" class="text-center">
                        <div class="font-bold">{{ ship.size}}-палубный</div>
                        <div class="text-gray-400">{{ ship.count}} шт.</div>
                    </div>
                </div>
            </div>
            
            <div class="grid grid-cols-10 gap-1 bg-gray-900/50 p-2 rounded-lg mb-6">
                <div v-for="row in 10" :key="row" class="contents">
                    <button v-for="col in 10" :key="col"
                        class="w-10 h-10 border border-gray-700 rounded flex items-center justify-center transition-all hover:bg-white/5"
                        :class="{'bg-blue-500/30': board[row-1][col-1] === 'ship'}"
                        @click="toggleShip(row-1, col-1)">
                        <span v-if="board[row-1][col-1] === 'ship'">🚢</span>
                    </button>
                </div>
            </div>
            
            <div class="flex gap-4">
                <button @click="randomPlacement" class="flex-1 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg">
                    Случайная расстановка
                </button>
                <button @click="confirmPlacement" 
                        :disabled="!allShipsPlaced"
                        class="flex-1 py-3 bg-green-600 hover:bg-green-700 rounded-lg disabled:opacity-50">
                    Подтвердить
                </button>
                <button @click="$emit('close')" class="flex-1 py-3 bg-gray-700 hover:bg-gray-600 rounded-lg">
                    Отмена
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['close', 'place-ships'])

const board = ref(Array(10).fill().map(() => Array(10).fill('empty')))
const ships = ref([
    { size: 4, count: 1 },
    { size: 3, count: 2 },
    { size: 2, count: 3 },
    { size: 1, count: 4 }
])

const allShipsPlaced = computed(() => {
    const shipCells = board.value.flat().filter(cell => cell === 'ship').length
    return shipCells === 20 // 4+3*2+2*3+1*4 = 20 клеток
})

function toggleShip(row, col) {
    if (board.value[row][col] === 'empty') {
        board.value[row][col] = 'ship'
    } else {
        board.value[row][col] = 'empty'
    }
}

function randomPlacement() {
    // Простая случайная расстановка
    board.value = Array(10).fill().map(() => Array(10).fill('empty'))
    
    ships.value.forEach(ship => {
        for (let i = 0; i < ship.count; i++) {
            let placed = false
            while (!placed) {
                const row = Math.floor(Math.random() * 10)
                const col = Math.floor(Math.random() * 10)
                
                // Проверяем, можно ли разместить
                if (board.value[row][col] === 'empty') {
                    board.value[row][col] = 'ship'
                    placed = true
                }
            }
        }
    })
}

function confirmPlacement() {
    if (!allShipsPlaced.value) {
        alert('Расставьте все корабли!')
        return
    }
    
    emit('place-ships', board.value)
    emit('close')
}
</script>
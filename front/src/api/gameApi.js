import { api } from './http'

export const gameApi = {
    createSession(payload) {
        console.log(payload)
        return api.post('/game/create', payload)
    },

    // gameApi.js
    joinSession(sessionId, playerName) {
        console.log('Подключение к игре:', {
            sessionId,
            playerName,
            url: `/game/${sessionId}/join`
        })

        // Уберите валидацию UUID для теста
        // const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
        // if (!uuidRegex.test(sessionId)) {
        //     console.error('Неправильный формат UUID:', sessionId)
        //     throw new Error('Неправильный формат ID игры')
        // }

        return api.post(`/game/${sessionId}/join`, { playerName })
            .then(response => {
                console.log('Успешное подключение:', response.data)
                return response
            })
            .catch(error => {
                console.error('Ошибка подключения API:', {
                    status: error.response?.status,
                    data: error.response?.data,
                    message: error.message
                })
                throw error
            })
    },

    getSession(sessionId) {
        return api.get(`/game/session/${sessionId}`)
    },

    getActiveSessions() {
        return api.get('/game/active-sessions')
    },

    getWaitingSessions() {
        return api.get('/game/waiting-sessions')
    },

    cancelSession(sessionId) {
        return api.post(`/game/${sessionId}/cancel`)
    },

    getStatus() {
        return api.get('/game/status')
    }
}

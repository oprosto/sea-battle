import { api } from './http'

export const historyApi = {
    getCompletedGames() {
        return api.get('/history/games')
    }
}

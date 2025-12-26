// api/http.js
import axios from 'axios'

export const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

api.interceptors.response.use(
    response => {
        console.log('✅ Ответ:', {
            url: response.config.url,
            status: response.status,
            method: response.config.method?.toUpperCase(),
            data: response.data
        })
        return response
    },
    error => {
        console.error('❌ Ошибка:', {
            url: error.config?.url,
            status: error.response?.status,
            method: error.config?.method?.toUpperCase(),
            data: error.response?.data,
            message: error.message
        })
        
        // Форматируем сообщение об ошибке
        if (error.response?.data?.error) {
            error.message = error.response.data.error
        } else if (error.response?.data) {
            error.message = JSON.stringify(error.response.data)
        }
        
        return Promise.reject(error)
    }
)
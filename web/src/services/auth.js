import { http } from './http'

export const login = (payload) => http.post('/auth/login', payload)
export const register = (payload) => http.post('/auth/register', payload)
export const sendCode = (payload) => http.post('/auth/codes', payload)
export const resetPassword = (payload) => http.post('/auth/password/reset', payload)
export const fetchPolicy = (type) => http.get(`/auth/policies/${type}`)

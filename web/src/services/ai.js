import { aiHttp } from './http'

export const chatWithAi = (payload) => aiHttp.post('/ai/chat', payload)
export const diagnoseImage = (payload) => aiHttp.post('/ai/diagnosis', payload)

// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { aiHttp, http } from './http'

const { axiosInstances } = vi.hoisted(() => ({
  axiosInstances: [],
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn((config) => {
      const instance = {
        config,
        interceptors: {
          request: { use: vi.fn() },
          response: { use: vi.fn() },
        },
      }
      axiosInstances.push(instance)
      return instance
    }),
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
  },
}))

describe('http clients', () => {
  const storage = new Map()

  beforeEach(() => {
    storage.clear()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => storage.get(key) ?? null),
      setItem: vi.fn((key, value) => storage.set(key, String(value))),
      removeItem: vi.fn((key) => storage.delete(key)),
    })
    window.history.pushState({}, '', '/login')
    ElMessage.error.mockClear()
  })

  it('creates normal and AI clients with expected timeout', () => {
    expect(axios.create).toHaveBeenCalledWith({ baseURL: '/api/v1', timeout: 10000 })
    expect(axios.create).toHaveBeenCalledWith({ baseURL: '/api/v1', timeout: 300000 })
    expect(http.config.timeout).toBe(10000)
    expect(aiHttp.config.timeout).toBe(300000)
  })

  it('attaches token only when it exists', () => {
    const attachToken = http.interceptors.request.use.mock.calls[0][0]

    expect(attachToken({ headers: {} })).toEqual({ headers: {} })

    storage.set('greenhouse_token', 'token-1')
    expect(attachToken({ headers: {} })).toEqual({
      headers: { Authorization: 'Bearer token-1' },
    })
  })

  it('unwraps successful api response and rejects failed api response', async () => {
    const unwrapResponse = http.interceptors.response.use.mock.calls[0][0]

    expect(unwrapResponse({ data: { code: 0, data: { id: 1 } } })).toEqual({ id: 1 })

    await expect(unwrapResponse({ data: { code: 400, message: 'bad request' } }))
      .rejects.toThrow('bad request')
    expect(ElMessage.error).toHaveBeenCalledWith('bad request')

    await expect(unwrapResponse({ data: null })).rejects.toThrow()
  })

  it('normalizes network errors and clears session on unauthorized response', async () => {
    const handleError = http.interceptors.response.use.mock.calls[0][1]
    storage.set('greenhouse_token', 'token-1')
    storage.set('greenhouse_profile', '{}')

    await expect(handleError({ code: 'ECONNABORTED', message: 'timeout' })).rejects.toThrow()
    await expect(handleError({
      message: 'denied',
      response: { status: 401, data: { message: 'login expired', code: 401 } },
    })).rejects.toThrow('login expired')

    expect(localStorage.getItem('greenhouse_token')).toBeNull()
    expect(localStorage.getItem('greenhouse_profile')).toBeNull()
  })

  it('redirects unauthorized users outside login page and falls back to generic message', async () => {
    const handleError = http.interceptors.response.use.mock.calls[0][1]
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    storage.set('greenhouse_token', 'token-2')
    storage.set('greenhouse_profile', '{}')
    window.history.pushState({}, '', '/dashboard')

    await expect(handleError({ response: { data: { code: 401 } } })).rejects.toThrow()
    await expect(handleError({})).rejects.toThrow()

    expect(localStorage.getItem('greenhouse_token')).toBeNull()
    expect(ElMessage.error).toHaveBeenCalled()
    consoleError.mockRestore()
  })
})

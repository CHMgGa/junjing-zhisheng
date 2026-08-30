import { describe, expect, it, vi } from 'vitest'
import { fetchPolicy, login, register, resetPassword, sendCode } from './auth'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('auth service', () => {
  it('posts auth payloads to backend endpoints', () => {
    login({ username: 'alice' })
    register({ username: 'bob' })
    sendCode({ receiver: 'a@example.com' })
    resetPassword({ receiver: 'a@example.com' })

    expect(http.post).toHaveBeenCalledWith('/auth/login', { username: 'alice' })
    expect(http.post).toHaveBeenCalledWith('/auth/register', { username: 'bob' })
    expect(http.post).toHaveBeenCalledWith('/auth/codes', { receiver: 'a@example.com' })
    expect(http.post).toHaveBeenCalledWith('/auth/password/reset', { receiver: 'a@example.com' })
  })

  it('fetches policy by type', () => {
    fetchPolicy('privacy')

    expect(http.get).toHaveBeenCalledWith('/auth/policies/privacy')
  })
})

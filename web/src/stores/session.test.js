// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSessionStore } from './session'
import { login, register } from '../services/auth'

vi.mock('../services/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
}))

describe('session store', () => {
  const storage = new Map()

  beforeEach(() => {
    storage.clear()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => storage.get(key) ?? null),
      setItem: vi.fn((key, value) => storage.set(key, String(value))),
      removeItem: vi.fn((key) => storage.delete(key)),
      clear: vi.fn(() => storage.clear()),
    })
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('stores normalized profile and token after sign in', async () => {
    login.mockResolvedValue({
      token: 'token-1',
      profile: { username: 'alice', role: ' farmer ' },
    })

    const store = useSessionStore()
    await store.signIn({ username: 'alice', password: 'secret' })

    expect(login).toHaveBeenCalledWith({ username: 'alice', password: 'secret' })
    expect(store.token).toBe('token-1')
    expect(store.profile.role).toBe('FARMER')
    expect(localStorage.getItem('greenhouse_token')).toBe('token-1')
  })

  it('stores registration session data', async () => {
    register.mockResolvedValue({
      token: 'token-2',
      profile: { username: 'bob', role: 'admin' },
    })

    const store = useSessionStore()
    await store.signUp({ username: 'bob' })

    expect(register).toHaveBeenCalledWith({ username: 'bob' })
    expect(store.profile.role).toBe('ADMIN')
  })

  it('clears session data on sign out', () => {
    const store = useSessionStore()
    store.setSession({ token: 'token-3', profile: { username: 'carl', role: 'ADMIN' } })

    store.signOut()

    expect(store.token).toBe('')
    expect(store.profile).toBeNull()
    expect(localStorage.getItem('greenhouse_token')).toBeNull()
    expect(localStorage.getItem('greenhouse_profile')).toBeNull()
  })
})

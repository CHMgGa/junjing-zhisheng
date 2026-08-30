// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from './index'
import { useSessionStore } from '../stores/session'

describe('router guards', () => {
  const storage = new Map()

  beforeEach(async () => {
    storage.clear()
    globalThis.localStorage = {
      getItem: (key) => storage.get(key) ?? null,
      setItem: (key, value) => storage.set(key, String(value)),
      removeItem: (key) => storage.delete(key),
      clear: () => storage.clear(),
    }
    setActivePinia(createPinia())
    await router.push('/login')
  })

  it('redirects protected pages to login when token is missing', async () => {
    await router.push('/devices')

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects authenticated login visit by role', async () => {
    const store = useSessionStore()
    store.setSession({ token: 'admin-token', profile: { username: 'admin', role: 'ADMIN' } })

    await router.push('/devices')
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/')

    store.setSession({ token: 'farmer-token', profile: { username: 'farmer', role: 'FARMER' } })
    await router.push('/profile')
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/farmer')
  })

  it('enforces route roles and dashboard fallback for farmers', async () => {
    const store = useSessionStore()
    store.setSession({ token: 'farmer-token', profile: { username: 'farmer', role: 'FARMER' } })

    await router.push('/users')
    expect(router.currentRoute.value.path).toBe('/farmer')

    await router.push('/')
    expect(router.currentRoute.value.path).toBe('/farmer')

    store.setSession({ token: 'admin-token', profile: { username: 'admin', role: 'ADMIN' } })
    await router.push('/devices')
    await router.push('/farmer')
    expect(router.currentRoute.value.path).toBe('/')
  })
})

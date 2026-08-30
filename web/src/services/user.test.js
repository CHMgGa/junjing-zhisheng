import { describe, expect, it, vi } from 'vitest'
import {
  bindFarmerGreenhouses,
  createUser,
  deleteUser,
  fetchAdmins,
  fetchFarmerGreenhouses,
  fetchFeedbackConversations,
  fetchFeedbackMessages,
  fetchFeedbacks,
  fetchProfile,
  fetchUnreadFeedback,
  fetchUsers,
  sendFeedbackMessage,
  submitFeedback,
  unbindFarmerGreenhouse,
  updateProfile,
  updateUser,
} from './user'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('user service', () => {
  it('builds profile and feedback endpoints', () => {
    fetchProfile(7)
    updateProfile(7, { displayName: 'A' })
    submitFeedback({ content: 'help' })
    fetchFeedbacks({ status: 'OPEN' })
    fetchFeedbackConversations()
    fetchUnreadFeedback()
    fetchFeedbackMessages(3)
    sendFeedbackMessage({ conversationId: 3, content: 'ok' })

    expect(http.get).toHaveBeenCalledWith('/users/7/profile')
    expect(http.put).toHaveBeenCalledWith('/users/7/profile', { displayName: 'A' })
    expect(http.post).toHaveBeenCalledWith('/users/feedback', { content: 'help' })
    expect(http.get).toHaveBeenCalledWith('/users/feedback', { params: { status: 'OPEN' } })
    expect(http.get).toHaveBeenCalledWith('/users/feedback/conversations')
    expect(http.get).toHaveBeenCalledWith('/users/feedback/unread')
    expect(http.get).toHaveBeenCalledWith('/users/feedback/conversations/3/messages')
    expect(http.post).toHaveBeenCalledWith('/users/feedback/messages', { conversationId: 3, content: 'ok' })
  })

  it('builds admin user management endpoints', () => {
    fetchUsers({ keyword: 'a' })
    createUser({ username: 'u' })
    updateUser(8, { username: 'v' })
    deleteUser(8)
    fetchAdmins()
    fetchFarmerGreenhouses(7)
    bindFarmerGreenhouses(7, { greenhouseIds: [1] })
    unbindFarmerGreenhouse(7, 1)

    expect(http.get).toHaveBeenCalledWith('/users', { params: { keyword: 'a' } })
    expect(http.post).toHaveBeenCalledWith('/users', { username: 'u' })
    expect(http.put).toHaveBeenCalledWith('/users/8', { username: 'v' })
    expect(http.delete).toHaveBeenCalledWith('/users/8')
    expect(http.get).toHaveBeenCalledWith('/users/admins')
    expect(http.get).toHaveBeenCalledWith('/users/7/greenhouses')
    expect(http.post).toHaveBeenCalledWith('/users/7/greenhouses', { greenhouseIds: [1] })
    expect(http.delete).toHaveBeenCalledWith('/users/7/greenhouses/1')
  })
})

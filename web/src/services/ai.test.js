import { describe, expect, it, vi } from 'vitest'
import { chatWithAi, diagnoseImage } from './ai'
import { aiHttp } from './http'

vi.mock('./http', () => ({
  aiHttp: {
    post: vi.fn(),
  },
}))

describe('ai service', () => {
  it('uses the long-timeout AI http client', () => {
    chatWithAi({ question: 'how' })
    diagnoseImage({ imageBase64: 'abc' })

    expect(aiHttp.post).toHaveBeenCalledWith('/ai/chat', { question: 'how' })
    expect(aiHttp.post).toHaveBeenCalledWith('/ai/diagnosis', { imageBase64: 'abc' })
  })
})

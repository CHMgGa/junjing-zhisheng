import { describe, expect, it, vi } from 'vitest'
import {
  alertCommand,
  createBatch,
  createBatchEvent,
  createDevice,
  createGreenhouse,
  deleteDevice,
  deleteGreenhouse,
  fetchAnalytics,
  fetchAlertDetails,
  fetchAlerts,
  fetchBatchDetail,
  fetchBatches,
  fetchDevices,
  fetchGreenhouses,
  fetchOverview,
  fetchTraceability,
  handleAlert,
  sendDeviceCommand,
  updateDevice,
  updateGreenhouse,
} from './greenhouse'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('greenhouse service', () => {
  it('passes query parameters when fetching analytics and devices', () => {
    fetchOverview(2)
    fetchAnalytics(3, 24)
    fetchGreenhouses()
    fetchDevices(5)
    fetchAlerts(6)
    fetchAlertDetails(7)
    fetchTraceability(8)
    fetchBatches({ greenhouseId: 9 })
    fetchBatchDetail(10)

    expect(http.get).toHaveBeenCalledWith('/greenhouses/overview', {
      params: { greenhouseId: 2 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/analytics', {
      params: { greenhouseId: 3, rangeHours: 24 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses')
    expect(http.get).toHaveBeenCalledWith('/greenhouses/devices', {
      params: { greenhouseId: 5 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/alerts', {
      params: { greenhouseId: 6 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/alerts/detail', {
      params: { greenhouseId: 7 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/traceability', {
      params: { greenhouseId: 8 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/batches', {
      params: { greenhouseId: 9 },
    })
    expect(http.get).toHaveBeenCalledWith('/greenhouses/batches/10')
  })

  it('builds resource urls for update and alert handling', () => {
    createGreenhouse({ name: 'A' })
    updateGreenhouse(7, { name: 'A07' })
    deleteGreenhouse(7)
    updateDevice(8, { name: 'fan' })
    deleteDevice(8)
    handleAlert(9, { action: 'RESOLVE' })
    alertCommand(9, { command: 'STOP' })

    expect(http.post).toHaveBeenCalledWith('/greenhouses', { name: 'A' })
    expect(http.put).toHaveBeenCalledWith('/greenhouses/7', { name: 'A07' })
    expect(http.delete).toHaveBeenCalledWith('/greenhouses/7')
    expect(http.put).toHaveBeenCalledWith('/greenhouses/devices/8', { name: 'fan' })
    expect(http.delete).toHaveBeenCalledWith('/greenhouses/devices/8')
    expect(http.post).toHaveBeenCalledWith('/greenhouses/alerts/9/handle', { action: 'RESOLVE' })
    expect(http.post).toHaveBeenCalledWith('/greenhouses/alerts/9/command', { command: 'STOP' })
  })

  it('posts device payloads to the expected endpoints', () => {
    createDevice({ name: 'fan' })
    createBatch({ batchNo: 'B1' })
    createBatchEvent(12, { title: 'seeded' })
    sendDeviceCommand({ deviceId: 1, command: 'START' })

    expect(http.post).toHaveBeenCalledWith('/greenhouses/devices', { name: 'fan' })
    expect(http.post).toHaveBeenCalledWith('/greenhouses/batches', { batchNo: 'B1' })
    expect(http.post).toHaveBeenCalledWith('/greenhouses/batches/12/events', { title: 'seeded' })
    expect(http.post).toHaveBeenCalledWith('/greenhouses/devices/commands', {
      deviceId: 1,
      command: 'START',
    })
  })
})

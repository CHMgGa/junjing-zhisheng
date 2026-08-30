export async function fetchTwinOverview() {
  const response = await fetch('/api/twin/overview')
  if (!response.ok) {
    throw new Error(`数字孪生接口异常：${response.status}`)
  }
  return response.json()
}

export async function updateDeviceCommand(device, runningStatus) {
  if (!device.dbId) {
    return { ok: true, localOnly: true }
  }

  const response = await fetch(`/api/twin/devices/${device.dbId}/command`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ runningStatus }),
  })

  if (!response.ok) {
    throw new Error(`设备控制接口异常：${response.status}`)
  }

  return response.json()
}

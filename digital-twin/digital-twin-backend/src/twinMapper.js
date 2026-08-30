const greenhousePositions = [
  [-7.4, -7.2],
  [7.4, -7.2],
  [-7.4, 7.2],
  [7.4, 7.2],
]

const sensorConfig = [
  { key: 'temperature', type: 'TEMPERATURE', name: '空气温度', unit: '℃', modelNode: 'SENSOR_TEMP_01', location: '中部', warning: (v) => v < 15 || v > 24 },
  { key: 'humidity', type: 'HUMIDITY', name: '空气湿度', unit: '%RH', modelNode: 'SENSOR_HUM_01', location: '中部', warning: (v) => v < 75 || v > 95 },
  { key: 'co2_ppm', type: 'CO2', name: 'CO2 浓度', unit: 'ppm', modelNode: 'SENSOR_CO2_01', location: '入口附近', warning: (v) => v < 400 || v > 1200 },
  { key: 'light_lux', type: 'LIGHT', name: '光照强度', unit: 'lx', modelNode: 'SENSOR_LIGHT_01', location: '种植区顶部', warning: (v) => v > 900 },
  { key: 'soil_moisture', type: 'SOIL_MOISTURE', name: '基质湿度', unit: '%', modelNode: 'SENSOR_SOIL_01', location: '种植床', warning: (v) => v < 60 || v > 90 },
]

function normalizeId(prefix, id) {
  return `${prefix}-${String(id).padStart(3, '0')}`
}

function pickPosition(index) {
  return greenhousePositions[index] || [
    index % 2 === 0 ? -7.4 : 7.4,
    7.2 + Math.floor(index / 2) * 14.4,
  ]
}

function normalizeStatus(status) {
  const value = String(status || '').toUpperCase()
  if (['ON', 'OPEN', 'RUNNING', 'ONLINE', 'ACTIVE', '1', 'TRUE', 'START'].includes(value)) return 'ON'
  return 'OFF'
}

function normalizeOnlineStatus(status) {
  const value = String(status || '').toUpperCase()
  if (['OFFLINE', 'DISCONNECTED', 'ERROR'].includes(value)) return 'OFFLINE'
  return 'ONLINE'
}

function inferDeviceModelNode(device) {
  const text = `${device.name || ''} ${device.category || ''} ${device.location || ''}`.toLowerCase()
  if (text.includes('风') || text.includes('通风') || text.includes('fan')) return 'FAN_01'
  if (text.includes('泵') || text.includes('水') || text.includes('灌溉') || text.includes('喷灌') || text.includes('pump')) return 'PUMP_01'
  if (text.includes('灯') || text.includes('光') || text.includes('light') || text.includes('lamp')) return 'LAMP_01'
  if (text.includes('帘') || text.includes('遮阳') || text.includes('遮光') || text.includes('shade')) return 'SHADE_01'
  return `DEVICE_${device.id}`
}

function buildSensorsForSnapshot(snapshot, greenhouse) {
  if (!snapshot || !greenhouse) return []

  return sensorConfig.map((config) => {
    const rawValue = snapshot[config.key]
    const value = rawValue === null || rawValue === undefined ? 0 : Number(rawValue)
    return {
      greenhouseId: greenhouse.id,
      deviceId: `${greenhouse.id}-${config.type}-01`,
      dbId: snapshot.id,
      modelNode: config.modelNode,
      type: config.type,
      name: config.name,
      value,
      unit: config.unit,
      status: config.warning(value) ? 'WARNING' : 'NORMAL',
      location: `${greenhouse.name}${config.location}`,
      collectedAt: snapshot.collected_at,
    }
  })
}

export function buildTwinOverview({ greenhouses, devices, telemetry, alerts }) {
  const normalizedGreenhouses = greenhouses.map((item, index) => ({
    id: normalizeId('GH', item.id),
    dbId: item.id,
    name: item.name || `${index + 1}号羊肚菌大棚`,
    location: item.location || '园区',
    position: pickPosition(index),
    status: alerts.some((alert) => alert.greenhouse_id === item.id && alert.status === 'OPEN') ? 'WARNING' : 'NORMAL',
    cropStage: item.crop_stage,
    area: item.area,
  }))

  const firstGreenhouse = normalizedGreenhouses[0]
  const greenhouseByDbId = new Map(normalizedGreenhouses.map((greenhouse) => [String(greenhouse.dbId), greenhouse]))

  const sensors = telemetry.flatMap((snapshot) => {
    const greenhouse = greenhouseByDbId.get(String(snapshot.greenhouse_id))
    return buildSensorsForSnapshot(snapshot, greenhouse)
  })

  const normalizedDevices = devices
    .map((device) => {
      const greenhouse = greenhouseByDbId.get(String(device.greenhouse_id))
      if (!greenhouse) return null

      return {
        greenhouseId: greenhouse.id,
        deviceId: normalizeId('DEV', device.id),
        dbId: device.id,
        modelNode: inferDeviceModelNode(device),
        name: device.name || device.category || `设备${device.id}`,
        category: device.category,
        location: device.location,
        runningStatus: normalizeStatus(device.status || device.last_command),
        onlineStatus: normalizeOnlineStatus(device.status),
        autoMode: Boolean(device.auto_mode),
        healthScore: device.health_score,
        lastCommand: device.last_command,
        virtual: false,
      }
    })
    .filter(Boolean)

  const normalizedAlerts = alerts.map((alert) => {
    const greenhouse = greenhouseByDbId.get(String(alert.greenhouse_id))
    return {
      id: alert.id,
      greenhouseId: greenhouse?.id || normalizeId('GH', alert.greenhouse_id),
      title: alert.title,
      description: alert.description,
      level: alert.level,
      status: alert.status,
      occurredAt: alert.occurred_at,
      resolvedAt: alert.resolved_at,
    }
  })

  return {
    greenhouse: firstGreenhouse || { id: 'GH-001', name: '1号羊肚菌大棚' },
    greenhouses: normalizedGreenhouses,
    sensors,
    devices: normalizedDevices,
    alerts: normalizedAlerts,
    updatedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
  }
}

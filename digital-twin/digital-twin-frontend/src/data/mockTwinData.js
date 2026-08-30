const greenhouses = [
  { id: 'GH-001', name: 'A01 羊肚菌智能大棚', position: [-7.4, -7.2], status: 'NORMAL', location: '园区东南侧', cropStage: '出菇期' },
  { id: 'GH-002', name: 'A02 羊肚菌智能大棚', position: [7.4, -7.2], status: 'NORMAL', location: '园区西南侧', cropStage: '菌丝培养期' },
  { id: 'GH-003', name: 'A03 羊肚菌智能大棚', position: [-7.4, 7.2], status: 'WARNING', location: '园区东北侧', cropStage: '出菇期' },
  { id: 'GH-004', name: 'A04 羊肚菌智能大棚', position: [7.4, 7.2], status: 'NORMAL', location: '园区西北侧', cropStage: '待接入' },
]

const deviceTemplates = [
  { suffix: 'FAN', modelNode: 'FAN_01', name: '循环风机组', location: '后墙风道', runningStatus: 'ON' },
  { suffix: 'PUMP', modelNode: 'PUMP_01', name: '喷灌水泵', location: '水肥区', runningStatus: 'OFF' },
  { suffix: 'LAMP', modelNode: 'LAMP_01', name: '补光灯组', location: '棚顶', runningStatus: 'ON' },
  { suffix: 'SHADE', modelNode: 'SHADE_01', name: '遮阳系统', location: '棚膜顶部', runningStatus: 'OFF' },
]

export const mockTwinData = {
  greenhouse: greenhouses[0],
  greenhouses,
  updatedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
  sensors: [
    { greenhouseId: 'GH-001', deviceId: 'GH-001-TEMPERATURE-01', modelNode: 'SENSOR_TEMP_01', type: 'TEMPERATURE', name: '空气温度', value: 21.8, unit: '℃', status: 'NORMAL', location: 'A01 棚中部' },
    { greenhouseId: 'GH-001', deviceId: 'GH-001-HUMIDITY-01', modelNode: 'SENSOR_HUM_01', type: 'HUMIDITY', name: '空气湿度', value: 84.6, unit: '%RH', status: 'NORMAL', location: 'A01 棚中部' },
    { greenhouseId: 'GH-001', deviceId: 'GH-001-CO2-01', modelNode: 'SENSOR_CO2_01', type: 'CO2', name: 'CO2 浓度', value: 790, unit: 'ppm', status: 'NORMAL', location: 'A01 入口附近' },
    { greenhouseId: 'GH-001', deviceId: 'GH-001-LIGHT-01', modelNode: 'SENSOR_LIGHT_01', type: 'LIGHT', name: '光照强度', value: 4280, unit: 'lx', status: 'WARNING', location: 'A01 种植区顶部' },
    { greenhouseId: 'GH-001', deviceId: 'GH-001-SOIL-01', modelNode: 'SENSOR_SOIL_01', type: 'SOIL_MOISTURE', name: '基质湿度', value: 62.5, unit: '%', status: 'NORMAL', location: 'A01 种植床' },
  ],
  devices: greenhouses.flatMap((greenhouse, greenhouseIndex) => (
    deviceTemplates.map((template, index) => ({
      greenhouseId: greenhouse.id,
      deviceId: `${greenhouse.id}-${template.suffix}`,
      dbId: null,
      modelNode: template.modelNode,
      name: template.name,
      runningStatus: greenhouseIndex === 2 && template.suffix === 'SHADE' ? 'ON' : template.runningStatus,
      onlineStatus: 'ONLINE',
      location: `${greenhouse.name}${template.location}`,
      virtual: true,
      healthScore: 96 - index,
    }))
  )),
  alerts: [
    {
      id: 1,
      greenhouseId: 'GH-003',
      title: '湿度波动偏高',
      description: '连续 8 分钟超过目标上限，请检查加湿策略。',
      level: 'WARNING',
      status: 'OPEN',
      occurredAt: new Date().toISOString(),
    },
  ],
}

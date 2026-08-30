import express from 'express'
import cors from 'cors'
import { query } from './db.js'
import { buildTwinOverview } from './twinMapper.js'

const app = express()
const port = Number(process.env.BACKEND_PORT || 8080)

app.use(cors())
app.use(express.json())

app.get('/api/health', async (_request, response) => {
  try {
    const rows = await query('SELECT 1 AS ok')
    response.json({ ok: true, database: rows[0]?.ok === 1 })
  } catch (error) {
    response.status(500).json({ ok: false, message: error.message })
  }
})

app.get('/api/twin/overview', async (_request, response) => {
  try {
    const [greenhouses, devices, telemetry, alerts] = await Promise.all([
      query(`
        SELECT id, owner_user_id, name, location, status, area, crop_stage, created_at, updated_at
        FROM greenhouse
        ORDER BY id
      `),
      query(`
        SELECT id, greenhouse_id, name, category, status, location, auto_mode, health_score, last_command, created_at, updated_at
        FROM greenhouse_device
        ORDER BY greenhouse_id, id
      `),
      query(`
        SELECT ts.id, ts.greenhouse_id, ts.temperature, ts.humidity, ts.light_lux, ts.co2_ppm, ts.soil_moisture, ts.collected_at
        FROM telemetry_snapshot ts
        INNER JOIN (
          SELECT greenhouse_id, MAX(collected_at) AS latest_collected_at
          FROM telemetry_snapshot
          GROUP BY greenhouse_id
        ) latest ON latest.greenhouse_id = ts.greenhouse_id
          AND latest.latest_collected_at = ts.collected_at
        ORDER BY ts.greenhouse_id
      `),
      query(`
        SELECT id, greenhouse_id, title, description, level, status, occurred_at, resolved_at, device_id
        FROM greenhouse_alert
        ORDER BY occurred_at DESC
        LIMIT 20
      `),
    ])

    response.json(buildTwinOverview({ greenhouses, devices, telemetry, alerts }))
  } catch (error) {
    response.status(500).json({
      message: '读取数字孪生数据失败',
      detail: error.message,
    })
  }
})

app.patch('/api/twin/devices/:id/command', async (request, response) => {
  const { id } = request.params
  const { runningStatus } = request.body
  const nextStatus = runningStatus === 'ON' ? 'RUNNING' : 'STOPPED'

  try {
    const result = await query(
      `
        UPDATE greenhouse_device
        SET status = ?, last_command = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
      `,
      [nextStatus, nextStatus, id],
    )

    if (result.affectedRows === 0) {
      response.status(404).json({ message: 'Device not found' })
      return
    }

    const devices = await query(
      `SELECT id, greenhouse_id, name, category, status, location, auto_mode, health_score, last_command, updated_at
       FROM greenhouse_device WHERE id = ?`,
      [id],
    )
    response.json({ ok: true, device: devices[0] })
  } catch (error) {
    response.status(500).json({
      message: '设备控制失败',
      detail: error.message,
    })
  }
})

app.listen(port, () => {
  console.log(`Digital twin backend is running at http://localhost:${port}`)
})

import 'dotenv/config'
import mysql from 'mysql2/promise'

export const pool = mysql.createPool({
  host: process.env.MYSQL_HOST || '101.42.99.139',
  port: Number(process.env.MYSQL_PORT || 3306),
  database: process.env.MYSQL_DATABASE || 'smart_greenhouse',
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '',
  waitForConnections: true,
  connectionLimit: 8,
  connectTimeout: 5_000,
})

export async function query(sql, params = []) {
  const [rows] = await pool.query(sql, params)
  return rows
}

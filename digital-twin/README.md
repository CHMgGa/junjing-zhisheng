# 智慧羊肚菌大棚数字孪生模块

本目录包含数字孪生大屏的前端与轻量后端接口。

## 目录结构

- `digital-twin-frontend`：Vue + Three.js 数字孪生大屏
- `digital-twin-backend`：Node.js 后端接口，用于连接独立部署的 MySQL 数据库。

## 前端启动

```powershell
cd digital-twin-frontend
npm install
npm run dev
```

## 后端启动

```powershell
cd digital-twin-backend
npm install
copy .env.example .env
npm start
```

数字孪生沿用独立数据库架构，默认连接 `101.42.99.139:3306/smart_greenhouse`。请在
`digital-twin-backend/.env` 中填写该 MySQL 实例的账号和密码；它不使用主 Java 后端的本机数据库。

如果暂时不连接数据库，只启动前端即可，页面会自动回退到模拟数据。

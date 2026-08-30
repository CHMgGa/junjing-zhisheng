# 后端服务与 MySQL 数据管理

本模块提供智慧羊肚菌大棚系统的后端服务、MySQL 数据库结构和示例数据。

## 配置

将 `src/main/resources/application.yml.example` 复制为 `application.yml`，再通过环境变量填写本机 MySQL、邮件与华为云 IoT 配置。

请勿将真实配置文件、数据库密码、云平台密钥或账号提交到 Git。

## 运行

```bash
mvn spring-boot:run
```

# java-plus-graduation (Stage 1: Spring Cloud)

## Структура модулей

- `core/`
  - `main-service` — основной сервис
  - `stats-server` — сервис статистики
- `infra/`
  - `discovery-server` — Spring Cloud Eureka
  - `config-server` — Spring Cloud Config Server (native backend)
  - `gateway-server` — Spring Cloud Gateway (порт `8080`)
- `config/` — конфигурации для Config Server (native)

## Локальный запуск (IntelliJ IDEA)

Запускайте приложения в таком порядке:

1. `infra/discovery-server`
2. `infra/config-server`
3. `core/stats-server`
4. `core/main-service`
5. `infra/gateway-server`

Проверка:

- Eureka UI: `http://localhost:8761`
- Ping через gateway: `http://localhost:8080/api/ping`


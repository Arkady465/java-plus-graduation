Запуск CI-окружения (docker-compose)

1) Построить и запустить:

```bash
cd ci
docker-compose up --build -d
```

2) Подождать готовность gateway и проверить health:

```bash
for i in {1..60}; do
  if curl -sSf http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo "gateway is up"
    break
  fi
  echo "waiting..."
  sleep 2
done

curl -v http://localhost:8080/actuator/health
```

3) Запуск Postman/Newman (если установлен):

```bash
newman run <collection.json> -e <env.json> --delay-request 500 --timeout-request 60000
```

4) Запуск сервисов как процессов через `ci/start-services.sh` (альтернатива docker-compose).

```bash
# В Linux/CI runner:
export HOST_IP=$(hostname -I | awk '{print $1}')
export EUREKA_PREFER_IP_ADDRESS=true
bash ../ci/start-services.sh

# Скрипт запустит discovery(8761), config(8888), stats(9090), main(8081) и gateway(8080) и дождётся их health.
```

Примечание: файл docker-compose.yml минимален и служит для локального/CI тестирования; в CI вы можете предпочесть запуск приложений как процессов (mvn spring-boot:run) с экспортом переменной HOST_IP, или использовать `ci/start-services.sh`.

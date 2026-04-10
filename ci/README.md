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

Примечание: файл docker-compose.yml минимален и служит для локального/CI тестирования; в CI вы можете предпочесть запуск приложений как процессов (mvn spring-boot:run) с экспортом переменной HOST_IP, или использовать `ci/start-services.sh` (ещё не добавлен).

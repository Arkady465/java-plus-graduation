#!/usr/bin/env bash
set -euo pipefail

# Скрипт для CI/local: запускает сервисы последовательно и ждёт их готовности.
# Требуемые env-vars (можно задать в CI job):
# HOST_IP (опционально) — ip адрес раннера, будет передан в eureka.instance.ip-address
# EUREKA_PREFER_IP_ADDRESS (true/false) — если true, сервисы зарегистрируются с ip

HOST_IP=${HOST_IP:-127.0.0.1}
EUREKA_PREF=${EUREKA_PREFER_IP_ADDRESS:-true}
MVN="mvn"

# Таймауты ожидания увеличены для CI
WAIT_ITERATIONS=120
WAIT_SLEEP=2

run_and_log() {
  name=$1
  module=$2
  run_args=${3:-}
  logfile=./logs/${name}.log
  mkdir -p ./logs
  echo "Starting $name (logs -> $logfile)"
  ("$MVN" -DskipTests -pl "$module" spring-boot:run -Dspring-boot.run.arguments="$run_args" -Dspring-boot.run.jvmArguments="-DHOST_IP=${HOST_IP} -DEUREKA_PREFER_IP_ADDRESS=${EUREKA_PREF}" > "$logfile" 2>&1) &
  pids["$name"]=$!
}

wait_for_health() {
  name=$1
  url=$2
  for i in $(seq 1 $WAIT_ITERATIONS); do
    if curl -sSf "$url" | grep -q '"status":"UP"'; then
      echo "$name is UP"
      return 0
    fi
    echo "waiting for $name... ($i/$WAIT_ITERATIONS)"
    sleep $WAIT_SLEEP
  done
  echo "Timed out waiting for $name"
  echo "--- Last logs for diagnosis ---"
  tail -n 200 ./logs/* || true
  exit 1
}

declare -A pids

# Start discovery-server
run_and_log discovery infra/discovery-server "--server.port=8761 --spring.profiles.active=local"
wait_for_health discovery http://localhost:8761/actuator/health

# Give Eureka a short moment to fully initialize
sleep 3

# Start config-server (explicit local profile and port)
run_and_log config infra/config-server "--server.port=8888 --spring.profiles.active=local"
wait_for_health config http://localhost:8888/actuator/health

# Start stats-server
run_and_log stats core/stats-server "--server.port=9090 --spring.profiles.active=local"
wait_for_health stats http://localhost:9090/actuator/health

# Start main-service
run_and_log main core/main-service "--server.port=8081 --spring.profiles.active=local"
wait_for_health main http://localhost:8081/actuator/health

# Start gateway
run_and_log gateway infra/gateway-server "--server.port=8080 --spring.profiles.active=local"
wait_for_health gateway http://localhost:8080/actuator/health

echo "All services started. PIDs:"
for k in "${!pids[@]}"; do
  echo "$k -> ${pids[$k]}"
done

# Wait on pids (so container/job doesn't exit)
wait

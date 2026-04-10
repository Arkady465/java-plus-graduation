#!/usr/bin/env bash
set -euo pipefail

# Скрипт для CI/local: запускает сервисы последовательно и ждёт их готовности.
# Требуемые env-vars (можно задать в CI job):
# HOST_IP (опционально) — ip адрес раннера, будет передан в eureka.instance.ip-address
# EUREKA_PREFER_IP_ADDRESS (true/false) — если true, сервисы зарегистрируются с ip

HOST_IP=${HOST_IP:-127.0.0.1}
EUREKA_PREF=${EUREKA_PREFER_IP_ADDRESS:-true}
MVN="mvn"

run_and_log() {
  name=$1
  shift
  logfile=./logs/${name}.log
  mkdir -p ./logs
  echo "Starting $name (logs -> $logfile)"
  ("$MVN" -DskipTests -pl "$2" spring-boot:run -Dspring-boot.run.jvmArguments="-DHOST_IP=${HOST_IP} -DEUREKA_PREFER_IP_ADDRESS=${EUREKA_PREF}" > "$logfile" 2>&1) &
  pids["$name"]=$!
}

wait_for_health() {
  name=$1
  url=$2
  for i in {1..60}; do
    if curl -sSf "$url" | grep -q '"status":"UP"'; then
      echo "$name is UP"
      return 0
    fi
    echo "waiting for $name..."
    sleep 2
  done
  echo "Timed out waiting for $name"
  tail -n +1 ./logs/*.log
  exit 1
}

declare -A pids

# Start discovery-server
run_and_log discovery infra/discovery-server
wait_for_health discovery http://localhost:8761/actuator/health

# Start config-server
run_and_log config infra/config-server
wait_for_health config http://localhost:8888/actuator/health || true

# Start stats-server
run_and_log stats core/stats-server
wait_for_health stats http://localhost:9090/actuator/health || true

# Start main-service
run_and_log main core/main-service
wait_for_health main http://localhost:8081/actuator/health || true

# Start gateway
run_and_log gateway infra/gateway-server
wait_for_health gateway http://localhost:8080/actuator/health

echo "All services started. PIDs:"
for k in "${!pids[@]}"; do
  echo "$k -> ${pids[$k]}"
done

# Wait on pids (so container/job doesn't exit)
wait

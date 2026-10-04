#!/usr/bin/env bash
# Runs every service as a local JVM against the Postgres + Kafka from docker compose.
#   docker compose up -d postgres kafka
#   mvn -q package -DskipTests
#   scripts/run-local.sh            # logs in ./logs, stop with scripts/run-local.sh stop
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs

SERVICES=(pricing-service cart-service payment-service checkout-service outbox-relay inventory-service
          receipt-service loyalty-service analytics-service ai-platform genai-assistant api-gateway)

if [[ "${1:-}" == "stop" ]]; then
  for s in "${SERVICES[@]}"; do
    [[ -f "logs/$s.pid" ]] && kill "$(cat "logs/$s.pid")" 2>/dev/null || true
    rm -f "logs/$s.pid"
  done
  exit 0
fi

export KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9094}"
for s in "${SERVICES[@]}"; do
  jar=$(ls "$s"/target/"$s"-*.jar | grep -v original | head -1)
  nohup java -jar "$jar" > "logs/$s.log" 2>&1 &
  echo $! > "logs/$s.pid"
  echo "started $s (pid $!)"
done

echo "waiting for health checks..."
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091; do
  for _ in $(seq 1 120); do
    curl -fs "http://localhost:$port/actuator/health" > /dev/null 2>&1 && break
    sleep 1
  done
  printf '  :%s %s\n' "$port" "$(curl -fs "http://localhost:$port/actuator/health" || echo DOWN)"
done
echo "POS client: http://localhost:8080"

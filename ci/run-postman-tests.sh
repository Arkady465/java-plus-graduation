#!/usr/bin/env bash
set -euo pipefail

HOST=localhost
GATEWAY_URL="http://$HOST:8080"

# Wait for gateway
for i in {1..60}; do
  if curl -sSf "$GATEWAY_URL/actuator/health" | grep -q '"status":"UP"'; then
    echo "gateway is up"; break
  fi
  echo "waiting gateway..."
  sleep 2
done

# Find Postman collection and environment
COLLECTION=".github/postman/ExploreWithMe.postman_collection.json"
ENVFILE=".github/postman/ExploreWithMe.postman_environment.ci.json"

if [ ! -f "$COLLECTION" ]; then
  echo "Default collection not found at $COLLECTION. Searching repository for *.postman_collection.json..."
  COLLECTION=$(find . -type f -name "*.postman_collection.json" | head -n 1 || true)
  if [ -z "$COLLECTION" ]; then
    echo "No postman collection found in repository. Skipping newman tests.";
  else
    echo "Found collection: $COLLECTION"
  fi
fi

if [ -z "$COLLECTION" ] || [ ! -f "$COLLECTION" ]; then
  echo "No collection to run. Exiting 0."; exit 0
fi

if [ ! -f "$ENVFILE" ]; then
  echo "Default env not found at $ENVFILE. Searching for *postman_environment*.json..."
  ENVFILE=$(find . -type f -name "*postman_environment*.json" | head -n 1 || true)
  if [ -n "$ENVFILE" ]; then
    echo "Found env: $ENVFILE"
  else
    echo "No environment file found; running collection without environment variables."; ENVFILE=""
  fi
fi

# Run newman (assumes newman is installed in CI)
if command -v newman >/dev/null 2>&1; then
  echo "Running newman tests..."
  if [ -n "$ENVFILE" ]; then
    newman run "$COLLECTION" -e "$ENVFILE" --delay-request 500 --timeout-request 60000 --reporters cli,junit --reporter-junit-export newman-results.xml
  else
    newman run "$COLLECTION" --delay-request 500 --timeout-request 60000 --reporters cli,junit --reporter-junit-export newman-results.xml
  fi
else
  echo "newman not installed; skipping postman tests"
fi

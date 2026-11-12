#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] && { set -a; . ./.env; set +a; }
: "${GEMINI_API_KEY:?GEMINI_API_KEY not set (check .env)}"

mvn -q compile exec:java \
  -Dexec.mainClass=com.google.adk.web.AdkWebServer \
  -Dexec.args="--adk.agents.source-dir=target --server.port=8080"
#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${1:-http://localhost:8080}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/docs/openapi.json"
curl -sf "${BASE_URL}/v3/api-docs" | python3 -m json.tool > "$OUT"
echo "Wrote $OUT ($(wc -c < "$OUT") bytes)"

#!/usr/bin/env bash
set -euo pipefail

SEED="${CASSANDRA_SEED:-cassandra1}"
PORT="${CASSANDRA_PORT:-9042}"
CQL_FILE="${CASSANDRA_CQL_FILE:-/init/shortener.cql}"
EXPECTED_UN="${CASSANDRA_EXPECTED_UN:-3}"

echo "[cassandra-init] Aguardando Cassandra responder em ${SEED}:${PORT}..."
until cqlsh "$SEED" "$PORT" -e "SELECT release_version FROM system.local" >/dev/null 2>&1; do
  sleep 2
done

echo "[cassandra-init] Aguardando cluster enxergar ${EXPECTED_UN} nós (system.peers_v2 + local)..."

until [ "$(cqlsh "$SEED" "$PORT" -e "SELECT count(*) FROM system.peers_v2;" 2>/dev/null | awk 'NR==4{print $1}')" != "" ] \
  && [ "$(( $(cqlsh "$SEED" "$PORT" -e "SELECT count(*) FROM system.peers_v2;" 2>/dev/null | awk 'NR==4{print $1}') + 1 ))" -ge "$EXPECTED_UN" ]; do
  sleep 3
done

echo "[cassandra-init] Aplicando CQL: ${CQL_FILE}"
cqlsh "$SEED" "$PORT" -f "$CQL_FILE"

echo "[cassandra-init] Finalizado com sucesso."
exit 0
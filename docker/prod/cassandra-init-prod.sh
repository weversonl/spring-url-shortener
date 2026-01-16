#!/usr/bin/env bash
set -euo pipefail

SEED="${CASSANDRA_SEED:-cassandra}"
PORT="${CASSANDRA_PORT:-9042}"
CQL_FILE="${CASSANDRA_CQL_FILE:-/init/shortener.cql}"

echo "[cassandra-init] Aguardando Cassandra responder em ${SEED}:${PORT}..."
until cqlsh "$SEED" "$PORT" -e "SELECT release_version FROM system.local" >/dev/null 2>&1; do
  sleep 2
done

echo "[cassandra-init] Aplicando CQL: ${CQL_FILE}"
cqlsh "$SEED" "$PORT" -f "$CQL_FILE"

echo "[cassandra-init] Finalizado com sucesso."
exit 0
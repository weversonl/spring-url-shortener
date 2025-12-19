#!/usr/bin/env sh
set -eu

echo "Aguardando Redis nodes subirem..."
for h in redis1 redis2 redis3 redis4 redis5 redis6; do
  until redis-cli -h "$h" -p 6379 ping | grep -q PONG; do
    sleep 1
  done
done

if redis-cli -h redis1 -p 6379 cluster info 2>/dev/null | grep -q 'cluster_state:ok'; then
  echo "Cluster já está OK. Nada a fazer."
  exit 0
fi

echo "Criando Redis Cluster (3 masters + 3 replicas)..."
yes yes | redis-cli --cluster create \
  redis1:6379 redis2:6379 redis3:6379 \
  redis4:6379 redis5:6379 redis6:6379 \
  --cluster-replicas 1

echo "Cluster pronto."
exit 0
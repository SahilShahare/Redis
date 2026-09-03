#!/bin/sh
set -e

# compile
(
  cd "$(dirname "$0")"
  mvn -q -B package -Ddir=/tmp/redis-java
)

echo "Build Complete ...."

# execute
exec java --enable-preview -jar /tmp/redis-java/redis.jar --port 6379 --replicaof "localhost 6379"


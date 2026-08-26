# Exit early id any commands fail
set -e

# compile
(
  cd "$(dirname "$0")"
  mvn -q -B package -Ddir=/tmp/redis-java
)

# execute
exec java --enable-preview -jar /tmp/redis-java/redis.jar


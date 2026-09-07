#!/bin/bash
# 积木报表一键启动脚本 (沙箱环境 systemd 不可用，手动拉起服务)
# 用法: bash /workspace/start-jimureport.sh
set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

JIMU_JAR=/workspace/jimureport-src/jimureport-example/target/jimureport-example-2.5.jar
JIMU_PORT=8085

echo "==================== 1. 启动 MySQL ===================="
if pgrep -x mysqld >/dev/null; then
  echo "[skip] MySQL already running (pid $(pgrep -x mysqld | head -1))"
else
  mkdir -p /var/run/mysqld /var/lib/mysql
  chown -R mysql:mysql /var/run/mysqld /var/lib/mysql
  [ -d /var/lib/mysql/mysql ] || mysqld --initialize-insecure --user=mysql
  nohup mysqld --user=mysql --datadir=/var/lib/mysql \
    --socket=/var/run/mysqld/mysqld.sock \
    --pid-file=/var/run/mysqld/mysqld.pid > /tmp/mysqld.log 2>&1 &
  sleep 8
  echo "[ok] MySQL started (pid $(pgrep -x mysqld | head -1))"
fi

echo "==================== 2. 启动 Redis ===================="
if redis-cli -h 127.0.0.1 -p 6379 ping 2>/dev/null | grep -q PONG; then
  echo "[skip] Redis already running"
else
  mkdir -p /var/run/redis /var/lib/redis /var/log/redis
  chown -R redis:redis /var/run/redis /var/lib/redis /var/log/redis 2>/dev/null || true
  nohup redis-server --daemonize no --bind 127.0.0.1 --port 6379 \
    --dir /var/lib/redis --logfile /var/log/redis/redis-server.log > /tmp/redis.log 2>&1 &
  sleep 3
  echo "[ok] Redis started ($(redis-cli ping 2>/dev/null))"
fi

echo "==================== 3. 启动积木报表 ===================="
if (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep -q ":$JIMU_PORT "; then
  echo "[skip] JimuReport already running on port $JIMU_PORT"
else
  [ -f "$JIMU_JAR" ] || { echo "[error] jar not found: $JIMU_JAR"; exit 1; }
  nohup java -Xmx1g -Xms512m -jar "$JIMU_JAR" > /tmp/jimureport.log 2>&1 &
  echo "[ok] JimuReport starting (pid $!), waiting for port $JIMU_PORT ..."
  for i in $(seq 1 30); do
    (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep -q ":$JIMU_PORT " && break
    sleep 2
  done
fi

echo ""
echo "==================== 部署完成 ===================="
echo "报表工作台 : http://localhost:$JIMU_PORT/jmreport/list"
echo "大屏工作台 : http://localhost:$JIMU_PORT/drag/list"
echo "ChatBI     : http://localhost:$JIMU_PORT/jimu/chat2bi/index"
echo "登录账号   : admin / 123456"
echo "日志       : /tmp/jimureport.log"

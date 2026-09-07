#!/bin/bash
# WMS + 积木报表 一体化启动脚本
# 用法: bash /workspace/start-wms-with-jimureport.sh
set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

WMS_JAR=/workspace/backend/target/wms-backend-0.0.1-SNAPSHOT.jar
WMS_PORT=8080

echo "==================== 1. 启动 MySQL ===================="
if pgrep -x mysqld >/dev/null; then
  echo "[skip] MySQL already running"
else
  mkdir -p /var/run/mysqld /var/lib/mysql
  chown -R mysql:mysql /var/run/mysqld /var/lib/mysql
  [ -d /var/lib/mysql/mysql ] || mysqld --initialize-insecure --user=mysql
  nohup mysqld --user=mysql --datadir=/var/lib/mysql \
    --socket=/var/run/mysqld/mysqld.sock \
    --pid-file=/var/run/mysqld/mysqld.pid > /tmp/mysqld.log 2>&1 &
  sleep 8
  echo "[ok] MySQL started"
fi

echo "==================== 2. 启动 WMS + 积木报表 ===================="
if (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep -q ":$WMS_PORT "; then
  echo "[skip] WMS already running on port $WMS_PORT"
else
  [ -f "$WMS_JAR" ] || { echo "[error] jar not found: $WMS_JAR"; exit 1; }
  export JWT_SECRET="${JWT_SECRET:-WmsIntegrationJimuReport2026SecretKeyForJwt_0123456789abcdef}"
  export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://localhost:8080}"
  nohup java -Xmx1g -Xms512m -jar "$WMS_JAR" > /tmp/wms.log 2>&1 &
  echo "[ok] WMS+积木 starting (pid $!), waiting for port $WMS_PORT ..."
  for i in $(seq 1 40); do
    (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep -q ":$WMS_PORT " && break
    sleep 2
  done
fi

echo ""
echo "==================== 部署完成 ===================="
echo ""
echo "WMS 业务接口:    http://localhost:$WMS_PORT/api/..."
echo "积木报表工作台:  http://localhost:$WMS_PORT/jmreport/list"
echo "积木大屏工作台:  http://localhost:$WMS_PORT/drag/list"
echo ""
echo "WMS 登录账号:    admin / admin123"
echo "（默认密码详见 DataInitializer）"
echo ""
echo "说明:"
echo "  - 积木报表路径已通过 Spring Security 放行 (/jmreport/**, /drag/**, /jm/** 等)"
echo "  - WMS 业务 API 仍需 JWT 鉴权"
echo "  - 积木与 WMS 共用同一 MySQL (库: wms)，积木 58 张表已导入"
echo "  - 日志: /tmp/wms.log"

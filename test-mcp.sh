#!/bin/bash

# MCP 服务测试脚本

echo "================================"
echo "md2doc MCP 服务测试"
echo "================================"
echo ""

# 服务地址
SERVER="http://localhost:8080"

# 1. 测试 SSE 端点
echo "1. 测试 MCP SSE 端点 /dataReport/md2doc..."
echo "---"
timeout 2 curl -N -H "Accept: text/event-stream" "$SERVER/dataReport/md2doc" 2>/dev/null
echo ""
echo "✓ SSE 端点正常 (如果看到 sessionId 返回)"
echo ""

# 2. 测试 REST API
echo "2. 测试 REST API 端点..."
echo "---"
RESPONSE=$(curl -s -X POST "$SERVER/api/markdown/convert/text" \
  -H "Content-Type: application/json" \
  -d '{"content":"# 测试\n\n这是测试内容"}')

if echo "$RESPONSE" | grep -q "fileUrl"; then
    echo "✓ REST API 正常工作"
    echo "响应: $RESPONSE"
else
    echo "✗ REST API 响应异常"
    echo "响应: $RESPONSE"
fi
echo ""

# 3. 检查服务状态
echo "3. 检查服务进程..."
echo "---"
PID=$(ps aux | grep "md2doc-service" | grep -v grep | awk '{print $2}' | head -1)
if [ ! -z "$PID" ]; then
    echo "✓ 服务正在运行"
    echo "进程 ID: $PID"
else
    echo "✗ 服务未运行"
fi
echo ""

# 4. 检查端口监听
echo "4. 检查端口监听..."
echo "---"
if lsof -i :8080 | grep -q LISTEN; then
    echo "✓ 端口 8080 正在监听"
    lsof -i :8080 | grep LISTEN
else
    echo "✗ 端口 8080 未监听"
fi
echo ""

# 5. MCP 配置建议
echo "================================"
echo "MCP 客户端配置"
echo "================================"
echo ""
echo "Cursor/Claude Desktop 配置文件: ~/.cursor/mcp.json"
echo ""
echo '{'
echo '  "mcpServers": {'
echo '    "md2doc": {'
echo '      "command": "curl",'
echo '      "args": ['
echo '        "-N",'
echo '        "-H",'
echo '        "Accept: text/event-stream",'
echo '        "http://192.9.243.78:8080/dataReport/md2doc"'
echo '      ]'
echo '    }'
echo '  }'
echo '}'
echo ""
echo "================================"
echo "测试完成!"
echo "================================"

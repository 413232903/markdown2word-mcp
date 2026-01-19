#!/usr/bin/env python3
"""测试 MCP 服务的完整流程"""
import requests
import time
import threading
from queue import Queue

def test_mcp_service():
    # 步骤 1: 建立连接
    session = requests.Session()

    # 1.1 连接 SSE 端点
    print("步骤 1: 连接 SSE 端点...")
    sse_response = session.get(
        "http://192.9.253.106:8080/dataReport/md2doc",
        stream=True,
        timeout=2
    )

    # 1.2 读取 SSE 返回的 sessionId
    session_id = None
    for line in sse_response.iter_lines():
        line = line.decode('utf-8')
        if line.startswith('data:') and 'sessionId' in line:
            session_id = line.split('sessionId=')[-1]
            print(f"获取到 sessionId: {session_id}")
            break
        if line.strip():
            print(f"  {line}")

    if not session_id:
        print("错误: 未能获取 sessionId")
        return

    # 步骤 2: 发送初始化请求
    print("\n步骤 2: 发送初始化请求...")
    init_request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {
                "name": "test-client",
                "version": "1.0"
            }
        }
    }

    try:
        msg_response = session.post(
            f"http://192.9.253.106:8080/dataReport/mcp/message?sessionId={session_id}",
            json=init_request,
            timeout=3
        )
        print(f"初始化响应状态: {msg_response.status_code}")
        if msg_response.status_code == 200:
            print(f"初始化响应: {msg_response.text[:200]}")
        else:
            print(f"错误响应: {msg_response.text[:500]}")
    except Exception as e:
        print(f"请求失败: {e}")

    # 步骤 3: 测试其他方法
    print("\n步骤 3: 测试 tools/list 方法...")
    tools_request = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/list"
    }

    try:
        msg_response = session.post(
            f"http://192.9.253.106:8080/dataReport/mcp/message?sessionId={session_id}",
            json=tools_request,
            timeout=3
        )
        print(f"tools/list 响应状态: {msg_response.status_code}")
        if msg_response.status_code == 200:
            print(f"tools/list 响应: {msg_response.text[:500]}")
    except Exception as e:
        print(f"请求失败: {e}")

if __name__ == "__main__":
    test_mcp_service()

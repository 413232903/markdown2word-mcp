#!/usr/bin/env python3
"""
MCP 协议测试脚本 - 模拟 MCP 客户端连接并列出工具
"""

import json
import requests
import time

SERVER = "http://localhost:8080"

def test_mcp_protocol():
    print("="*60)
    print("MCP 协议完整测试")
    print("="*60)
    print()

    # Step 1: Connect to SSE endpoint to get session
    print("步骤 1: 连接 SSE 端点获取 sessionId...")
    print("-"*60)
    try:
        response = requests.get(
            f"{SERVER}/sse",
            headers={"Accept": "text/event-stream"},
            stream=True,
            timeout=5
        )

        session_id = None
        for line in response.iter_lines(decode_unicode=True):
            if line.startswith("data:"):
                data = line[5:].strip()
                if "/mcp/message" in data:
                    # 提取 sessionId
                    if "sessionId=" in data:
                        session_id = data.split("sessionId=")[1]
                        print(f"✓ 获取到 sessionId: {session_id}")
                        break

        if not session_id:
            print("✗ 未能获取 sessionId")
            return False

    except Exception as e:
        print(f"✗ SSE 连接失败: {e}")
        return False

    print()

    # Step 2: Initialize MCP connection
    print("步骤 2: 初始化 MCP 连接...")
    print("-"*60)
    init_request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2024-11-05",
            "capabilities": {
                "roots": {"listChanged": True},
                "sampling": {}
            },
            "clientInfo": {
                "name": "test-client",
                "version": "1.0.0"
            }
        }
    }

    try:
        response = requests.post(
            f"{SERVER}/mcp/message",
            params={"sessionId": session_id},
            json=init_request,
            headers={"Content-Type": "application/json"}
        )

        if response.status_code == 200:
            result = response.json()
            print("✓ MCP 初始化成功")
            print(f"服务器信息: {json.dumps(result.get('result', {}), indent=2, ensure_ascii=False)}")
        else:
            print(f"✗ 初始化失败: {response.status_code}")
            print(f"响应: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 初始化请求失败: {e}")
        return False

    print()

    # Step 3: List tools
    print("步骤 3: 列出可用工具...")
    print("-"*60)
    tools_request = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/list"
    }

    try:
        response = requests.post(
            f"{SERVER}/mcp/message",
            params={"sessionId": session_id},
            json=tools_request,
            headers={"Content-Type": "application/json"}
        )

        if response.status_code == 200:
            result = response.json()
            tools = result.get("result", {}).get("tools", [])

            if tools:
                print(f"✓ 找到 {len(tools)} 个工具:")
                print()
                for i, tool in enumerate(tools, 1):
                    print(f"{i}. {tool.get('name', 'unknown')}")
                    print(f"   描述: {tool.get('description', 'N/A')}")
                    print(f"   输入模式: {json.dumps(tool.get('inputSchema', {}), indent=6, ensure_ascii=False)}")
                    print()
            else:
                print("✗ 没有找到任何工具")
                print(f"完整响应: {json.dumps(result, indent=2, ensure_ascii=False)}")
                return False
        else:
            print(f"✗ 列出工具失败: {response.status_code}")
            print(f"响应: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 工具列表请求失败: {e}")
        return False

    print("="*60)
    print("✓ 所有测试通过! MCP 工具已正确注册")
    print("="*60)
    return True

if __name__ == "__main__":
    test_mcp_protocol()

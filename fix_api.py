# -*- coding: utf-8 -*-
import subprocess, os

# Restore from git
result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/services/api.js"], capture_output=True)
content = result.stdout.decode("utf-8")

BT = "`"
insert = """
  // 删除指定会话
  async deleteChat(type, chatId) {
    try {
      const response = await fetch(""" + BT + """${BASE_URL}/ai/history/${type}/${chatId}""" + BT + """, {
        method: 'DELETE'
      })
      if (!response.ok) {
        throw new Error(""" + BT + """HTTP error! status: ${response.status}""" + BT + """)
      }
      return true
    } catch (error) {
      console.error('删除会话失败:', error)
      return false
    }
  }
"""

idx = content.rfind("\n}")
content = content[:idx] + insert + "\n}"

with open("src/main/resources/static/spring-ai-protal/src/services/api.js", "w", encoding="utf-8") as f:
    f.write(content)
print("Done")

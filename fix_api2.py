import subprocess

result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/services/api.js"], capture_output=True)
content = result.stdout.decode("utf-8")

BT = chr(96)
insert = ",\n  // \u5220\u9664\u6307\u5b9a\u4f1a\u8bdd\n  async deleteChat(type, chatId) {\n    try {\n      const response = await fetch(" + BT + "${BASE_URL}/ai/history/${type}/${chatId}" + BT + ", {\n        method: 'DELETE'\n      })\n      if (!response.ok) {\n        throw new Error(" + BT + "HTTP error! status: ${response.status}" + BT + ")\n      }\n      return true\n    } catch (error) {\n      console.error('\u5220\u9664\u4f1a\u8bdd\u5931\u8d25:', error)\n      return false\n    }\n  }\n"

idx = content.rfind("\n}")
content = content[:idx] + insert + "\n}"

with open("src/main/resources/static/spring-ai-protal/src/services/api.js", "w", encoding="utf-8") as f:
    f.write(content)
print("Done")

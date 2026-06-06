with open("src/main/java/com/itheima/ai/config/CommonConfiguration.java", "r", encoding="utf-8") as f:
    content = f.read()
print("=== CommonConfiguration.java ===")

# Find ChatClient beans
import re
for m in re.finditer(r'@Bean.*?\n(?:    )?public ChatClient (\w+)', content):
    print(f"  ChatClient: {m.group(1)}")

# Find tools
for m in re.finditer(r'defaultTools\((\w+)\)', content):
    print(f"  Tool: {m.group(1)}")

# Find advisors
for m in re.finditer(r'new (\w+)[(]', content):
    name = m.group(1)
    if "Advisor" in name:
        print(f"  Advisor: {name}")

# Find system prompts
for m in re.finditer(r'defaultSystem\((\w+)\)', content):
    print(f"  SystemPrompt reference: {m.group(1)}")

import subprocess, os

result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue"], capture_output=True)
content = result.stdout.decode("utf-8")

# Step 1: history items - use sentinel approach
history_open = 'class="history-item"'
history_end = "ChatBubbleLeftRightIcon"
delim = "VVV_DELIM_VVV"

# Find the history block to replace
start = content.find("v-for=\"chat in chatHistory\"")
end_block = content.find(delim)  # will fail
print(f"History start at: {start}")

# Better approach: find the specific block using line-by-line
lines = content.split("\n")
for i, line in enumerate(lines):
    if 'v-for="chat in chatHistory"' in line:
        print(f"Found history at line {i+1}: {line}")
        break

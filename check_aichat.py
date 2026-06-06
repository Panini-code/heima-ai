with open("src/main/resources/static/spring-ai-protal/src/views/AIChat.vue", "r", encoding="utf-8") as f:
    lines = f.readlines()
print("=== History items ===")
for i, line in enumerate(lines):
    if "v-for" in line and "chatHistory" in line:
        for j in range(i, i+18):
            if j < len(lines):
                print(f"{j+1}: {lines[j].rstrip()}")
        break
print()
print("=== deleteTarget/confirmDelete/doDelete ===")
for i, line in enumerate(lines):
    if "deleteTarget" in line or "confirmDelete" in line or "doDelete" in line:
        print(f"{i+1}: {line.rstrip()}")
print()
print("=== TrashIcon ===")
for i, line in enumerate(lines):
    if "TrashIcon" in line:
        print(f"{i+1}: {line.rstrip()}")
print()
print("=== Modal overlay ===")
for i, line in enumerate(lines):
    if "modal-overlay" in line:
        for j in range(i, i+12):
            if j < len(lines):
                print(f"{j+1}: {lines[j].rstrip()}")
        break

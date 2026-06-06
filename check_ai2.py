with open("src/main/resources/static/spring-ai-protal/src/views/AIChat.vue", "r", encoding="utf-8") as f:
    content = f.read()

# Check imports
if "TrashIcon" in content:
    print("TrashIcon import: FOUND")
else:
    print("TrashIcon import: MISSING")

# Check modal HTML in template
if "modal-overlay" in content.split("<script")[0]:
    print("Modal HTML: FOUND in template")
else:
    print("Modal HTML: MISSING from template")

# Check history-item-content
if "history-item-content" in content:
    print("history-item-content: FOUND")
else:
    print("history-item-content: MISSING")

# Check delete-btn in template
template_part = content.split("<script")[0]
if "delete-btn" in template_part:
    print("delete-btn in template: FOUND")
else:
    print("delete-btn in template: MISSING")

with open("src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue", "r", encoding="utf-8") as f:
    content = f.read()

template_part = content.split("<script")[0]
if "delete-btn" in template_part:
    print("CustomerService delete-btn in template: FOUND")
else:
    print("CustomerService delete-btn in template: MISSING")
if "history-item-content" in template_part:
    print("CustomerService history-item-content: FOUND")
else:
    print("CustomerService history-item-content: MISSING")
if "TrashIcon" in content:
    print("CustomerService TrashIcon import: FOUND")
else:
    print("CustomerService TrashIcon import: MISSING")

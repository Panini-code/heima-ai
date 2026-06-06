with open("src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue", "r", encoding="utf-8") as f:
    content = f.read()
template = content.split("<script")[0]
if "delete-btn" in template and "history-item-content" in template and "modal-overlay" in template:
    print("CustomerService: ALL GOOD")
else:
    print("CustomerService: ISSUES FOUND")

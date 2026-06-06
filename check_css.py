import subprocess, re

for fname in ["AIChat.vue", "CustomerService.vue", "ChatPDF.vue"]:
    path = f"src/main/resources/static/spring-ai-protal/src/views/{fname}"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    # Find first class definition in style section
    style = content.split("<style")[1] if "<style" in content else ""
    # Find top-level class and its position:fixed
    for match in re.finditer(r'\.\w[\w-]*\s*\{[^}]*position:\s*fixed[^}]*\}', style):
        print(f"{fname}: {match.group()[:100]}")
    for match in re.finditer(r'\.\w[\w-]*\s*\{[^}]*overflow[^}]*\}', style):
        print(f"{fname}: {match.group()[:100]}")

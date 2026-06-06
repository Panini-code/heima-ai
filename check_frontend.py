import re

# Check frontend components
import os
views_dir = "src/main/resources/static/spring-ai-protal/src/views"
for fname in sorted(os.listdir(views_dir)):
    if fname.endswith(".vue"):
        path = os.path.join(views_dir, fname)
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        # Get the first template div class
        m = re.search(r'<template>.*?<div class="([^"]*)"', content, re.DOTALL)
        cls = m.group(1) if m else "?"
        # Get component imports
        comps = re.findall(r"import (\w+) from ['\"]\.\./components/", content)
        print(f"  {fname}: root=_.{cls}, components={comps}")

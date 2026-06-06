import re
with open("pom.xml", "r", encoding="utf-8") as f:
    content = f.read()

# Find all spring-ai dependencies
for m in re.finditer(r"<artifactId>(spring-ai[^<]*)</artifactId>\n\s*<version>([^<]*)</version>", content):
    print(f"  {m.group(1)}: {m.group(2)}")

# Find all other AI-related deps
for m in re.finditer(r"<artifactId>([^<]*ai[^<]*)</artifactId>", content):
    print(f"  {m.group(1)}")
for m in re.finditer(r"<artifactId>([^<]*(embedding|vector|chroma|milvus|pgvector|redis|pdf|ocr|tts|asr)[^<]*)</artifactId>", content):
    print(f"  {m.group(1)}")

# Check key AI services
files_to_check = [
    "service/QueryRewriteService.java",
    "service/ReRankerService.java",
    "service/DocumentChunkingService.java",
    "advisor/ReRankerAdvisor.java",
    "controller/PdfController.java",
    "controller/ChatController.java",
    "controller/CustomerServiceController.java",
    "tools/CourseTools.java",
    "repository/RedisChatMemory.java",
    "model/AlibabaOpenAiChatModel.java",
    "utils/VectorDistanceUtils.java",
]

for fname in files_to_check:
    path = f"src/main/java/com/itheima/ai/{fname}"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    lines = content.split("\n")
    # Find class declaration and key annotations
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("public class") or stripped.startswith("@Service") or stripped.startswith("@Component") or stripped.startswith("@Repository") or stripped.startswith("public interface"):
            print(f"{fname}: {stripped[:80]}")
        if "implements" in stripped and "public class" in stripped:
            print(f"  -> {stripped[stripped.find('implements'):stripped.find('implements')+60]}")

import subprocess

# Restore CustomerService.vue from git
result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue"], capture_output=True)
content = result.stdout.decode("utf-8")

# 1. history items
old = """          <div 
            v-for="chat in chatHistory" 
            :key="chat.id"
            class="history-item"
            :class="{ \"active\": currentChatId === chat.id }"
            @click="loadChat(chat.id)"
          >
            <ChatBubbleLeftRightIcon class=\"icon\" />
            <span class=\"title\">{{ chat.title || '\u65b0\u54a8\u8be2' }}</span>
          </div>"""

new = """          <div 
            v-for="chat in chatHistory" 
            :key="chat.id"
            class="history-item"
            :class="{ \"active\": currentChatId === chat.id }"
          >
            <div class=\"history-item-content\" @click=\"loadChat(chat.id)\">
              <ChatBubbleLeftRightIcon class=\"icon\" />
              <span class=\"title\">{{ chat.title || '\u65b0\u54a8\u8be2' }}</span>
            </div>
            <button class=\"delete-btn\" @click.stop=\"confirmDelete(chat)\" title=\"\u5220\u9664\u4f1a\u8bdd\">
              <TrashIcon class=\"icon\" />
            </button>
          </div>"""

assert old in content
content = content.replace(old, new, 1)
print("Step 1 OK")

# 2. modal
old = "  </div>\n</template>"
new = """  </div>
  
  <!-- \u5220\u9664\u786e\u8ba4\u5f39\u7a97 -->
  <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
    <div class="modal-card">
      <div class="modal-header">\u786e\u8ba4\u5220\u9664</div>
      <div class="modal-body">\u786e\u5b9a\u8981\u5220\u9664\u6b64\u4f1a\u8bdd\u5417\uff1f\u5220\u9664\u540e\u76f8\u5173\u804a\u5929\u8bb0\u5f55\u5c06\u88ab\u6e05\u9664\u3002\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\u3002</div>
      <div class="modal-footer">
        <button class="btn btn-cancel" @click="deleteTarget = null">\u53d6\u6d88</button>
        <button class="btn btn-danger" @click="doDelete">\u786e\u8ba4\u5220\u9664</button>
      </div>
    </div>
  </div>
</template>"""

assert old in content
content = content.replace(old, new, 1)
print("Step 2 OK")

# 3. TrashIcon import
old = "  ComputerDesktopIcon\n} from '@heroicons/vue/24/outline'"
new = "  ComputerDesktopIcon,\n  TrashIcon\n} from '@heroicons/vue/24/outline'"
assert old in content
content = content.replace(old, new, 1)
print("Step 3 OK")

# 4. deleteTarget ref
old = "const bookingInfo = ref('')"
new = "const bookingInfo = ref('')\nconst deleteTarget = ref(null)"
assert old in content
content = content.replace(old, new, 1)
print("Step 4 OK")

# 5. methods
old = "onMounted(() => {\n  loadChatHistory()\n})"
new = """const confirmDelete = (chat) => {
  deleteTarget.value = chat
}

const doDelete = async () => {
  if (!deleteTarget.value) return
  try {
    await chatAPI.deleteChat('service', deleteTarget.value.id)
    chatHistory.value = chatHistory.value.filter(c => c.id !== deleteTarget.value.id)
    if (currentChatId.value === deleteTarget.value.id) {
      currentChatId.value = null
      currentMessages.value = []
    }
  } catch (err) {
    console.error('\u5220\u9664\u5931\u8d25:', err)
  }
  deleteTarget.value = null
}

onMounted(() => {
  loadChatHistory()
})"""

assert old in content, "onMounted pattern not found!"
content = content.replace(old, new, 1)
print("Step 5 OK")

# 6. CSS
css = """

// ===== \u5220\u9664\u6309\u94ae =====
.history-item {
  position: relative;
  
  .history-item-content {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem;
    cursor: pointer;
    flex: 1;
    min-width: 0;
  }
  
  .delete-btn {
    position: absolute;
    right: 0.5rem;
    top: 50%;
    transform: translateY(-50%);
    opacity: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 0.5rem;
    background: transparent;
    color: #999;
    cursor: pointer;
    transition: all 0.2s ease;
    
    &:hover {
      background: #ff4d4f;
      color: white;
    }
    
    .icon {
      width: 1.2rem;
      height: 1.2rem;
    }
  }
  
  &:hover .delete-btn {
    opacity: 1;
  }
}

// ===== \u786e\u8ba4\u5f39\u7a97 =====
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  min-width: 360px;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.modal-body {
  font-size: 0.95rem;
  color: #666;
  line-height: 1.5;
  margin-bottom: 1.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn {
  padding: 0.5rem 1.25rem;
  border-radius: 8px;
  border: none;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  background: #f0f0f0;
  color: #333;
  &:hover { background: #e0e0e0; }
}

.btn-danger {
  background: #ff4d4f;
  color: white;
  &:hover { background: #e04345; }
}

// \u6697\u8272\u6a21\u5f0f\u5f39\u7a97\u6837\u5f0f
.dark {
  .modal-card {
    background: #333;
    .modal-header { color: #fff; }
    .modal-body { color: #ccc; }
  }
  .btn-cancel {
    background: #555;
    color: #eee;
    &:hover { background: #666; }
  }
}
"""

old = "</style>"
assert old in content
content = content.replace(old, css + "\n" + old, 1)
print("Step 6 OK")

with open("src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("ALL DONE")

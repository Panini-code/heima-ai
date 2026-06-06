import subprocess

result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/views/AIChat.vue"], capture_output=True)
content = result.stdout.decode("utf-8")

# Step 1: Replace history template - match exact original text
old_lines = [
    "          <div ",
    '            v-for="chat in chatHistory" ',
    '            :key="chat.id"',
    '            class="history-item"',
    "            :class=\"{ 'active': currentChatId === chat.id }\"",
    '            @click="loadChat(chat.id)"',
    "          >",
    '            <ChatBubbleLeftRightIcon class="icon" />',
    "            <span class=\"title\">{{ chat.title || '新对话' }}</span>",
    "          </div>",
]
old_text = "\n".join(old_lines)

new_lines = [
    "          <div ",
    '            v-for="chat in chatHistory" ',
    '            :key="chat.id"',
    '            class="history-item"',
    "            :class=\"{ 'active': currentChatId === chat.id }\"",
    "          >",
    '            <div class="history-item-content" @click="loadChat(chat.id)">',
    '              <ChatBubbleLeftRightIcon class="icon" />',
    "              <span class=\"title\">{{ chat.title || '新对话' }}</span>",
    "            </div>",
    '            <button class="delete-btn" @click.stop="confirmDelete(chat)" title="删除会话">',
    '              <TrashIcon class="icon" />',
    "            </button>",
    "          </div>",
]
new_text = "\n".join(new_lines)

assert old_text in content, "HISTORY TEMPLATE NOT FOUND!"
content = content.replace(old_text, new_text, 1)
print("Step 1 OK")

# Step 2: Add modal
assert "  </div>\n</template>" in content, "CLOSE NOT FOUND!"
content = content.replace(
    "  </div>\n</template>",
    """  </div>
  
  <!-- 删除确认弹窗 -->
  <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
    <div class="modal-card">
      <div class="modal-header">确认删除</div>
      <div class="modal-body">确定要删除此会话吗？删除后相关聊天记录将被清除。此操作不可恢复。</div>
      <div class="modal-footer">
        <button class="btn btn-cancel" @click="deleteTarget = null">取消</button>
        <button class="btn btn-danger" @click="doDelete">确认删除</button>
      </div>
    </div>
  </div>
</template>""",
    1,
)
print("Step 2 OK")

# Step 3: Import TrashIcon
content = content.replace(
    "  XMarkIcon\n} from '@heroicons/vue/24/outline'",
    "  XMarkIcon,\n  TrashIcon\n} from '@heroicons/vue/24/outline'",
    1,
)
print("Step 3 OK")

# Step 4: Add deleteTarget ref
content = content.replace(
    "const selectedFiles = ref([])",
    "const selectedFiles = ref([])\nconst deleteTarget = ref(null)",
    1,
)
print("Step 4 OK")

# Step 5: Add methods before onMounted
content = content.replace(
    "onMounted(() => {\n  loadChatHistory()\n  adjustTextareaHeight()\n})",
    """const confirmDelete = (chat) => {
  deleteTarget.value = chat
}

const doDelete = async () => {
  if (!deleteTarget.value) return
  try {
    await chatAPI.deleteChat('chat', deleteTarget.value.id)
    chatHistory.value = chatHistory.value.filter(c => c.id !== deleteTarget.value.id)
    if (currentChatId.value === deleteTarget.value.id) {
      currentChatId.value = null
      currentMessages.value = []
    }
  } catch (err) {
    console.error('删除失败:', err)
  }
  deleteTarget.value = null
}

onMounted(() => {
  loadChatHistory()
  adjustTextareaHeight()
})""",
    1,
)
print("Step 5 OK")

# Step 6: Add CSS
css = """

// ===== 删除按钮 =====
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

// ===== 确认弹窗 =====
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

// 暗色模式弹窗样式
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
assert "</style>" in content, "STYLE CLOSE NOT FOUND!"
content = content.replace("</style>", css + "\n</style>", 1)
print("Step 6 OK")

with open("src/main/resources/static/spring-ai-protal/src/views/AIChat.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("ALL DONE")

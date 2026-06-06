import subprocess

result = subprocess.run(["git", "show", "HEAD:src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue"], capture_output=True)
content = result.stdout.decode("utf-8")
lines = content.split("\n")

# Step 1: Find history block and add delete button + content wrapper
history_start = -1
history_end_line = -1
for i, line in enumerate(lines):
    if 'v-for="chat in chatHistory"' in line.strip():
        history_start = i
    if history_start is not None and history_start >= 0 and i > history_start:
        stripped = line.strip()
        if stripped.startswith("<") and stripped.endswith(">") and "</" not in stripped and "/>" not in stripped:
            # Look for simple opening tag
            content_in_line = stripped.replace(">","")
            if not content_in_line.startswith("<") and "<" not in content_in_line[1:]:
                pass
        if stripped == "</div>":
            history_end_line = i
            break

print(f"History block: lines {history_start+1} to {history_end_line+1}")

# Actually, simpler approach - find exact patterns
# History block in template
old_hist_lines = []
in_hist = False
started = False
for i, line in enumerate(lines):
    if 'v-for="chat in chatHistory"' in line.strip():
        started = True
    if started:
        old_hist_lines.append((i, line))
    if started and line.strip() == "</div>" and i > 6:
        # This is the first </div> after v-for (closing the history item div)
        break

# Now old_hist_lines contains the history item template
# Create new version
new_hist_lines = []
for idx, (i, line) in enumerate(old_hist_lines):
    stripped = line.rstrip()
    if '@click="loadChat' in stripped:
        # Change this line - remove click from the outer div
        continue
    elif stripped.strip() == ">":
        # This is the closing > of opening div tag, add content wrapper
        new_hist_lines.append((i, line.rstrip()))
        new_hist_lines.append((i, '            <div class="history-item-content" @click="loadChat(chat.id)">'))
    elif '<ChatBubbleLeftRightIcon' in stripped:
        new_hist_lines.append((i, line))
    elif '<span class="title"' in stripped:
        new_hist_lines.append((i, line))
    elif stripped.strip() == "</div>" and idx == len(old_hist_lines)-1:
        # Last </div> - close content div and add delete button
        new_hist_lines.append((i, '            </div>'))
        new_hist_lines.append((i, '            <button class="delete-btn" @click.stop="confirmDelete(chat)" title="删除会话">'))
        new_hist_lines.append((i, '              <TrashIcon class="icon" />'))
        new_hist_lines.append((i, '            </button>'))
        new_hist_lines.append((i, line))
    else:
        new_hist_lines.append((i, line))

# Apply changes
for (i, new_line) in new_hist_lines:
    if i < len(lines):
        lines[i] = new_line

print("Step 1 OK")

# Step 2: Add modal before </template>
for i in range(len(lines)-1, -1, -1):
    if lines[i].strip() == "</template>":
        modal = """  </div>
  
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
</template>"""
        lines[i] = modal
        break
print("Step 2 OK")

# Step 3: Add TrashIcon import
for i in range(len(lines)):
    if "ComputerDesktopIcon" in lines[i] and "from '@heroicons" in lines[i+1]:
        lines[i] = lines[i].rstrip() + ",\n  TrashIcon"
        break
    if "ComputerDesktopIcon" in lines[i] and i+1 < len(lines) and "TrashIcon" not in lines[i]:
        lines[i] = lines[i].rstrip() + ",\n  TrashIcon"
        break
print("Step 3 OK")

# Step 4: Add deleteTarget ref
for i in range(len(lines)):
    if "const bookingInfo = ref('')" in lines[i]:
        lines[i] = lines[i] + "\nconst deleteTarget = ref(null)"
        break
print("Step 4 OK")

# Step 5: Add methods before onMounted
for i in range(len(lines)):
    if lines[i].strip().startswith("onMounted("):
        methods = """const confirmDelete = (chat) => {
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
    console.error('删除失败:', err)
  }
  deleteTarget.value = null
}

"""
        lines[i] = methods + lines[i]
        break
print("Step 5 OK")

# Step 6: Add CSS before </style>
for i in range(len(lines)-1, -1, -1):
    if lines[i].strip() == "</style>" or lines[i].strip().startswith("</style"):
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
        lines[i] = css + "\n" + lines[i]
        break
print("Step 6 OK")

result = "\n".join(lines)
with open("src/main/resources/static/spring-ai-protal/src/views/CustomerService.vue", "w", encoding="utf-8") as f:
    f.write(result)
print("ALL DONE")

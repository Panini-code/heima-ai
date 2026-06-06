import sys
sys.stdout.reconfigure(encoding="utf-8")

with open("src/main/resources/static/spring-ai-protal/src/views/AIChat.vue", "r", encoding="utf-8") as f:
    content = f.read()

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

old = "</style>"
assert old in content
content = content.replace(old, css + "\n" + old, 1)
print("CSS added OK")

with open("src/main/resources/static/spring-ai-protal/src/views/AIChat.vue", "w", encoding="utf-8") as f:
    f.write(content)
print("DONE")

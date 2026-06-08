<template>
  <div v-if="visibleSteps.length > 0" class="agent-thinking">
    <!-- 思考过程步骤 -->
    <div v-for="(step, index) in visibleSteps" :key="index" class="thinking-step" :class="step.type">
      <!-- 思考（模型推理） -->
      <div v-if="step.type === 'reasoning'" class="step-card step-reasoning">
        <div class="step-header">
          <span class="step-icon">💭</span>
          <span class="step-label">思考分析</span>
          <button class="toggle-btn" @click="toggleCollapse(index)">
            <span v-if="step.collapsed">展开</span>
            <span v-else>收起</span>
          </button>
        </div>
        <div v-show="!step.collapsed" class="step-content">
          <p style="white-space: pre-line;">{{ step.content }}</p>
        </div>
      </div>

      <!-- 工具调用 -->
      <div v-if="step.type === 'tool_call'" class="step-card step-tool-call">
        <div class="step-header">
          <span class="step-icon">🔧</span>
          <span class="step-label">调用工具: {{ step.toolName || '未知工具' }}</span>
          <button class="toggle-btn" @click="toggleCollapse(index)">
            <span v-if="step.collapsed">展开</span>
            <span v-else>收起</span>
          </button>
        </div>
        <div v-show="!step.collapsed" class="step-content">
          <div class="tool-args">
            <span class="arg-label">输入参数:</span>
            <pre class="arg-json">{{ formatJson(step.toolInput) }}</pre>
          </div>
        </div>
      </div>

      <!-- 工具结果 -->
      <div v-if="step.type === 'tool_result'" class="step-card step-tool-result">
        <div class="step-header">
          <span class="step-icon">📋</span>
          <span class="step-label">工具返回: {{ step.toolName || '未知工具' }}</span>
          <button class="toggle-btn" @click="toggleCollapse(index)">
            <span v-if="step.collapsed">展开</span>
            <span v-else>收起</span>
          </button>
        </div>
        <div v-show="!step.collapsed" class="step-content">
          <p style="white-space: pre-line;">{{ step.toolOutput || step.content }}</p>
        </div>
      </div>

      <!-- 错误 -->
      <div v-if="step.type === 'error'" class="step-card step-error">
        <div class="step-header">
          <span class="step-icon">❌</span>
          <span class="step-label">处理出错</span>
        </div>
        <div class="step-content">
          <p>{{ step.content }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => []
  }
})

// 为每个步骤添加折叠状态
const collapsedState = ref({})

const visibleSteps = computed(() => {
  // 只保留需要展示的步骤类型（过滤掉 answer，answer 在主消息区展示）
  return props.steps.filter(s => s.type !== 'answer').map((s, i) => ({
    ...s,
    collapsed: collapsedState.value[i] !== undefined ? collapsedState.value[i] : false
  }))
})

function toggleCollapse(index) {
  collapsedState.value[index] = !collapsedState.value[index]
}

function formatJson(str) {
  if (!str) return ''
  try {
    const obj = JSON.parse(str)
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}
</script>

<style scoped lang="scss">
.agent-thinking {
  margin: 0.5rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.step-card {
  border-radius: 10px;
  font-size: 0.85rem;
  border: 1px solid transparent;
  overflow: hidden;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 0.75rem;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}

.step-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.step-label {
  font-size: 0.85rem;
  flex: 1;
}

.toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.75rem;
  color: inherit;
  opacity: 0.6;
  padding: 2px 6px;
  border-radius: 4px;
  &:hover {
    opacity: 1;
    background: rgba(0,0,0,0.05);
  }
}

.step-content {
  padding: 0 0.75rem 0.5rem;
  line-height: 1.5;
  p { margin: 0.25rem 0; }
}

// 思考
.step-reasoning {
  background: rgba(100, 100, 100, 0.06);
  border-left: 3px solid #999;
  .step-header { color: #777; }
  .step-label { color: #999; }
  .step-content { color: #666; }
}

// 工具调用
.step-tool-call {
  background: rgba(33, 150, 243, 0.06);
  border-left: 3px solid #2196F3;
  .step-header { color: #1565C0; }
  .step-label { color: #1976D2; }
  .step-content { color: #444; }
}

.tool-args {
  margin-top: 0.25rem;
}

.arg-label {
  font-size: 0.8rem;
  color: #666;
  display: block;
  margin-bottom: 0.25rem;
}

.arg-json {
  background: rgba(0,0,0,0.04);
  border-radius: 4px;
  padding: 0.5rem;
  font-size: 0.8rem;
  overflow-x: auto;
  white-space: pre-wrap;
  margin: 0;
}

// 工具结果
.step-tool-result {
  background: rgba(76, 175, 80, 0.06);
  border-left: 3px solid #4CAF50;
  .step-header { color: #2E7D32; }
  .step-label { color: #388E3C; }
  .step-content { color: #444; }
  p { white-space: pre-wrap; }
}

// 错误
.step-error {
  background: rgba(244, 67, 54, 0.06);
  border-left: 3px solid #F44336;
  .step-header { color: #C62828; }
  .step-label { color: #D32F2F; }
  .step-content { color: #C62828; }
}

// 暗色模式
.dark {
  .step-reasoning {
    background: rgba(255, 255, 255, 0.03);
    .step-content { color: #bbb; }
    .step-header { color: #999; }
    .step-label { color: #aaa; }
  }
  .step-tool-call {
    background: rgba(33, 150, 243, 0.08);
    .step-content { color: #ccc; }
    .step-header { color: #64B5F6; }
    .step-label { color: #64B5F6; }
  }
  .step-tool-result {
    background: rgba(76, 175, 80, 0.08);
    .step-content { color: #ccc; }
    .step-header { color: #81C784; }
    .step-label { color: #81C784; }
  }
  .step-error {
    background: rgba(244, 67, 54, 0.08);
    .step-content { color: #E57373; }
    .step-header { color: #E57373; }
    .step-label { color: #E57373; }
  }
  .arg-json {
    background: rgba(255,255,255,0.06);
    color: #ccc;
  }
}
</style>

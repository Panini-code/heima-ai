<template>
  <div class="pdf-view">
    <!-- 文件标签栏 -->
    <div class="file-tabs" v-if="files && files.length > 0">
      <div
        v-for="f in files"
        :key="f"
        class="file-tab"
        :class="{ active: f === activeFile }"
        @click="$emit('fileSelect', f)"
      >
        <DocumentTextIcon class="tab-icon" />
        <span class="tab-name">{{ f }}</span>
        <button
          class="tab-close"
          @click.stop="confirmDelete(f)"
          title="删除此文件"
        >
          &times;
        </button>
      </div>
      <!-- 追加文件按钮 -->
      <button class="add-file-btn" @click="$emit('addFile')" title="上传更多文件">
        <PlusIcon class="add-icon" />
      </button>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal-card">
        <div class="modal-header">
          <TrashIcon class="modal-icon" />
          <span>确认删除</span>
        </div>
        <p class="modal-body">确定要删除「{{ deleteTarget }}」吗？<br/>该文件的所有向量数据将被同步清除。</p>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="deleteTarget = null">取消</button>
          <button class="btn btn-danger" @click="doDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- PDF 预览区域 -->
    <div class="pdf-content">
      <div v-if="isLoading" class="pdf-loading">
        <div class="loading-spinner"></div>
        <p class="loading-text">正在加载 PDF...</p>
      </div>
      <div class="pdf-container" ref="viewerRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { DocumentTextIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { useDark } from '@vueuse/core'

const isDark = useDark()
const props = defineProps({
  file: { type: [File, null], default: null },
  fileName: { type: String, default: '' },
  files: { type: Array, default: () => [] },
  activeFile: { type: String, default: '' }
})

const emit = defineEmits(['fileSelect', 'fileDelete', 'addFile'])

const isLoading = ref(false)
const viewerRef = ref(null)
const deleteTarget = ref(null)
let instance = null

const confirmDelete = (filename) => {
  deleteTarget.value = filename
}

const doDelete = () => {
  if (deleteTarget.value) {
    emit('fileDelete', deleteTarget.value)
  }
  deleteTarget.value = null
}

const renderPdf = (file) => {
  if (!file || !viewerRef.value) return
  try {
    isLoading.value = true
    if (instance?.url) {
      URL.revokeObjectURL(instance.url)
    }
    const iframe = document.createElement('iframe')
    iframe.style.width = '100%'
    iframe.style.height = '100%'
    iframe.style.border = 'none'
    if (isDark.value) {
      iframe.style.backgroundColor = '#1a1a1a'
    }
    const url = URL.createObjectURL(file)
    iframe.src = url
    viewerRef.value.innerHTML = ''
    viewerRef.value.appendChild(iframe)
    iframe.onload = () => { isLoading.value = false }
    instance = { url, iframe }
  } catch (error) {
    console.error('渲染 PDF 失败:', error)
    isLoading.value = false
  }
}

onMounted(() => {
  if (props.file) renderPdf(props.file)
})

watch(() => props.file, (newFile) => {
  if (newFile) renderPdf(newFile)
})

onUnmounted(() => {
  if (instance?.url) URL.revokeObjectURL(instance.url)
})
</script>

<style scoped lang="scss">
.pdf-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  min-width: 0;

  // ===== 文件标签栏 =====
  .file-tabs {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.8rem;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    background: rgba(0, 0, 0, 0.02);
    overflow-x: auto;
    flex-shrink: 0;

    &::-webkit-scrollbar { height: 3px; }
    &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.15); border-radius: 2px; }

    .file-tab {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.35rem 0.6rem;
      border-radius: 6px;
      font-size: 0.8rem;
      cursor: pointer;
      white-space: nowrap;
      border: 1px solid transparent;
      transition: all 0.2s;
      color: #555;

      .tab-icon { width: 1rem; height: 1rem; flex-shrink: 0; color: #999; }
      .tab-name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; }
      .tab-close {
        margin-left: 0.15rem;
        background: none;
        border: none;
        font-size: 1.1rem;
        line-height: 1;
        color: #aaa;
        cursor: pointer;
        padding: 0 0.15rem;
        border-radius: 3px;
        transition: all 0.15s;
        &:hover { color: #e74c3c; background: rgba(231, 76, 60, 0.1); }
      }

      &:hover { background: rgba(0, 0, 0, 0.04); border-color: rgba(0, 0, 0, 0.06); }
      &.active {
        background: #fff;
        border-color: #007CF0;
        color: #007CF0;
        .tab-icon { color: #007CF0; }
      }
    }

    .add-file-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 1.8rem;
      height: 1.8rem;
      border-radius: 6px;
      border: 1px dashed rgba(0, 0, 0, 0.2);
      background: none;
      cursor: pointer;
      flex-shrink: 0;
      transition: all 0.2s;
      .add-icon { width: 1rem; height: 1rem; color: #999; }
      &:hover { border-color: #007CF0; .add-icon { color: #007CF0; } }
    }
  }

  // ===== 删除确认弹窗 =====
  .modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }
  .modal-card {
    background: #fff;
    border-radius: 12px;
    padding: 1.5rem 2rem;
    min-width: 340px;
    box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  }
  .modal-header {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    font-weight: 600;
    font-size: 1rem;
    margin-bottom: 1rem;
    .modal-icon { width: 1.3rem; height: 1.3rem; color: #e74c3c; }
  }
  .modal-body {
    color: #666;
    font-size: 0.9rem;
    line-height: 1.6;
    margin-bottom: 1.2rem;
  }
  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.6rem;
    .btn {
      padding: 0.45rem 1.2rem;
      border-radius: 6px;
      border: none;
      cursor: pointer;
      font-size: 0.85rem;
      transition: all 0.15s;
    }
    .btn-cancel { background: #f0f0f0; color: #555; &:hover { background: #e0e0e0; } }
    .btn-danger { background: #e74c3c; color: #fff; &:hover { background: #c0392b; } }
  }

  // ===== PDF 内容区域 =====
  .pdf-content {
    flex: 1;
    position: relative;
    overflow: hidden;
    .pdf-container { width: 100%; height: 100%; }
    .pdf-loading {
      position: absolute;
      top: 50%; left: 50%;
      transform: translate(-50%, -50%);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      background: rgba(255,255,255,0.9);
      padding: 2rem;
      border-radius: 1rem;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
      z-index: 2;
      .loading-spinner {
        width: 48px; height: 48px;
        border: 4px solid rgba(0,124,240,0.1);
        border-left-color: #007CF0;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
      .loading-text { color: #666; font-size: 1rem; font-weight: 500; }
    }
  }
}

// === 暗色模式 ===
.dark .pdf-view {
  background: #1a1a1a;
  border-right-color: rgba(255,255,255,0.1);

  .file-tabs {
    background: rgba(255,255,255,0.03);
    border-bottom-color: rgba(255,255,255,0.08);
    .file-tab {
      color: #aaa;
      .tab-icon { color: #777; }
      .tab-close { color: #666; &:hover { color: #e74c3c; background: rgba(231,76,60,0.2); } }
      &:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.08); }
      &.active { background: rgba(255,255,255,0.08); border-color: #007CF0; color: #007CF0; .tab-icon { color: #007CF0; } }
    }
    .add-file-btn {
      border-color: rgba(255,255,255,0.15);
      .add-icon { color: #777; }
      &:hover { border-color: #007CF0; .add-icon { color: #007CF0; } }
    }
  }

  .modal-card { background: #2a2a2a; }
  .modal-header { color: #eee; }
  .modal-body { color: #aaa; }
  .modal-footer .btn-cancel { background: #444; color: #ccc; &:hover { background: #555; } }

  .pdf-content .pdf-loading {
    background: rgba(30,30,30,0.9);
    .loading-text { color: #999; }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
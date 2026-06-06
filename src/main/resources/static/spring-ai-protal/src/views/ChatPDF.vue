<template>
  <div class="chat-pdf" :class="{ 'dark': isDark }">
    <div class="chat-container">
      <!-- 左侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <a href="#" class="logo-link" @click="handleLogoClick">
            <DocumentTextIcon class="logo" />
            <h1 class="title">ChatPDF</h1>
          </a>
        </div>

        <div class="history-list">
          <div class="history-header">
            <span>历史记录</span>
            <button class="new-chat-btn" @click="startNewChat">
              <PlusIcon class="icon" />
              新聊天
            </button>
          </div>
          <div 
            v-for="chat in chatHistory" 
            :key="chat.id"
            class="history-item"
            :class="{ 'active': currentChatId === chat.id }"
            @click="loadChat(chat.id)"
          >
            <DocumentTextIcon class="icon" />
            <span class="title">{{ chat.title || 'PDF对话' }}</span>
          </div>
        </div>
      </div>
      
      <!-- 主要内容区域 -->
      <div class="chat-main">
        <!-- 未上传文件时显示上传界面 -->
        <div v-if="!currentChatId && !isUploading" class="upload-welcome">
          <h1 class="main-title">
            与任何 <span class="highlight">PDF</span> 对话
          </h1>
          <div 
            class="drop-zone"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="handleDrop"
            :class="{ 'dragging': isDragging }"
          >
            <div class="upload-content">
              <DocumentArrowUpIcon class="upload-icon" />
              <p class="upload-text">点击上传，或将PDF拖拽到此处</p>
              <input 
                type="file"
                accept=".pdf"
                @change="handleFileUpload"
                :disabled="isUploading"
                class="file-input"
                multiple
              >
              <button class="upload-button" @click="triggerFileInput">
                <ArrowUpTrayIcon class="icon" />
                上传PDF
              </button>
            </div>
          </div>
        </div>

        <!-- 已上传文件时显示分栏界面 -->
        <div v-else class="split-view">
          <!-- PDF 文件区域 -->
          <PDFViewer 
            :file="pdfFile"
            :fileName="activeFileName"
            :files="fileList"
            :activeFile="activeFileName"
            @fileSelect="switchFile"
            @fileDelete="handleFileDelete"
            @addFile="triggerFileInput"
          />

          <!-- 聊天区域 -->
          <div class="chat-view">
            <!-- 上传进度提示 -->
            <div v-if="isUploading" class="upload-banner">
              <div class="upload-spinner"></div>
              <span>{{ uploadingFileName ? '正在上传: ' + uploadingFileName : '正在上传...' }}</span>
            </div>

            <!-- 成功/失败提示 -->
            <div v-if="uploadMessage" class="upload-banner" :class="uploadMessageType">
              <span>{{ uploadMessage }}</span>
              <button class="banner-close" @click="uploadMessage = ''">&times;</button>
            </div>

            <div class="messages" ref="messagesRef">
              <ChatMessage
                v-for="(message, index) in currentMessages"
                :key="index"
                :message="message"
                :is-stream="isStreaming && index === currentMessages.length - 1"
              />
              <!-- 无文件提示 -->
              <div v-if="fileList.length === 0 && currentChatId" class="no-file-hint">
                <p>当前会话没有关联的 PDF 文件</p>
                <button class="upload-again-btn" @click="triggerFileInput">上传文件</button>
              </div>
            </div>
            
            <div class="input-area">
              <textarea
                v-model="userInput"
                @keydown.enter.prevent="sendMessage()"
                placeholder="请输入您的问题..."
                rows="1"
                ref="inputRef"
                :disabled="fileList.length === 0"
              ></textarea>
              <button 
                class="send-button" 
                @click="sendMessage()"
                :disabled="isStreaming || !userInput.trim() || fileList.length === 0"
              >
                <PaperAirplaneIcon class="icon" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useDark } from '@vueuse/core'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { 
  DocumentArrowUpIcon,
  DocumentTextIcon,
  PaperAirplaneIcon,
  ArrowUpTrayIcon,
  PlusIcon
} from '@heroicons/vue/24/outline'
import ChatMessage from '../components/ChatMessage.vue'
import { chatAPI } from '../services/api'
import { useRouter } from 'vue-router'
import PDFViewer from '../components/PDFViewer.vue'

const isDark = useDark()
const router = useRouter()
const messagesRef = ref(null)
const inputRef = ref(null)
const userInput = ref('')
const isStreaming = ref(false)
const isUploading = ref(false)
const uploadingFileName = ref('')
const currentChatId = ref(null)
const chatHistory = ref([])
const currentPdfName = ref('')
const currentMessages = ref([])
const isDragging = ref(false)
const isDownloadingPdf = ref(false)
const pdfFile = ref(null)
const uploadMessage = ref('')
const uploadMessageType = ref('success')

// ===== 多文件管理新增状态 =====
const fileList = ref([])
const activeFileName = ref('')

const BASE_URL = 'http://localhost:8080'

marked.setOptions({ breaks: true, gfm: true, sanitize: false })

const adjustTextareaHeight = () => {
  const textarea = inputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = textarea.scrollHeight + 'px'
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const cleanupResources = () => {
  currentPdfName.value = ''
  currentMessages.value = []
  pdfFile.value = null
  currentChatId.value = null
  isDownloadingPdf.value = false
  isUploading.value = false
  uploadingFileName.value = ''
  userInput.value = ''
  isStreaming.value = false
  fileList.value = []
  activeFileName.value = ''
  uploadMessage.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
}

const handleLogoClick = (event) => {
  event.preventDefault()
  cleanupResources()
  router.push('/')
}

const startNewChat = () => {
  cleanupResources()
  pdfFile.value = null
  currentPdfName.value = ''
  currentChatId.value = null
  currentMessages.value = []
  isUploading.value = false
  uploadingFileName.value = ''
  fileList.value = []
  activeFileName.value = ''
  userInput.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
  if (messagesRef.value) {
    messagesRef.value.scrollTop = 0
  }
}

// ===== 文件列表管理 =====
const refreshFileList = async (chatId) => {
  const files = await chatAPI.getPdfFiles(chatId)
  fileList.value = files
  // 如果没有选中的文件或选中的文件已被删除，默认选中第一个
  if (files.length > 0) {
    if (!activeFileName.value || !files.includes(activeFileName.value)) {
      activeFileName.value = files[0]
    }
  }
}

const switchFile = async (filename) => {
  if (filename === activeFileName.value) return
  activeFileName.value = filename
  // 从服务器获取该文件的 PDF blob
  isDownloadingPdf.value = true
  try {
    const response = await fetch(`${BASE_URL}/ai/pdf/file/${currentChatId.value}?fileName=${encodeURIComponent(filename)}`)
    if (!response.ok) throw new Error('获取 PDF 失败')
    const blob = await response.blob()
    // 用当前文件名创建 File 对象
    pdfFile.value = new File([blob], filename, { type: 'application/pdf' })
  } catch (error) {
    console.error('加载 PDF 失败:', error)
  } finally {
    isDownloadingPdf.value = false
  }
}

const handleFileDelete = async (filename) => {
  if (!currentChatId.value) return
  const ok = await chatAPI.deletePdfFile(currentChatId.value, filename)
  if (ok) {
    showUploadMessage('文件已删除', 'success')
    await refreshFileList(currentChatId.value)
    // 如果当前查看的文件被删了，切换到第一个
    if (activeFileName.value !== fileList.value[0] || !fileList.value.includes(activeFileName.value)) {
      if (fileList.value.length > 0) {
        await switchFile(fileList.value[0])
      } else {
        // 所有文件都删了
        activeFileName.value = ''
        pdfFile.value = null
        currentChatId.value = null
      }
    }
  } else {
    showUploadMessage('删除失败，请重试', 'error')
  }
}

const showUploadMessage = (msg, type = 'success') => {
  uploadMessage.value = msg
  uploadMessageType.value = type
  setTimeout(() => { uploadMessage.value = '' }, 3000)
}

// ===== 上传逻辑 =====
const uploadFile = async (file) => {
  if (file.type !== 'application/pdf') {
    showUploadMessage('只能上传 PDF 文件', 'error')
    return
  }
  isUploading.value = true
  uploadingFileName.value = file.name
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadChatId = currentChatId.value || `pdf_${Date.now()}`
    const response = await fetch(`${BASE_URL}/ai/pdf/upload/${uploadChatId}`, {
      method: 'POST',
      body: formData
    })
    if (!response.ok) throw new Error(`上传失败: ${response.status}`)
    await response.json()

    currentChatId.value = uploadChatId

    // 刷新文件列表
    await refreshFileList(currentChatId.value)

    // 显示成功提示（首次上传时显示欢迎消息）
    if (!currentPdfName.value) {
      currentPdfName.value = file.name
      currentMessages.value.push({
        role: 'assistant',
        content: `已上传 PDF 文件: ${file.name}。您可以开始提问了。`,
        timestamp: new Date(),
        isMarkdown: true
      })
    } else {
      showUploadMessage(`${file.name} 上传成功`, 'success')
    }

    // 如果当前没有 PDF 显示，切换到新文件
    if (!pdfFile.value) {
      pdfFile.value = file
      activeFileName.value = file.name
    }

    // 添加到聊天历史
    const newChat = { id: currentChatId.value, title: `PDF对话: ${file.name.slice(0, 20)}${file.name.length > 20 ? '...' : ''}` }
    if (!chatHistory.value.some(chat => chat.id === currentChatId.value)) {
      chatHistory.value = [newChat, ...chatHistory.value]
    }
  } catch (error) {
    console.error('上传失败:', error)
    showUploadMessage('文件上传失败，请重试', 'error')
  } finally {
    isUploading.value = false
    uploadingFileName.value = ''
  }
}

const handleDrop = async (event) => {
  isDragging.value = false
  const files = event.dataTransfer.files
  if (files.length === 0) return
  // 上传所有拖拽的文件
  for (const file of files) {
    await uploadFile(file)
  }
}

const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  for (const file of files) {
    await uploadFile(file)
  }
  event.target.value = ''
}

const handleDragOver = () => { isDragging.value = true }
const handleDragLeave = () => { isDragging.value = false }
const triggerFileInput = () => { document.querySelector('.file-input').click() }

// ===== 聊天逻辑 =====
const loadChat = async (chatId) => {
  if (!chatId) return
  cleanupResources()
  currentChatId.value = chatId
  try {
    const messages = await chatAPI.getChatMessages(chatId, 'pdf')
    currentMessages.value = messages.map(msg => ({ ...msg, isMarkdown: msg.role === 'assistant' }))

    // 加载文件列表
    await refreshFileList(chatId)

    if (fileList.value.length > 0) {
      activeFileName.value = fileList.value[0]
      isDownloadingPdf.value = true
      const response = await fetch(`${BASE_URL}/ai/pdf/file/${chatId}?fileName=${encodeURIComponent(fileList.value[0])}`)
      if (!response.ok) throw new Error('获取 PDF 失败')
      const blob = await response.blob()
      currentPdfName.value = fileList.value[0]
      const chatIndex = chatHistory.value.findIndex(c => c.id === chatId)
      if (chatIndex !== -1) {
        chatHistory.value[chatIndex].title = fileList.value[0]
      }
      pdfFile.value = new File([blob], fileList.value[0], { type: 'application/pdf' })
    }
  } catch (error) {
    console.error('加载失败:', error)
    currentMessages.value.push({ role: 'assistant', content: '加载失败，请重试。', timestamp: new Date(), isMarkdown: true })
  } finally {
    isDownloadingPdf.value = false
  }
}

const loadChatHistory = async () => {
  try {
    const history = await chatAPI.getChatHistory('pdf')
    chatHistory.value = history || []
    if (history && history.length > 0) {
      await loadChat(history[0].id)
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
    chatHistory.value = []
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isStreaming.value || fileList.value.length === 0) return
  const userMessage = { role: 'user', content: userInput.value, timestamp: new Date() }
  currentMessages.value.push(userMessage)
  const input = userInput.value
  userInput.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
  await scrollToBottom()

  const assistantMessageIndex = currentMessages.value.length
  currentMessages.value.push({ role: 'assistant', content: '', timestamp: new Date(), isMarkdown: true })
  isStreaming.value = true
  try {
    const reader = await chatAPI.sendPdfMessage(input, currentChatId.value, activeFileName.value)
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      currentMessages.value[assistantMessageIndex].content = DOMPurify.sanitize(marked.parse(buffer))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    if (currentMessages.value[assistantMessageIndex].content === '') {
      currentMessages.value[assistantMessageIndex].content = '抱歉，发送消息失败，请重试。'
    }
  } finally {
    isStreaming.value = false
    await scrollToBottom()
  }
}

onMounted(() => {
  loadChatHistory()
})
</script>

<style scoped lang="scss">
.chat-pdf {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.chat-container {
  width: 100%;
  height: 100vh;
  display: flex;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
}

// ===== 左侧边栏 =====
.sidebar {
  width: 260px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
}

.sidebar-header {
  padding: 1.2rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);

  .logo-link {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    text-decoration: none;
    color: inherit;

    .logo {
      width: 1.8rem;
      height: 1.8rem;
      color: #007CF0;
    }

    .title {
      font-size: 1.2rem;
      font-weight: 700;
      color: #1a1a1a;
    }
  }
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.8rem;

  .history-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.8rem;
    padding: 0 0.4rem;
    font-size: 0.75rem;
    font-weight: 500;
    color: #999;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    .new-chat-btn {
      display: flex;
      align-items: center;
      gap: 0.3rem;
      background: none;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 6px;
      padding: 0.25rem 0.6rem;
      font-size: 0.75rem;
      cursor: pointer;
      transition: all 0.2s;
      color: #555;

      .icon { width: 0.8rem; height: 0.8rem; }
      &:hover { background: #007CF0; color: #fff; border-color: #007CF0; }
    }
  }

  .history-item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.6rem 0.8rem;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    margin-bottom: 0.2rem;

    .icon { width: 1rem; height: 1rem; color: #999; flex-shrink: 0; }
    .title { font-size: 0.85rem; color: #444; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

    &:hover { background: rgba(0, 0, 0, 0.04); }
    &.active { background: rgba(0, 124, 240, 0.08); .icon, .title { color: #007CF0; } }
  }
}

// ===== 主区域 =====
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

// ===== 初始上传界面 =====
.upload-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  padding: 2rem;

  .main-title {
    font-size: 2.2rem;
    font-weight: 700;
    color: #1a1a1a;

    .highlight { color: #007CF0; background: linear-gradient(135deg, #007CF0, #00b4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  }
}

.drop-zone {
  width: 100%;
  max-width: 420px;
  padding: 3rem 2rem;
  border: 2px dashed rgba(0, 0, 0, 0.15);
  border-radius: 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.8);

  &:hover, &.dragging { border-color: #007CF0; background: rgba(0, 124, 240, 0.04); transform: translateY(-2px); }

  .upload-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;

    .upload-icon { width: 3rem; height: 3rem; color: #999; }
    .upload-text { color: #888; font-size: 0.9rem; }
    .file-input { display: none; }
    .upload-button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.7rem 1.5rem;
      background: #007CF0;
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      cursor: pointer;
      transition: all 0.2s;
      .icon { width: 1rem; height: 1rem; }
      &:hover { background: #005bb5; transform: translateY(-1px); }
    }
  }
}

// ===== 分栏视图 =====
.split-view {
  flex: 1;
  display: flex;
  height: 100vh;

  .chat-view {
    flex: 1;
    min-width: 380px;
    max-width: 50%;
    display: flex;
    flex-direction: column;
    background: #fff;
    position: relative;
  }
}

// ===== 上传进度条 =====
.upload-banner {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 1rem;
  font-size: 0.85rem;
  color: #555;
  background: #f0f7ff;
  border-bottom: 1px solid rgba(0,124,240,0.1);

  &.success { background: #f0fff4; color: #2e7d32; border-bottom-color: rgba(46,125,50,0.1); }
  &.error { background: #fff0f0; color: #c62828; border-bottom-color: rgba(198,40,40,0.1); }

  .upload-spinner {
    width: 16px; height: 16px;
    border: 2px solid rgba(0,124,240,0.2);
    border-left-color: #007CF0;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
  .banner-close {
    margin-left: auto;
    background: none;
    border: none;
    font-size: 1.2rem;
    cursor: pointer;
    color: inherit;
    opacity: 0.5;
    &:hover { opacity: 1; }
  }
}

// ===== 消息区域 =====
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.no-file-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
  padding: 3rem 1rem;
  color: #999;
  font-size: 0.9rem;

  .upload-again-btn {
    padding: 0.5rem 1.2rem;
    background: #007CF0;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.85rem;
    transition: all 0.2s;
    &:hover { background: #005bb5; }
  }
}

// ===== 输入区域 =====
.input-area {
  flex-shrink: 0;
  padding: 1.5rem 2rem;
  background: rgba(255, 255, 255, 0.98);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  
  textarea {
    flex: 1;
    resize: none;
    border: 1px solid rgba(0, 0, 0, 0.1);
    background: white;
    border-radius: 0.75rem;
    padding: 1rem;
    color: inherit;
    font-family: inherit;
    font-size: 1rem;
    line-height: 1.5;
    max-height: 150px;
    
    &:focus { outline: none; border-color: #333; box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1); }
    &:disabled { background: #f5f5f5; cursor: not-allowed; }
  }
  
  .send-button {
    background: #333;
    color: white;
    border: none;
    border-radius: 0.5rem;
    width: 2.5rem;
    height: 2.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color 0.3s;
    
    &:hover:not(:disabled) { background: #000; }
    &:disabled { background: #ccc; cursor: not-allowed; }
    .icon { width: 1.25rem; height: 1.25rem; }
  }
}

// ===== 暗色模式 =====
.dark {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);

  .chat-container { background: rgba(0, 0, 0, 0.4); }

  .sidebar {
    background: rgba(30, 30, 30, 0.8);
    border-right-color: rgba(255, 255, 255, 0.08);
  }

  .sidebar-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);
    .logo-link .title { color: #eee; }
  }

  .history-list .history-item {
    .title { color: #aaa; }
    &:hover { background: rgba(255, 255, 255, 0.06); }
    &.active { background: rgba(0, 124, 240, 0.15); .icon, .title { color: #007CF0; } }
  }

  .history-list .history-header .new-chat-btn {
    color: #aaa;
    border-color: rgba(255,255,255,0.15);
    &:hover { background: #007CF0; color: #fff; }
  }

  .upload-welcome .main-title { color: #eee; }

  .drop-zone {
    border-color: rgba(255,255,255,0.15);
    background: rgba(255,255,255,0.05);
    &:hover, &.dragging { border-color: #007CF0; }
    .upload-text { color: #aaa; }
  }

  .chat-view { background: #1a1a1a; }
  .input-area { background: rgba(30, 30, 30, 0.98); border-top-color: rgba(255,255,255,0.05); }
  .messages { background: #1a1a1a; }

  .upload-banner {
    background: rgba(0,124,240,0.1);
    color: #ccc;
    &.success { background: rgba(46,125,50,0.15); color: #81c784; }
    &.error { background: rgba(198,40,40,0.15); color: #ef9a9a; }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>


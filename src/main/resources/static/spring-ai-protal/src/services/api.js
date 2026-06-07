const BASE_URL = 'http://localhost:8080'

export const chatAPI = {
  // 发送聊天消息
  async sendMessage(data, chatId) {
    try {
      const url = new URL(`${BASE_URL}/ai/chat`)
      if (chatId) {
        url.searchParams.append('chatId', chatId)
      }
      
      const response = await fetch(url, {
        method: 'POST',
        body: data instanceof FormData ? data : 
          new URLSearchParams({ prompt: data })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      return response.body.getReader()
    } catch (error) {
      console.error('API Error:', error)
      throw error
    }
  },

  // 获取聊天历史列表
  async getChatHistory(type = 'chat') {
    try {
      const response = await fetch(`${BASE_URL}/ai/history/${type}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const chatIds = await response.json()
      return chatIds.map(id => ({
        id,
        title: type === 'pdf' ? `PDF对话 ${id.slice(-6)}` : 
               type === 'service' ? `咨询 ${id.slice(-6)}` :
               `对话 ${id.slice(-6)}`
      }))
    } catch (error) {
      console.error('API Error:', error)
      return []
    }
  },

  // 获取特定对话的消息历史
  async getChatMessages(chatId, type = 'chat') {
    try {
      const response = await fetch(`${BASE_URL}/ai/history/${type}/${chatId}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const messages = await response.json()
      return messages.map(msg => ({
        ...msg,
        timestamp: new Date()
      }))
    } catch (error) {
      console.error('API Error:', error)
      return []
    }
  },

  // 发送游戏消息
  async sendGameMessage(prompt, chatId) {
    try {
      const response = await fetch(`${BASE_URL}/ai/game?prompt=${encodeURIComponent(prompt)}&chatId=${chatId}`, {
        method: 'GET',
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      return response.body.getReader()
    } catch (error) {
      console.error('API Error:', error)
      throw error
    }
  },

  // 发送客服消息
  async sendServiceMessage(prompt, chatId) {
    try {
      const response = await fetch(`${BASE_URL}/ai/service?prompt=${encodeURIComponent(prompt)}&chatId=${chatId}`, {
        method: 'GET',
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      return response.body.getReader()
    } catch (error) {
      console.error('API Error:', error)
      throw error
    }
  },

  // 发送 PDF 问答消息（支持多文件，指定 fileName）
  async sendPdfMessage(prompt, chatId, fileName) {
    try {
      let url = `${BASE_URL}/ai/pdf/chat?prompt=${encodeURIComponent(prompt)}&chatId=${chatId}`
      if (fileName) {
        url += `&fileName=${encodeURIComponent(fileName)}`
      }
      const response = await fetch(url, {
        method: 'GET',
        signal: AbortSignal.timeout(30000)
      })

      if (!response.ok) {
        throw new Error(`API error: ${response.status}`)
      }

      return response.body.getReader()
    } catch (error) {
      console.error('API Error:', error)
      throw error
    }
  },

  // ===== 多文件管理接口 =====

  // 获取会话的文件列表
  async getPdfFiles(chatId) {
    try {
      const response = await fetch(`${BASE_URL}/ai/pdf/files/${chatId}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const result = await response.json()
      return result.data || []
    } catch (error) {
      console.error('获取文件列表失败:', error)
      return []
    }
  },

  // 删除指定文件
  async deletePdfFile(chatId, filename) {
    try {
      const response = await fetch(
        `${BASE_URL}/ai/pdf/file/${chatId}?filename=${encodeURIComponent(filename)}`,
        { method: 'DELETE' }
      )
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const result = await response.json()
      return result.ok === 1
    } catch (error) {
      console.error('删除文件失败:', error)
      return false
    }
  },
  // 删除指定会话
  async deleteChat(type, chatId) {
    try {
      const response = await fetch(`${BASE_URL}/ai/history/${type}/${chatId}`, {
        method: 'DELETE'
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return true
    } catch (error) {
      console.error('删除会话失败:', error)
      return false
    }
  },

  // 发送 Agent PDF 消息（深度分析模式）
  async sendAgentPdfMessage(prompt, chatId) {
    try {
      let url = `${BASE_URL}/ai/pdf/agent/chat?prompt=${encodeURIComponent(prompt)}&chatId=${chatId}`
      const response = await fetch(url, {
        method: 'GET',
        signal: AbortSignal.timeout(60000)
      })
      if (!response.ok) {
        throw new Error(`API error: ${response.status}`)
      }
      return response.body.getReader()
    } catch (error) {
      console.error('Agent PDF API Error:', error)
      throw error
    }
  }
}
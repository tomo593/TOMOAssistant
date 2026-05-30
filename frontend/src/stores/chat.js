import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const currentConversationId = ref(null)
  const currentKnowledgeBaseId = ref(null)
  const isStreaming = ref(false)
  const conversations = ref([])

  function addMessage(message) {
    messages.value.push(message)
  }

  function updateLastMessage(content) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content += content
    }
  }

  function setConversation(id) {
    currentConversationId.value = id
  }

  function setKnowledgeBase(id) {
    currentKnowledgeBaseId.value = id
  }

  function clearMessages() {
    messages.value = []
  }

  function loadMessages(msgs) {
    messages.value = msgs
  }

  function updateConversationTitle(id, title) {
    const conv = conversations.value.find(c => c.id === id)
    if (conv) {
      conv.title = title
    }
  }

  return {
    messages,
    currentConversationId,
    currentKnowledgeBaseId,
    isStreaming,
    conversations,
    addMessage,
    updateLastMessage,
    setConversation,
    setKnowledgeBase,
    clearMessages,
    loadMessages,
    updateConversationTitle
  }
})

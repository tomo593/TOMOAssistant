<template>
  <div class="chat-view">
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">🐷</div>
        <h3>欢迎使用 TOMO Assistant</h3>
        <p>上传文档到知识库，然后在这里提问。</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper" :class="msg.role">
        <MessageBubble :message="msg" />
        <div v-if="isStreaming && index === messages.length - 1 && msg.role === 'assistant'" class="streaming-indicator">
          <div v-if="statusText" class="status-text">{{ statusText }}</div>
          <div v-else class="dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <ChatInput @send="handleSend" :disabled="isStreaming" />
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from 'vue'
import { useChatStore } from '../stores/chat'
import { storeToRefs } from 'pinia'
import { streamChat } from '../api/chat'
import { getMessages } from '../api/conversation'
import MessageBubble from '../components/chat/MessageBubble.vue'
import ChatInput from '../components/chat/ChatInput.vue'

const chatStore = useChatStore()
const { messages, currentConversationId, currentKnowledgeBaseId } = storeToRefs(chatStore)
const messagesContainer = ref(null)
const isStreaming = ref(false)
const statusText = ref('')

onMounted(async () => {
  if (currentConversationId.value) {
    await loadMessages(currentConversationId.value)
  }
})

watch(currentConversationId, async (id) => {
  if (id) {
    await loadMessages(id)
  }
})

async function loadMessages(convId) {
  try {
    const res = await getMessages(convId)
    chatStore.loadMessages((res.data || []).map(m => ({
      role: m.role.toLowerCase(),
      content: m.content,
      citations: m.citations || [],
      createdAt: m.createdAt,
      imageData: m.imageData || null,
      imageName: m.imageName || null
    })))
  } catch (e) {
    console.error('Failed to load messages', e)
  }
}

async function handleSend({ text, imageData, imageName }) {
  if (!text.trim() && !imageData) return
  if (isStreaming.value) return

  // Add user message
  chatStore.addMessage({
    role: 'user',
    content: text,
    citations: [],
    imageData: imageData || null,
    imageName: imageName || null
  })

  // Add empty assistant message
  chatStore.addMessage({
    role: 'assistant',
    content: '',
    citations: []
  })

  isStreaming.value = true
  scrollToBottom()

  const request = {
    message: text,
    conversationId: currentConversationId.value,
    knowledgeBaseId: currentKnowledgeBaseId.value,
    stream: true,
    imageData: imageData || null,
    imageName: imageName || null
  }

  streamChat(
    request,
    // onToken
    (token) => {
      statusText.value = ''
      chatStore.updateLastMessage(token)
      scrollToBottom()
    },
    // onCitation
    (citation) => {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg) {
        lastMsg.citations.push(citation)
      }
    },
    // onDone
    () => {
      isStreaming.value = false
      statusText.value = ''
      scrollToBottom()
    },
    // onError
    (error) => {
      isStreaming.value = false
      statusText.value = ''
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg) {
        lastMsg.content = `Error: ${error}`
      }
    },
    // onStatus
    (status) => {
      statusText.value = status
      scrollToBottom()
    },
    // onTitle
    (title, conversationId) => {
      chatStore.updateConversationTitle(conversationId, title)
      if (!currentConversationId.value) {
        chatStore.setConversation(conversationId)
      }
    }
  )
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f0eb;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.chat-messages::-webkit-scrollbar {
  display: none;
}

.chat-messages {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  color: #666;
  margin-bottom: 8px;
}

.empty-state p {
  color: #999;
  font-size: 14px;
}

.message-wrapper {
  position: relative;
  padding: 0 16px;
}

.streaming-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  margin-left: 44px;
  align-items: center;
}

.status-text {
  font-size: 13px;
  color: #999;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.dots {
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #8b7355;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>

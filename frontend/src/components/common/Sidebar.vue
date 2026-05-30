<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2 class="logo">TOMO Assistant</h2>
      <el-button type="primary" @click="newChat" class="new-chat-btn">
        <el-icon><Plus /></el-icon>
        新建对话
      </el-button>
    </div>

    <div class="sidebar-section">
      <div class="section-title">知识库</div>
      <el-select
        v-model="selectedKB"
        placeholder="选择知识库"
        class="kb-select"
        @change="onKBChange"
        clearable
      >
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :label="kb.name"
          :value="kb.id"
        />
      </el-select>
    </div>

    <div class="sidebar-section conversation-section">
      <div class="section-title">对话列表</div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === chatStore.currentConversationId }"
          @click="selectConversation(conv)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="conv-title">{{ conv.title || '新对话' }}</span>
          <el-button
            type="danger"
            size="small"
            circle
            class="delete-btn"
            @click.stop="deleteConv(conv.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="sidebar-footer">
      <div class="mode-section">
        <div class="mode-label">
          <span class="mode-dot" :class="currentMode"></span>
          <span>{{ currentMode === 'online' ? '在线 API' : '本地 Ollama' }}</span>
        </div>
        <el-button
          text
          size="small"
          class="switch-btn"
          @click="openSwitchDialog"
        >
          切换
        </el-button>
      </div>
      <el-button @click="$router.push('/knowledge-base')" text class="footer-btn">
        <el-icon><FolderOpened /></el-icon>
        管理知识库
      </el-button>
    </div>

    <!-- Mode switch dialog -->
    <el-dialog
      v-model="showDialog"
      :show-close="false"
      width="400px"
      :close-on-click-modal="false"
      class="mode-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <span class="dialog-title">{{ targetMode === 'online' ? '切换到在线 API 模式' : '切换到本地 Ollama 模式' }}</span>
        </div>
      </template>

      <div v-if="targetMode === 'online'" class="dialog-body">
        <div class="warning-banner">
          <div class="warning-icon-wrapper">
            <el-icon :size="20"><WarningFilled /></el-icon>
          </div>
          <div class="warning-text">
            <p>切换到<strong>在线 API 模式</strong>后，您的对话数据（包括上传的图片和文件）将被发送到第三方云端大模型服务进行处理。</p>
            <p class="warning-highlight">这可能导致您的敏感数据泄露风险。</p>
          </div>
        </div>
        <p class="confirm-hint">请确认您了解此风险并同意继续。</p>
      </div>

      <div v-else class="dialog-body">
        <div class="info-banner">
          <div class="info-icon-wrapper">
            <el-icon :size="20"><CircleCheckFilled /></el-icon>
          </div>
          <div class="info-text">
            <p>切换后所有模型将使用本地部署的 Ollama 服务，<strong>数据不会离开您的设备</strong>。</p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false" class="cancel-btn">取消</el-button>
          <el-button
            :disabled="countdown > 0"
            @click="confirmSwitch"
            :loading="switching"
            class="confirm-btn"
          >
            {{ countdown > 0 ? `${countdown}s` : '确认切换' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '../../stores/chat'
import { getConversations, deleteConversation } from '../../api/conversation'
import { getKnowledgeBases } from '../../api/knowledgeBase'
import { getLlmMode, setLlmMode } from '../../api/settings'
import { ElMessage } from 'element-plus'
import { WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const chatStore = useChatStore()
const knowledgeBases = ref([])
const { conversations } = storeToRefs(chatStore)
const selectedKB = ref(null)

// Mode switch state
const currentMode = ref('online')
const showDialog = ref(false)
const targetMode = ref('')
const countdown = ref(0)
const switching = ref(false)
let countdownTimer = null

onMounted(async () => {
  await loadKnowledgeBases()
  await loadConversations()
  await loadCurrentMode()
})

async function loadCurrentMode() {
  try {
    const res = await getLlmMode()
    currentMode.value = res.data.mode
  } catch (e) {
    console.error('Failed to load LLM mode', e)
  }
}

function openSwitchDialog() {
  targetMode.value = currentMode.value === 'online' ? 'ollama' : 'online'
  showDialog.value = true

  if (targetMode.value === 'online') {
    countdown.value = 5
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } else {
    countdown.value = 0
  }
}

async function confirmSwitch() {
  switching.value = true
  try {
    const res = await setLlmMode(targetMode.value)
    currentMode.value = res.data.mode
    showDialog.value = false
    ElMessage.success(`已切换到${currentMode.value === 'online' ? '在线 API' : '本地 Ollama'}模式`)
  } catch (e) {
    ElMessage.error('切换失败: ' + (e.response?.data?.message || e.message))
  } finally {
    switching.value = false
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
  }
}

async function loadKnowledgeBases() {
  try {
    const res = await getKnowledgeBases()
    knowledgeBases.value = res.data || []
  } catch (e) {
    console.error('Failed to load KBs', e)
  }
}

async function loadConversations() {
  try {
    const res = await getConversations(selectedKB.value)
    conversations.value = res.data || []
  } catch (e) {
    console.error('Failed to load conversations', e)
  }
}

function onKBChange(kbId) {
  chatStore.setKnowledgeBase(kbId)
  loadConversations()
}

function selectConversation(conv) {
  chatStore.setConversation(conv.id)
  if (conv.knowledgeBaseId) {
    chatStore.setKnowledgeBase(conv.knowledgeBaseId)
    selectedKB.value = conv.knowledgeBaseId
  }
  if (route.path !== '/chat') {
    router.push('/chat')
  }
}

function newChat() {
  chatStore.setConversation(null)
  chatStore.clearMessages()
  if (route.path !== '/chat') {
    router.push('/chat')
  }
}

async function deleteConv(id) {
  try {
    await deleteConversation(id)
    if (chatStore.currentConversationId === id) {
      chatStore.setConversation(null)
      chatStore.clearMessages()
    }
    await loadConversations()
  } catch (e) {
    console.error('Failed to delete conversation', e)
  }
}
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
}

.sidebar-header {
  margin-bottom: 20px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #8b7355;
}

.new-chat-btn {
  width: 100%;
  background: #8b7355;
  border: none;
}

.new-chat-btn:hover {
  background: #7a6548;
}

.sidebar-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.kb-select {
  width: 100%;
}

.conversation-section {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  scrollbar-width: none;
}

.conversation-list::-webkit-scrollbar {
  display: none;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
  color: #555;
}

.conversation-item:hover {
  background: rgba(139, 115, 85, 0.1);
}

.conversation-item.active {
  background: rgba(139, 115, 85, 0.2);
  color: #333;
}

.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e0d5ca;
}

.mode-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  margin-bottom: 8px;
}

.mode-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.mode-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.mode-dot.online {
  background: #67c23a;
}

.mode-dot.ollama {
  background: #409eff;
}

.switch-btn {
  color: #8b7355 !important;
  font-size: 12px;
}

.switch-btn:hover {
  color: #7a6548 !important;
}

.footer-btn {
  width: 100%;
  color: #888 !important;
}

.footer-btn:hover {
  color: #8b7355 !important;
}

/* Dialog styles */
.mode-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

.mode-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.mode-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.mode-dialog :deep(.el-dialog__footer) {
  padding: 0;
}

.dialog-header {
  background: #8b7355;
  padding: 16px 20px;
}

.dialog-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}

.dialog-body {
  padding: 24px 20px;
}

.warning-banner {
  display: flex;
  gap: 12px;
  background: #fdf6ec;
  border: 1px solid #f0d9a8;
  border-radius: 8px;
  padding: 14px 16px;
}

.warning-icon-wrapper {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: #e6a23c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-top: 2px;
}

.warning-text p {
  color: #666;
  line-height: 1.6;
  font-size: 13px;
  margin: 0;
}

.warning-text p + p {
  margin-top: 6px;
}

.warning-highlight {
  color: #c45656;
  font-weight: 600;
}

.confirm-hint {
  color: #999;
  font-size: 12px;
  margin: 14px 0 0;
  text-align: center;
}

.info-banner {
  display: flex;
  gap: 12px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 8px;
  padding: 14px 16px;
}

.info-icon-wrapper {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: #67c23a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-top: 2px;
}

.info-text p {
  color: #666;
  line-height: 1.6;
  font-size: 13px;
  margin: 0;
}

.info-text strong {
  color: #67c23a;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid #e0d5ca;
  background: #faf7f3;
}

.cancel-btn {
  border-color: #d9cfc4;
  color: #666;
}

.cancel-btn:hover {
  border-color: #8b7355;
  color: #8b7355;
}

.confirm-btn {
  background: #8b7355;
  border-color: #8b7355;
  color: #fff;
}

.confirm-btn:hover:not(:disabled) {
  background: #7a6548;
  border-color: #7a6548;
}

.confirm-btn:disabled {
  background: #c4b8a8;
  border-color: #c4b8a8;
  color: #fff;
}
</style>

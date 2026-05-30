<template>
  <div class="kb-view">
    <div class="view-header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
        <el-icon><Plus /></el-icon>
        创建知识库
      </el-button>
    </div>

    <div class="kb-list" v-if="knowledgeBases.length > 0">
      <el-card
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="kb-card"
        shadow="hover"
      >
        <div class="kb-card-header">
          <h3>{{ kb.name }}</h3>
          <el-tag :type="kb.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
            {{ kb.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
        <div class="kb-stats">
          <span><el-icon><Document /></el-icon> {{ kb.docCount }} 个文档</span>
          <span>创建于: {{ formatDate(kb.createdAt) }}</span>
        </div>
        <div class="kb-actions">
          <el-button size="small" @click="$router.push(`/knowledge-base/${kb.id}/documents`)" class="doc-btn">
            <el-icon><FolderOpened /></el-icon>
            文档
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(kb.id)" plain>
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <div v-else class="empty-state">
      <el-empty description="暂无知识库" />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建知识库" width="500px" class="create-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" />
        </el-form-item>
        <el-form-item label="分块大小">
          <el-input-number v-model="form.chunkSize" :min="100" :max="2000" :step="100" />
          <span class="form-hint">字符数</span>
        </el-form-item>
        <el-form-item label="分块重叠">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="500" :step="50" />
          <span class="form-hint">字符数</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating" class="create-btn">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '../api/knowledgeBase'
import { ElMessageBox, ElMessage } from 'element-plus'

const knowledgeBases = ref([])
const showCreateDialog = ref(false)
const creating = ref(false)
const form = ref({
  name: '',
  description: '',
  chunkSize: 512,
  chunkOverlap: 64
})

onMounted(() => {
  loadKBs()
})

async function loadKBs() {
  try {
    const res = await getKnowledgeBases()
    knowledgeBases.value = res.data || []
  } catch (e) {
    console.error('Failed to load KBs', e)
  }
}

async function handleCreate() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  creating.value = true
  try {
    await createKnowledgeBase(form.value)
    showCreateDialog.value = false
    form.value = { name: '', description: '', chunkSize: 512, chunkOverlap: 64 }
    await loadKBs()
    ElMessage.success('知识库创建成功')
  } catch (e) {
    console.error('Failed to create KB', e)
  } finally {
    creating.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除此知识库吗？', '确认', {
      type: 'warning'
    })
    await deleteKnowledgeBase(id)
    await loadKBs()
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete KB', e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.kb-view {
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  overflow-y: auto;
}

.kb-view::-webkit-scrollbar {
  display: none;
}

.kb-view {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.view-header h2 {
  color: #5d4e37;
  font-size: 24px;
  font-weight: 600;
}

.create-btn {
  background: #8b7355;
  border-color: #8b7355;
}

.create-btn:hover {
  background: #7a6548;
  border-color: #7a6548;
}

.kb-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.kb-card {
  background: #fff;
  border: 1px solid #e8ddd2;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(139, 115, 85, 0.12);
}

.kb-card :deep(.el-card__body) {
  padding: 20px;
}

.kb-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.kb-card-header h3 {
  color: #5d4e37;
  font-size: 18px;
  font-weight: 600;
}

.kb-desc {
  color: #999;
  font-size: 13px;
  margin-bottom: 16px;
  line-height: 1.5;
  min-height: 20px;
}

.kb-stats {
  display: flex;
  gap: 20px;
  color: #b0a598;
  font-size: 12px;
  margin-bottom: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0ebe5;
}

.kb-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.kb-actions {
  display: flex;
  gap: 12px;
}

.doc-btn {
  background: #f5f0eb;
  border-color: #e8ddd2;
  color: #8b7355;
}

.doc-btn:hover {
  background: #8b7355;
  border-color: #8b7355;
  color: #fff;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.form-hint {
  margin-left: 12px;
  color: #b0a598;
  font-size: 12px;
}

.create-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f0ebe5;
  padding-bottom: 16px;
  margin-bottom: 16px;
}

.create-dialog :deep(.el-dialog__title) {
  color: #5d4e37;
  font-weight: 600;
}
</style>

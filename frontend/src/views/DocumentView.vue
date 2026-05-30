<template>
  <div class="doc-view">
    <div class="view-header">
      <div class="header-left">
        <el-button @click="$router.push('/knowledge-base')" text class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>文档管理</h2>
      </div>
      <el-upload
        :auto-upload="false"
        :on-change="handleFileChange"
        multiple
        :show-file-list="false"
      >
        <el-button type="primary" class="upload-btn">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </el-upload>
    </div>

    <el-table :data="documents" class="doc-table" stripe v-if="documents.length > 0">
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.fileType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="90" align="center">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="plain">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
<!--      <el-table-column label="创建时间" width="140" align="center">-->
<!--        <template #default="{ row }">-->
<!--          {{ formatDate(row.createdAt) }}-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)" class="detail-btn">
            详情
          </el-button>
          <el-button size="small" @click="handleReprocess(row.id)" :disabled="row.status === 1" class="reprocess-btn">
            重新处理
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)" plain>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-else class="empty-state">
      <el-empty description="暂无文档" />
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="showDetailDialog" title="文档详情" width="500px" class="detail-dialog">
      <div v-if="currentDoc" class="detail-content">
        <div class="detail-item">
          <span class="detail-label">文件名</span>
          <span class="detail-value">{{ currentDoc.fileName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">文件类型</span>
          <span class="detail-value">{{ currentDoc.fileType }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">文件大小</span>
          <span class="detail-value">{{ formatSize(currentDoc.fileSize) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">状态</span>
          <span class="detail-value">
            <el-tag :type="statusType(currentDoc.status)" size="small" effect="plain">
              {{ statusText(currentDoc.status) }}
            </el-tag>
          </span>
        </div>
        <div class="detail-item">
          <span class="detail-label">分块数</span>
          <span class="detail-value">{{ currentDoc.chunkCount || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ formatDate(currentDoc.createdAt) }}</span>
        </div>
        <div class="detail-item" v-if="currentDoc.updatedAt">
          <span class="detail-label">更新时间</span>
          <span class="detail-value">{{ formatDate(currentDoc.updatedAt) }}</span>
        </div>
        <div class="detail-item" v-if="currentDoc.errorMessage">
          <span class="detail-label">错误信息</span>
          <span class="detail-value error-text">{{ currentDoc.errorMessage }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDocuments, uploadDocuments, deleteDocument, reprocessDocument } from '../api/document'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const kbId = route.params.id
const documents = ref([])
const uploading = ref(false)
const showDetailDialog = ref(false)
const currentDoc = ref(null)

onMounted(() => {
  loadDocuments()
})

async function loadDocuments() {
  try {
    const res = await getDocuments(kbId)
    documents.value = res.data || []
  } catch (e) {
    console.error('Failed to load documents', e)
  }
}

async function handleFileChange(file) {
  uploading.value = true
  try {
    await uploadDocuments(kbId, [file.raw])
    await loadDocuments()
    ElMessage.success('文档已上传，正在处理...')
  } catch (e) {
    console.error('Upload failed', e)
  } finally {
    uploading.value = false
  }
}

async function handleDelete(docId) {
  try {
    await ElMessageBox.confirm('确定要删除此文档吗？', '确认', { type: 'warning' })
    await deleteDocument(kbId, docId)
    await loadDocuments()
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') console.error('Failed to delete', e)
  }
}

async function handleReprocess(docId) {
  try {
    await reprocessDocument(kbId, docId)
    await loadDocuments()
    ElMessage.success('重新处理已开始')
  } catch (e) {
    console.error('Failed to reprocess', e)
  }
}

function showDetail(doc) {
  currentDoc.value = doc
  showDetailDialog.value = true
}

function formatSize(bytes) {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function statusType(status) {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

function statusText(status) {
  const texts = { 0: '待处理', 1: '处理中', 2: '已完成', 3: '失败' }
  return texts[status] || '未知'
}
</script>

<style scoped>
.doc-view {
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  overflow-y: auto;
}

.doc-view::-webkit-scrollbar {
  display: none;
}

.doc-view {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  color: #5d4e37;
  font-size: 24px;
  font-weight: 600;
}

.back-btn {
  color: #8b7355;
}

.back-btn:hover {
  color: #7a6548;
}

.upload-btn {
  background: #8b7355;
  border-color: #8b7355;
}

.upload-btn:hover {
  background: #7a6548;
  border-color: #7a6548;
}

.doc-table {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e8ddd2;
}

.doc-table :deep(.el-table__header-wrapper th) {
  background: #f5f0eb;
  color: #5d4e37;
  font-weight: 600;
}

.doc-table :deep(.el-table__row) {
  background: #fff;
  color: #5d4e37;
}

.doc-table :deep(.el-table__row:hover > td) {
  background: #faf7f3;
}

.doc-table :deep(.el-table__cell) {
  border-bottom: 1px solid #f0ebe5;
}

.detail-btn {
  background: #f5f0eb;
  border-color: #e8ddd2;
  color: #8b7355;
}

.detail-btn:hover {
  background: #8b7355;
  border-color: #8b7355;
  color: #fff;
}

.reprocess-btn {
  background: #f5f0eb;
  border-color: #e8ddd2;
  color: #8b7355;
}

.reprocess-btn:hover:not(:disabled) {
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

.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f0ebe5;
  padding-bottom: 16px;
  margin-bottom: 16px;
}

.detail-dialog :deep(.el-dialog__title) {
  color: #5d4e37;
  font-weight: 600;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.detail-label {
  color: #b0a598;
  font-size: 14px;
  min-width: 80px;
  flex-shrink: 0;
}

.detail-value {
  color: #5d4e37;
  font-size: 14px;
  word-break: break-all;
}

.error-text {
  color: #e6a23c;
}
</style>

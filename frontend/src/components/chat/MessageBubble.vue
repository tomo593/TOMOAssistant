<template>
  <div class="message-bubble" :class="message.role">
    <div class="avatar" v-if="message.role === 'assistant'">
      <el-icon><Monitor /></el-icon>
    </div>

    <div class="message-content">
      <!-- Image display for user messages -->
      <div v-if="message.imageData" class="message-image">
        <el-image
          :src="'data:image/*;base64,' + message.imageData"
          :preview-src-list="['data:image/*;base64,' + message.imageData]"
          fit="cover"
          class="image-thumb"
        />
      </div>
      <div class="message-text" v-html="renderedContent"></div>

      <!-- Citations -->
      <div v-if="message.citations && message.citations.length" class="citations">
        <div class="citations-header">参考资料</div>
        <div
          v-for="(cite, i) in message.citations"
          :key="i"
          class="citation-item"
        >
          <span class="citation-index">[{{ i + 1 }}]</span>
          <span class="citation-name">{{ cite.documentName || '未知文档' }}</span>
          <span class="citation-score">相似度: {{ (cite.similarityScore * 100).toFixed(1) }}%</span>
        </div>
      </div>
    </div>

    <div class="avatar" v-if="message.role === 'user'">
      <el-icon><User /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  message: { type: Object, required: true }
})

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  return marked(props.message.content, { breaks: true })
})
</script>

<style scoped>
.message-bubble {
  display: flex;
  gap: 8px;
  padding: 12px 0;
  max-width: 800px;
  margin: 0 auto;
  align-items: flex-start;
}

.message-bubble.user {
  justify-content: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user .avatar {
  background: #8b7355;
  color: #fff;
}

.assistant .avatar {
  background: #a0845c;
  color: #fff;
}

.message-content {
  min-width: 0;
}

.assistant .message-content {
  flex: 1;
}

.user .message-content {
  flex: none;
  max-width: 70%;
}

.message-text {
  line-height: 1.6;
  color: #333;
}

.message-text :deep(p) {
  margin-bottom: 8px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
}

.message-text :deep(pre) {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.message-text :deep(li) {
  margin-bottom: 4px;
}

.message-text :deep(blockquote) {
  border-left: 3px solid #8b7355;
  padding-left: 12px;
  margin: 8px 0;
  color: #666;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3) {
  margin-top: 12px;
  margin-bottom: 8px;
  color: #222;
}

.message-text :deep(a) {
  color: #8b7355;
  text-decoration: none;
}

.message-text :deep(a:hover) {
  text-decoration: underline;
}

.message-text :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.message-text :deep(th),
.message-text :deep(td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.message-text :deep(th) {
  background: #f5f5f5;
}

.user .message-text {
  background: #8b7355;
  color: #fff;
  padding: 12px 16px;
  border-radius: 16px 16px 4px 16px;
  display: inline-block;
  max-width: 100%;
}

.message-image {
  margin-bottom: 8px;
}

.message-image .image-thumb {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}

.user .message-image .image-thumb {
  border-radius: 16px 16px 4px 16px;
}

.assistant .message-text {
  background: #fff;
  padding: 12px 16px;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.citations {
  margin-top: 12px;
  border-top: 1px solid #eee;
  padding-top: 12px;
}

.citations-header {
  font-size: 12px;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.citation-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #faf7f3;
  border: 1px solid #e8ddd2;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 4px;
  font-size: 13px;
}

.citation-index {
  color: #8b7355;
  font-weight: 600;
  flex-shrink: 0;
}

.citation-name {
  color: #555;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.citation-score {
  color: #a0845c;
  font-size: 12px;
  flex-shrink: 0;
}
</style>

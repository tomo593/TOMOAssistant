<template>
  <div class="chat-input">
    <!-- Image preview area -->
    <div v-if="imagePreview" class="image-preview-area">
      <div class="image-preview">
        <img :src="imagePreview" alt="preview" />
        <el-button
          type="danger"
          :icon="Close"
          circle
          size="small"
          class="remove-btn"
          @click="removeImage"
        />
        <span class="image-name">{{ imageName }}</span>
      </div>
    </div>

    <div class="input-wrapper">
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileSelect"
      />
      <el-button
        :icon="PictureFilled"
        circle
        class="upload-btn"
        @click="triggerImageUpload"
        :disabled="disabled"
      />
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :placeholder="imagePreview ? '添加描述（可选）...' : '输入消息...'"
        @keydown.enter.exact.prevent="handleSend"
        :disabled="disabled"
        class="message-input"
      />
      <el-button
        type="primary"
        @click="handleSend"
        :disabled="(!inputText.trim() && !imagePreview) || disabled"
        class="send-btn"
        circle
      >
        <el-icon><Promotion /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PictureFilled, Close, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])
const inputText = ref('')
const fileInput = ref(null)
const imageFile = ref(null)
const imagePreview = ref('')
const imageName = ref('')

function triggerImageUpload() {
  fileInput.value.click()
}

function handleFileSelect(event) {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    return
  }

  imageFile.value = file
  imageName.value = file.name
  imagePreview.value = URL.createObjectURL(file)

  // Reset file input so the same file can be selected again
  event.target.value = ''
}

function removeImage() {
  if (imagePreview.value) {
    URL.revokeObjectURL(imagePreview.value)
  }
  imageFile.value = null
  imagePreview.value = ''
  imageName.value = ''
}

function handleSend() {
  const hasText = inputText.value.trim()
  const hasImage = imageFile.value

  if ((!hasText && !hasImage) || props.disabled) return

  if (hasImage) {
    const reader = new FileReader()
    reader.onload = () => {
      const base64Data = reader.result.split(',')[1]
      emit('send', {
        text: inputText.value.trim(),
        imageData: base64Data,
        imageName: imageName.value
      })
      inputText.value = ''
      removeImage()
    }
    reader.readAsDataURL(imageFile.value)
  } else {
    emit('send', {
      text: inputText.value.trim(),
      imageData: null,
      imageName: null
    })
    inputText.value = ''
  }
}
</script>

<style scoped>
.chat-input {
  padding: 16px 20px;
  background: #f0ebe5;
  border-top: 1px solid #e0d5ca;
}

.image-preview-area {
  max-width: 800px;
  margin: 0 auto 8px;
}

.image-preview {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #d9cfc4;
  border-radius: 8px;
  padding: 6px 8px;
}

.image-preview img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.image-name {
  font-size: 12px;
  color: #666;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
}

.input-wrapper {
  display: flex;
  gap: 8px;
  max-width: 800px;
  margin: 0 auto;
  align-items: flex-end;
}

.upload-btn {
  width: 40px;
  height: 40px;
  background: #fff;
  border: 1px solid #d9cfc4;
  color: #8b7355;
  flex-shrink: 0;
}

.upload-btn:hover:not(:disabled) {
  border-color: #8b7355;
  color: #7a6548;
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  background: #fff;
  border: 1px solid #d9cfc4;
  color: #333;
  border-radius: 12px;
  padding: 12px 16px;
  resize: none;
}

.message-input :deep(.el-textarea__inner):focus {
  border-color: #8b7355;
}

.message-input :deep(.el-textarea__inner)::placeholder {
  color: #b0a598;
}

.send-btn {
  width: 40px;
  height: 40px;
  background: #8b7355;
  border: none;
}

.send-btn:hover:not(:disabled) {
  background: #7a6548;
}

.send-btn:disabled {
  opacity: 0.5;
}
</style>

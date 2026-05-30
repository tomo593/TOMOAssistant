import request from './request'

export function getDocuments(kbId) {
  return request.get(`/knowledge-bases/${kbId}/documents`)
}

export function uploadDocuments(kbId, files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post(`/knowledge-bases/${kbId}/documents/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteDocument(kbId, docId) {
  return request.delete(`/knowledge-bases/${kbId}/documents/${docId}`)
}

export function reprocessDocument(kbId, docId) {
  return request.post(`/knowledge-bases/${kbId}/documents/${docId}/reprocess`)
}

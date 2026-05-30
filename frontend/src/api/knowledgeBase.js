import request from './request'

export function getKnowledgeBases() {
  return request.get('/knowledge-bases')
}

export function getKnowledgeBase(id) {
  return request.get(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data) {
  return request.post('/knowledge-bases', data)
}

export function updateKnowledgeBase(id, data) {
  return request.put(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id) {
  return request.delete(`/knowledge-bases/${id}`)
}

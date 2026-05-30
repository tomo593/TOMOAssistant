import request from './request'

export function getConversations(knowledgeBaseId) {
  const params = knowledgeBaseId ? { knowledgeBaseId } : {}
  return request.get('/conversations', { params })
}

export function getConversation(id) {
  return request.get(`/conversations/${id}`)
}

export function createConversation(data) {
  return request.post('/conversations', data)
}

export function deleteConversation(id) {
  return request.delete(`/conversations/${id}`)
}

export function getMessages(conversationId) {
  return request.get(`/conversations/${conversationId}/messages`)
}

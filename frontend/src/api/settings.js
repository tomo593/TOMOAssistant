import request from './request'

export function getLlmMode() {
  return request.get('/settings/llm-mode')
}

export function setLlmMode(mode) {
  return request.put('/settings/llm-mode', { mode })
}

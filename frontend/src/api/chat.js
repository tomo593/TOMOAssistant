import request from './request'

export function streamChat(data, onToken, onCitation, onDone, onError, onStatus, onTitle) {
  const controller = new AbortController()

  fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
    signal: controller.signal
  }).then(async response => {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          const eventType = line.substring(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (!data) continue
          try {
            const parsed = JSON.parse(data)
            switch (parsed.type) {
              case 'TOKEN':
                onToken(parsed.content)
                break
              case 'CITATION':
                onCitation(parsed.citation)
                break
              case 'STATUS':
                if (onStatus) onStatus(parsed.content)
                break
              case 'TITLE':
                if (onTitle) onTitle(parsed.title, parsed.conversationId)
                break
              case 'DONE':
                onDone()
                break
              case 'ERROR':
                onError(parsed.content)
                break
            }
          } catch (e) {
            // Skip non-JSON lines
          }
        }
      }
    }
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError(err.message)
    }
  })

  return { abort: () => controller.abort() }
}

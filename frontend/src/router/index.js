import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatView.vue')
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    component: () => import('../views/KnowledgeBaseView.vue')
  },
  {
    path: '/knowledge-base/:id/documents',
    name: 'Documents',
    component: () => import('../views/DocumentView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

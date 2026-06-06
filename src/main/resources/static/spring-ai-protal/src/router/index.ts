import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/ai-chat',
      name: 'aiChat',
      component: () => import('../views/AIChat.vue'),
    },
    {
      path: '/game',
      name: 'game',
      component: () => import('../views/GameChat.vue'),
    },
    {
      path: '/customer-service',
      name: 'customerService',
      component: () => import('../views/CustomerService.vue'),
    },
    {
      path: '/comfort-simulator',
      name: 'comfortSimulator',
      component: () => import('../views/ComfortSimulator.vue'),
    },
    {
      path: '/chat-pdf',
      name: 'chatPDF',
      component: () => import('../views/ChatPDF.vue'),
    },
  ],
})

export default router

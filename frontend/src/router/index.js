import { createRouter, createWebHistory } from 'vue-router'
import FileList from '../views/FileList.vue'
import FileDetail from '../views/FileDetail.vue'

const routes = [
  {
    path: '/',
    name: 'FileList',
    component: FileList
  },
  {
    path: '/detail/:fileId',
    name: 'FileDetail',
    component: FileDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

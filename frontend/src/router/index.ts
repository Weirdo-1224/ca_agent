import { createRouter, createWebHistory } from 'vue-router';
import TaskCreate from '@/views/TaskCreate.vue';
import TaskDetail from '@/views/TaskDetail.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'TaskCreate', component: TaskCreate },
    { path: '/tasks/:taskId', name: 'TaskDetail', component: TaskDetail, props: true },
  ],
});

export default router;

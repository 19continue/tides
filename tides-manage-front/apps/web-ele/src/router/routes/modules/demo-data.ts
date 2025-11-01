import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:database-backup',
      order: 6,
      title: '演示数据',
    },
    name: 'demoData',
    path: '/demoData',
    redirect: '/demoData/snapshot',
    children: [
      {
        alias: '/demoDataSnapshot',
        path: 'snapshot',
        name: 'demoDataSnapshot',
        meta: {
          title: '数据基准恢复',
        },
        component: () => import('#/views/demo-data/snapshot/index.vue'),
      },
    ],
  },
];

export default routes;

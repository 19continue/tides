import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:calendar-clock',
      order: 7,
      title: '时间维护',
    },
    name: 'timeMaintenance',
    path: '/timeMaintenance',
    redirect: '/timeMaintenance/renewal',
    children: [
      {
        alias: '/timeRenewal',
        component: () => import('#/views/time-renewal/index.vue'),
        meta: {
          title: '时间续期',
        },
        name: 'timeRenewal',
        path: 'renewal',
      },
    ],
  },
];

export default routes;

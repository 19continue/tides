import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:film',
      order: 2,
      title: '电影运营',
    },
    name: 'movieData',
    path: '/movieData',
    redirect: '/movieData/movieWorkbench',
    children: [
      {
        alias: '/movieWorkbench',
        component: () => import('#/views/movie/index/list.vue'),
        meta: {
          title: '电影工作台',
        },
        name: 'movieWorkbench',
        path: 'movieWorkbench',
      },
    ],
  },
];

export default routes;

<template>
  <div class="footer">
    <div class="footer-sec">
      <div class="footer-main">
        <div class="footer-brand">
          <span class="brand-name">潮声</span>
          <span class="brand-desc">演出、电影与城市现场体验</span>
          <p>面向演出抢票、电影选座和订单服务的统一购票平台。</p>
        </div>
        <div class="footer-columns">
          <div class="footer-column" v-for="group in footerGroups" :key="group.title">
            <strong>{{ group.title }}</strong>
            <router-link v-for="item in group.links" :key="item.label" :to="item.to">{{ item.label }}</router-link>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© 2026 潮声文化服务平台</span>
        <span>购票须知 · 观影须知 · 演出须知 · 隐私与协议</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const footerGroups = [
  {
    title: '购票入口',
    links: [
      {label: '全部演出', to: '/allType/index'},
      {label: '电影专区', to: '/movie/index'},
      {label: '我的订单', to: '/orderManagement/index'},
    ],
  },
  {
    title: '服务支持',
    links: [
      {label: '账号资料', to: '/personInfo/index'},
      {label: '购票人管理', to: '/personInfo/ticketUser'},
      {label: '安全设置', to: '/accountSettings/index'},
    ],
  },
  {
    title: '城市现场',
    links: [
      {label: '热门演唱会', to: {path: '/allType/index', query: {name: '演唱会'}}},
      {label: '剧场舞台', to: {path: '/allType/index', query: {name: '话剧歌剧'}}},
      {label: '热映影片', to: '/movie/index'},
    ],
  },
]
</script>

<style scoped lang="scss">
.footer {
  width: 100%;
  margin-top: clamp(54px, 7vw, 86px);
  padding: 58px 0 38px;
  background:
      radial-gradient(circle at 14% 0%, rgba(245, 194, 65, .14), transparent 30%),
      #11100e;
  box-shadow: 0 0 0 100vmax #11100e;
  clip-path: inset(0 -100vmax);
  border-top: 1px solid rgba(255, 255, 255, .08);
  color: rgba(255, 255, 255, .72);

  .footer-sec {
    width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
    margin: 0 auto;
  }

  .footer-main {
    min-height: 158px;
    display: grid;
    grid-template-columns: minmax(260px, 1.1fr) minmax(0, 1.6fr);
    gap: clamp(34px, 7vw, 92px);
    align-items: start;
  }

  .footer-brand {
    display: flex;
    flex-direction: column;
    align-items: flex-start;

    .brand-name {
      color: var(--brand-primary);
      font-size: 30px;
      line-height: 1;
      font-weight: 700;
    }

    .brand-desc {
      margin-top: 14px;
      color: rgba(255, 255, 255, .9);
      font-size: 15px;
    }

    p {
      width: min(360px, 100%);
      margin: 16px 0 0;
      color: rgba(255, 255, 255, .52);
      font-size: 14px;
      line-height: 1.8;
    }
  }

  .footer-columns {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 28px;
  }

  .footer-column {
    display: flex;
    flex-direction: column;
    gap: 12px;

    strong {
      margin-bottom: 4px;
      color: rgba(255, 255, 255, .92);
      font-size: 15px;
      font-weight: 600;
    }

    a {
      color: rgba(255, 255, 255, .55);
      font-size: 14px;
      transition: color .16s ease;
    }

    a:hover {
      color: var(--brand-primary);
    }
  }

  .footer-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    margin-top: 48px;
    padding-top: 22px;
    border-top: 1px solid rgba(255, 255, 255, .1);

    span {
      color: rgba(255, 255, 255, .42);
      font-size: 13px;
    }
  }
}

.btn {
  display: none;
}

@media (max-width: 640px) {
  .footer {
    padding: 42px 0 30px;

    .footer-sec {
      width: calc(100% - 28px);
    }

    .footer-main,
    .footer-columns {
      grid-template-columns: 1fr;
    }

    .footer-columns {
      gap: 24px;
    }

    .footer-bottom {
      align-items: flex-start;
      flex-direction: column;
      margin-top: 34px;
    }
  }
}
</style>

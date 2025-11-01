<template>
  <Header></Header>
  <section class="home-hero">
    <el-carousel :interval="5600" arrow="always" trigger="click" :pause-on-hover="true" class="carousel-lamp">
      <el-carousel-item v-for="item in heroSlides" :key="item.title">
        <article :class="['hero-slide', `hero-slide-${item.tone}`, `hero-layout-${item.layout}`, item.banner ? 'hero-has-banner' : 'hero-has-poster']">
          <router-link class="hero-banner-link" :to="item.to" :aria-label="`查看${item.title}`"></router-link>
          <img class="hero-backdrop" :src="item.banner || item.poster" :alt="item.title" :style="{objectPosition: item.position}" @error="hideBrokenImage">
          <span class="hero-shade"></span>
          <div class="hero-inner">
            <div class="hero-copy">
              <p class="hero-eyebrow">{{ item.eyebrow }}</p>
              <router-link :to="item.to" class="hero-title-link">
                <h1>{{ item.title }}</h1>
              </router-link>
              <p class="hero-desc">{{ item.desc }}</p>
              <div class="hero-meta">
                <span v-for="point in item.points" :key="point">{{ point }}</span>
              </div>
              <div :class="['hero-entry', `hero-entry-${item.interaction}`]">
                <router-link v-if="item.interaction === 'button'" :to="item.to" class="hero-primary">{{ item.action }}</router-link>
                <router-link v-else-if="item.interaction === 'strip'" :to="item.to" class="hero-ticket-strip">
                  <span>{{ item.venue }}</span>
                  <strong>{{ item.date }}</strong>
                  <em>{{ item.action }}</em>
                </router-link>
                <router-link v-else-if="item.interaction === 'text'" :to="item.to" class="hero-text-link">{{ item.action }}</router-link>
                <span v-else class="hero-hint">{{ item.action }}</span>
              </div>
            </div>
            <router-link v-if="!item.banner" :to="item.to" class="hero-poster" :aria-label="`查看${item.title}`">
              <img :src="item.poster" :alt="item.title" @error="hideBrokenImage">
              <span>{{ item.posterTag || '查看详情' }}</span>
            </router-link>
            <div class="hero-side-list">
              <span>{{ item.sideTitle }}</span>
              <router-link v-for="related in item.related" :key="related.label" :to="related.to">
                {{ related.label }}
              </router-link>
            </div>
          </div>
        </article>
      </el-carousel-item>
    </el-carousel>
  </section>

  <main class="app-container">
    <section class="category">
      <div class="category-head">
        <span>快速进入</span>
        <strong>按当前城市展示可购场次</strong>
      </div>
      <ul>
        <li v-for="item in categoryArr" :key="item.id" :class="['category-card', getCategoryMeta(item).className]">
          <router-link :to="getCategoryRoute(item)">
            <span class="category-icon"><span class="icon-mark"></span></span>
            <span class="category-copy">
              <span class="category-name">{{ item.name }}</span>
              <em>{{ getCategoryMeta(item).desc }}</em>
            </span>
          </router-link>
        </li>
      </ul>
    </section>

    <section class="movie-showcase" v-if="movieShowcase.length">
      <div class="showcase-head">
        <div>
          <span>电影专区</span>
          <h2>正在热映与预售</h2>
        </div>
        <router-link to="/movie/index">全部影片</router-link>
      </div>
      <div class="showcase-grid">
        <router-link class="showcase-card" v-for="item in movieShowcase" :key="item.programId" :to="getProgramRoute(item)">
          <img :src="item.itemPicture" :alt="item.title" @error="hideBrokenImage">
          <strong>{{ item.title }}</strong>
          <span v-if="item.actor || item.genre">{{ item.actor || item.genre }}</span>
          <em v-if="item.minPrice">¥{{ item.minPrice }}起</em>
        </router-link>
      </div>
    </section>

    <section class="diffrentType" v-for="(item,index) in programList" :key="item.categoryId || index">
      <div class="name">
        <span>{{ item.categoryName }}</span>
        <router-link :to="{ path: '/allType/index', query: {type:1,name:item.categoryName,id:item.categoryId} }" class="more">
          查看全部
        </router-link>
      </div>
      <div class="box" v-if="item.programListVoList && item.programListVoList.length">
        <div class="box-left">
          <router-link :to="getProgramRoute(item.programListVoList[0])">
            <img :src="item.programListVoList[0].itemPicture" alt="" @error="hideBrokenImage">
          </router-link>
        </div>
        <div class="box-right">
          <article class="rtLink" v-for="(dict,ind) in item.programListVoList.slice(1)" :key="dict.id || ind">
            <router-link :to="getProgramRoute(dict)">
              <img :src="dict.itemPicture" alt="" @error="hideBrokenImage">
              <div class="info">
                <div class="img-title">{{ dict.title }}</div>
                <div class="local">{{ dict.place }}</div>
                <div class="showTime">{{ dict.showTime }}{{ dict.showWeekTime }}</div>
                <div class="price">{{ dict.minPrice }} <span class="rise">起</span></div>
              </div>
            </router-link>
          </article>
        </div>
      </div>
      <div class="empty-section" v-else>暂无场次</div>
    </section>

  </main>
  <Footer></Footer>
</template>

<script setup>
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {onMounted, ref, watch} from 'vue'
import {getcategoryType, getMainCategory} from '@/api/index'
import {getMoviePage} from '@/api/movie'
import useCityStore from '@/store/modules/city'
import jayChouPoster from '@/assets/home/hero/jay-chou.jpg'
import devilWearsPradaBanner from '@/assets/home/hero/prada-banner.png'
import devilWearsPradaPoster from '@/assets/home/hero/devil-wears-prada-2.jpg'
import gemGloriaPoster from '@/assets/home/hero/gem-gloria.png'
import marioGalaxyBanner from '@/assets/home/hero/mario-banner.jpg'
import marioGalaxyPoster from '@/assets/home/hero/mario-galaxy.jpg'
import starWarsGroguBanner from '@/assets/home/hero/star-wars-banner.jpg'
import starWarsGroguPoster from '@/assets/home/hero/star-wars-grogu.jpg'
import liRonghaoPoster from '@/assets/home/hero/li-ronghao.jpg'
import dearYouPoster from '@/assets/home/hero/dear-you.jpg'
import vanishingPointPoster from '@/assets/home/hero/vanishing-point.jpg'
import ggBondRacingPoster from '@/assets/home/hero/gg-bond-racing.jpg'

const heroSlides = [
  {
    eyebrow: '演唱会 · 北京',
    title: '周杰伦“嘉年华”巡演',
    desc: '热门演唱会场次集中展示，票档、余量和订单链路保持同步。',
    poster: jayChouPoster,
    action: '查看余票',
    to: {name: 'detial', params: {id: 34}},
    position: 'center 34%',
    points: ['工人体育馆', '预售中', '实名购票'],
    interaction: 'strip',
    venue: '工人体育馆',
    date: '演唱会 · 票档同步',
    posterTag: '进入详情',
    sideTitle: '同城热度',
    tone: 'concert',
    layout: 'poster-right',
    related: [
      {label: '邓紫棋 I AM GLORIA', to: {name: 'detial', params: {id: 38}}},
      {label: '梁静茹世界巡回演唱会', to: {name: 'detial', params: {id: 46}}},
      {label: '蔡依林 Ugly Beauty', to: {name: 'detial', params: {id: 50}}}
    ]
  },
  {
    eyebrow: '电影 · 热映预售',
    title: '穿普拉达的女王2',
    desc: '从影片详情到影院排片、选座和支付，按真实购票路径进入。',
    banner: devilWearsPradaBanner,
    poster: devilWearsPradaPoster,
    action: '点海报查看影城排片',
    to: {name: 'movieDetail', params: {id: 26008}},
    position: 'center center',
    points: ['剧情/喜剧', 'IMAX 2D', '安妮·海瑟薇'],
    interaction: 'poster',
    venue: '多影院排片',
    date: '4月30日上映',
    sideTitle: '正在售票',
    tone: 'movie',
    layout: 'editorial',
    related: [
      {label: '星球大战：曼达洛人与古古', to: {name: 'movieDetail', params: {id: 26012}}},
      {label: '超级马力欧银河大电影', to: {name: 'movieDetail', params: {id: 26016}}},
      {label: '全部电影', to: {path: '/movie/index'}}
    ]
  },
  {
    eyebrow: '演唱会 · 北京',
    title: '邓紫棋 I AM GLORIA',
    desc: '保留演出抢票入口的节奏，适合快速演示热门项目下单。',
    poster: gemGloriaPoster,
    action: '进入抢票',
    to: {name: 'detial', params: {id: 38}},
    position: 'center 28%',
    points: ['国家体育馆', '高热度', '可选票档'],
    interaction: 'button',
    venue: '国家体育馆',
    date: '演唱会',
    posterTag: '立即查看',
    sideTitle: '演出推荐',
    tone: 'concert',
    layout: 'poster-feature',
    related: [
      {label: '周杰伦“嘉年华”巡演', to: {name: 'detial', params: {id: 34}}},
      {label: '蔡依林 Ugly Beauty', to: {name: 'detial', params: {id: 50}}},
      {label: '李荣浩“纵横四海”', to: {name: 'detial', params: {id: 30}}}
    ]
  },
  {
    eyebrow: '亲子电影 · 嘉兴',
    title: '超级马力欧银河大电影',
    desc: '亲子观影、黄金场和座位价格区分一起展示，适合周末购票。',
    banner: marioGalaxyBanner,
    poster: marioGalaxyPoster,
    action: '查看影院排片',
    to: {name: 'movieDetail', params: {id: 26016}},
    position: 'center center',
    points: ['动画/冒险', '亲子场', '座位图'],
    interaction: 'text',
    venue: '嘉兴多影院',
    date: '黄金场较多',
    sideTitle: '家庭观影',
    tone: 'family',
    layout: 'family',
    related: [
      {label: '猪猪侠大电影之竞速小英雄', to: {name: 'movieDetail', params: {id: 26020}}},
      {label: '给阿嬷的情书', to: {name: 'movieDetail', params: {id: 26000}}},
      {label: '全部电影', to: {path: '/movie/index'}}
    ]
  },
  {
    eyebrow: '电影 · 科幻冒险',
    title: '星球大战：曼达洛人与古古',
    desc: '大片排片适合展示影厅、票价和选座库存的实时变化。',
    banner: starWarsGroguBanner,
    poster: starWarsGroguPoster,
    action: '查看场次',
    to: {name: 'movieDetail', params: {id: 26012}},
    position: 'center center',
    points: ['动作/科幻', 'CINITY 2D', '黄金场'],
    interaction: 'strip',
    venue: 'IMAX / CINITY',
    date: '5月22日上映',
    sideTitle: '热映预售',
    tone: 'movie',
    layout: 'cinema',
    related: [
      {label: '穿普拉达的女王2', to: {name: 'movieDetail', params: {id: 26008}}},
      {label: '消失的人', to: {name: 'movieDetail', params: {id: 26004}}},
      {label: '给阿嬷的情书', to: {name: 'movieDetail', params: {id: 26000}}}
    ]
  },
  {
    eyebrow: 'Livehouse · 深圳',
    title: '李荣浩“纵横四海”',
    desc: '小型现场也放在首页轮播里，和演唱会、电影形成更完整的购票入口。',
    poster: liRonghaoPoster,
    action: '点海报或标题进入详情',
    to: {name: 'detial', params: {id: 30}},
    position: 'center 30%',
    points: ['Livehouse', '预售中', '电子票'],
    interaction: 'poster',
    venue: 'MAOLivehouse深圳',
    date: '现场演出',
    posterTag: '查看演出',
    sideTitle: '更多现场',
    tone: 'livehouse',
    layout: 'poster-minimal',
    related: [
      {label: '周杰伦“嘉年华”巡演', to: {name: 'detial', params: {id: 34}}},
      {label: '邓紫棋 I AM GLORIA', to: {name: 'detial', params: {id: 38}}},
      {label: '全部场次', to: {path: '/allType/index'}}
    ]
  }
]

const moviePosterMap = {
  26000: dearYouPoster,
  26004: vanishingPointPoster,
  26008: devilWearsPradaPoster,
  26012: starWarsGroguPoster,
  26016: marioGalaxyPoster,
  26020: ggBondRacingPoster,
}

const allProgramCategory = {
  id: 'all-programs',
  name: '全部场次',
  type: 1,
  route: {path: '/allType/index'}
}
const categoryArr = ref([])
const programList = ref([])
const movieShowcase = ref([])
const cityStore = useCityStore()
const homeReady = ref(false)
let mainCategoryRequestId = 0
let movieShowcaseRequestId = 0
const queryParams = ref({
  areaId: undefined,
  areaIds: undefined,
  parentProgramCategoryIds: []
})
const categoryMetaMap = {
  演唱会: {className: 'cat-concert', desc: '热声现场'},
  话剧歌剧: {className: 'cat-theatre', desc: '剧场舞台'},
  体育: {className: 'cat-sports', desc: '赛场热力'},
  儿童亲子: {className: 'cat-family', desc: '周末陪伴'},
  展览休闲: {className: 'cat-exhibit', desc: '展览体验'},
  音乐会: {className: 'cat-classic', desc: '古典回响'},
  曲苑杂坛: {className: 'cat-opera', desc: '曲艺国风'},
  舞蹈芭蕾: {className: 'cat-dance', desc: '身体叙事'},
  二次元: {className: 'cat-acg', desc: '同好聚场'},
  旅游展览: {className: 'cat-travel', desc: '城市漫游'},
  电影: {className: 'cat-movie', desc: '银幕新片'},
  全部场次: {className: 'cat-all', desc: '一键浏览'}
}

onMounted(async () => {
  await cityStore.initCity({useBrowserLocation: true})
  await getCategoryList().catch(() => {
    categoryArr.value = []
  })
  homeReady.value = true
  reloadHomeData()
})

watch(
    () => cityStore.selectedCity,
    () => {
      if (homeReady.value) {
        reloadHomeData()
      }
    }
)

function getCategoryList() {
  return getcategoryType({type: 1}).then(response => {
    categoryArr.value = buildHomeCategories(response.data || [])
  })
}

function buildHomeCategories(list) {
  const categories = Array.isArray(list) ? [...list] : []
  const hasAllEntry = categories.some(item => item?.id === allProgramCategory.id || item?.name === allProgramCategory.name)
  if (!hasAllEntry && categories.length === 11) {
    categories.push(allProgramCategory)
  }
  return categories
}

function getCategoryMeta(item) {
  return categoryMetaMap[item.name] || {className: 'cat-default', desc: '精选场次'}
}

function getCategoryRoute(item) {
  if (item?.route) {
    return item.route
  }
  return item?.name === '电影' || Number(item?.id) === 22
      ? {path: '/movie/index'}
      : { path: '/allType/index', query: {type:item.type,name:item.name,id:item.id} }
}

function applySelectedCity() {
  queryParams.value.areaId = cityStore.programAreaId
  queryParams.value.areaIds = cityStore.programAreaIds
}

function reloadHomeData() {
  applySelectedCity()
  getMainCategoryList()
  getMovieShowcase()
}

function getMainCategoryList() {
  const categories = categoryArr.value || []
  const parentProgramCategoryIds = categories
      .map(item => item.id)
      .filter(id => id && Number.isFinite(Number(id)))
      .slice(0, 4)
  queryParams.value.parentProgramCategoryIds = parentProgramCategoryIds
  const requestId = ++mainCategoryRequestId
  const params = {
    ...queryParams.value,
    parentProgramCategoryIds
  }
  getMainCategory(params).then(response => {
    if (requestId !== mainCategoryRequestId) {
      return
    }
    programList.value = response.data || []
  })
}

function getMovieShowcase() {
  const requestId = ++movieShowcaseRequestId
  const params = {
    pageNumber: 1,
    pageSize: 6,
    areaId: cityStore.movieAreaId,
  }
  getMoviePage(params).then(response => {
    if (requestId !== movieShowcaseRequestId) {
      return
    }
    const data = response.data || {}
    const rows = data.records || data.list || []
    movieShowcase.value = rows.map(item => ({
      ...item,
      id: item.programId || item.id,
      programId: item.programId || item.id,
      title: item.movieName || item.title || '',
      actor: item.actors || item.actor || '',
      itemPicture: moviePosterMap[Number(item.programId || item.id)] || item.poster || item.itemPicture || '',
      minPrice: item.lowestPrice || item.minPrice,
      parentProgramCategoryId: 22,
    })).filter(item => item.programId && item.title && item.itemPicture).slice(0, 6)
  }).catch(() => {
    if (requestId !== movieShowcaseRequestId) {
      return
    }
    movieShowcase.value = []
  })
}

function getProgramRoute(item) {
  return Number(item?.parentProgramCategoryId) === 22
      ? {name: 'movieDetail', params: {id: item.programId || item.id}}
      : {name: 'detial', params: {id: item.id}}
}

function hideBrokenImage(event) {
  event.target.style.visibility = 'hidden'
}
</script>

<style scoped lang="scss">
.home-hero {
  position: relative;
  width: 100%;
  min-height: 470px;
  overflow: hidden;
  background: #11100e;

  .carousel-lamp {
    width: 100%;
    height: 470px;
    overflow: hidden;

    :deep(.el-carousel__container) {
      height: 470px;
    }

    :deep(.el-carousel__indicators) {
      left: max(calc((100% - var(--layout-width)) / 2), calc(var(--layout-gutter) / 2));
      right: auto;
      bottom: 24px;
      transform: none;
    }

    :deep(.el-carousel__button) {
      width: 42px;
      height: 3px;
      border-radius: 999px;
      background: rgba(255, 255, 255, .9);
      opacity: .42;
    }

    :deep(.el-carousel__indicator.is-active .el-carousel__button) {
      opacity: 1;
      background: var(--brand-primary);
    }

    :deep(.el-carousel__arrow) {
      width: 38px;
      height: 38px;
      border: 1px solid rgba(255, 255, 255, .24);
      background: rgba(18, 17, 15, .36);
      backdrop-filter: blur(10px);
    }

    :deep(.el-carousel__arrow:hover) {
      background: rgba(18, 17, 15, .58);
    }
  }

  .hero-slide {
    position: relative;
    height: 470px;
    overflow: hidden;
    color: #fff;
    isolation: isolate;
  }

  .hero-banner-link {
    position: absolute;
    inset: 0;
    z-index: 2;
  }

  .hero-backdrop {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    opacity: 1;
    transform: scale(1.03);
    animation: heroImageDrift 5.8s ease forwards;
  }

  .hero-has-poster .hero-backdrop {
    inset: -28px;
    width: calc(100% + 56px);
    height: calc(100% + 56px);
    filter: blur(22px) saturate(.9);
    opacity: .72;
    transform: scale(1.12);
    animation-name: heroPosterBackdropDrift;
  }

  .hero-shade {
    position: absolute;
    inset: 0;
    z-index: 1;
    pointer-events: none;
    background:
        linear-gradient(90deg, rgba(13, 12, 10, .9) 0%, rgba(13, 12, 10, .68) 35%, rgba(13, 12, 10, .18) 68%, rgba(13, 12, 10, .46) 100%),
        linear-gradient(180deg, rgba(12, 11, 10, .1), rgba(12, 11, 10, .72));
  }

  .hero-has-poster .hero-shade {
    background:
        linear-gradient(90deg, rgba(13, 12, 10, .9) 0%, rgba(13, 12, 10, .74) 42%, rgba(13, 12, 10, .35) 74%, rgba(13, 12, 10, .68) 100%),
        linear-gradient(180deg, rgba(12, 11, 10, .12), rgba(12, 11, 10, .74));
  }

  .hero-inner {
    position: relative;
    z-index: 3;
    width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
    height: 100%;
    margin: 0 auto;
    display: grid;
    grid-template-columns: minmax(0, 560px) 220px;
    align-items: center;
    gap: 36px;
    padding: 34px 0 54px;
    animation: heroCopyIn .58s ease both;
  }

  .hero-copy {
    min-width: 0;
    max-width: 560px;
  }

  .hero-eyebrow {
    margin: 0 0 12px;
    color: var(--brand-primary);
    font-size: 14px;
    font-weight: 500;
  }

  .hero-title-link {
    display: block;
    color: inherit;
  }

  h1 {
    margin: 0;
    color: #fff;
    font-size: 36px;
    line-height: 1.18;
    font-weight: 600;
  }

  .hero-desc {
    width: min(500px, 100%);
    margin: 15px 0 0;
    color: rgba(255, 255, 255, .8);
    font-size: 15px;
    line-height: 1.72;
  }

  .hero-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 20px;
  }

  .hero-meta span {
    min-height: 28px;
    display: inline-flex;
    align-items: center;
    padding: 0 10px;
    border: 1px solid rgba(255, 255, 255, .12);
    border-radius: 7px;
    color: rgba(255, 255, 255, .78);
    background: rgba(255, 255, 255, .08);
    font-size: 12px;
    font-weight: 400;
  }

  .hero-entry {
    margin-top: 24px;
  }

  .hero-primary {
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 18px;
    border-radius: 7px;
    color: var(--brand-dark);
    background: var(--brand-primary);
    font-size: 14px;
    font-weight: 600;
    transition: transform .18s ease, background .18s ease;
  }

  .hero-primary:hover {
    transform: translateY(-1px);
    background: #f6cf52;
  }

  .hero-ticket-strip {
    width: min(410px, 100%);
    min-height: 50px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    align-items: center;
    gap: 12px;
    padding: 0 14px;
    border: 1px solid rgba(255, 255, 255, .18);
    border-radius: 8px;
    color: #fff;
    background: rgba(255, 255, 255, .1);
    backdrop-filter: blur(12px);
  }

  .hero-ticket-strip span,
  .hero-ticket-strip strong,
  .hero-ticket-strip em {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .hero-ticket-strip span {
    color: rgba(255, 255, 255, .7);
    font-size: 13px;
    font-weight: 400;
  }

  .hero-ticket-strip strong {
    color: rgba(255, 255, 255, .9);
    font-size: 13px;
    font-weight: 500;
  }

  .hero-ticket-strip em {
    color: var(--brand-primary);
    font-size: 13px;
    font-style: normal;
    font-weight: 600;
  }

  .hero-text-link {
    color: var(--brand-primary);
    font-size: 15px;
    font-weight: 500;
  }

  .hero-text-link::after {
    content: '';
    width: 30px;
    height: 1px;
    display: inline-flex;
    margin-left: 10px;
    vertical-align: middle;
    background: currentColor;
  }

  .hero-hint {
    color: rgba(255, 255, 255, .72);
    font-size: 14px;
  }

  .hero-poster {
    position: relative;
    width: 252px;
    aspect-ratio: 3 / 4;
    display: block;
    overflow: hidden;
    border-radius: 8px;
    background: rgba(255, 255, 255, .08);
    box-shadow: 0 24px 54px rgba(0, 0, 0, .38);
    transition: transform .2s ease, box-shadow .2s ease;
  }

  .hero-poster:hover {
    transform: translateY(-4px);
    box-shadow: 0 30px 62px rgba(0, 0, 0, .46);
  }

  .hero-poster img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  .hero-poster span {
    position: absolute;
    left: 12px;
    right: 12px;
    bottom: 12px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 7px;
    color: #fff;
    background: rgba(0, 0, 0, .46);
    backdrop-filter: blur(10px);
    font-size: 12px;
  }

  .hero-side-list {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-left: 22px;
    border-left: 1px solid rgba(255, 255, 255, .16);
  }

  .hero-side-list > span {
    margin-bottom: 8px;
    color: rgba(255, 255, 255, .54);
    font-size: 12px;
  }

  .hero-side-list a {
    min-height: 36px;
    display: flex;
    align-items: center;
    color: rgba(255, 255, 255, .82);
    border-bottom: 1px solid rgba(255, 255, 255, .08);
    font-size: 13px;
    line-height: 1.35;
    transition: color .16s ease, padding-left .16s ease;
  }

  .hero-side-list a:hover {
    color: var(--brand-primary);
    padding-left: 3px;
  }

  .hero-layout-poster-right {
    .hero-inner {
      grid-template-columns: minmax(0, 520px) 252px 220px;
      justify-content: start;
    }

    .hero-copy {
      order: 1;
    }

    .hero-side-list {
      order: 3;
    }

    .hero-poster {
      order: 2;
    }
  }

  .hero-layout-editorial {
    .hero-inner {
      grid-template-columns: minmax(0, 560px);
      justify-content: start;
    }

    .hero-copy {
      max-width: 560px;
    }

    h1 {
      font-size: 34px;
      letter-spacing: 0;
    }

    .hero-side-list {
      display: none;
    }
  }

  .hero-layout-center-stage {
    .hero-inner {
      grid-template-columns: minmax(0, 560px);
      justify-content: center;
    }

    .hero-copy {
      text-align: center;
    }

    .hero-meta {
      justify-content: center;
    }

    .hero-entry {
      display: flex;
      justify-content: center;
    }

    .hero-side-list {
      display: none;
    }
  }

  .hero-layout-poster-feature {
    .hero-inner {
      grid-template-columns: minmax(0, 520px) 278px;
      justify-content: center;
      gap: 58px;
    }

    .hero-copy {
      text-align: left;
    }

    .hero-side-list {
      display: none;
    }

    .hero-poster {
      width: 278px;
      border-radius: 4px;
    }
  }

  .hero-layout-poster-minimal {
    .hero-inner {
      grid-template-columns: minmax(0, 500px) 280px;
      justify-content: space-between;
      gap: 64px;
    }

    .hero-side-list {
      display: none;
    }

    .hero-poster {
      width: 280px;
      border-radius: 4px;
    }
  }

  .hero-layout-family {
    .hero-inner {
      grid-template-columns: minmax(0, 520px) 190px;
      justify-content: space-between;
    }

    .hero-copy {
      order: 1;
    }

    .hero-side-list {
      order: 2;
      border-left-color: rgba(255, 255, 255, .12);
    }

    h1 {
      font-size: 33px;
    }
  }

  .hero-layout-cinema {
    .hero-inner {
      grid-template-columns: 205px minmax(0, 560px);
      gap: 36px;
    }

    .hero-side-list {
      order: 1;
      padding: 14px 16px;
      border: 1px solid rgba(255, 255, 255, .12);
      border-radius: 8px;
      background: rgba(255, 255, 255, .07);
      backdrop-filter: blur(10px);
    }

    .hero-copy {
      order: 2;
      max-width: 580px;
    }
  }

  .hero-layout-minimal {
    .hero-inner {
      grid-template-columns: minmax(0, 520px);
      justify-content: end;
    }

    .hero-copy {
      order: 1;
    }

    .hero-side-list {
      display: none;
    }

    h1 {
      font-size: 32px;
    }
  }
}

@keyframes heroImageDrift {
  from {
    transform: scale(1.05);
  }
  to {
    transform: scale(1.01);
  }
}

@keyframes heroPosterBackdropDrift {
  from {
    transform: scale(1.16);
  }
  to {
    transform: scale(1.1);
  }
}

@keyframes heroCopyIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-container {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  margin: 0 auto;
  padding: 28px 0 44px;
}

.category,
.diffrentType,
.movie-showcase {
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.category {
  padding: 18px 20px;

  .category-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 14px;

    span {
      color: var(--brand-dark);
      font-size: 20px;
      font-weight: 800;
    }

    strong {
      color: var(--brand-muted);
      font-size: 14px;
      font-weight: 500;
    }
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 10px;
  }

  li a {
    min-height: 58px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 10px;
    border-radius: 8px;
    background: #f8f3e7;
    color: var(--brand-text);
  }

  .category-copy {
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .category-name,
  em {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .category-name {
    color: var(--brand-dark);
    font-size: 15px;
    font-weight: 800;
  }

  em {
    margin-top: 2px;
    color: var(--brand-muted);
    font-size: 12px;
    font-style: normal;
  }
}

.category-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #171612;
  position: relative;
  overflow: hidden;

  .icon-mark {
    width: 18px;
    height: 18px;
    display: block;
    border: 3px solid var(--brand-primary);
    border-radius: 6px;
  }
}

.movie-showcase {
  margin-top: 18px;
  padding: 24px;

  .showcase-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;

    span {
      color: var(--brand-primary-strong);
      font-size: 13px;
      font-weight: 900;
    }

    h2 {
      margin: 6px 0 0;
      color: var(--brand-dark);
      font-size: 24px;
      line-height: 1.1;
    }

    a {
      height: 34px;
      display: inline-flex;
      align-items: center;
      padding: 0 12px;
      border-radius: 999px;
      background: #f8f3e7;
      color: var(--brand-muted);
      font-size: 14px;
      font-weight: 800;
    }
  }

  .showcase-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 14px;
  }

  .showcase-card {
    min-width: 0;
    display: block;
    overflow: hidden;
    border-radius: 8px;
    background: #f8f3e7;

    img {
      width: 100%;
      aspect-ratio: 3 / 4;
      display: block;
      object-fit: cover;
    }

    strong,
    span,
    em {
      display: block;
      margin: 0 10px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      margin-top: 10px;
      color: var(--brand-dark);
      font-size: 14px;
    }

    span {
      margin-top: 5px;
      color: var(--brand-muted);
      font-size: 12px;
    }

    em {
      margin-top: 7px;
      margin-bottom: 12px;
      color: var(--brand-primary-strong);
      font-style: normal;
      font-weight: 900;
    }
  }
}

.diffrentType {
  margin-top: 18px;
  padding: 24px;

  .name {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: var(--brand-dark);
    font-size: 23px;
    line-height: 1.2;
    font-weight: 800;

    .more {
      height: 34px;
      line-height: 34px;
      padding: 0 12px;
      border-radius: 999px;
      background: #f8f3e7;
      color: var(--brand-muted);
      font-size: 14px;
    }
  }
}

.box {
  display: flex;
  gap: 22px;
  margin-top: 18px;

  .box-left {
    flex: 0 0 244px;
    height: 326px;
    position: relative;
    overflow: hidden;
    border-radius: 10px;
    background: #f7f4e8;

    img {
      width: 100%;
      height: 100%;
      position: absolute;
      inset: 0;
      object-fit: cover;
    }
  }

  .box-right {
    flex: 1;
    min-width: 0;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }
}

.rtLink {
  height: 148px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 10px;
  overflow: hidden;

  a {
    display: flex;
    height: 100%;
    color: inherit;
  }

  img {
    width: 102px;
    height: 132px;
    display: block;
    border-radius: 8px;
    object-fit: cover;
  }

  .info {
    min-width: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 2px 0 0 14px;
  }

  .img-title,
  .local,
  .showTime {
    overflow: hidden;
    display: -webkit-box;
    -webkit-box-orient: vertical;
  }

  .img-title {
    -webkit-line-clamp: 2;
    color: var(--brand-text);
    font-size: 14px;
    line-height: 20px;
    font-weight: 700;
  }

  .local,
  .showTime {
    -webkit-line-clamp: 2;
    margin-top: 14px;
    color: var(--brand-muted);
    font-size: 12px;
  }

  .price {
    margin-top: auto;
    color: var(--brand-primary-strong);
    font-size: 19px;
    font-weight: 800;
  }
}

.empty-section {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 18px;
  color: var(--brand-muted);
  border: 1px dashed var(--brand-border);
  background: #fffaf0;
}

@media (max-width: 980px) {
  .home-hero {
    min-height: 420px;

    .carousel-lamp,
    .hero-slide,
    :deep(.el-carousel__container) {
      height: 420px;
    }

    .hero-inner {
      grid-template-columns: minmax(0, 1fr);
      gap: 24px;
      padding-bottom: 48px;
    }

    .hero-layout-poster-right .hero-inner,
    .hero-layout-poster-feature .hero-inner,
    .hero-layout-poster-minimal .hero-inner,
    .hero-layout-editorial .hero-inner,
    .hero-layout-center-stage .hero-inner,
    .hero-layout-family .hero-inner,
    .hero-layout-cinema .hero-inner,
    .hero-layout-minimal .hero-inner {
      grid-template-columns: minmax(0, 1fr);
      justify-content: initial;
      gap: 24px;
    }

    .hero-layout-center-stage .hero-copy {
      justify-self: auto;
      text-align: left;
    }

    .hero-layout-center-stage .hero-meta,
    .hero-layout-center-stage .hero-entry {
      justify-content: flex-start;
    }

    .hero-layout-poster-right .hero-copy,
    .hero-layout-poster-feature .hero-copy,
    .hero-layout-poster-minimal .hero-copy,
    .hero-layout-family .hero-copy,
    .hero-layout-cinema .hero-copy,
    .hero-layout-minimal .hero-copy {
      order: 1;
    }

    .hero-poster {
      display: none;
    }

    .hero-side-list,
    .hero-layout-cinema .hero-side-list {
      display: none;
    }

    h1 {
      font-size: 31px;
    }
  }

  .category ul,
  .movie-showcase .showcase-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .box {
    display: block;

    .box-left {
      display: none;
    }

    .box-right {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
}

@media (max-width: 640px) {
  .home-hero {
    min-height: 420px;

    .carousel-lamp,
    .hero-slide,
    :deep(.el-carousel__container) {
      height: 420px;
    }

    .hero-inner {
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      align-items: flex-start;
      padding-bottom: 48px;
    }

    .hero-layout-poster-right .hero-inner,
    .hero-layout-poster-feature .hero-inner,
    .hero-layout-poster-minimal .hero-inner,
    .hero-layout-editorial .hero-inner,
    .hero-layout-center-stage .hero-inner,
    .hero-layout-family .hero-inner,
    .hero-layout-cinema .hero-inner,
    .hero-layout-minimal .hero-inner {
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      align-items: flex-start;
      gap: 0;
      padding-bottom: 48px;
    }

    .hero-copy {
      max-width: 100%;
    }

    .hero-layout-center-stage .hero-copy {
      text-align: left;
    }

    .hero-layout-center-stage .hero-meta,
    .hero-layout-center-stage .hero-entry {
      justify-content: flex-start;
    }

    h1 {
      font-size: 27px;
    }

    .hero-desc {
      font-size: 14px;
      line-height: 1.66;
    }

    .hero-ticket-strip {
      grid-template-columns: 1fr;
      gap: 4px;
      align-items: flex-start;
      padding: 10px 12px;
    }
  }

  .category .category-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .category ul,
  .movie-showcase .showcase-grid,
  .box .box-right {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <Header></Header>
  <main class="movie-channel">
    <section class="movie-hero" v-if="heroMovie">
      <img class="hero-bg" :src="heroMovie.poster" alt="" @error="hideBrokenImage">
      <div class="hero-mask"></div>
      <div class="hero-inner">
        <div class="hero-copy">
          <span class="eyebrow">电影专区 · {{ currentCityText }}</span>
          <h1>{{ heroTitle }}</h1>
          <p>{{ heroDescription }}</p>
          <div class="hero-actions">
            <router-link class="primary-action" :to="getMovieRoute(heroMovie)">查看影片</router-link>
            <a class="secondary-action" href="#movieList">浏览片单</a>
          </div>
        </div>
        <router-link class="hero-poster" :to="getMovieRoute(heroMovie)">
          <img :src="heroMovie.poster" :alt="heroMovie.plainTitle" @error="hideBrokenImage">
          <div>
            <strong v-html="sanitizeHighlight(heroMovie.title)"></strong>
            <span v-if="getMovieSearchLine(heroMovie)" v-html="sanitizeHighlight(getMovieSearchLine(heroMovie))"></span>
          </div>
        </router-link>
      </div>
    </section>
    <section class="empty-hero" v-else>
      <span>电影专区 · {{ currentCityText }}</span>
      <strong>{{ emptyHeroTitle }}</strong>
      <p>{{ emptyHeroDescription }}</p>
    </section>

    <section class="movie-toolbar">
      <div>
        <span>{{ toolbarKicker }}</span>
        <h2>{{ pageTitle }}</h2>
      </div>
      <div class="toolbar-actions">
        <router-link v-if="keyword" class="clear-search" :to="{name: 'movieIndex'}">清除搜索</router-link>
        <div class="filter-tabs">
          <button
              v-for="item in filters"
              :key="item.value"
              type="button"
              :class="{active: activeFilter === item.value}"
              @click="changeFilter(item.value)"
          >{{ item.label }}</button>
        </div>
      </div>
    </section>

    <section id="movieList" class="movie-grid" v-loading="loading">
      <article class="movie-card" v-for="item in movieItems" :key="item.programId">
        <router-link class="poster" :to="getMovieRoute(item)">
          <img :src="item.poster" :alt="item.plainTitle" @error="hideBrokenImage">
          <span class="status" v-if="getStatusText(item)">{{ getStatusText(item) }}</span>
        </router-link>
        <div class="movie-info">
          <router-link class="movie-title" :to="getMovieRoute(item)" v-html="sanitizeHighlight(item.title)"></router-link>
          <p v-if="getMovieSearchLine(item)" v-html="sanitizeHighlight(getMovieSearchLine(item))"></p>
          <div class="meta-row">
            <span v-if="formatDate(item.showTime)">{{ formatDate(item.showTime) }}</span>
            <strong v-if="item.minPrice">¥{{ item.minPrice }}起</strong>
          </div>
          <router-link class="buy-link" :to="getMovieRoute(item)">影片详情</router-link>
        </div>
      </article>
      <div class="empty" v-if="!loading && movieItems.length === 0">
        {{ emptyText }}
      </div>
    </section>

    <div class="pager" v-if="total > pageParams.pageSize">
      <span class="pager-info">共 {{ total }} 部影片</span>
      <el-pagination
          small
          background
          layout="prev, pager, next"
          v-model:current-page="pageParams.pageNumber"
          :page-size="pageParams.pageSize"
          :pager-count="5"
          :total="total"
          :disabled="loading"
          @current-change="handlePageChange"
      />
    </div>
  </main>
  <Footer></Footer>
</template>

<script setup>
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import useCityStore from '@/store/modules/city'
import {getMoviePage} from '@/api/movie'
import {useMitt} from '@/utils/index'
import {resolveMovieImageUrl} from '@/utils/movie-media'
import {getCityDisplayName} from '@/utils/city'
import {useRoute} from 'vue-router'

const filters = [
  {label: '全部', value: ''},
  {label: '正在热映', value: 23},
  {label: '预售待映', value: 24},
]

const emitter = useMitt()
const route = useRoute()
const cityStore = useCityStore()
const activeFilter = ref('')
const loading = ref(false)
const total = ref(0)
const movieRows = ref([])
const currentCity = ref(null)
const keyword = ref('')
const pageParams = reactive({
  pageNumber: 1,
  pageSize: 10,
})

const movieItems = computed(() => movieRows.value)
const heroMovie = computed(() => movieItems.value[0] || null)
const heroTitle = computed(() => keyword.value ? `电影搜索：${keyword.value}` : '热映与预售影片')
const heroDescription = computed(() => {
  return keyword.value
      ? '按当前城市从真实电影库中筛选片名、导演、主演和类型，结果只来自已上架且可售的电影数据。'
      : '按当前真实城市展示数据库中可购影片，包含真实海报、预告、剧照、演职员和可抢票场次。'
})
const pageTitle = computed(() => {
  if (keyword.value) {
    return `搜索“${keyword.value}”`
  }
  const item = filters.find(filter => filter.value === activeFilter.value)
  return item?.value ? item.label : '全部电影'
})
const currentCityText = computed(() => {
  if (!currentCity.value) {
    return '定位中'
  }
  return getCityDisplayName(currentCity.value) || currentCity.value.name
})
const toolbarKicker = computed(() => keyword.value ? `电影搜索 · ${currentCityText.value}` : currentCityText.value)
const emptyText = computed(() => {
  return keyword.value
      ? `没有找到与“${keyword.value}”匹配的真实可售电影。`
      : '当前城市没有真实可售电影，请确认已执行电影种子 SQL 或在管理端上架场次。'
})
const emptyHeroTitle = computed(() => keyword.value ? '没有找到匹配的真实电影' : '当前城市暂无真实电影数据')
const emptyHeroDescription = computed(() => {
  return keyword.value
      ? `当前城市没有与“${keyword.value}”匹配的已上架电影。`
      : '请切换城市，或在管理端确认该城市已有上架影片和可售场次。'
})

onMounted(() => {
  keyword.value = normalizeKeyword(route.query.keyword)
  emitter.on('cityChange', handleCityChange)
  initCity()
})

onUnmounted(() => {
  emitter.off('cityChange', handleCityChange)
})

watch(
    () => route.query.keyword,
    (value) => {
      const nextKeyword = normalizeKeyword(value)
      if (nextKeyword === keyword.value) {
        return
      }
      keyword.value = nextKeyword
      pageParams.pageNumber = 1
      loadMovies()
    }
)

async function initCity() {
  const selectedCity = await cityStore.initCity({useBrowserLocation: true})
  if (selectedCity) {
    applyCity(selectedCity)
  }
  loadMovies()
}

function applyCity(city, notify = false) {
  const selectedCity = cityStore.applyCity(city)
  if (!selectedCity) {
    return
  }
  currentCity.value = selectedCity
  if (notify) {
    emitter.emit('cityChange', selectedCity)
  }
}

function handleCityChange(city) {
  const cityId = city?.id ?? city?.areaId
  if (cityId === undefined || Number(cityId) === Number(currentCity.value?.id)) {
    return
  }
  applyCity(city)
  pageParams.pageNumber = 1
  loadMovies()
}

function normalizeMovieItem(item) {
  const title = item.movieName || item.title || ''
  const actor = item.actors || item.actor || ''
  return {
    id: item.programId || item.id,
    movieId: item.movieId,
    programId: item.programId || item.id,
    title,
    plainTitle: stripHighlight(title),
    actor,
    plainActor: stripHighlight(actor),
    director: item.director || '',
    genre: item.genre || '',
    poster: resolveMovieImageUrl(item.poster || item.itemPicture, 'poster'),
    showTime: item.nearestShowTime || item.showTime,
    minPrice: item.lowestPrice || item.minPrice,
    releaseStatus: item.releaseStatus,
    cinemaCount: item.cinemaCount,
    screeningCount: item.screeningCount,
  }
}

function hasHighlight(value) {
  return /<em>/i.test(String(value || ''))
}

function getMovieSearchLine(item) {
  if (!item) {
    return ''
  }
  const actorLine = item.actor ? `主演：${item.actor}` : ''
  const directorLine = item.director ? `导演：${item.director}` : ''
  const genreLine = item.genre ? `类型：${item.genre}` : ''
  if (hasHighlight(item.actor)) {
    return actorLine
  }
  if (hasHighlight(item.director)) {
    return directorLine
  }
  if (hasHighlight(item.genre)) {
    return genreLine
  }
  return actorLine || directorLine || genreLine
}

function loadMovies() {
  loading.value = true
  return getMoviePage({
    pageNumber: pageParams.pageNumber,
    pageSize: pageParams.pageSize,
    areaId: currentCity.value?.id || undefined,
    programCategoryId: activeFilter.value || undefined,
    content: keyword.value || undefined,
  }).then(response => {
    const data = response.data || {}
    const rows = data.records || data.list || []
    movieRows.value = rows.map(normalizeMovieItem).filter(item => item.programId && item.title && item.poster)
    total.value = Number(data.total || data.totalSize || movieRows.value.length)
  }).catch(() => {
    movieRows.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function changeFilter(value) {
  activeFilter.value = value
  pageParams.pageNumber = 1
  loadMovies()
}

function handlePageChange() {
  loadMovies()
}

function getMovieRoute(item) {
  return {name: 'movieDetail', params: {id: item.programId || item.id}}
}

function getStatusText(item) {
  if (Number(item.releaseStatus) === 1) return '预售'
  if (Number(item.releaseStatus) === 2) return '热映'
  return ''
}

function formatDate(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) {
    return ''
  }
  const date = new Date(value)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function hideBrokenImage(event) {
  event.target.style.visibility = 'hidden'
}

function normalizeKeyword(value) {
  const target = Array.isArray(value) ? value[0] : value
  return String(target || '').trim()
}

function sanitizeHighlight(value) {
  return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/&lt;em&gt;/gi, '<em>')
      .replace(/&lt;\/em&gt;/gi, '</em>')
}

function stripHighlight(value) {
  return String(value || '').replace(/<\/?em>/gi, '')
}
</script>

<style scoped lang="scss">
.movie-channel {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.movie-hero,
.empty-hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: #111;
  color: #fff;
  box-shadow: var(--brand-shadow);
}

.movie-hero {
  min-height: 360px;
}

.empty-hero {
  min-height: 220px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 32px;
  text-align: center;

  span {
    color: var(--brand-primary);
    font-weight: 900;
  }

  strong {
    font-size: 28px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, .7);
  }
}

.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(.85);
}

.hero-mask {
  position: absolute;
  inset: 0;
  background:
      linear-gradient(90deg, rgba(18, 17, 15, .92), rgba(18, 17, 15, .64) 46%, rgba(18, 17, 15, .2)),
      linear-gradient(180deg, rgba(18, 17, 15, .2), rgba(18, 17, 15, .72));
}

.hero-inner {
  position: relative;
  z-index: 1;
  min-height: 360px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 28px;
  align-items: center;
  padding: 38px;
}

.hero-copy {
  .eyebrow {
    color: var(--brand-primary);
    font-weight: 900;
  }

  h1 {
    width: min(620px, 100%);
    margin: 12px 0 0;
    font-size: 44px;
    line-height: 1.12;
    font-weight: 900;
  }

  p {
    width: min(560px, 100%);
    margin: 16px 0 0;
    color: rgba(255, 255, 255, .78);
    font-size: 16px;
    line-height: 1.8;
  }
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action,
.buy-link {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 800;
}

.primary-action,
.buy-link {
  color: var(--brand-dark);
  background: var(--brand-primary);
}

.secondary-action {
  color: #fff;
  border: 1px solid rgba(255, 255, 255, .36);
}

.hero-poster {
  display: block;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, .18);
  border-radius: 12px;
  background: rgba(255, 255, 255, .08);

  img {
    width: 100%;
    height: 320px;
    display: block;
    object-fit: cover;
  }

  div {
    padding: 12px 14px 14px;
  }

  strong,
  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    margin-top: 4px;
    color: rgba(255, 255, 255, .68);
    font-size: 13px;
  }
}

.movie-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin: 28px 0 16px;

  span {
    color: var(--brand-primary-strong);
    font-size: 13px;
    font-weight: 900;
  }

  h2 {
    margin: 6px 0 0;
    color: var(--brand-dark);
    font-size: 28px;
    line-height: 1.1;
  }
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.clear-search {
  height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: #fff;
  color: var(--brand-primary-strong);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.filter-tabs {
  display: flex;
  gap: 8px;

  button {
    height: 34px;
    padding: 0 13px;
    border: 1px solid var(--brand-line);
    border-radius: 999px;
    background: var(--brand-card);
    color: var(--brand-muted);
    font-weight: 800;
    cursor: pointer;

    &.active {
      color: var(--brand-dark);
      background: var(--brand-primary);
      border-color: var(--brand-primary);
    }
  }
}

.movie-grid {
  min-height: 260px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.movie-card {
  overflow: hidden;
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.poster {
  position: relative;
  display: block;
  aspect-ratio: 3 / 4;
  background: #f2ecdf;

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  .status {
    position: absolute;
    left: 10px;
    top: 10px;
    height: 26px;
    display: inline-flex;
    align-items: center;
    padding: 0 10px;
    border-radius: 999px;
    color: var(--brand-dark);
    background: var(--brand-primary);
    font-size: 12px;
    font-weight: 900;
  }
}

.movie-info {
  padding: 14px;

  .movie-title {
    display: block;
    overflow: hidden;
    color: var(--brand-dark);
    font-size: 16px;
    line-height: 1.35;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    height: 40px;
    margin: 8px 0 0;
    overflow: hidden;
    color: var(--brand-muted);
    font-size: 13px;
    line-height: 20px;
  }
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 12px 0;
  color: var(--brand-muted);
  font-size: 13px;

  strong {
    color: var(--brand-primary-strong);
    white-space: nowrap;
  }
}

.buy-link {
  width: 100%;
}

:deep(em) {
  color: var(--brand-primary-strong);
  font-style: normal;
  font-weight: 900;
}

.empty {
  grid-column: 1 / -1;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-muted);
  border: 1px dashed var(--brand-border);
  border-radius: var(--radius-lg);
  background: #fffaf0;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 26px;

  .pager-info {
    color: var(--brand-muted);
    font-size: 13px;
  }
}

:deep(.pager .el-pagination) {
  --el-pagination-button-width: 30px;
  --el-pagination-button-height: 30px;
  --el-pagination-border-radius: 7px;
}

:deep(.pager .el-pager li),
:deep(.pager .btn-prev),
:deep(.pager .btn-next) {
  font-weight: 500;
}

:deep(.pager .el-pager li.is-active) {
  background: var(--brand-primary) !important;
  color: var(--brand-dark) !important;
}

@media (max-width: 980px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }

  .hero-poster {
    display: none;
  }

  .movie-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .movie-channel {
    width: calc(100% - 24px);
  }

  .hero-inner {
    padding: 28px 22px;
  }

  .hero-copy h1,
  .empty-hero strong {
    font-size: 32px;
  }

  .movie-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .movie-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .movie-info {
    padding: 12px;
  }

  .pager {
    flex-direction: column;
    gap: 10px;
  }
}
</style>

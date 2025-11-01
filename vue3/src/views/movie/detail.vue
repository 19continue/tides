<template>
  <Header></Header>
  <main class="movie-detail-page" v-loading="loading">
    <section class="empty-detail" v-if="!loading && !movie.programId">
      <strong>没有查询到真实电影资料</strong>
      <p>请返回电影列表，或切换城市后重新选择可售影片。</p>
      <router-link to="/movie/index">返回电影列表</router-link>
    </section>

    <template v-else>
      <section class="movie-hero-detail">
        <img v-if="moviePoster" class="hero-bg" :src="moviePoster" alt="" @error="hideBrokenImage">
        <div class="hero-mask"></div>
        <div class="hero-content">
          <div class="poster-frame" v-if="moviePoster">
            <img :src="moviePoster" :alt="movieTitle" @error="hideBrokenImage">
            <button type="button" class="play-button" :disabled="!featuredTrailer" @click="openMedia(featuredTrailer)">
              预告片
            </button>
          </div>
          <div class="hero-info">
            <div class="movie-tags">
              <span v-if="releaseStatusText">{{ releaseStatusText }}</span>
              <span v-if="movie.genre">{{ movie.genre }}</span>
              <span v-if="currentCityText">{{ currentCityText }}</span>
            </div>
            <h1>{{ movieTitle }}</h1>
            <p class="alias" v-if="movie.movieAlias">{{ movie.movieAlias }}</p>
            <p class="summary" v-if="movieIntro">{{ movieIntro }}</p>
            <div class="score-strip" v-if="scoreItems.length">
              <div v-for="item in scoreItems" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
            <div class="hero-actions">
              <button type="button" class="primary-action" :disabled="!selectedScreening" @click="buySelected">
                {{ selectedScreening ? '选座购票' : '暂无可购场次' }}
              </button>
              <a class="secondary-action" href="#schedule">查看影院场次</a>
            </div>
          </div>
        </div>
      </section>

      <section class="quick-info" v-if="quickInfoRows.length">
        <div v-for="item in quickInfoRows" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <div class="content-grid">
        <div class="main-column">
          <section class="section-block" v-if="movieIntro">
            <div class="section-head">
              <span>简介</span>
              <h2>电影介绍</h2>
            </div>
            <p class="long-desc">{{ movieIntro }}</p>
          </section>

          <section class="section-block">
            <div class="section-head inline-head">
              <div>
                <span>演职员</span>
                <h2>导演与演员</h2>
              </div>
              <button v-if="artistList.length > visibleArtistList.length" type="button" @click="artistDialogVisible = true">
                全部演职员
              </button>
            </div>
            <div class="artist-grid" v-if="visibleArtistList.length">
              <article class="artist-card" v-for="item in visibleArtistList" :key="item.key">
                <div v-if="item.avatar" class="artist-avatar">
                  <img :src="resolveMovieImageUrl(item.avatar, 'avatar')" :alt="item.name" @error="hideBrokenImage">
                </div>
                <div class="artist-meta">
                  <strong>{{ item.name }}</strong>
                  <span v-if="item.role" class="artist-role">{{ item.role }}</span>
                  <small v-if="item.professionText" class="artist-type">{{ item.professionText }}</small>
                </div>
              </article>
            </div>
            <div class="empty-inline" v-else>暂无演职员数据</div>
          </section>

          <section class="section-block">
            <div class="section-head">
              <span>预告与剧照</span>
              <h2>官方物料</h2>
            </div>
            <div class="media-grid" v-if="mediaItems.length">
              <button
                  v-for="item in mediaItems"
                  :key="item.id || item.title"
                  type="button"
                  class="media-card"
                  @click="openMedia(item)"
              >
                <span class="media-cover">
                  <img
                      v-if="mediaCoverUrl(item)"
                      :src="mediaCoverUrl(item)"
                      :alt="item.title"
                      @error="hideBrokenImage"
                  >
                  <span v-if="isMediaVideo(item)" class="media-play-mark" aria-hidden="true"></span>
                </span>
                <span class="media-type">{{ mediaTypeText(item.mediaType) }}</span>
                <strong v-if="item.title || movie.movieName">{{ item.title || movieTitle }}</strong>
              </button>
            </div>
            <div class="empty-inline" v-else>暂无预告片或剧照</div>
          </section>

          <section id="schedule" class="section-block">
            <div class="section-head">
              <span>购票</span>
              <h2>影院与场次</h2>
            </div>
            <div class="day-tabs" v-if="showDays.length">
              <button
                  v-for="day in showDays"
                  :key="day"
                  type="button"
                  :class="{active: selectedDay === day}"
                  @click="selectDay(day)"
              >{{ formatDay(day) }}</button>
            </div>
            <div class="schedule-layout" v-if="cinemaList.length">
              <aside class="cinema-list">
                <button
                    v-for="cinema in cinemaList"
                    :key="cinema.cinemaId"
                    type="button"
                    :class="{active: Number(selectedCinemaId) === Number(cinema.cinemaId)}"
                    @click="selectCinema(cinema.cinemaId)"
                >
                  <strong>{{ cinema.cinemaName }}</strong>
                  <span v-if="cinema.cinemaAddress">{{ cinema.cinemaAddress }}</span>
                  <em v-if="cinemaMeta(cinema)">{{ cinemaMeta(cinema) }}</em>
                </button>
              </aside>
              <div class="screening-list">
                <article
                    v-for="item in selectedCinemaScreenings"
                    :key="item.id"
                    :class="{active: selectedScreening && Number(selectedScreening.id) === Number(item.id)}"
                    @click="chooseScreening(item)"
                >
                  <div class="time">
                    <strong>{{ formatTime(item.showTime) }}</strong>
                    <span v-if="formatTime(item.endTime)">{{ formatTime(item.endTime) }}散场</span>
                  </div>
                  <div class="hall">
                    <strong v-if="[item.version, item.language].filter(Boolean).length">{{ [item.version, item.language].filter(Boolean).join(' / ') }}</strong>
                    <span v-if="item.hallName || item.hallType">{{ item.hallName || item.hallType }}</span>
                  </div>
                  <div class="price" v-if="screeningPrice(item)">{{ screeningPrice(item) }}</div>
                  <button type="button" @click.stop="buyScreening(item)">选座</button>
                </article>
              </div>
            </div>
            <div class="empty-schedule" v-else>当前城市暂无可购场次。</div>
          </section>
        </div>

        <aside class="side-column">
          <section class="side-card" v-if="profileRows.length">
            <h3>影片资料</h3>
            <dl>
              <div v-for="item in profileRows" :key="item.label"><dt>{{ item.label }}</dt><dd>{{ item.value }}</dd></div>
            </dl>
          </section>
          <section class="side-card notice-card">
            <h3>购票须知</h3>
            <p>电影票承载指定场次的观影服务，具有时效性和座位库存限制。支付后将锁定所选座位，请按影院、影厅和座位号入场。</p>
          </section>
        </aside>
      </div>
    </template>
  </main>
  <Footer></Footer>

  <el-dialog
      v-model="mediaDialogVisible"
      width="min(1040px, 94vw)"
      class="movie-media-dialog"
      destroy-on-close
      append-to-body
  >
    <template #header>
      <div class="media-dialog-head">
        <div>
          <span>{{ mediaTypeText(activeMedia?.mediaType) }}</span>
          <strong>{{ activeMediaTitle }}</strong>
        </div>
        <button type="button" @click="closeMedia">关闭</button>
      </div>
    </template>
    <div class="media-viewer">
      <button
          type="button"
          class="viewer-nav viewer-prev"
          :disabled="mediaItems.length < 2"
          @click="showPreviousMedia"
      >上一张</button>
      <div class="viewer-stage">
        <video
            v-if="activeMediaIsVideo"
            :key="activeMediaUrl"
            :src="activeMediaUrl"
            :poster="activeMediaCoverUrl"
            controls
            autoplay
            playsinline
        ></video>
        <img
            v-else-if="activeMediaUrl && !isBrokenMedia(activeMediaUrl)"
            :key="activeMediaUrl"
            :src="activeMediaUrl"
            :alt="activeMediaTitle"
            @error="markMediaImageBroken(activeMediaUrl)"
        >
        <div v-else class="media-unavailable">资源暂时不可访问</div>
      </div>
      <button
          type="button"
          class="viewer-nav viewer-next"
          :disabled="mediaItems.length < 2"
          @click="showNextMedia"
      >下一张</button>
      <div class="media-thumbs" v-if="mediaItems.length > 1">
        <button
            v-for="(item, index) in mediaItems"
            :key="item.id || item.title || index"
            type="button"
            :class="{active: index === activeMediaIndex}"
            @click="selectMedia(index)"
        >
          <img v-if="mediaCoverUrl(item)" :src="mediaCoverUrl(item)" :alt="item.title" @error="hideBrokenImage">
          <span v-if="isMediaVideo(item)" class="thumb-play-mark" aria-hidden="true"></span>
        </button>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="artistDialogVisible" width="860px" class="movie-artist-dialog" title="全部演职员">
    <div class="artist-grid dialog-grid">
      <article class="artist-card" v-for="item in artistList" :key="`dialog-${item.key}`">
        <div v-if="item.avatar" class="artist-avatar">
          <img :src="resolveMovieImageUrl(item.avatar, 'avatar')" :alt="item.name" @error="hideBrokenImage">
        </div>
        <div class="artist-meta">
          <strong>{{ item.name }}</strong>
          <span v-if="item.role" class="artist-role">{{ item.role }}</span>
          <small v-if="item.professionText" class="artist-type">{{ item.professionText }}</small>
        </div>
      </article>
    </div>
  </el-dialog>
</template>

<script setup>
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import useCityStore from '@/store/modules/city'
import {getMovieDetail, getMovieScreeningList} from '@/api/movie'
import {useMitt} from '@/utils/index'
import {isMovieVideoUrl, resolveMovieImageUrl} from '@/utils/movie-media'
import {getCityDisplayName} from '@/utils/city'

const route = useRoute()
const router = useRouter()
const emitter = useMitt()
const cityStore = useCityStore()
const loading = ref(false)
const movie = ref({})
const selectedDay = ref('')
const selectedCinemaId = ref('')
const selectedScreening = ref(null)
const mediaDialogVisible = ref(false)
const artistDialogVisible = ref(false)
const activeMediaIndex = ref(0)
const brokenMediaUrls = ref(new Set())
const currentCity = ref(null)

const posterMediaUrl = computed(() => {
  return (movie.value.mediaList || []).find(item => Number(item.mediaType) === 1)?.mediaUrl || ''
})
const movieTitle = computed(() => movie.value.movieName ? `《${movie.value.movieName}》` : '电影详情')
const moviePoster = computed(() => resolveMovieImageUrl(movie.value.poster || posterMediaUrl.value, 'poster'))
const releaseStatusText = computed(() => {
  if (Number(movie.value.releaseStatus) === 2) return '正在热映'
  if (Number(movie.value.releaseStatus) === 3) return '已下映'
  if (Number(movie.value.releaseStatus) === 1) return '预售中'
  return ''
})
const currentCityText = computed(() => {
  if (!currentCity.value) {
    return ''
  }
  return getCityDisplayName(currentCity.value) || currentCity.value.name
})
const movieIntro = computed(() => movie.value.longDescription || movie.value.description || '')
const scoreItems = computed(() => {
  const items = []
  if (hasRealValue(movie.value.ratingScore)) {
    items.push({label: '评分', value: movie.value.ratingScore})
  }
  const wantWatchCount = formatCount(movie.value.wantWatchCount)
  if (wantWatchCount) {
    items.push({label: '想看', value: wantWatchCount})
  }
  const cinemaCount = movie.value.cinemaCount || cinemaList.value.length
  if (hasRealValue(cinemaCount)) {
    items.push({label: '影院', value: cinemaCount})
  }
  const lowestPrice = formatMoviePrice(movie.value.lowestPrice)
  if (lowestPrice) {
    items.push({label: '最低价', value: lowestPrice})
  }
  return items
})
const quickInfoRows = computed(() => {
  const rows = []
  if (movie.value.director) {
    rows.push({label: '导演', value: movie.value.director})
  }
  if (movie.value.actors) {
    rows.push({label: '主演', value: movie.value.actors})
  }
  if (movie.value.durationMinutes) {
    rows.push({label: '片长', value: `${movie.value.durationMinutes}分钟`})
  }
  const regionLanguage = [movie.value.region, movie.value.language].filter(Boolean).join(' / ')
  if (regionLanguage) {
    rows.push({label: '地区/语言', value: regionLanguage})
  }
  return rows
})
const profileRows = computed(() => {
  const rows = []
  const releaseDate = formatDate(movie.value.releaseDate)
  if (releaseDate) {
    rows.push({label: '上映日期', value: releaseDate})
  }
  if (movie.value.producer) {
    rows.push({label: '出品方', value: movie.value.producer})
  }
  if (movie.value.distributor) {
    rows.push({label: '发行方', value: movie.value.distributor})
  }
  if (movie.value.ageTips) {
    rows.push({label: '年龄提示', value: movie.value.ageTips})
  }
  return rows
})
const showDays = computed(() => {
  const values = (movie.value.screeningList || []).map(item => normalizeDateKey(item.showDayTime || item.showTime)).filter(Boolean)
  return Array.from(new Set(values))
})
const dayScreenings = computed(() => {
  const list = movie.value.screeningList || []
  if (!selectedDay.value) return list
  return list.filter(item => normalizeDateKey(item.showDayTime || item.showTime) === selectedDay.value)
})
const cinemaList = computed(() => {
  const map = new Map()
  dayScreenings.value.forEach(item => {
    const key = item.cinemaId || item.cinemaName
    if (!key || !item.cinemaName) return
    const exists = map.get(key) || {
      cinemaId: item.cinemaId,
      cinemaName: item.cinemaName,
      cinemaAddress: item.cinemaAddress || '',
      lowestPrice: item.lowestPrice || movie.value.lowestPrice || 0,
      count: 0,
    }
    exists.count += 1
    const price = Number(item.lowestPrice || 0)
    if (price && (!exists.lowestPrice || price < Number(exists.lowestPrice))) {
      exists.lowestPrice = item.lowestPrice
    }
    map.set(key, exists)
  })
  return Array.from(map.values())
})
const selectedCinemaScreenings = computed(() => {
  if (!selectedCinemaId.value) return dayScreenings.value
  return dayScreenings.value.filter(item => Number(item.cinemaId) === Number(selectedCinemaId.value))
})
const genericRoleNames = new Set(['演员', '主演', '主创', '配音', '声演', '演员表'])
const artistList = computed(() => {
  return (movie.value.artistList || []).map(item => {
    const role = artistRoleText(item)
    const professionText = artistProfessionText(item)
    return {
      key: `artist-${item.artistId || item.artistName || item.englishName}`,
      name: item.artistName || item.englishName || '',
      role,
      professionText: professionText && professionText !== role ? professionText : '',
      avatar: item.avatar,
    }
  }).filter(item => item.name)
})
const visibleArtistList = computed(() => artistList.value.slice(0, 8))
const mediaItems = computed(() => {
  return (movie.value.mediaList || []).filter(item => mediaUrl(item))
})
const featuredTrailer = computed(() => mediaItems.value.find(item => Number(item.mediaType) === 3 && item.mediaUrl))
const activeMedia = computed(() => mediaItems.value[activeMediaIndex.value] || null)
const activeMediaTitle = computed(() => activeMedia.value?.title || movieTitle.value)
const activeMediaUrl = computed(() => mediaUrl(activeMedia.value))
const activeMediaCoverUrl = computed(() => mediaCoverUrl(activeMedia.value))
const activeMediaIsVideo = computed(() => isMediaVideo(activeMedia.value))

onMounted(() => {
  emitter.on('cityChange', handleCityChange)
  initCity()
})

onUnmounted(() => {
  emitter.off('cityChange', handleCityChange)
})

watch(
    () => route.params.id,
    id => {
      if (currentCity.value) {
        loadMovieDetail(id)
      }
    }
)

async function initCity() {
  const selectedCity = await cityStore.initCity({useBrowserLocation: true})
  if (selectedCity) {
    currentCity.value = cityStore.applyCity(selectedCity)
  }
  loadMovieDetail(route.params.id)
}

function handleCityChange(city) {
  const cityId = city?.id ?? city?.areaId
  if (cityId === undefined || Number(cityId) === Number(currentCity.value?.id)) {
    return
  }
  currentCity.value = cityStore.applyCity(city)
  loadMovieDetail(route.params.id)
}

function loadMovieDetail(programId) {
  if (!programId) return
  loading.value = true
  getMovieDetail({programId: Number(programId), areaId: currentCity.value?.id || undefined}).then(response => {
    movie.value = response.data || {}
    return loadScreeningList(programId)
  }).catch(() => {
    movie.value = {}
    resetSelection()
    ElMessage.error('电影详情接口暂不可用')
  }).finally(() => {
    loading.value = false
  })
}

function loadScreeningList(programId) {
  return getMovieScreeningList({
    programId: Number(programId),
    areaId: currentCity.value?.id || undefined,
  }).then(response => {
    movie.value = {
      ...movie.value,
      screeningList: response.data || [],
    }
    applyDefaultSelection()
  }).catch(() => {
    movie.value = {
      ...movie.value,
      screeningList: [],
    }
    applyDefaultSelection()
  })
}

function applyDefaultSelection() {
  selectedDay.value = showDays.value[0] || ''
  selectedCinemaId.value = cinemaList.value[0]?.cinemaId || ''
  selectedScreening.value = selectedCinemaScreenings.value[0] || dayScreenings.value[0] || null
}

function resetSelection() {
  selectedDay.value = ''
  selectedCinemaId.value = ''
  selectedScreening.value = null
}

function selectDay(day) {
  selectedDay.value = day
  selectedCinemaId.value = cinemaList.value[0]?.cinemaId || ''
  selectedScreening.value = selectedCinemaScreenings.value[0] || null
}

function selectCinema(cinemaId) {
  selectedCinemaId.value = cinemaId
  selectedScreening.value = selectedCinemaScreenings.value[0] || null
}

function chooseScreening(screening) {
  selectedScreening.value = screening
}

function buyScreening(screening) {
  chooseScreening(screening)
  buySelected()
}

function buySelected() {
  if (!selectedScreening.value) {
    ElMessage.warning('当前电影暂无可购场次')
    return
  }
  const screening = selectedScreening.value
  const detailList = {
    id: movie.value.programId,
    programId: movie.value.programId,
    movieId: movie.value.movieId,
    title: movieTitle.value,
    parentProgramCategoryId: 22,
    parentProgramCategoryName: '电影',
    itemPicture: moviePoster.value,
    permitChooseSeat: '1',
    permitRefund: '0',
    refundExplain: '电影票承载指定场次的观影服务，具有时效性和座位库存限制，一旦订购成功，不支持随意退换。',
    relNameTicketEntrance: '1',
    relNameTicketEntranceExplain: '本项目需要实名制购票及入场。',
    electronicDeliveryTicket: '1',
    electronicDeliveryTicketExplain: '支付成功后前往票夹查看取票码或入场凭证。',
    electronicInvoice: '1',
    electronicInvoiceExplain: '观影结束后可在订单详情页提交发票申请。',
    chooseSeatExplain: '支持影厅座位图选座，请按所选座位入场观影。',
    showTime: screening.showTime,
    showWeekTime: screening.showWeekTime,
    areaName: screening.cinemaAddress,
    place: [screening.cinemaName, screening.hallName].filter(Boolean).join(' '),
    screeningId: screening.id,
    selectedMovieScreening: screening,
    performanceDuration: movie.value.durationMinutes ? `${movie.value.durationMinutes}分钟` : '',
    mainActor: movie.value.actors,
    minPerformanceDuration: [screening.version, screening.language].filter(Boolean).join(' / '),
  }
  router.push({
    path: '/order/seatSelect',
    state: {
      detailList: JSON.stringify(detailList),
      screeningId: screening.id,
    },
  })
}

function openMedia(media) {
  if (!media) {
    return
  }
  const index = mediaItems.value.findIndex(item => mediaKey(item) === mediaKey(media))
  activeMediaIndex.value = index >= 0 ? index : 0
  mediaDialogVisible.value = true
}

function closeMedia() {
  mediaDialogVisible.value = false
}

function selectMedia(index) {
  if (index < 0 || index >= mediaItems.value.length) {
    return
  }
  activeMediaIndex.value = index
}

function showPreviousMedia() {
  if (!mediaItems.value.length) {
    return
  }
  activeMediaIndex.value = (activeMediaIndex.value - 1 + mediaItems.value.length) % mediaItems.value.length
}

function showNextMedia() {
  if (!mediaItems.value.length) {
    return
  }
  activeMediaIndex.value = (activeMediaIndex.value + 1) % mediaItems.value.length
}

function mediaKey(media) {
  return String(media?.id || media?.mediaUrl || media?.coverUrl || media?.title || '')
}

function isMediaVideo(media) {
  return Number(media?.mediaType) === 3 || isMovieVideoUrl(media?.mediaUrl)
}

function mediaCoverUrl(media) {
  if (!media) {
    return ''
  }
  const url = media.coverUrl || (!isMediaVideo(media) ? media.mediaUrl : '')
  return resolveMovieImageUrl(url, isMediaVideo(media) ? 'wide' : Number(media.mediaType) === 1 ? 'poster' : 'wide')
}

function mediaUrl(media) {
  if (!media) {
    return ''
  }
  if (isMediaVideo(media)) {
    return media.mediaUrl || ''
  }
  return resolveMovieImageUrl(media.mediaUrl || media.coverUrl, Number(media.mediaType) === 1 ? 'poster' : 'origin')
}

function hasRealValue(value) {
  return value !== undefined && value !== null && value !== '' && Number(value) !== 0
}

function formatMoviePrice(value) {
  if (!hasRealValue(value)) {
    return ''
  }
  return `¥${value}`
}

function cinemaMeta(cinema) {
  return [
    cinema.lowestPrice ? `¥${cinema.lowestPrice}起` : '',
    cinema.count ? `${cinema.count}场` : '',
  ].filter(Boolean).join(' · ')
}

function screeningPrice(screening) {
  return formatMoviePrice(screening?.lowestPrice || movie.value.lowestPrice)
}

function artistProfessionText(item) {
  const profession = normalizeArtistText(item?.profession)
  const roleType = normalizeArtistText(item?.roleType).toLowerCase()
  if (roleType.includes('voice')) return '配音'
  if (profession) return profession
  if (roleType.includes('director')) return '导演'
  if (roleType.includes('actor')) return '演员'
  return ''
}

function artistRoleText(item) {
  const roleType = normalizeArtistText(item?.roleType).toLowerCase()
  const roleName = normalizeArtistText(item?.roleName)
  const profession = normalizeArtistText(item?.profession)
  const isDirector = roleType.includes('director') || roleName.includes('导演') || profession.includes('导演')
  const isVoice = roleType.includes('voice') || roleName.includes('配音') || profession.includes('配音') || profession.includes('声优')
  const isActor = roleType.includes('actor') || profession.includes('演员')

  if (isDirector) {
    return roleName && !genericRoleNames.has(roleName) ? roleName : '导演'
  }
  if (isVoice) {
    const cleanRoleName = cleanArtistRoleName(roleName)
    return cleanRoleName && !genericRoleNames.has(cleanRoleName) ? `配音 ${cleanRoleName}` : '配音'
  }
  if (isActor) {
    const cleanRoleName = cleanArtistRoleName(roleName)
    if (!cleanRoleName || genericRoleNames.has(cleanRoleName)) return profession || '演员'
    return `饰 ${cleanRoleName}`
  }
  return roleName || profession || normalizeArtistText(item?.roleType)
}

function cleanArtistRoleName(roleName) {
  return normalizeArtistText(roleName).replace(/^(饰演|饰|配音|配|声演)\s*/u, '')
}

function normalizeArtistText(value) {
  return String(value || '').trim()
}

function markMediaImageBroken(url) {
  if (!url) {
    return
  }
  brokenMediaUrls.value = new Set([...brokenMediaUrls.value, url])
}

function isBrokenMedia(url) {
  return brokenMediaUrls.value.has(url)
}

function normalizeDateKey(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) return ''
  const date = new Date(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatDay(value) {
  if (!value) return ''
  const date = new Date(value)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${date.getMonth() + 1}月${date.getDate()}日 ${week}`
}

function formatDate(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) return ''
  const date = new Date(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatTime(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) return ''
  const date = new Date(value)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatCount(value) {
  const number = Number(value || 0)
  if (!number) return ''
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return String(number)
}

function mediaTypeText(type) {
  if (Number(type) === 3) return '预告片'
  if (Number(type) === 2) return '剧照'
  return '海报'
}

function hideBrokenImage(event) {
  event.target.style.visibility = 'hidden'
}
</script>

<style scoped lang="scss">
.movie-detail-page {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.empty-detail {
  min-height: 320px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 36px;
  border: 1px dashed var(--brand-border);
  border-radius: var(--radius-lg);
  background: #fffaf0;
  color: var(--brand-muted);
  text-align: center;

  strong {
    color: var(--brand-dark);
    font-size: 28px;
  }

  p {
    margin: 0;
  }

  a {
    height: 38px;
    display: inline-flex;
    align-items: center;
    padding: 0 16px;
    border-radius: 999px;
    color: var(--brand-dark);
    background: var(--brand-primary);
    font-weight: 900;
  }
}

.movie-hero-detail {
  position: relative;
  min-height: 430px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: #111;
  color: #fff;
  box-shadow: var(--brand-shadow);
}

.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(18px) saturate(.9);
  transform: scale(1.08);
}

.hero-mask {
  position: absolute;
  inset: 0;
  background:
      linear-gradient(90deg, rgba(18, 17, 15, .9), rgba(18, 17, 15, .66) 48%, rgba(18, 17, 15, .24)),
      linear-gradient(180deg, rgba(18, 17, 15, .12), rgba(18, 17, 15, .78));
}

.hero-content {
  position: relative;
  z-index: 1;
  min-height: 430px;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 34px;
  align-items: center;
  padding: 38px;
}

.poster-frame {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: rgba(255, 255, 255, .08);
  box-shadow: 0 18px 40px rgba(0, 0, 0, .28);

  img {
    width: 260px;
    height: 348px;
    display: block;
    object-fit: cover;
  }
}

.play-button {
  position: absolute;
  right: 12px;
  bottom: 12px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, .28);
  border-radius: 999px;
  color: #fff;
  background: rgba(0, 0, 0, .48);
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: .48;
  }
}

.movie-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    height: 28px;
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

.hero-info {
  min-height: 348px;
  display: flex;
  flex-direction: column;
  align-self: stretch;
  justify-content: center;
  padding: 8px 0;

  h1 {
    margin: 14px 0 0;
    font-size: 42px;
    line-height: 1.14;
    font-weight: 900;
  }

  .alias {
    margin: 8px 0 0;
    color: rgba(255, 255, 255, .62);
  }

  .summary {
    width: min(680px, 100%);
    min-height: 86px;
    margin: 18px 0 0;
    overflow: hidden;
    color: rgba(255, 255, 255, .76);
    font-size: 16px;
    line-height: 1.8;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
  }
}

.score-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  width: min(680px, 100%);
  min-height: 76px;
  margin-top: 24px;
  border: 1px solid rgba(255, 255, 255, .16);
  border-radius: 10px;
  background: rgba(255, 255, 255, .16);

  div {
    padding: 14px;
    background: rgba(255, 255, 255, .08);
  }

  span,
  strong {
    display: block;
  }

  span {
    color: rgba(255, 255, 255, .58);
    font-size: 12px;
  }

  strong {
    margin-top: 6px;
    color: #fff;
    font-size: 20px;
  }
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 26px;
}

.primary-action,
.secondary-action {
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-radius: 999px;
  font-weight: 900;
}

.primary-action {
  border: 0;
  color: var(--brand-dark);
  background: var(--brand-primary);
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: .56;
  }
}

.secondary-action {
  color: #fff;
  border: 1px solid rgba(255, 255, 255, .34);
}

.quick-info {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-top: 18px;
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
  background: var(--brand-line);
  box-shadow: var(--brand-shadow-soft);

  div {
    min-height: 82px;
    padding: 15px 16px;
    background: var(--brand-card);
  }

  span {
    display: block;
    color: var(--brand-muted);
    font-size: 12px;
  }

  strong {
    display: block;
    margin-top: 8px;
    color: var(--brand-dark);
    font-size: 15px;
    line-height: 1.45;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
  margin-top: 20px;
}

.section-block,
.side-card {
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.section-block {
  padding: 22px;
  margin-bottom: 18px;
}

.section-head {
  margin-bottom: 16px;

  span {
    color: var(--brand-primary-strong);
    font-size: 13px;
    font-weight: 900;
  }

  h2 {
    margin: 5px 0 0;
    color: var(--brand-dark);
    font-size: 24px;
  }
}

.inline-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;

  button {
    height: 34px;
    border: 1px solid var(--brand-line);
    border-radius: 999px;
    padding: 0 13px;
    background: #fff;
    color: var(--brand-muted);
    font-weight: 800;
    cursor: pointer;
  }
}

.long-desc {
  margin: 0;
  color: var(--brand-text);
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-line;
}

.artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 14px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.artist-card {
  min-width: 0;
  min-height: 78px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8f3e7;
}

.artist-avatar {
  flex: 0 0 56px;
  width: 56px;
  height: 56px;
  overflow: hidden;
  border-radius: 7px;
  background: #eadfca;

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }
}

.artist-meta {
  min-width: 0;

  strong,
  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--brand-dark);
    font-size: 14px;
  }

  span {
    margin-top: 4px;
    color: var(--brand-muted);
    font-size: 12px;
  }
}

.artist-type {
  height: 20px;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  margin-top: 6px;
  padding: 0 8px;
  border-radius: 999px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #7b5b1f;
  background: rgba(213, 159, 43, .18);
  font-size: 11px;
  font-weight: 800;
  vertical-align: top;
}

.media-card {
  padding: 0;
  border: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f8f3e7;
  text-align: left;
  cursor: pointer;

  .media-cover {
    position: relative;
    width: 100%;
    aspect-ratio: 16 / 9;
    display: block;
    overflow: hidden;
    background: #111;
  }

  img,
  .media-empty-cover {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  .media-empty-cover {
    display: grid;
    place-items: center;
    color: rgba(255, 255, 255, .72);
    font-size: 12px;
  }

  .media-play-mark {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    background: linear-gradient(180deg, rgba(0, 0, 0, .05), rgba(0, 0, 0, .46));

    &::before {
      content: "";
      width: 0;
      height: 0;
      border-top: 15px solid transparent;
      border-bottom: 15px solid transparent;
      border-left: 23px solid #fff;
      filter: drop-shadow(0 2px 8px rgba(0, 0, 0, .5));
    }
  }

  .media-type,
  strong {
    display: block;
    margin: 0 12px;
  }

  .media-type {
    margin-top: 10px;
    color: var(--brand-primary-strong);
    font-size: 12px;
    font-weight: 900;
  }

  strong {
    margin-top: 4px;
    margin-bottom: 12px;
    color: var(--brand-dark);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.empty-inline,
.empty-schedule {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-muted);
  border: 1px dashed var(--brand-border);
  border-radius: 10px;
  background: #fffaf0;
}

.day-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;

  button {
    height: 34px;
    padding: 0 13px;
    border: 1px solid var(--brand-line);
    border-radius: 999px;
    background: #fff;
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

.schedule-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 14px;
}

.cinema-list {
  display: flex;
  flex-direction: column;
  gap: 8px;

  button {
    padding: 12px;
    border: 1px solid var(--brand-line);
    border-radius: 10px;
    background: #fff;
    text-align: left;
    cursor: pointer;

    &.active {
      background: #fff7dc;
      border-color: var(--brand-primary-strong);
    }
  }

  strong,
  span,
  em {
    display: block;
  }

  strong {
    color: var(--brand-dark);
  }

  span,
  em {
    margin-top: 5px;
    color: var(--brand-muted);
    font-size: 12px;
    font-style: normal;
    line-height: 1.45;
  }
}

.screening-list {
  display: flex;
  flex-direction: column;
  gap: 8px;

  article {
    display: grid;
    grid-template-columns: 92px minmax(0, 1fr) 90px 74px;
    gap: 12px;
    align-items: center;
    padding: 12px;
    border: 1px solid var(--brand-line);
    border-radius: 10px;
    background: #fff;
    cursor: pointer;

    &.active {
      background: #fff7dc;
      border-color: var(--brand-primary-strong);
    }
  }

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 4px;
    color: var(--brand-muted);
    font-size: 12px;
  }

  .price {
    color: var(--brand-primary-strong);
    font-size: 18px;
    font-weight: 900;
  }

  button {
    height: 34px;
    border: 0;
    border-radius: 999px;
    color: var(--brand-dark);
    background: var(--brand-primary);
    font-weight: 900;
    cursor: pointer;
  }
}

.side-column {
  position: sticky;
  top: 92px;
}

.side-card {
  padding: 18px;
  margin-bottom: 16px;

  h3 {
    margin: 0 0 12px;
    color: var(--brand-dark);
    font-size: 18px;
  }

  dl {
    margin: 0;
  }

  div {
    padding: 10px 0;
    border-top: 1px solid var(--brand-line);
  }

  dt {
    color: var(--brand-muted);
    font-size: 12px;
  }

  dd {
    margin: 5px 0 0;
    color: var(--brand-text);
    font-size: 14px;
    line-height: 1.5;
  }

  p {
    margin: 0;
    color: var(--brand-muted);
    font-size: 14px;
    line-height: 1.8;
  }
}

.dialog-grid {
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
}

:deep(.movie-media-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.movie-media-dialog .el-dialog__header) {
  margin: 0;
  padding: 14px 16px;
  border-bottom: 1px solid #2a2a2a;
  background: #111;
}

:deep(.movie-media-dialog .el-dialog__body) {
  padding: 0;
  background: #111;
}

.media-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: #fff;

  span,
  strong {
    display: block;
  }

  span {
    color: var(--brand-primary);
    font-size: 12px;
    font-weight: 900;
  }

  strong {
    margin-top: 4px;
    font-size: 16px;
    line-height: 1.3;
  }

  button {
    height: 32px;
    padding: 0 12px;
    border: 1px solid rgba(255, 255, 255, .24);
    border-radius: 999px;
    color: #fff;
    background: rgba(255, 255, 255, .08);
    cursor: pointer;
  }
}

.media-viewer {
  position: relative;
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 76px;
  gap: 0;
  align-items: stretch;
  min-height: min(72vh, 680px);
}

.viewer-stage {
  min-width: 0;
  min-height: 420px;
  display: grid;
  place-items: center;
  background: #050505;

  video,
  img {
    width: 100%;
    max-height: 72vh;
    display: block;
    object-fit: contain;
    background: #050505;
  }

  video {
    height: min(72vh, 680px);
  }

  img {
    height: auto;
  }
}

.viewer-nav {
  border: 0;
  color: rgba(255, 255, 255, .82);
  background: #111;
  font-weight: 900;
  cursor: pointer;

  &:disabled {
    color: rgba(255, 255, 255, .22);
    cursor: not-allowed;
  }
}

.media-unavailable {
  min-height: 260px;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, .72);
}

.media-thumbs {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 12px 14px 14px;
  border-top: 1px solid #2a2a2a;
  background: #111;

  button {
    position: relative;
    flex: 0 0 104px;
    height: 62px;
    overflow: hidden;
    padding: 0;
    border: 2px solid transparent;
    border-radius: 7px;
    background: #222;
    cursor: pointer;

    &.active {
      border-color: var(--brand-primary);
    }
  }

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  .thumb-play-mark {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    background: rgba(0, 0, 0, .28);

    &::before {
      content: "";
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-left: 13px solid #fff;
    }
  }
}

@media (max-width: 980px) {
  .hero-content,
  .content-grid,
  .schedule-layout {
    grid-template-columns: 1fr;
  }

  .poster-frame {
    display: none;
  }

  .hero-info {
    min-height: 320px;
  }

  .quick-info,
  .score-strip,
  .artist-grid,
  .media-grid,
  .dialog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .side-column {
    position: static;
  }
}

@media (max-width: 640px) {
  .movie-detail-page {
    width: calc(100% - 24px);
  }

  .hero-content {
    padding: 30px 22px;
  }

  .hero-info h1 {
    font-size: 31px;
  }

  .hero-info {
    min-height: auto;
  }

  .hero-info .summary {
    min-height: 76px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .quick-info,
  .score-strip,
  .artist-grid,
  .media-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

  .screening-list article {
    grid-template-columns: 1fr;
  }

  .media-viewer {
    grid-template-columns: 1fr;
  }

  .viewer-nav {
    min-height: 42px;
  }

  .viewer-stage {
    min-height: 300px;
  }
}
</style>

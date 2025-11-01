<template>
  <!--详情-->
  <Header></Header>
  <div class="app-container">
    <div class="result-head">
      <div class="result-title">
        <h1>{{ resultTitle }}</h1>
        <p>{{ resultSubtitle }}</p>
      </div>
      <div class="goods"><span>{{ goods }}</span> 个结果</div>
    </div>
    <div class="box-main">
      <div class="box-main-left">
        <div class="box-tLeft">
          <div class="box-type">
            <div class="filter-row">
              <div class="filter-label">城市</div>
              <div class="filter-body">
                <div class="filter-current">当前：{{ currentCity }}</div>
                <ul>
                  <li v-show="isShow" v-for="(item,index) in cityArr.slice(0,visibleCityCount)" :key="item.id"
                      @click="cityClick(item,index)"
                  >
                    <span :class="{active: activeCityIndex == index}">{{ item.name }}</span>
                  </li>
                  <li v-show="!isShow" v-for="(item,index) in cityArr" :key="item.id"
                      @click="cityClick(item,index)">
                    <span :class="{active: activeCityIndex == index}">{{ item.name }}</span>
                  </li>
                </ul>
                <button class="filter-more" type="button" v-show="cityArr.length>visibleCityCount"
                        @click="isShow=!isShow">{{ isShow ? '更多城市' : '收起' }}</button>
              </div>
            </div>
            <div class="filter-row">
              <div class="filter-label">分类</div>
              <div class="filter-body">
                <ul>
                  <li v-for="(item,ind) in categoryArr" :key="item.id"
                      @click="categoryClick(item,ind)"
                  >
                    <span :class="{active: isCategoryActive(item, ind)}">{{ item.name }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-row" v-if="isShowChildren">
              <div class="filter-label">子类</div>
              <div class="filter-body">
                <ul>
                  <li v-for="(item,index) in childrenArr" :key="item.id"
                      @click="childrenClick(item,index)">
                    <span :class="{active: activeChildrenIndex == index}">{{ item.name }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-row">
              <div class="filter-label">时间</div>
              <div class="filter-body">
                <ul>
                  <li v-for="(item,index) in timeArr" :key="item.id"
                      @click="timeClick(item,index)">
                    <span :class="{active: activeTimeIndex == index}">{{ item.name }}</span>
                  </li>
                  <li class="liDate">
                    <el-date-picker
                        v-if="isShowDate"
                        v-model="value1"
                        class="date-range-picker"
                        type="daterange"
                        unlink-panels
                        value-format="YYYY-MM-DD"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        @change="handleChangeDate"
                    />
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div class="box-sort">
            <el-tabs v-model="activeSortName" type="border-card" class="box-tabs" @tab-click="handleClickTab">
              <el-tab-pane v-for="tab in sortTabs" :key="tab.type" :name="String(tab.type)">
                <template #label>{{ tab.label }}</template>
                <ul v-if="cardArr.length">
                  <li class="program-card" v-for="item in cardArr" :key="item.id">
                    <router-link :to="getProgramRoute(item)" class="link">
                      <img v-if="hasPoster(item)" :src="item.itemPicture" alt="" @error="setProgramImageFallback($event, item)">
                      <div v-else class="poster-placeholder">
                        <strong>{{ shortTitle(item.title) }}</strong>
                        <span>{{ item.programCategoryName || activeCategoryName || '现场演出' }}</span>
                      </div>
                      <span class="poster-badge">可订</span>
                    </router-link>
                    <div class="item-txt">
                      <div class="item-title">
                        <span class="area-badge">{{ item.areaName || '全国' }}</span>
                        <router-link :to="getProgramRoute(item)" class="link-detial" v-if="titleIsShow">
                          {{ item.title }}
                        </router-link>
                        <router-link :to="getProgramRoute(item)" class="link-detial" v-if="!titleIsShow"
                                     v-html="sanitizeHighlight(item.title)"></router-link>
                      </div>
                      <div class="item-content item-actor" v-if="titleIsShow">
                        <span class="meta-label">{{ getPeopleLabel(item) }}</span>{{ item.actor || '待公布' }}
                      </div>
                      <div class="item-content item-actor" v-if="!titleIsShow">
                        <span class="meta-label">{{ getPeopleLabel(item) }}</span>
                        <span v-html="sanitizeHighlight(item.actor || '待公布')"></span>
                      </div>
                      <div class="item-meta">
                        <span>{{ item.place || '场馆待公布' }}</span>
                        <span v-if="item.programCategoryName">{{ item.programCategoryName }}</span>
                      </div>
                      <div class="item-content item-time">{{ formatDateWithWeekday(item.showTime, item.showWeekTime) }}</div>
                      <div class="item-action">
                        <div class="item-price">
                          <span class="price">{{ formatPriceText(item) }}</span>
                          <span class="sale-status">售票中</span>
                        </div>
                        <router-link class="buy-action" :to="getProgramRoute(item)">立即抢票</router-link>
                      </div>
                    </div>
                  </li>
                </ul>
                <div class="empty-list" v-else>
                  {{ emptyResultText }}
                </div>
                <pagination
                    v-show="total > 0"
                    :total="total"
                    v-model:page="queryParams.pageNum"
                    v-model:limit="queryParams.pageSize"
                    @pagination="handlePagination"
                />
              </el-tab-pane>
            </el-tabs>

          </div>
        </div>
      </div>
      <div class="box-main-right">
        <div class="box-like">
          <span>同城热门</span>
          <small>近期可订</small>
        </div>
        <ul class="search__box">
          <li class="search__item" v-for="item in recommendList" :key="item.id">
            <router-link :to="getProgramRoute(item)" class="link">
              <img v-if="hasPoster(item)" :src="item.itemPicture" alt="" @error="setProgramImageFallback($event, item)">
              <div v-else class="poster-placeholder small">
                <strong>{{ shortTitle(item.title) }}</strong>
              </div>
            </router-link>
            <div class="search_item_info">
              <router-link :to="getProgramRoute(item)" class="link__title">
                {{ item.title }}
              </router-link>
              <div class="search__item__info__venue">{{ item.place }}</div>
              <div class="search__item__info__venue">{{ formatDateWithWeekday(item.showTime, item.showWeekTime) }}</div>
              <div class="search__item__info__price"><strong>{{ item.minPrice }} 起</strong></div>
            </div>
          </li>
        </ul>
        <div class="recommend-empty" v-if="!recommendList.length">暂无推荐演出</div>
      </div>
    </div>
    <Footer></Footer>
  </div>
</template>

<script setup>
//引入reactive
import {computed, getCurrentInstance, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import useCityStore from '@/store/modules/city'
import {getcategoryType} from "@/api/index";
import {getCurrentDate, useMitt, formatDateWithWeekday} from "@/utils/index";
import {getChildrenType, getProgramPageType, getProgramSearch} from "@/api/allType";
import {getProgramRecommendList} from "@/api/recommendlist.js"
import {ALL_CITY_FILTER, getCityDisplayName, getProgramQueryAreaId, getProgramQueryAreaIds} from '@/utils/city'
import {useRoute} from 'vue-router'
//引入路由器
const emitter = useMitt();
const cityStore = useCityStore()

const goods = ref(0)
const keyword = ref('')

const cityArr = ref([])
const categoryArr = ref([])
const childrenArr = ref([])
const MOVIE_CATEGORY_NAME = '电影'
const MOVIE_CATEGORY_ID = 22
const visibleCityCount = 10
const currentCity = ref(ALL_CITY_FILTER.name)
const currentCityId = ref('')
const currentSelectedCity = ref(null)
const parentProgramCategoryId = ref('')
const isShow = ref(true)
const activeIndex = ref('')
const activeCityIndex = ref('')
const activeChildrenIndex = ref('')
const activeTimeIndex = ref(0)
const isShowChildren = ref(false)
const queryParams = ref({pageNum: 1, pageSize: 10})
const searchParams = ref({
  content: '',
  pageNumber: 1,
  pageSize: 10,
  areaId: undefined,
  areaIds: undefined,
  timeType: 0,
  type: 1
})
const total = ref(0)
const isShowDate = ref(false)
const value1 = ref([])
const timeType = ref(0)
const cardArr = ref([])
const brokenPosterKeys = ref(new Set())
const titleIsShow = ref(true)
const sortTabs = [
  {label: '综合排序', type: 1},
  {label: '热度优先', type: 2},
  {label: '近期演出', type: 3},
  {label: '最新上架', type: 4},
]
const activeSortName = ref('1')
//推荐列表数据
const recommendList = ref([])
const isActive = ref(false)
const activeCategoryName = ref('')
const resultTitle = computed(() => {
  if (keyword.value) {
    return `搜索“${keyword.value}”`
  }
  return activeCategoryName.value || '演出列表'
})
const resultSubtitle = computed(() => {
  const categoryText = activeCategoryName.value || '全部类型'
  const timeName = timeArr.value.find(item => item.id === timeType.value)?.name
  const timeText = timeName && timeName !== '全部' ? timeName : '全部时间'
  return `${currentCity.value} · ${categoryText} · ${timeText}`
})
const emptyResultText = computed(() => {
  if (keyword.value) {
    return `没有找到“${keyword.value}”匹配的演出。`
  }
  return '暂无可售演出。'
})
const pageParams = ref({
  areaId: undefined,
  areaIds: undefined,
  endDateTime: undefined,
  pageNumber: undefined,
  pageSize: undefined,
  parentProgramCategoryId: undefined,
  programCategoryId: undefined,
  startDateTime: undefined,
  timeType: undefined,
  type: 1//1:相关度排序(默认) 2:推荐排序 3:最近开场 4:最新上架
})
//推荐节目列表入参
const recommendParams = reactive({
  areaId: undefined,
  areaIds: undefined,
  parentProgramCategoryId: 1,
  programId: undefined
})
const {proxy} = getCurrentInstance();
const route = useRoute()

//获取城市数据
const getcityList = async () => {
  await cityStore.loadCityOptions()
  cityArr.value = uniqueCityList([{...ALL_CITY_FILTER}, ...cityStore.allCityList])
  syncActiveCity()
}
getcityList()

//当前城市
const getCurrent = async () => {
  const selectedCity = await cityStore.initCity({useBrowserLocation: true})
  applySelectedCity(selectedCity)
  return selectedCity
}

//获取分类
const getTypeList = () => {
  return getcategoryType({type: 1}).then(response => {
    const list = (response.data || []).filter(item => !isMovieCategory(item))
    categoryArr.value = [{name: '全部', id: ''}, ...list]
    return categoryArr.value
  })
}

//获取子类
const getChildrenTypeList = () => {
  return getChildrenType({parentProgramCategoryId: parentProgramCategoryId.value}).then(response => {
    childrenArr.value = [{name: '全部', id: ''}, ...(response.data || [])]
    isShowChildren.value = childrenArr.value.length > 1
    return childrenArr.value
  })
}

function syncActiveCity() {
  activeCityIndex.value = cityArr.value.findIndex(item => String(item.id) === String(currentCityId.value))
  if (activeCityIndex.value < 0 && currentCityId.value === '') {
    activeCityIndex.value = 0
  }
}

function uniqueCityList(list = []) {
  const cityMap = new Map()
  for (const item of list) {
    const key = item?.id ? `id-${item.id}` : `name-${item?.name || ''}`
    if (!cityMap.has(key)) {
      cityMap.set(key, item)
    }
  }
  const nameMap = new Map()
  return Array.from(cityMap.values()).filter(item => {
    const name = item?.name || ''
    if (!name || nameMap.has(name)) {
      return false
    }
    nameMap.set(name, true)
    return true
  })
}

function applySelectedCity(city) {
  const selectedCity = city ? cityStore.applyCity(city) : cityStore.selectedCity
  if (!selectedCity) {
    applyAllCityFilter()
    return
  }
  currentCity.value = getCityDisplayName(selectedCity)
  currentCityId.value = selectedCity.id
  currentSelectedCity.value = selectedCity
  pageParams.value.areaId = getProgramQueryAreaId(selectedCity)
  pageParams.value.areaIds = getProgramQueryAreaIds(selectedCity)
  recommendParams.areaId = getProgramQueryAreaId(selectedCity)
  recommendParams.areaIds = getProgramQueryAreaIds(selectedCity)
  syncActiveCity()
}

function applyAllCityFilter() {
  currentCity.value = ALL_CITY_FILTER.name
  currentCityId.value = ''
  currentSelectedCity.value = null
  pageParams.value.areaId = undefined
  pageParams.value.areaIds = undefined
  recommendParams.areaId = undefined
  recommendParams.areaIds = undefined
  syncActiveCity()
}

//分类

//点击分类每一项
const categoryClick = (item, ind) => {
  keyword.value = ''
  queryParams.value.pageNum = 1
  activeIndex.value = ind
  const isAll = item.name == '全部' || item.id == ''
  activeCategoryName.value = isAll ? '' : item.name
  isActive.value = isAll
  parentProgramCategoryId.value = item.id
  pageParams.value.programCategoryId = undefined
  activeChildrenIndex.value = ''
  childrenArr.value = []

  if (isAll) {
    isShowChildren.value = false
    pageParams.value.parentProgramCategoryId = undefined
    recommendParams.parentProgramCategoryId = 1
  } else {
    isShowChildren.value = true
    pageParams.value.parentProgramCategoryId = item.id;
    //推荐节目列表入参中的父节目类型字段
    recommendParams.parentProgramCategoryId = item.id;
    getChildrenTypeList()
  }
  getList()
  getRecommendList()

}
//点击城市
const cityClick = (item, index) => {
  queryParams.value.pageNum = 1
  activeCityIndex.value = index
  if (!item.id) {
    applyAllCityFilter()
  } else {
    applySelectedCity(item)
    if (currentSelectedCity.value) {
      emitter.emit('cityChange', currentSelectedCity.value)
    }
  }
  if (keyword.value) {
    runKeywordSearch(keyword.value)
  } else {
    getList()
  }
  getRecommendList()
}
//点击子类
const childrenClick = (item, index) => {
  keyword.value = ''
  queryParams.value.pageNum = 1
  activeChildrenIndex.value = index
  pageParams.value.programCategoryId = item.id || undefined
  getList()
}
//点击时间
const timeClick = (item, index) => {
  queryParams.value.pageNum = 1
  activeTimeIndex.value = index
  timeType.value = item.id
  pageParams.value.timeType = item.id
  if (item.id == 5) {
    isShowDate.value = true
    pageParams.value.timeType = 5
  } else {
    isShowDate.value = false
    pageParams.value.startDateTime = undefined
    pageParams.value.endDateTime = undefined
    if (keyword.value) {
      runKeywordSearch(keyword.value)
    } else {
      getList()
    }
  }

}
const handleChangeDate = (selection) => {
  if (!selection || selection.length < 2) {
    pageParams.value.startDateTime = undefined
    pageParams.value.endDateTime = undefined
    return
  }
  keyword.value = ''
  queryParams.value.pageNum = 1
  pageParams.value.startDateTime = getCurrentDate(selection[0])
  pageParams.value.endDateTime = getCurrentDate(selection[1])
  getList()
}

//时间数组
const timeArr = ref(
    [{
      name: '全部',
      id: 0
    }, {
      name: '今天',
      id: 1
    },
      {
        name: '明天',
        id: 2
      },
      {
        name: '一周内',
        id: 3
      },
      {
        name: '一个月内',
        id: 4
      }, {
      name: '按日历',
      id: 5
    },
    ])


const getList = () => {
  titleIsShow.value = true
  pageParams.value.timeType = timeType.value
  pageParams.value.pageNumber = queryParams.value.pageNum
  pageParams.value.pageSize = queryParams.value.pageSize
  getProgramPageType(pageParams.value).then(response => {
    const data = response.data || {}
    cardArr.value = filterProgramList(data.list || [])
    total.value = Number(data.totalSize || 0)
    goods.value = total.value
  })
}

//节目推荐列表
const getRecommendList = () => {
  //如果没有选择父类型，则默认为演唱会
  if (recommendParams.parentProgramCategoryId == '') {
    recommendParams.parentProgramCategoryId = 1
  }
  getProgramRecommendList(recommendParams).then(response => {
    recommendList.value = filterProgramList(response.data || []).slice(0, 3);
  })
}

function applySearchData(data) {
  cardArr.value = filterProgramList(data?.list || [])
  total.value = Number(data?.totalSize || 0)
  goods.value = total.value
  titleIsShow.value = false
}

function runKeywordSearch(content) {
  const nextKeyword = String(content || '').trim()
  keyword.value = nextKeyword
  if (!nextKeyword) {
    getList()
    return Promise.resolve()
  }
  searchParams.value.content = nextKeyword
  searchParams.value.areaId = getProgramQueryAreaId(currentSelectedCity.value)
  searchParams.value.areaIds = getProgramQueryAreaIds(currentSelectedCity.value)
  searchParams.value.pageNumber = queryParams.value.pageNum
  searchParams.value.pageSize = queryParams.value.pageSize
  searchParams.value.timeType = timeType.value
  searchParams.value.type = pageParams.value.type
  return getProgramSearch({...searchParams.value}).then(response => {
    const data = response.data || {}
    applySearchData(data)
    return data
  })
}

const handleSearchList = (payload) => {
  applyKeywordSearchScope(payload?.scope)
  keyword.value = payload?.keyword || keyword.value || proxy.$route.query.keyword || ''
  if (payload?.result) {
    applySearchData(payload.result)
  } else if (keyword.value) {
    queryParams.value.pageNum = 1
    runKeywordSearch(keyword.value)
  }
}

watch(
    () => [route.query.keyword, route.query.scope],
    ([value, scope], oldValue = []) => {
      const nextKeyword = normalizeKeyword(value)
      const scopeChanged = scope !== oldValue[1]
      if (nextKeyword === keyword.value && !scopeChanged) {
        return
      }
      keyword.value = nextKeyword
      queryParams.value.pageNum = 1
      applyKeywordSearchScope(scope)
      if (nextKeyword) {
        runKeywordSearch(nextKeyword)
      } else {
        getList()
      }
    },
    {flush: 'post'}
)

onMounted(async () => {
  await getTypeList()
  await getCurrent()
  pageParams.value.pageNumber = 1
  pageParams.value.pageSize = 10
  pageParams.value.areaId = getProgramQueryAreaId(currentSelectedCity.value)
  pageParams.value.areaIds = getProgramQueryAreaIds(currentSelectedCity.value)
  pageParams.value.timeType = timeType.value
  const queryCategoryName = proxy.$route.query.name || ''
  const queryCategoryId = proxy.$route.query.id
  if (isMovieCategory({id: queryCategoryId, name: queryCategoryName})) {
    proxy.$router.replace({path: '/movie/index'})
    return
  }
  const matchedCategory = queryCategoryId
      ? categoryArr.value.find(item => Number(item.id) === Number(queryCategoryId))
      : categoryArr.value.find(item => item.name === queryCategoryName)
  const resolvedCategoryId = queryCategoryId || matchedCategory?.id || ''
  pageParams.value.parentProgramCategoryId = resolvedCategoryId || undefined
  parentProgramCategoryId.value = resolvedCategoryId
  activeCategoryName.value = resolvedCategoryId ? (matchedCategory?.name || queryCategoryName) : ''
  activeIndex.value = categoryArr.value.findIndex(item => String(item.id) === String(resolvedCategoryId))
  isActive.value = !resolvedCategoryId
  if (parentProgramCategoryId.value) {
    recommendParams.parentProgramCategoryId = parentProgramCategoryId.value
    getChildrenTypeList()
  } else {
    recommendParams.parentProgramCategoryId = 1
  }
  keyword.value = normalizeKeyword(proxy.$route.query.keyword)
  applyKeywordSearchScope()
  if (keyword.value) {
    await runKeywordSearch(keyword.value)
  } else {
    getList()
  }
  getRecommendList()
  emitter.on('searchList', handleSearchList)
  emitter.on('cityChange', handleCityChange)
})

onUnmounted(() => {
  emitter.off('searchList', handleSearchList)
  emitter.off('cityChange', handleCityChange)
})

function handleCityChange(city) {
  const cityId = city?.id ?? city?.areaId
  if (cityId === undefined) {
    return
  }
  if (Number(cityId) === Number(currentCityId.value)) {
    return
  }
  applySelectedCity(city)
  queryParams.value.pageNum = 1
  if (keyword.value) {
    runKeywordSearch(keyword.value)
  } else {
    getList()
  }
  getRecommendList()
}

function handleClickTab(tab, event) {
  const tabName = tab?.props?.name || tab?.paneName || activeSortName.value
  pageParams.value.type = Number(tabName) || Number(tab.index) + 1
  activeSortName.value = String(pageParams.value.type)
  queryParams.value.pageNum = 1
  if (keyword.value) {
    runKeywordSearch(keyword.value)
  } else {
    getList()
  }
}

function handlePagination() {
  if (keyword.value) {
    runKeywordSearch(keyword.value)
  } else {
    getList()
  }
}

function sanitizeHighlight(value) {
  return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/&lt;em&gt;/gi, '<em>')
      .replace(/&lt;\/em&gt;/gi, '</em>')
}

function normalizeKeyword(value) {
  const target = Array.isArray(value) ? value[0] : value
  return String(target || '').trim()
}

function applyKeywordSearchScope(scope = proxy.$route.query.scope) {
  if (scope === 'all') {
    applyAllCityFilter()
    resetCategoryScope()
    resetTimeScope()
    activeSortName.value = '1'
    pageParams.value.type = 1
  }
}

function resetCategoryScope() {
  activeCategoryName.value = ''
  activeIndex.value = 0
  isActive.value = true
  parentProgramCategoryId.value = ''
  pageParams.value.parentProgramCategoryId = undefined
  pageParams.value.programCategoryId = undefined
  activeChildrenIndex.value = ''
  childrenArr.value = []
  isShowChildren.value = false
  recommendParams.parentProgramCategoryId = 1
}

function resetTimeScope() {
  activeTimeIndex.value = 0
  timeType.value = 0
  isShowDate.value = false
  value1.value = []
  pageParams.value.timeType = 0
  pageParams.value.startDateTime = undefined
  pageParams.value.endDateTime = undefined
}

function isCategoryActive(item, index) {
  const isAll = item.name === '全部' || item.id === ''
  return isAll ? isActive.value : activeIndex.value === index
}

function isMovieItem(item) {
  return Number(item?.parentProgramCategoryId) === MOVIE_CATEGORY_ID
      || item?.parentProgramCategoryName === MOVIE_CATEGORY_NAME
}

function getPeopleLabel(item) {
  return '艺人：'
}

function getProgramRoute(item) {
  return {name: 'detial', params: {id: item.id}}
}

function isMovieCategory(item) {
  return item?.name === MOVIE_CATEGORY_NAME || Number(item?.id) === MOVIE_CATEGORY_ID
}

function filterProgramList(list = []) {
  return (list || []).filter(item => !isMovieItem(item))
}

function formatPriceText(item) {
  if (!item?.minPrice && item?.minPrice !== 0) {
    return '价格待定'
  }
  if (!item.maxPrice) {
    return `${item.minPrice}元起`
  }
  return `${item.minPrice}-${item.maxPrice}元`
}

function hasPoster(item) {
  return Boolean(String(item?.itemPicture || '').trim()) && !brokenPosterKeys.value.has(getPosterKey(item))
}

function getPosterKey(item) {
  return String(item?.id || item?.programId || item?.title || item?.itemPicture || '')
}

function shortTitle(value) {
  const title = String(value || '')
      .replace(/<\/?em>/gi, '')
      .replace(/[【】《》]/g, '')
      .trim()
  return title ? title.slice(0, 8) : '潮声票务'
}

function setProgramImageFallback(event, item) {
  const key = getPosterKey(item)
  if (!key || brokenPosterKeys.value.has(key)) {
    return
  }
  const nextKeys = new Set(brokenPosterKeys.value)
  nextKeys.add(key)
  brokenPosterKeys.value = nextKeys
}


</script>

<style scoped lang="scss">
.app-container {
  --category-surface: oklch(99.1% 0.004 58);
  --category-panel: oklch(97.9% 0.008 58);
  --category-panel-muted: oklch(95.3% 0.012 58);
  --category-hover: oklch(96.8% 0.012 58);
  --category-selected: oklch(90.8% 0.078 82);
  --category-selected-bg: oklch(95.8% 0.032 82);
  --category-accent-soft: oklch(96.8% 0.024 82);
  --category-accent-cool: oklch(41% 0.07 190);
  --category-accent-cool-soft: oklch(94% 0.026 190);
  --category-border: oklch(90% 0.014 58);
  --category-border-soft: oklch(93.8% 0.01 58);
  --category-border-strong: oklch(84.5% 0.04 78);
  --category-shadow: 0 1px 4px rgba(52, 40, 24, .05);

  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  margin: 0 auto;
  padding: 22px 0 48px;

  .result-head {
    min-height: 42px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 12px;
    padding: 0 2px;
  }

  .result-title {
    min-width: 0;
    display: flex;
    align-items: baseline;
    gap: 12px;
    flex-wrap: wrap;

    h1 {
      margin: 0;
      color: var(--brand-dark);
      font-size: 22px;
      line-height: 28px;
      font-weight: 650;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    p {
      margin: 0;
      color: var(--brand-muted);
      font-size: 13px;
      line-height: 20px;
    }
  }

  .goods {
    display: inline-flex;
    align-items: center;
    height: auto;
    padding: 0;
    flex: 0 0 auto;
    margin-bottom: 0;
    color: var(--brand-muted);
    font-size: 13px;
    background: transparent;
    border: 0;

    span {
      padding: 0 4px;
      color: var(--brand-text);
      font-weight: 650;
    }
  }

  .box-main {
    display: flex;
    align-items: flex-start;
    gap: 18px;

    .box-main-left {
      flex: 1;
      min-width: 0;

      .box-tLeft {
        width: 100%;

        .box-type {
          padding: 4px 18px;
          border: 0;
          border-radius: 10px;
          background: var(--category-surface);
          box-shadow: var(--category-shadow);

          .filter-row {
            display: grid;
            grid-template-columns: 54px minmax(0, 1fr);
            gap: 12px;
            padding: 12px 0;
            border-bottom: 1px solid var(--category-border-soft);

            &:last-child {
              border-bottom: 0;
            }
          }

          .filter-label {
            padding-top: 5px;
            color: var(--brand-muted);
            font-size: 13px;
            font-weight: 600;
          }

          .filter-body {
            min-width: 0;
            display: flex;
            align-items: flex-start;
            gap: 8px 10px;
            flex-wrap: wrap;
          }

          ul {
            flex: 1 1 auto;
            min-width: 0;
            margin: 0;
            padding: 0;
            display: flex;
            flex-wrap: wrap;
            gap: 8px 10px;

            li {
              list-style: none;
              display: inline-flex;
              align-items: center;
              height: 30px;
              line-height: 30px;
              padding: 0;
              margin: 0;
              color: var(--brand-text);
              white-space: nowrap;
              cursor: pointer;

              > span,
              &.active {
                display: inline-flex;
                align-items: center;
                height: 30px;
                padding: 0 10px;
                border-radius: 999px;
                border: 1px solid transparent;
                transition: background .18s ease, color .18s ease;
              }

              > span:not(.active):hover {
                background: var(--category-hover);
                color: var(--brand-dark);
              }
            }

            .liDate {
              width: 330px;
              height: 32px;
              line-height: 32px;
              padding: 0 !important;
            }
          }

          .filter-more {
            height: 30px;
            padding: 0 10px;
            border: 1px solid transparent;
            border-radius: 999px;
            background: transparent;
            color: var(--brand-primary-strong);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: background .18s ease, color .18s ease;

            &:hover {
              background: var(--category-hover);
              color: var(--brand-dark);
            }
          }
        }

        .filter-current {
          display: inline-flex;
          align-items: center;
          flex: 0 0 auto;
          min-width: 0;
          height: 30px;
          padding: 0;
          border-radius: 0;
          background: transparent;
          color: var(--brand-muted);
          font-size: 13px;
          font-weight: 400;
        }

        .box-sort {
          margin-top: 16px;

          .box-tabs {
            border: 0;
            border-radius: 10px;
            overflow: hidden;
            background: var(--category-surface);
            box-shadow: var(--category-shadow);

            ul {
              margin: 0;
              padding: 6px 12px 12px;

              .program-card {
                list-style-type: none;
                position: relative;
                display: flex;
                gap: 16px;
                min-height: 176px;
                padding: 18px 4px;
                border: 0;
                border-bottom: 1px solid var(--category-border-soft);
                margin: 0;
                border-radius: 0;
                background: transparent;

                &:last-child {
                  border-bottom: 0;
                }

                .link {
                  position: relative;
                  display: block;
                  flex: 0 0 132px;
                  width: 132px;
                  height: 176px;
                  overflow: hidden;
                  border-radius: 8px;
                  background: var(--category-accent-cool-soft);
                  color: inherit;
                  text-decoration: none;

                  img {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                  }

                  img.is-fallback-image {
                    padding: 34px;
                    object-fit: contain;
                    background:
                        radial-gradient(circle at 50% 42%, var(--category-accent-soft), transparent 58%),
                        var(--category-panel-muted);
                  }

                  .poster-placeholder {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    gap: 10px;
                    padding: 18px;
                    background:
                        linear-gradient(160deg, var(--category-accent-cool-soft), var(--category-panel-muted));
                    color: var(--category-accent-cool);
                    text-align: center;

                    strong {
                      max-width: 100%;
                      color: var(--category-accent-cool);
                      font-size: 18px;
                      line-height: 24px;
                      font-weight: 650;
                      overflow-wrap: anywhere;
                    }

                    span {
                      height: 24px;
                      padding: 0 8px;
                      border: 1px solid oklch(84% 0.038 190);
                      border-radius: 999px;
                      color: var(--category-accent-cool);
                      font-size: 12px;
                      font-weight: 600;
                      line-height: 22px;
                    }
                  }

                  .poster-badge {
                    position: absolute;
                    left: 8px;
                    top: 8px;
                    height: 24px;
                    padding: 0 8px;
                    border-radius: 999px;
                    background: rgba(18, 17, 15, .72);
                    color: var(--category-surface);
                    font-size: 12px;
                    font-weight: 600;
                    line-height: 24px;
                  }
                }

                .item-txt {
                  flex: 1;
                  min-width: 0;
                  min-height: 176px;
                  display: flex;
                  flex-direction: column;
                  line-height: 24px;
                  padding-top: 2px;

                  .item-title {
                    display: flex;
                    align-items: flex-start;
                    gap: 8px;
                    margin-bottom: 10px;
                    color: var(--brand-text);
                    font-size: 17px;
                    font-weight: 600;
                    line-height: 24px;

                    span {
                      flex: 0 0 auto;
                      color: var(--brand-primary-strong);
                    }

                    .area-badge {
                      max-width: 96px;
                      height: 24px;
                      padding: 0 6px;
                      border-radius: 4px;
                      background: var(--category-panel-muted);
                      border: 0;
                      color: var(--brand-muted);
                      font-size: 12px;
                      font-weight: 500;
                      line-height: 22px;
                      overflow: hidden;
                      text-overflow: ellipsis;
                      white-space: nowrap;
                    }

                    .link-detial {
                      flex: 1;
                      min-width: 0;
                      color: var(--brand-text);
                      text-decoration: none;
                      outline: 0;
                      overflow: hidden;
                      display: -webkit-box;
                      -webkit-box-orient: vertical;
                      -webkit-line-clamp: 2;
                    }
                  }

                  .item-content {
                    margin-bottom: 6px;
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                    color: var(--brand-muted);
                    font-size: 14px;
                  }

                  .item-actor {
                    max-width: 680px;
                  }

                  .meta-label {
                    color: var(--brand-subtle);
                  }

                  .item-meta {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    min-width: 0;
                    margin-bottom: 6px;
                    color: var(--brand-muted);
                    font-size: 14px;

                    span {
                      min-width: 0;
                      overflow: hidden;
                      white-space: nowrap;
                      text-overflow: ellipsis;

                      &:first-child {
                        flex: 0 1 auto;
                      }
                    }
                  }

                  .item-time {
                    color: var(--brand-text);
                    font-weight: 500;
                  }

                  .item-action {
                    display: flex;
                    align-items: flex-end;
                    justify-content: space-between;
                    gap: 16px;
                    margin-top: auto;
                    padding-top: 8px;
                    border-top: 0;

                    .item-price {
                      display: flex;
                      align-items: baseline;
                      gap: 8px;
                      color: var(--brand-muted);
                      overflow: hidden;
                    }

                    .price {
                      color: var(--brand-primary-strong);
                      font-size: 20px;
                      margin-right: 0;
                      font-style: normal;
                      font-weight: 700;
                    }

                    .sale-status {
                      color: var(--brand-muted);
                      font-size: 13px;
                    }

                    .buy-action {
                      flex: 0 0 auto;
                      height: 34px;
                      padding: 0 14px;
                      border-radius: 999px;
                      background: var(--brand-primary);
                      border: 1px solid var(--brand-primary);
                      color: var(--brand-dark);
                      font-size: 14px;
                      font-weight: 650;
                      line-height: 32px;
                      text-decoration: none;
                      transition: background .18s ease, border-color .18s ease, box-shadow .18s ease;

                      &:hover {
                        background: var(--category-selected);
                        border-color: var(--category-selected);
                        box-shadow: none;
                      }
                    }
                  }
                }
              }
            }

            .page {
              margin: 12px 12px 20px 0;
              display: flex;
              justify-content: flex-end;
            }
          }

          .empty-list {
            min-height: 220px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 12px 10px;
            border: 1px dashed var(--brand-border);
            border-radius: var(--radius-lg);
            background: var(--category-panel);
            color: var(--brand-muted);
            font-size: 14px;
          }
        }
      }
    }

    .box-main-right {
      flex: 0 0 254px;
      position: sticky;
      top: 96px;
      border: 0;
      border-radius: 10px;
      margin-left: 0;
      max-height: none;
      overflow: hidden;
      background: var(--category-surface);
      box-shadow: var(--category-shadow);

      .box-like {
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        background-color: var(--category-surface);
        border-bottom: 0;
        color: var(--brand-dark);
        padding: 0 16px;

        span {
          font-size: 15px;
          font-weight: 650;
        }

        small {
          color: var(--brand-muted);
          font-size: 12px;
          font-weight: 400;
        }
      }

      .search__box {
        margin: 0;
        padding: 0 14px 14px;

        .search__item {
          list-style: none;
          display: flex;
          gap: 10px;
          background-color: transparent;
          border: none;
          border-bottom: 0;
          padding: 14px 0;
          margin: 0;
          box-shadow: none;

          &:last-child {
            border-bottom: 0;
          }

          .link {
            display: block;
            flex: 0 0 78px;
            width: 78px;
            height: 104px;
            overflow: hidden;
            border-radius: 8px;
            background: var(--category-panel-muted);
            color: inherit;
            text-decoration: none;
          }

          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }

          img.is-fallback-image {
            padding: 20px;
            object-fit: contain;
            background: var(--category-panel-muted);
          }

          .poster-placeholder {
            width: 100%;
            height: 100%;
            display: grid;
            place-items: center;
            padding: 10px;
            background: var(--category-accent-cool-soft);
            color: var(--category-accent-cool);
            text-align: center;

            strong {
              color: var(--category-accent-cool);
              font-size: 13px;
              line-height: 17px;
              font-weight: 650;
              overflow-wrap: anywhere;
            }
          }

          .search_item_info {
            min-width: 0;
            flex: 1;

            .link__title {
              color: var(--brand-text);
              font-size: 13px;
              font-weight: 600;
              line-height: 18px;
              text-decoration: none;
              overflow: hidden;
              display: -webkit-box;
              -webkit-box-orient: vertical;
              -webkit-line-clamp: 2;
            }

            .search__item__info__venue {
              margin-top: 7px;
              color: var(--brand-muted);
              font-size: 12px;
              line-height: 18px;
              overflow: hidden;
              white-space: nowrap;
              text-overflow: ellipsis;
            }

            .search__item__info__price {
              margin-top: 8px;
              font-size: 12px;

              strong {
                color: var(--brand-primary-strong);
                font-weight: 600;
              }
            }
          }
        }
      }

      .recommend-empty {
        padding: 18px 14px;
        color: var(--brand-muted);
        font-size: 13px;
        text-align: center;
      }
    }
  }
}

.active {
  background-color: var(--category-selected-bg);
  border-color: transparent !important;
  color: var(--brand-dark) !important;
  display: inline-flex;
  align-items: center;
  height: 30px;
  line-height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  white-space: nowrap;
  cursor: pointer;
  font-weight: 600;
}

:deep(.el-collapse) {
  border: 0;
}

:deep(.el-collapse-item) {
  display: block;
  border-bottom: 1px solid var(--category-border);
}

:deep(.el-collapse-item:last-child) {
  border-bottom: 0;
}

:deep(.el-collapse-item__header) {
  height: 44px;
  line-height: 44px;
  border: 0;
  background: transparent;
  color: var(--brand-muted);
}

:deep(.el-collapse-item__header .title) {
  width: 56px;
  display: inline-block;
  color: var(--brand-dark);
  font-weight: 600;
  text-align: left;
}

:deep(.el-collapse-item__arrow) {
  display: none;
}

:deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  height: 44px;
  display: flex;
  align-items: center;
  background: var(--category-surface);
  border-bottom: 1px solid var(--category-border-soft);
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__nav-wrap) {
  height: 44px;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__nav) {
  height: 44px;
  display: flex;
  align-items: center;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item:first-child) {
  margin-left: 8px;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item) {
  position: relative;
  height: 44px;
  line-height: 44px;
  margin: 0;
  padding: 0 12px;
  border-radius: 0;
  color: var(--brand-muted);
  border: 0;
  font-weight: 500;
  transition: color .18s ease;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item:not(.is-active):hover) {
  color: var(--brand-dark);
  background: transparent;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  color: var(--brand-dark);
  background: transparent;
  border: 0;
  font-weight: 600;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active::after) {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 5px;
  height: 2px;
  border-radius: 999px;
  background: var(--brand-primary-strong);
}

:deep(.el-tabs--border-card > .el-tabs__content) {
  padding: 0;
}

:deep(.pagination-container) {
  position: relative !important;
  height: auto !important;
  min-height: 40px;
  display: flex;
  justify-content: flex-end;
  margin: 16px 0 0 !important;
  padding: 16px 10px 4px !important;
  overflow: visible !important;
  background: transparent !important;
}

:deep(.pagination-container .el-pagination) {
  position: static !important;
  right: auto !important;
  width: 100%;
  min-height: 32px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 4px;
  white-space: normal;
}

:deep(.pagination-container .el-pagination .el-pager) {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}

:deep(.pagination-container .el-pagination button),
:deep(.pagination-container .el-pagination .el-pager li) {
  flex: 0 0 auto;
}

:deep(.date-range-picker.el-date-editor) {
  width: 100%;
  height: 34px;
  box-shadow: none;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
}

:deep(em) {
  font-weight: 650;
  color: var(--brand-primary-strong);
}

@media (max-width: 980px) {
  .app-container {
    .result-head {
      align-items: flex-start;
      flex-direction: column;
      gap: 12px;
    }

    .box-main {
      display: block;

      .box-main-right {
        position: static;
        margin-top: 18px;
      }
    }
  }
}

@media (max-width: 640px) {
  .app-container {
    .result-title h1 {
      font-size: 20px;
      line-height: 26px;
    }

    :deep(.el-tabs--border-card > .el-tabs__header .el-tabs__nav) {
      width: 100%;
      display: flex;
    }

    :deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item) {
      flex: 1 1 0;
      min-width: 0;
      padding: 0 4px;
      text-align: center;
      font-size: 13px;
    }

    .box-main .box-main-left .box-tLeft .box-sort .box-tabs ul .program-card {
      display: block;

      .link {
        width: 100%;
        height: 220px;
        margin-bottom: 14px;
      }

      .item-txt {
        min-height: auto;
      }

      .item-txt .item-price {
        position: static;
        margin-top: 14px;
      }

      .item-txt .item-action {
        align-items: flex-start;
        flex-direction: column;
      }

      .item-txt .item-action .buy-action {
        width: 100%;
        text-align: center;
      }
    }

    .box-main .box-main-left .box-tLeft .box-type .filter-body ul .liDate {
      width: 100%;
      margin-left: 0;
    }
  }
}
</style>

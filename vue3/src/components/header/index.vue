<template>
  <div class="app-header">
    <div class="header">
      <router-link to="/index" class="brand-link">
        <img :src="logo" alt="潮声">
        <span class="brand-name">潮声</span>
      </router-link>
      <div class="localHeader" v-if="isShowHeader">
        <el-icon :size="16">
          <Location/>
        </el-icon>

        <el-popover v-model:visible="visible" placement="bottom" popper-class="city-popper" trigger="click">
          <template #reference>
            <span class="city-location">{{ localName || '定位中' }}<el-icon :size="12"> <CaretBottom/></el-icon></span>
          </template>
          <div class="city">
            <div class="now-city">
              <span class="title-city">当前城市：</span><span class="city-name select-city">{{ localName }}</span>
            </div>
            <div class="hot-city">
              <span class="title-city">热门城市：</span>
              <ul class="list-city">
                <li class="city-name" v-for="item in hotCity" :key="item.id" @click="getCityInfoList(item)">
                  {{ item.name }}
                </li>
              </ul>
            </div>
            <div class="others-city">
              <span class="title-city">其他城市：</span>
              <ul class="list-city">
                <li class="city-name" v-for="item in otherCity" :key="item.id" @click="getCityInfoList(item)">
                  {{ item.name }}
                </li>
              </ul>
            </div>
          </div>
        </el-popover>


      </div>
      <div class="recommendHeader" v-if="isShowHeader">
        <router-link to="/index" class="routeHome" tag="div">首页</router-link>
        <router-link to="/allType/index" class="routeType" tag="div">分类</router-link>
        <router-link to="/movie/index" class="routeMovie" tag="div">电影</router-link>
      </div>
      <div class="searchHeader" v-if="isShowHeader">
        <el-input
            v-model="iptSearch"
            :placeholder="searchPlaceholder"
            class="input-with-search"
            @keyup.enter="getProgramSearchList"
        >
          <template #prepend>
            <div class="search-prepend">
              <el-icon :size="18">
                <Search/>
              </el-icon>
              <button
                  v-for="item in searchDomains"
                  :key="item.value"
                  type="button"
                  class="search-domain"
                  :class="{active: searchDomain === item.value}"
                  @click="setSearchDomain(item.value)"
              >{{ item.label }}</button>
            </div>
          </template>
          <template #suffix>
            <button
                v-if="iptSearch"
                type="button"
                class="search-clear"
                aria-label="清除搜索"
                @mousedown.prevent
                @click.stop="clearSearch"
            >×</button>
          </template>
          <template #append>
            <el-button class="searchBtn" @click="getProgramSearchList">{{ searchButtonText }}</el-button>
          </template>
        </el-input>
      </div>
      <div class="rightHeader" v-if="isShowHeader">
        <div class="box-left">

          <el-popover :width="160" :disabled="!isHasToken">
            <template #reference>
              <span class="account-trigger"><img :src="photo" alt="" class="">
                <router-link to="/login" class="log">{{ isLoginToken }}</router-link></span>
            </template>
            <template #default v-if="isHasToken">
              <ul class="loginInfo">
                <li>
                  <router-link to="/personInfo/index">个人信息</router-link>
                </li>
                <li>
                  <router-link to="/accountSettings/index">账号设置</router-link>
                </li>
                <li>
                  <router-link to="/orderManagement/index">订单管理</router-link>
                </li>
                <li @click="loginOut" class="logOut" v-if="isHasToken">
                  <span class="loginOut">退出登录</span>
                </li>
              </ul>
            </template>
          </el-popover>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>

import logo from '@/assets/login/logo.png'
import photo from '@/assets/login/photo.png'
import {computed, ref, onMounted, onUnmounted, watch} from 'vue'
import {getUserIdKey, removeToken, removeUserIdKey, removeName} from "../../utils/auth";
import useUserStore from '@/store/modules/user'
import useCityStore from '@/store/modules/city'
import {getPersonInfoId} from '@/api/personInfo'
import {useRoute, useRouter} from 'vue-router'
import {useMitt} from "@/utils/index";

const emitter = useMitt();

const route = useRoute()
const router = useRouter()
const SEARCH_DOMAIN_PERFORMANCE = 'performance'
const SEARCH_DOMAIN_MOVIE = 'movie'
const searchDomains = [
  {label: '演出', value: SEARCH_DOMAIN_PERFORMANCE},
  {label: '电影', value: SEARCH_DOMAIN_MOVIE},
]
const isLoginToken = ref('登录')
const isHasToken = ref(false)
const iptSearch = ref('')
const searchDomain = ref(SEARCH_DOMAIN_PERFORMANCE)
const isShowHeader = ref(true)
const userStore = useUserStore()
const cityStore = useCityStore()
const localName = computed(() => cityStore.localName)
const hotCity = computed(() => cityStore.hotCity)
const otherCity = computed(() => cityStore.otherCity)
const visible = ref(false)
const queryParams = ref({
  content: '',
  pageNumber: 1,
  pageSize: 10,
  areaId: undefined,
  areaIds: undefined,
  timeType: 0
})
const emits = defineEmits(['updateValue'])
const searchPlaceholder = computed(() => {
  return searchDomain.value === SEARCH_DOMAIN_MOVIE
      ? '搜索电影、导演、主演'
      : '搜索明星、演出、体育赛事'
})
const searchButtonText = computed(() => searchDomain.value === SEARCH_DOMAIN_MOVIE ? '搜电影' : '搜演出')

watch(
    () => [route.path, route.query.keyword],
    ([path]) => {
      syncHeaderByRoute(path)
    },
    {immediate: true}
)

//退出到首页,设置token为空，并且昵称变为登录
function loginOut() {
  userStore.logOut().then(() => {
    location.href = '/';
    isLoginToken.value = '登录'
    removeToken('Admin-Token')
    removeUserIdKey('userId')
    removeName('userName')
    isHasToken.value = false
  })

}

//初始化如果cookie存在id，通过id获取昵称，回显到登录位置
if (getUserIdKey()) {
  getNickName()
}

function getNickName() {
  const id = getUserIdKey()
  getPersonInfoId({id: id}).then(response => {
    if (response.data != null) {
      let {name} = response.data
      isLoginToken.value = name
      if (isLoginToken.value && isLoginToken.value.length > 2) {
        isLoginToken.value = isLoginToken.value.slice(0,2)+"..."
      }
      isHasToken.value = true
    }
  })
}

onMounted(() => {
  getCurrent()
  getCityOptions()
  emitter.on('cityChange', handleCityChange)
})

onUnmounted(() => {
  emitter.off('cityChange', handleCityChange)
})

//当前城市
async function getCurrent() {
  const selectedCity = await cityStore.initCity({useBrowserLocation: true})
  notifyCityChange(selectedCity)
}

function applyCity(city, notify = true) {
  const selectedCity = cityStore.applyCity(city)
  if (!selectedCity) {
    return
  }
  publishCityChange(selectedCity, notify)
}

function handleCityChange(city) {
  const cityId = city?.id ?? city?.areaId
  if (cityId === undefined || Number(cityId) === Number(cityStore.selectedCity?.id)) {
    return
  }
  applyCity(city, false)
}

function notifyCityChange(city) {
  if (!city) {
    return
  }
  queryParams.value.areaId = cityStore.programAreaId
  queryParams.value.areaIds = cityStore.programAreaIds
  emits('updateValue', city)
}

function publishCityChange(city, notify = true) {
  notifyCityChange(city)
  if (notify) {
    emitter.emit('cityChange', city)
  }
}

function getCityOptions() {
  cityStore.loadCityOptions()
}

/**
 * 点击改变当前地点后，获取初始化接口更新地点
 * @param params
 */
function getCityInfoList(params) {
  cityStore.selectCity(params).then(selectedCity => {
    if (selectedCity) {
      publishCityChange(selectedCity)
    }
    visible.value = false
    getCityOptions()
  })
}

function normalizeKeyword(value) {
  const target = Array.isArray(value) ? value[0] : value
  return String(target || '').trim()
}

function isMoviePath(path) {
  return String(path || '').startsWith('/movie')
}

function syncHeaderByRoute(path) {
  isShowHeader.value = path !== '/login' && path !== '/register'
  searchDomain.value = isMoviePath(path) ? SEARCH_DOMAIN_MOVIE : SEARCH_DOMAIN_PERFORMANCE
  iptSearch.value = normalizeKeyword(route.query.keyword)
}

function setSearchDomain(value) {
  searchDomain.value = value
}

function clearSearch() {
  iptSearch.value = ''
  const targetPath = searchDomain.value === SEARCH_DOMAIN_MOVIE ? '/movie/index' : '/allType/index'
  if (searchDomain.value === SEARCH_DOMAIN_PERFORMANCE) {
    emitter.emit('searchList', {keyword: '', scope: 'all'})
  }
  router.push({path: targetPath})
}

function getProgramSearchList() {
  const keyword = iptSearch.value.trim()
  const isMovieSearch = searchDomain.value === SEARCH_DOMAIN_MOVIE
  const targetPath = isMovieSearch ? '/movie/index' : '/allType/index'
  if (!keyword) {
    router.push({path: targetPath})
    return
  }
  if (!isMovieSearch) {
    emitter.emit('searchList', {keyword, scope: 'all'})
  }
  router.push({path: targetPath, query: isMovieSearch ? {keyword} : {keyword, scope: 'all'}});
}

</script>

<style scoped lang="scss">
.app-header {
  width: 100%;
  height: 74px;
  background: rgba(255, 253, 248, .9);
  border-bottom: 1px solid var(--brand-line);
  box-shadow: 0 10px 28px rgba(40, 32, 18, .06);
  position: sticky;
  top: 0;
  z-index: 99;
  backdrop-filter: blur(16px);

  .header {
    width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
    margin: 0 auto;
    height: 74px;
    display: flex;
    align-items: center;
    gap: 20px;

    .brand-link {
      height: 74px;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      color: var(--brand-dark);
      flex: 0 0 auto;

      img {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        object-fit: cover;
        box-shadow: 0 10px 22px rgba(40, 32, 18, .14);
      }

      .brand-name {
        font-size: 24px;
        font-weight: 900;
        line-height: 1;
        color: var(--brand-dark);
      }
    }

    .localHeader {
      min-width: 100px;
      height: 36px;
      position: relative;
      line-height: 36px;
      white-space: nowrap;
      cursor: pointer;
      color: var(--brand-text);
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 0 12px;
      border-radius: 999px;
      background: #f2ecdf;
      border: 1px solid transparent;
      transition: border-color .18s ease, background .18s ease;

      &:hover {
        background: #fff;
        border-color: var(--brand-line);
      }

      .city-location {
        max-width: 72px;
        font-size: 14px;
        color: var(--brand-text);
        display: inline-flex;
        align-items: center;
        gap: 3px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        border: none;

        &:hover {
          background: none;
        }
      }


    }

    .recommendHeader {
      min-width: auto;
      height: 36px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      line-height: normal;
      overflow: visible;

      .routeHome,
      .routeType,
      .routeMovie {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 36px;
        padding: 0 12px;
        border-radius: 999px;
        font-size: 15px;
        color: var(--brand-text);
        white-space: nowrap;
        transition: background .18s ease, color .18s ease;
      }

      .routeHome.router-link-active {
        color: var(--brand-dark);
        background: #f2ecdf;
        font-weight: 700;
      }

      .routeType.router-link-active,
      .routeMovie.router-link-active {
        color: var(--brand-dark);
        background: #f2ecdf;
        font-weight: 700;
      }
    }

    .searchHeader {
      flex: 1 1 520px;
      max-width: 540px;
      min-width: 0;
      height: 46px;
      line-height: 46px;
      position: relative;

      .input-with-search {
        width: 100%;
        height: 42px;
        font-size: 15px;
        outline: 0;
        -webkit-appearance: none;
        border: 0;
        border-radius: 999px;
        background-color: #f2ecdf;
        box-sizing: border-box;
        overflow: hidden;

        :deep(.el-input-group__prepend) {
          padding: 0 8px 0 12px;
          box-shadow: none;
          border: 0;
          border-radius: 999px 0 0 999px;
          background: #f2ecdf;
        }

        :deep(.el-input__wrapper) {
          box-shadow: none !important;
          background-color: #f2ecdf !important;
        }

        :deep(.el-input__suffix) {
          align-items: center;
        }

        :deep(.el-input-group__append) {
          width: 92px;
          flex: 0 0 92px;
          overflow: hidden;
          padding: 4px 4px 4px 0;
          border: 0;
          border-radius: 0 999px 999px 0;
          background: #f2ecdf;
          box-shadow: none;
        }
      }

      .search-prepend {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 42px;
        color: var(--brand-muted);
      }

      .search-domain {
        height: 26px;
        padding: 0 8px;
        border: 0;
        border-radius: 999px;
        background: transparent;
        color: var(--brand-muted);
        font-size: 13px;
        font-weight: 800;
        line-height: 26px;
        white-space: nowrap;
        cursor: pointer;
        transition: background .18s ease, color .18s ease;

        &:hover {
          color: var(--brand-primary-strong);
        }

        &.active {
          color: var(--brand-dark);
          background: #fff;
          box-shadow: 0 4px 10px rgba(40, 32, 18, .08);
        }
      }

      .search-clear {
        width: 22px;
        height: 22px;
        margin-right: 4px;
        border: 0;
        border-radius: 50%;
        background: rgba(40, 32, 18, .12);
        color: #6f6759;
        font-size: 18px;
        line-height: 20px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        transition: background .18s ease, color .18s ease;

        &:hover {
          color: var(--brand-dark);
          background: rgba(40, 32, 18, .2);
        }
      }

      .searchBtn {
        width: 88px;
        height: 34px;
        background: var(--brand-primary);
        font-size: 14px;
        text-align: center;
        color: var(--brand-dark);
        border: 1px solid var(--brand-primary);
        border-radius: 999px;
        letter-spacing: 0;
        font-weight: 800;
        cursor: pointer;
        transition: background .18s ease, border-color .18s ease;

        &:hover {
          background: #f1bc29;
          border-color: #f1bc29;
        }
      }
    }

    .rightHeader {
      min-width: 55px;
      height: 100%;
      flex: 0 0 auto;
      position: relative;
      line-height: 74px;
      margin-left: auto;

      .box-left {
        height: 100%;
        display: inline-flex;
        align-items: center;
        line-height: normal;
        cursor: pointer;
        position: relative;
        margin-left: 0;

        .account-trigger {
          height: 36px;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 0 12px;
          border-radius: 999px;
          transition: background .18s ease, color .18s ease;
        }

        .account-trigger:hover {
          color: var(--brand-primary-strong);
          background: #f2ecdf;
        }

        img {
          width: 26px;
          height: 26px;
          display: block;
          margin-right: 0;
          border-radius: 50%;
        }

        .log {
          max-width: 48px;
          color: inherit;
          line-height: 1;
          white-space: nowrap;
          overflow: hidden; /* 超出容器部分隐藏 */
          text-overflow: ellipsis;
        }

      }
    }
  }
}

.loginInfo {
  width: 132px;
  box-sizing: border-box;
  list-style-type: none;
  margin: 0;
  padding: 6px;

  li {
    height: 36px;
    line-height: 36px;

    a,
    .loginOut {
      width: 100%;
      height: 36px;
      display: block;
      padding: 0 12px;
      color: var(--brand-text);
      font-size: 14px;
      border-radius: 8px;
      transition: background .18s ease, color .18s ease;

      &:hover {
        color: var(--brand-primary-strong);
        background: #fff6cf;
      }
    }
  }

  .logOut {
    cursor: pointer;
  }
}


:global(.city-popper) {
  padding: 0 !important;
  border-radius: 14px !important;
  border: 0 !important;
  box-shadow: none !important;
}

.city {
  width: 626px;
  box-sizing: border-box;
  z-index: 999;
  position: relative;
  margin-top: 4px;
  background: var(--brand-card);
  border: 1px solid var(--brand-line);
  box-shadow: var(--brand-shadow);
  border-radius: 14px;
  padding: 21px;
  max-height: 1500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .now-city,
  .hot-city,
  .others-city {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    line-height: 28px;
  }

  .others-city {
    padding-top: 16px;
    border-top: 1px solid var(--brand-line);
  }

  .title-city {
    width: 86px;
    flex: 0 0 86px;
    font-size: 15px;
    color: #111;
    font-weight: 700;
  }

  .city-name {
    display: inline-flex;
    align-items: center;
    min-height: 28px;
    padding: 0 8px;
    font-size: 14px;
    color: #111;
    border-radius: 999px;
  }

  .select-city {
    color: var(--brand-dark);
    background-color: var(--brand-surface);
    font-weight: 800;
  }

  .list-city {
    list-style: none;
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 10px;
    margin: 0;
    padding: 0;
    line-height: normal;

    .city-name {
      cursor: pointer;
      transition: background .18s ease, color .18s ease;

      &:hover {
        color: var(--brand-primary-strong);
        background: #f2ecdf;
      }
    }
  }
}

@media (max-width: 980px) {
  .app-header {
    height: auto;

    .header {
      height: auto;
      min-height: 74px;
      flex-wrap: wrap;
      gap: 10px 14px;
      padding: 10px 0;

      .brand-link {
        height: 44px;
      }

      .recommendHeader {
        order: 4;
        width: 100%;
        height: 36px;
      }

      .searchHeader {
        order: 5;
        flex-basis: 100%;
        max-width: none;
        min-width: 0;
      }

      .rightHeader {
        height: 44px;
        min-width: 88px;
        line-height: 44px;

        .box-left .account-trigger {
          padding: 0 10px;
        }
      }
    }
  }
}

@media (max-width: 680px) {
  .app-header {
    .header {
      width: calc(100% - 24px);

      .brand-link .brand-name {
        font-size: 21px;
      }

      .localHeader {
        min-width: auto;
      }
    }
  }

  .city {
    width: calc(100vw - 28px);
  }
}


</style>

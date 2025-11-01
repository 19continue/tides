<template>
  <!--点击进入单独界面详情-->
  <Header></Header>
  <main v-if="detailLoading" class="detail-state detail-loading">
    <div class="poster-skeleton"></div>
    <div class="copy-skeleton">
      <span></span>
      <span></span>
      <span></span>
      <div></div>
    </div>
  </main>
  <main v-else-if="detailError || !detailList.id" class="detail-state detail-empty-page">
    <h1>{{ detailError || '项目详情暂不可用' }}</h1>
    <p>请返回列表重新选择项目，或稍后再试。</p>
    <button type="button" @click="router.back()">返回上一页</button>
  </main>
  <div v-else class="app-container">
    <div class="wrapper">
      <div class="box-left">
          <div class="box-detail">
            <div class="count">
              <div class="box-img"><img :src="detailList.itemPicture" alt="" @error="setImageFallback"></div>
              <div class="order">
                <div class="title">
                  <span class="tips">{{ detailCopy.badge }}</span>
                  <span>{{ detailList.title }}</span>
                </div>
                <div class="address">
                  <div class="time">{{ detailCopy.timeLabel }}：{{ safeFormatDate(detailList.showTime, detailList.showWeekTime) }}</div>
                  <div class="place">
                    <div class="addr">{{ detailCopy.placeLabel }}：{{ placeLine }}</div>
                  </div>
                </div>
                <!--                预售-->
                <div class="notice" v-show="detailList.preSell=='1'">
                  <div class="ticket-type"><span v-if="detailList.preSell=='1'">预售</span></div>
                  <div class="content">
                    <div>{{ detailList.preSellInstruction }}</div>
                    <div class="notice-content">
                      {{ detailList.importantNotice }}
                    </div>
                  </div>
                </div>
                <div class="citys" :class="{ 'movie-venue': isMovieProgram() }">
                  <span>{{ detailCopy.venueSelectorLabel }}</span>
                  <div class="city-list">
                    <div
                      v-for="item in venueOptions"
                      :key="item.screeningId || item.cinemaId || item.programId || item.areaId || item.place"
                      class="city-item"
                      :class="{ activeCity: isCurrentVenue(item) }"
                      :id="item.areaId || item.cinemaId"
                      @click="switchVenue(item)"
                    >
                      <span class="venue-title">{{ getVenueTitle(item) }}</span>
                      <span class="venue-meta" v-if="getVenueMeta(item)">{{ getVenueMeta(item) }}</span>
                    </div>
                  </div>
                  <div class="city-more" v-if="!isMovieProgram() && venueOptions.length > 1">查看更多</div>
                </div>
                <div class="order-box">
                  <div class="notice-time">{{ detailCopy.localTimeNotice }}</div>
                  <div class="order-time">
                    <div class="order-name">{{ detailCopy.sessionLabel }}</div>
                    <div class="select">
                      <template v-if="isMovieProgram() && screeningOptions.length">
                        <div class="select-list" v-for="item in screeningOptions" :key="item.id">
                          <div class="select-list-item" @click="screeningClick(item)"
                               :class="{ activeCity: selectedMovieScreening && Number(selectedMovieScreening.id) === Number(item.id) }">
                            <span>{{ safeFormatDate(item.showTime, item.showWeekTime) }}</span>
                            <span class="venue-meta" v-if="getScreeningMeta(item)">{{ getScreeningMeta(item) }}</span>
                          </div>
                        </div>
                      </template>
                      <div class="select-list" v-else>
                        <div class="select-list-item activeCity">
                          <span>{{ safeFormatDate(detailList.showTime, detailList.showWeekTime) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="order-box" v-if="ticketCategoryVoList.length || !isMovieProgram()">
                  <div class="order-time">
                    <div class="order-name">{{ detailCopy.ticketLabel }}</div>
                    <div class="select">
                      <div class="select-list" v-for="(item,index) in  ticketCategoryVoList">
                        <div class="select-list-item " @click="ticketClick(item,index)"
                             :class="{ticket: actvieIndex == index}">
                          <span>{{ item.introduce }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="order-price" v-if="detailList.permitChooseSeat!='1'">
                  <div class="num">数量</div>
                  <div class="count">
                    <div class="count-info">
                      <el-input-number v-model="num" :min="1" :max="ticketLimit" @change="handleChange"/>
                    </div>
                    <div class="num-limit">每笔订单限购{{ ticketLimit }}张</div>
                  </div>
                </div>
                <div class="order-box" v-if="detailList.permitChooseSeat!='1'">
                  <div class="order-time">
                    <div class="order-name">合计</div>
                    <div class="order-count" v-if="allPrice==''">￥{{countPrice }}</div>
                    <div class="order-count" v-else>￥{{allPrice }}</div>
                  </div>
                </div>
                <div class="buy">
                  <div class="buy-link-now disabled" v-if="!canBuy">{{ buyDisabledText }}</div>
                  <div class="buy-link-now" v-else-if="detailList.permitChooseSeat!='1'" @click="nowBuy">{{ detailCopy.buyNowLabel }}</div>
                  <div class="buy-link-now" v-else @click="seatBuy">{{ detailCopy.seatBuyLabel }}</div>
                  <!--                    <router-link class="buy-link" to="/order/index">不，选座购买</router-link>-->
<!--                  <div class="subtitle">请您移步手机端购买</div>
                  <div class="qrcode">
                    <div class="tip">手机扫码购买更便捷</div>
                    <div class="J_qrcodeImg"></div>
                    <div class="buy-link" @click="nowBuy">不，立即购买</div>

                  </div>-->
                </div>

              </div>
            </div>
          </div>
          <div class="box-item">
            <div class="box-menu">
              <button type="button" class="menu-children" @click="detialClick('projectDetial',1)"
                           :class="{menuActive: menuActive == 1}">{{ detailCopy.projectMenuLabel }}
              </button>
              <button type="button" class="menu-children" @click="detialClick('ticketNeed',2)"
                           :class="{menuActive: menuActive == 2}">{{ detailCopy.ticketNoticeLabel }}
              </button>
              <button type="button" class="menu-children" @click="detialClick('watchNeed',3)"
                           :class="{menuActive: menuActive == 3}">{{ detailCopy.watchNoticeLabel }}
              </button>
            </div>
            <div id="projectDetial">
              <div class="proDetial">{{ detailCopy.detailTitle }}</div>
              <div v-if="detailBlocks.length" class="detail-content">
                <template v-for="(block, index) in detailBlocks" :key="`${block.type}-${index}-${block.value.slice(0, 24)}`">
                  <div v-if="block.type === 'html'" class="detail-html" v-html="block.value"></div>
                  <figure v-else-if="block.type === 'image'" class="detail-figure">
                    <img :src="block.value" alt="" @error="hideBrokenDetailImage">
                  </figure>
                  <p v-else class="detail-paragraph">{{ block.value }}</p>
                </template>
              </div>
              <div v-else class="detail-empty">{{ detailCopy.emptyDetailText }}</div>
            </div>
            <div id="ticketNeed">
              <div class="proDetial">{{ detailCopy.ticketNoticeLabel }}</div>
              <ul>
                <li v-for="item in ticketNeedInfo">
                  <span>{{ item.name }}</span>
                  <div>{{ item.value }}</div>
                </li>

              </ul>
            </div>
            <div id="watchNeed">
              <div class="proDetial">{{ detailCopy.watchNoticeLabel }}</div>
              <ul>
                <li v-for="item in watchNeedInfo">
                  <span v-if="item.value!=''">{{ item.name }}</span>
                  <div v-if="item.value!=''">{{ item.value }}</div>
                </li>

              </ul>
            </div>
          </div>
        </div>
        <div class="box-right">
          <div class="service">

            <div class="service-note">
              <div class="service-name" v-if="detailList.permitRefund!=''">
                <i class="icon-no" v-if="detailList.permitRefund=='0'"></i><span v-if="detailList.permitRefund=='0'">不支持退</span>
                <i class="icon-yes" v-if="detailList.permitRefund=='1'"></i><span v-if="detailList.permitRefund=='1'">条件退</span>
                <i class="icon-yes" v-if="detailList.permitRefund=='2'"></i><span v-if="detailList.permitRefund=='2'">全部退</span>
              </div>
              <div class="service-desc" v-if="detailList.refundExplain!=''">{{ detailList.refundExplain }}</div>
              <div class="service-name" v-if="detailList.relNameTicketEntrance!=''">
                <i class="icon-no" v-if="detailList.relNameTicketEntrance=='0'"></i><span
                  v-if="detailList.relNameTicketEntrance=='0'">{{ detailCopy.noRealNameText }}</span>
                <i class="icon-yes" v-if="detailList.relNameTicketEntrance=='1'"></i><span
                  v-if="detailList.relNameTicketEntrance=='1'">{{ detailCopy.realNameText }}</span>

              </div>
              <div class="service-desc"  v-if="detailList.relNameTicketEntranceExplain!=''">{{ detailList.relNameTicketEntranceExplain }}</div>
              <div class="service-name"   v-if="detailList.permitChooseSeat!=''">
                <i class="icon-no" v-if="detailList.permitChooseSeat=='0'"></i><span
                  v-if="detailList.permitChooseSeat=='0'">不支持选座</span>
                <i class="icon-yes" v-if="detailList.permitChooseSeat=='1'"></i><span
                  v-if="detailList.permitChooseSeat=='1'">支持选座</span>

              </div>
              <div class="service-desc"  v-if="detailList.chooseSeatExplain!=''">{{ detailList.chooseSeatExplain }}</div>
              <div class="service-name" v-if="detailList.electronicDeliveryTicket!=''">
                <i class="icon-no" v-if="detailList.electronicDeliveryTicket=='0'"></i><span
                  v-if="detailList.electronicDeliveryTicket=='0'">无票</span>
                <i class="icon-yes" v-if="detailList.electronicDeliveryTicket=='1'"></i><span
                  v-if="detailList.electronicDeliveryTicket=='1'">{{ detailCopy.badge }}</span>
                <i class="icon-yes" v-if="detailList.electronicDeliveryTicket=='2'"></i><span
                  v-if="detailList.electronicDeliveryTicket=='2'">快递票</span>

              </div>
              <div class="service-desc"  v-if="detailList.electronicDeliveryTicketExplain!=''">{{ detailList.electronicDeliveryTicketExplain }}</div>
              <div class="service-name"  v-if="detailList.electronicInvoice!=''">
                <i class="icon-no" v-if="detailList.electronicInvoice=='0'"></i><span
                  v-if="detailList.electronicInvoice=='0'">纸质发票</span>
                <i class="icon-yes" v-if="detailList.electronicInvoice=='1'"></i><span
                  v-if="detailList.electronicInvoice=='1'">电子发票</span>

              </div>
              <div class="service-desc"  v-if="detailList.electronicInvoiceExplain!=''">{{ detailList.electronicInvoiceExplain }}</div>
            </div>

          </div>
          <div class="box-like">
            为你推荐
          </div>
          <ul class="search__box">
            <li class="search__item" v-for="item in recommendList">
                <router-link :to="{name:'detial',params:{id:item.id}}" class="link" >
                  <img :src="item.itemPicture" alt="" @error="setImageFallback">

                </router-link>

              <div class="search_item_info">
                  <router-link :to="{name:'detial',params:{id:item.id}}"  class="link__title" >
                    {{ item.title }}

                  </router-link>
                <div class="search__item__info__venue">{{ item.place }}</div>
                <div class="search__item__info__venue">{{ safeFormatDate(item.showTime, item.showWeekTime) }}</div>
                <div class="search__item__info__price">￥<strong>{{ item.minPrice }}</strong> 起</div>
              </div>

            </li>
          </ul>
        </div>
      </div>
  </div>
  <Footer></Footer>
</template>

<script setup name="detial">
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {formatDateWithWeekday, setImageFallback} from '@/utils/index'
import {useRoute, useRouter} from 'vue-router'
import {getMovieScreeningList, getProgramDetials} from '@/api/contentDetail'
import {computed, nextTick, reactive, ref, watch} from 'vue'
import {   useMitt } from "@/utils/index";
import {getProgramRecommendList} from "@/api/recommendlist.js"
const emitter = useMitt();
const route = useRoute();
const router = useRouter();
// 获取路由参数
const detailList = ref({})
const ticketCategoryVoList = ref([])
const actvieIndex = ref('')
const menuActive = ref('')
const ticketNeedInfo = ref([])
const watchNeedInfo = ref([])
const detailLoading = ref(false)
const detailError = ref('')
const num = ref(1)
const countPrice = ref('')
const allPrice = ref('')
//票档id
const ticketCategoryId = ref('')
const movieScreeningList = ref([])
const movieScreeningLoading = ref(false)
const selectedScreeningIndex = ref(0)
const MOVIE_CATEGORY_NAME = '电影'
const MOVIE_PARENT_CATEGORY_ID = 22
//推荐节目列表入参
const recommendParams = reactive({
  areaId: undefined,
  parentProgramCategoryId: undefined,
  programId: undefined
})
//推荐列表数据
const recommendList = ref([])
const ticketLimit = computed(() => Number(detailList.value?.perOrderLimitPurchaseCount || 6))
const selectedMovieScreening = computed(() => movieScreeningList.value[selectedScreeningIndex.value] || null)
const screeningOptions = computed(() => {
  if (!isMovieProgram()) {
    return []
  }
  const cinemaId = selectedMovieScreening.value?.cinemaId
  if (!cinemaId) {
    return movieScreeningList.value
  }
  return movieScreeningList.value.filter(item => Number(item.cinemaId) === Number(cinemaId))
})
const canBuy = computed(() => {
  if (isMovieProgram()) {
    return Boolean(detailList.value?.screeningId)
        || movieScreeningList.value.some(item => Boolean(item?.id))
  }
  return Boolean(ticketCategoryVoList.value.length)
})
const buyDisabledText = computed(() => {
  if (isMovieProgram()) {
    return movieScreeningLoading.value ? '场次加载中' : '暂无可售场次'
  }
  return '暂不可购买'
})
const detailCopy = computed(() => {
  if (isMovieProgram()) {
    return {
      badge: '电子影票',
      timeLabel: '放映时间',
      placeLabel: '影院',
      venueSelectorLabel: '影院',
      localTimeNotice: '放映时间以影院当地时间为准',
      sessionLabel: '场次',
      ticketLabel: '影厅/票档',
      buyNowLabel: '立即购票',
      seatBuyLabel: '选座购票',
      projectMenuLabel: '影片详情',
      ticketNoticeLabel: '购票须知',
      watchNoticeLabel: '观影须知',
      detailTitle: '影片介绍',
      emptyDetailText: '暂无影片图文介绍',
      noRealNameText: '不实名购票和入场',
      realNameText: '实名购票和入场'
    }
  }
  return {
    badge: '电子票',
    timeLabel: '时间',
    placeLabel: '场馆',
    venueSelectorLabel: '城市',
    localTimeNotice: '场次时间均为演出当地时间',
    sessionLabel: '场次',
    ticketLabel: '票档',
    buyNowLabel: '立即购买',
    seatBuyLabel: '选座购买',
    projectMenuLabel: '项目详情',
    ticketNoticeLabel: '购票须知',
    watchNoticeLabel: '观演须知',
    detailTitle: '活动介绍',
    emptyDetailText: '暂无项目图文介绍',
    noRealNameText: '不实名购票和入场',
    realNameText: '实名购票和入场'
  }
})
const placeLine = computed(() => {
  if (isMovieProgram()) {
    return detailList.value?.place || ''
  }
  return [detailList.value?.areaName, detailList.value?.place].filter(Boolean).join('|')
})
function decodeDetailContent(value) {
  return String(value || '')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&amp;/g, '&')
      .trim()
}

function isLikelyImageUrl(value) {
  const text = String(value || '').trim()
  if (!text) {
    return false
  }
  return /^data:image\//i.test(text)
      || /^(https?:)?\/\/.+\.(png|jpe?g|webp|gif|svg)(\?.*)?$/i.test(text)
      || /^\/.+\.(png|jpe?g|webp|gif|svg)(\?.*)?$/i.test(text)
}

const imageUrlPattern = /((?:https?:)?\/\/[^\s"'<>，,；;]+?\.(?:png|jpe?g|webp|gif|svg)(?:\?[^\s"'<>，,；;]*)?|\/[^\s"'<>，,；;]+?\.(?:png|jpe?g|webp|gif|svg)(?:\?[^\s"'<>，,；;]*)?|data:image\/[a-zA-Z+.-]+;base64,[A-Za-z0-9+/=]+)/ig
const detailContent = computed(() => decodeDetailContent(detailList.value?.detail))
const detailContentIsHtml = computed(() => /<\/?[a-z][\s\S]*>/i.test(detailContent.value))
const detailBlocks = computed(() => {
  const content = detailContent.value
  if (!content) {
    return []
  }
  if (detailContentIsHtml.value) {
    return [{
      type: 'html',
      value: enhanceDetailHtml(content)
    }]
  }
  return buildPlainDetailBlocks(content)
})

function enhanceDetailHtml(html) {
  return String(html || '')
      .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
      .replace(/<img(?![^>]*\bloading=)([^>]*)>/gi, '<img loading="lazy"$1>')
      .replace(/<img(?![^>]*\bonerror=)([^>]*)>/gi, `<img onerror="this.style.display='none'"$1>`)
}

function buildPlainDetailBlocks(content) {
  const blocks = []
  const lines = String(content || '').split(/[\n\r]+/)
  lines.forEach(line => {
    const text = line.trim()
    if (!text) {
      return
    }
    imageUrlPattern.lastIndex = 0
    let cursor = 0
    let matched = false
    let match
    while ((match = imageUrlPattern.exec(text)) !== null) {
      matched = true
      const before = text.slice(cursor, match.index).replace(/^[,，;；\s]+|[,，;；\s]+$/g, '')
      if (before) {
        blocks.push({type: 'text', value: before})
      }
      blocks.push({type: 'image', value: match[0]})
      cursor = match.index + match[0].length
    }
    if (!matched) {
      blocks.push({
        type: isLikelyImageUrl(text) ? 'image' : 'text',
        value: text
      })
      return
    }
    const rest = text.slice(cursor).replace(/^[,，;；\s]+|[,，;；\s]+$/g, '')
    if (rest) {
      blocks.push({type: 'text', value: rest})
    }
  })
  return blocks
}

function hideBrokenDetailImage(event) {
  const figure = event?.target?.closest?.('.detail-figure')
  if (figure) {
    figure.classList.add('is-broken')
  }
}

const venueOptions = computed(() => {
  if (!detailList.value?.id) {
    return []
  }
  if (isMovieProgram()) {
    const cinemaMap = new Map()
    movieScreeningList.value.forEach((screening, index) => {
      const cinemaKey = screening.cinemaId || screening.cinemaName || index
      if (cinemaMap.has(cinemaKey)) {
        return
      }
      cinemaMap.set(cinemaKey, {
        ...screening,
        screeningId: screening.id,
        screeningIndex: index,
        programId: detailList.value?.id,
        place: screening.cinemaName || detailList.value?.place,
        areaIdName: screening.cinemaAddress || detailList.value?.areaName
      })
    })
    if (cinemaMap.size) {
      return Array.from(cinemaMap.values())
    }
  }
  const groupOptions = detailList.value?.programGroupVo?.programSimpleInfoVoList || []
  if (groupOptions.length) {
    return groupOptions
  }
  return [{
    programId: detailList.value?.id,
    areaId: detailList.value?.areaId,
    areaIdName: detailList.value?.areaName,
    place: detailList.value?.place,
    showTime: detailList.value?.showTime,
    showWeekTime: detailList.value?.showWeekTime
  }]
})
watch(
  () => route.params.id,
  id => {
    getProgramDetialsList(Number(id))
  },
  {immediate: true}
)

function isMovieProgram(program = detailList.value) {
  return program?.parentProgramCategoryName === MOVIE_CATEGORY_NAME
      || Number(program?.parentProgramCategoryId) === MOVIE_PARENT_CATEGORY_ID
}

function safeFormatDate(date, week) {
  if (!date || Number.isNaN(new Date(date).getTime())) {
    return '时间待定'
  }
  return formatDateWithWeekday(date, week || '')
}

function isCurrentVenue(item) {
  if (isMovieProgram()) {
    return Number(item?.cinemaId) === Number(selectedMovieScreening.value?.cinemaId)
  }
  return Number(item?.programId) === Number(detailList.value?.id)
}

function getVenueTitle(item) {
  if (isMovieProgram()) {
    return item?.cinemaName || item?.place || detailList.value?.place || item?.areaIdName || detailList.value?.areaName
  }
  return item?.areaIdName || detailList.value?.areaName
}

function getVenueMeta(item) {
  if (!isMovieProgram()) {
    return ''
  }
  if (item?.cinemaAddress) {
    return item.cinemaAddress
  }
  if (!item?.showTime) {
    return item?.areaIdName || ''
  }
  return safeFormatDate(item.showTime, item.showWeekTime)
}

function switchVenue(item) {
  if (isMovieProgram()) {
    if (isCurrentVenue(item)) {
      return
    }
    const index = typeof item?.screeningIndex === 'number'
        ? item.screeningIndex
        : movieScreeningList.value.findIndex(screening => Number(screening.cinemaId) === Number(item?.cinemaId))
    applyMovieScreening(movieScreeningList.value[index] || item, index < 0 ? 0 : index)
    return
  }
  if (!item?.programId || isCurrentVenue(item)) {
    return
  }
  router.replace({ name: 'detial', params: { id: item.programId } })
}

function resetDetailState() {
  detailList.value = {}
  ticketCategoryVoList.value = []
  ticketNeedInfo.value = []
  watchNeedInfo.value = []
  recommendList.value = []
  actvieIndex.value = ''
  menuActive.value = 1
  ticketCategoryId.value = ''
  countPrice.value = ''
  allPrice.value = ''
  num.value = 1
  movieScreeningList.value = []
  movieScreeningLoading.value = false
  selectedScreeningIndex.value = 0
}

function getProgramDetialsList(programId) {
  if (!programId) {
    resetDetailState()
    detailError.value = '项目地址无效'
    detailLoading.value = false
    return
  }
  resetDetailState()
  detailError.value = ''
  detailLoading.value = true
  recommendParams.programId = programId
  getProgramDetials({id: programId}).then(response => {
    if (String(response.code) !== '0' || !response.data?.id) {
      detailError.value = response.message || '项目详情暂不可用'
      return
    }
    detailList.value = response.data || {}
    if (isMovieProgram()) {
      detailList.value.permitChooseSeat = '1'
    }
    ticketCategoryVoList.value = detailList.value.ticketCategoryVoList || []
    const firstTicketCategory = ticketCategoryVoList.value[0]
    if (firstTicketCategory) {
      actvieIndex.value = 0
      countPrice.value = firstTicketCategory.price
      ticketCategoryId.value = firstTicketCategory.id
      allPrice.value = Number(countPrice.value || 0) * Number(num.value || 1)
    } else {
      countPrice.value = 0
      ticketCategoryId.value = ''
      allPrice.value = 0
    }
    buildNoticeInfo()
    recommendParams.areaId = detailList.value.areaId
    recommendParams.parentProgramCategoryId = detailList.value.parentProgramCategoryId
    if (isMovieProgram()) {
      loadMovieScreenings(programId)
    }
    getRecommendList()
  }).catch(() => {
    resetDetailState()
    detailError.value = '项目详情加载失败'
  }).finally(() => {
    detailLoading.value = false
  })
}

function buildNoticeInfo() {
  const movie = isMovieProgram()
  ticketNeedInfo.value = [{
      name: '限购规则',
      value: detailList.value.purchaseLimitRule,
    }, {
      name: movie ? '退改规则' : '退票/换票规则',
      value: detailList.value.refundTicketRule,
    }, {
      name: movie ? '入场/取票规则' : '入场规则',
      value: detailList.value.entryRule,
    }, {
      name: movie ? '儿童观影' : '儿童购票',
      value: detailList.value.childPurchase,
    }, {
      name: '发票说明',
      value: detailList.value.invoiceSpecification,
    }, {
      name: '实名购票规则',
      value: detailList.value.realTicketPurchaseRule,
    }, {
      name: movie ? '异常订单说明' : '异常排单说明',
      value: detailList.value.abnormalOrderDescription,
    }]
  watchNeedInfo.value = movie ? [{
    name: '片长',
    value: detailList.value.performanceDuration
  }, {
    name: '入场时间',
    value: detailList.value.entryTime
  }, {
    name: '主演',
    value: detailList.value.mainActor
  }, {
    name: '放映版本',
    value: detailList.value.minPerformanceDuration
  }, {
    name: '观影须知',
    value: detailList.value.prohibitedItem
  }, {
    name: '寄存说明',
    value: detailList.value.depositSpecification
  }] : [{
    name: '演出时长',
    value: detailList.value.performanceDuration
  }, {
    name: '入场时间',
    value: detailList.value.entryTime
  }, {
    name: '最低演出曲目',
    value: detailList.value.minPerformanceCount
  }, {
    name: '主要演员',
    value: detailList.value.mainActor
  }, {
    name: '最低演出时长',
    value: detailList.value.minPerformanceDuration
  }, {
    name: '禁止携带物品',
    value: detailList.value.prohibitedItem
  }, {
    name: '寄存说明',
    value: detailList.value.depositSpecification
  }]
}

function normalizeScreeningPlace(screening) {
  return [screening?.cinemaName, screening?.hallName].filter(Boolean).join(' ')
}

function getScreeningMeta(screening) {
  return [screening?.hallName, screening?.version, screening?.language].filter(Boolean).join(' / ')
}

function applyMovieScreening(screening, index = 0) {
  if (!screening) {
    return
  }
  selectedScreeningIndex.value = index
  const place = normalizeScreeningPlace(screening)
  detailList.value = {
    ...detailList.value,
    screeningId: screening.id,
    showTime: screening.showTime,
    showWeekTime: screening.showWeekTime,
    areaName: screening.cinemaAddress || detailList.value.areaName,
    place: place || screening.cinemaName || detailList.value.place,
    selectedMovieScreening: screening
  }
  if (screening.lowestPrice) {
    countPrice.value = screening.lowestPrice
    allPrice.value = Number(countPrice.value || 0) * Number(num.value || 1)
  }
}

function screeningClick(screening) {
  const index = movieScreeningList.value.findIndex(item => Number(item.id) === Number(screening?.id))
  applyMovieScreening(screening, index < 0 ? 0 : index)
}

function loadMovieScreenings(programId) {
  movieScreeningLoading.value = true
  getMovieScreeningList({programId}).then(response => {
    movieScreeningList.value = response.data || []
    applyMovieScreening(movieScreeningList.value[0], 0)
  }).catch(() => {
    movieScreeningList.value = []
  }).finally(() => {
    movieScreeningLoading.value = false
  })
}

const ticketClick = (item, index) => {
    actvieIndex.value = index;
    countPrice.value = item.price; // 显式更新单价
    allPrice.value = item.price * num.value; // 总价 = 新单价 × 当前数量
    ticketCategoryId.value = item.id;
    // actvieIndex.value = index     原本代码
    // allPrice.value = item.price      原本代码
    // ticketCategoryId.value = item.id      原本代码
}
const detialClick = (targetId, index) => {
  menuActive.value = index
  nextTick(() => {
    document.getElementById(targetId)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  })
}
const handleChange = (value) => {
  const priceEach = Number(countPrice.value || 0)
  allPrice.value = priceEach * value

}
const nowBuy=()=>{
  router.replace({path:'/order/index',state:
        {'detailList':JSON.stringify(detailList.value),'allPrice':allPrice.value,
          'countPrice':countPrice.value,'num':num.value,'ticketCategoryId':ticketCategoryId.value,
          'screeningId': detailList.value.screeningId}})

}

const seatBuy=()=>{
  router.replace({path:'/order/seatSelect',state:
        {'detailList':JSON.stringify(detailList.value),'screeningId': detailList.value.screeningId}})
}

//节目推荐列表
function getRecommendList(){
  getProgramRecommendList(recommendParams).then(response => {
    recommendList.value = (response.data || []).slice(0,6);
  })
}







</script>

<style scoped lang="scss">
.detail-state {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  min-height: 520px;
  margin: 28px auto 72px;
  border-radius: var(--radius-lg);
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.detail-loading {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 28px;
  padding: 24px;

  .poster-skeleton,
  .copy-skeleton span,
  .copy-skeleton div {
    border-radius: 10px;
    background: linear-gradient(90deg, #f2ecdf 0%, #fbf7ed 48%, #f2ecdf 100%);
    background-size: 220% 100%;
    animation: detailPulse 1.4s ease-in-out infinite;
  }

  .poster-skeleton {
    width: 260px;
    height: 346px;
  }

  .copy-skeleton {
    padding-top: 8px;

    span {
      display: block;
      height: 18px;
      margin-bottom: 18px;
    }

    span:first-child {
      width: 62%;
      height: 28px;
    }

    span:nth-child(2) {
      width: 46%;
    }

    span:nth-child(3) {
      width: 68%;
    }

    div {
      width: 180px;
      height: 40px;
      margin-top: 28px;
      border-radius: 999px;
    }
  }
}

.detail-empty-page {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 56px 24px;
  color: var(--brand-muted);
  text-align: center;

  h1 {
    margin: 0;
    color: var(--brand-dark);
    font-size: 22px;
    line-height: 32px;
    font-weight: 500;
  }

  p {
    margin: 10px 0 22px;
    font-size: 14px;
  }

  button {
    height: 38px;
    padding: 0 18px;
    border: 0;
    border-radius: 999px;
    background: var(--brand-primary);
    color: var(--brand-dark);
    cursor: pointer;
  }
}

@keyframes detailPulse {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}

.app-container {
  width: 1200px;
  margin: 0 auto;

  .wrapper {
    display: flex;

    .box-left {
      flex: 1;

      .box-detail {
        position: relative;
        padding: 40px 0 30px;
        min-height: 360px;

        .count {
          padding-left: 330px;
          font-size: 22px;
          color: #000;

          .box-img {
            img {
              position: absolute;
              left: 30px;
              top: 40px;
              width: 270px;
              height: 360px;
            }
          }

          .order {
            position: relative;
            padding-right: 30px;

            .title {
              .tips {
                display: inline-block;
                width: 60px;
                height: 24px;
                position: relative;
                top: -3px;
                text-align: center;
                line-height: 24px;
                //background: -webkit-linear-gradient(135deg, var(--brand-primary-strong), #ff5593);
                //background: -moz-linear-gradient(135deg, var(--brand-primary-strong) 0, #ff5593 100%);
                //background: linear-gradient(-45deg, var(--brand-primary-strong), #ff5593);
                background: #96A3FF;
                z-index: 10;
                font-size: 14px;
                color: #fff;
                border-bottom-right-radius: 10px;
                border-top-left-radius: 3px;
                border-top-right-radius: 3px
              }

              span {

              }
            }

            .address {
              position: relative;
              font-size: 16px;
              color: #4a4a4a;
              margin-top: 21px;
              zoom: 1;

              .time {
                padding-bottom: 10px;
              }

              .place {
                .addr {
                  display: inline-block;
                }
              }
            }

            .notice {
              margin-top: 18px;
              padding: 12px 15px;
              font-size: 12px;
              background: #f6f6f6;
              border-radius: 4px;
              position: relative;

              .ticket-type {
                display: inline-block;
                height: 24px;
                line-height: 23px;
                text-align: center;
                padding: 0 7px;
                color: var(--brand-primary-strong);
                background: #ffe7ef;
                border-radius: 0 100px 100px 0;
                margin-bottom: 10px;
                font-size: 14px;

                span {
                  vertical-align: middle;
                }
              }

              .content {
                -webkit-box-flex: 1;
                -webkit-flex: 1;
                -moz-box-flex: 1;
                flex: 1;
                display: -webkit-box;
                -webkit-line-clamp: 3;
                -webkit-box-orient: vertical;
                overflow: hidden;
                text-overflow: ellipsis;
                cursor: pointer;

                div {

                }

                .notice-content {
                  color: #999;
                }
              }
            }

              .citys {
                margin-top: 24px;

                span {
                display: inline-block;
                font-size: 16px;
                color: #000;
              }

              .city-list {
                display: inline-block;
                font-size: 12px;
                width: 420px;
                height: 40px;
                overflow: hidden;
                vertical-align: top;

                .city-item {
                  color: #000;
                  width: 78px;
                  height: 40px;
                  -webkit-box-sizing: border-box;
                  -moz-box-sizing: border-box;
                  box-sizing: border-box;
                  border: 1px solid #eee;
                  text-align: center;
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                  display: -webkit-inline-flex;
                  display: inline-flex;
                  -webkit-box-align: center;
                  -webkit-align-items: center;
                  -moz-box-align: center;
                  align-items: center;
                  -webkit-box-pack: center;
                  -webkit-justify-content: center;
                  -moz-box-pack: center;
                  justify-content: center;
                  margin-right: 6px;
                  margin-bottom: 8px;
                  cursor: pointer;
                  border-radius: 3px;
                  margin-left: 17px;
                }


              }

                .city-more {
                  float: right;
                  font-size: 12px;
                  color: #4a4a4a;
                  cursor: pointer;
                  margin-top: 10px;
                }

                &.movie-venue {
                  display: flex;
                  align-items: flex-start;
                  gap: 15px;

                  > span {
                    flex: 0 0 auto;
                    line-height: 40px;
                  }

                  .city-list {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 8px;
                    width: auto;
                    height: auto;
                    overflow: visible;
                    flex: 1;

                    .city-item {
                      width: auto;
                      min-width: 190px;
                      height: auto;
                      min-height: 46px;
                      padding: 8px 12px;
                      margin: 0;
                      flex-direction: column;
                      align-items: flex-start;
                      text-align: left;
                      white-space: normal;
                      line-height: 1.35;

                      .venue-title {
                        display: block;
                        font-size: 13px;
                        color: #111;
                        max-width: 230px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                      }

                      .venue-meta {
                        display: block;
                        margin-top: 3px;
                        font-size: 12px;
                        color: #777;
                      }
                    }
                  }
                }
              }

            .order-box {
              .notice-time {
                color: #999;
                font-size: 12px;
                margin: 24px 0 9px;
              }

              .order-time {
                display: -webkit-box;
                display: -webkit-flex;
                display: -moz-box;
                display: flex;
                margin-top: 0px;

                .order-name {
                  display: inline-block;
                  font-size: 16px;
                  color: #000;
                  height: 48px;
                }

                .select {
                  display: inline-block;
                  vertical-align: top;
                  margin-left: 15px;
                  -webkit-box-flex: 1;
                  -webkit-flex: 1;
                  -moz-box-flex: 1;
                  flex: 1;

                  .select-list {
                    display: -webkit-box;
                    display: -webkit-flex;
                    display: -moz-box;
                    display: flex;
                    -webkit-box-orient: horizontal;
                    -webkit-box-direction: normal;
                    -webkit-flex-direction: row;
                    -moz-box-orient: horizontal;
                    -moz-box-direction: normal;
                    flex-direction: row;
                    -webkit-flex-wrap: wrap;
                    flex-wrap: wrap;
                    float: left;

                    .select-list-item {
                      -webkit-box-orient: vertical;
                      -webkit-box-direction: normal;
                      -webkit-flex-direction: column;
                      -moz-box-orient: vertical;
                      -moz-box-direction: normal;
                      flex-direction: column;
                      -webkit-box-pack: center;
                      -webkit-justify-content: center;
                      -moz-box-pack: center;
                      justify-content: center;
                      display: -webkit-box;
                      display: -webkit-flex;
                      display: -moz-box;
                      display: flex;
                      float: left;
                      font-size: 12px;
                      color: #000;
                      padding: 10px 24px;
                      margin: 0 6px 6px 0;
                      position: relative;
                      cursor: pointer;
                      border-radius: 3px;
                      background: #f6f7f8;
                      border: 1px solid rgba(0, 0, 0, .1);
                      text-align: left;

                      span {
                        text-align: left;
                        margin: 1px 0;
                      }

                      .notticket {
                        background-color: transparent;
                        color: #6a7a99 !important;
                        border-color: #6a7a99 !important;
                      }
                    }
                  }
                }

                .order-count {
                  font-size: 21px;
                  color: var(--brand-primary-strong);
                  margin-left: 9px;
                  font-weight: bold;
                }
                .count-detial{
                  position: relative;
                  font-size: 12px;
                  color: #000;
                  cursor: pointer;
                  margin-top: 10px;
                  margin-left: 5px;
                }
              }


            }

            .order-price {
              display: flex;

              .num {
                font-size: 16px;
                color: #000;
                height: 48px;
                flex: 0.1;
              }

              .count {
                display: inline-block;
                padding-left: 0px;
                width: 340px;
                flex: 1;

                .count-info {
                  //margin-top: 30px;
                  float: left;
                }

                .num-limit {
                  font-size: 12px;
                  color: #999;
                  line-height: 22px;
                  vertical-align: text-top;
                  display: inline-block;
                  float: left;
                  margin-top: 12px;
                  margin-left: 10px;
                }
              }
            }

            .buy {
              display: inline-block;
              margin-top: 20px;
              .buy-link-now{
                width: 100px;
                display: inline-block;
                margin-bottom: 24px;
                margin-right: 15px;
                height: 35px;
                line-height: 35px;
                font-size: 12px;
                text-align: center;
                color: #fff;
                cursor: pointer;
                background-color: var(--brand-primary-strong);
                border-radius: 36px;
                &.disabled {
                  cursor: not-allowed;
                  opacity: .58;
                }
              }
              .buy-link-seat{
                width: 100px;
                display: inline-block;
                margin-bottom: 24px;
                height: 35px;
                line-height: 35px;
                font-size: 12px;
                text-align: center;
                color: var(--brand-primary-strong);
                cursor: pointer;
                background-color: #fff;
                border: 1px solid var(--brand-primary-strong);
                border-radius: 36px;
              }
              .buy-link-seat:hover{
                background-color: rgba(244, 183, 0, 0.12);
              }


              .title {
                font-size: 18px;
                line-height: 24px;
                color: var(--brand-primary-strong);
                cursor: pointer;
              }

              .subtitle {
                margin-top: 4px;
                font-size: 12px;
                line-height: 18px;
                color: #999;

              }

              .qrcode {
                margin-top: 12px;
                border: 1px solid #ededed;
                border-radius: 12px;
                padding: 0 28px;

                .tip {
                  font-size: 14px;
                  line-height: 22px;
                  color: #333;
                  margin: 12px auto 4px;
                }

                .J_qrcodeImg {
                  display: -webkit-box;
                  display: -webkit-flex;
                  display: -moz-box;
                  display: flex;
                  -webkit-box-align: center;
                  -webkit-align-items: center;
                  -moz-box-align: center;
                  align-items: center;
                  margin-bottom: 8px;
                }

                .buy-link {
                  font-size: 12px;
                  line-height: 18px;
                  color: #999;
                  margin: 4px auto 12px;
                  text-decoration: underline;
                  cursor: pointer;
                }
              }

            }

          }
        }

      }

      .box-item {
        width: 100%;
        height: 800px;

        .box-menu {
          height: 54px;
          line-height: 54px;
          padding-left: 30px;
          border-top: 1px solid #e2e2e2;
          border-bottom: 1px solid #e2e2e2;
          //padding: 60px 30px 0;

          .menu-children {
            appearance: none;
            height: 54px;
            padding: 0;
            border: 0;
            border-bottom: 3px solid transparent;
            background: transparent;
            font-family: inherit;
            font-size: 16px;
            color: #9c9ca5;
            margin-right: 60px;
            cursor: pointer;
          }

        }

        #projectDetial {
          //width: 100%;
          //height:100%;
          padding: 60px 30px 0;

          .proDetial {
            padding-bottom: 14px;
            margin-bottom: 23px;
            font-size: 20px;
            color: #000;
            border-bottom: 1px solid #e2e2e2;
          }

          img {
            width: 100%;
            height:100%;
            display: block;
            padding-bottom: 50px;
          }
        }

        #ticketNeed {
          width: 100%;
          height: 600px;
          padding: 60px 30px 0;

          .proDetial {
            padding-bottom: 14px;
            margin-bottom: 23px;
            font-size: 20px;
            color: #000;
            border-bottom: 1px solid #e2e2e2;
          }

          ul {
            margin: 0;
            padding: 0;

            li {
              list-style: none;

              span {
                width: 100%;
                height: 20px;
                line-height: 20px;
                display: block;
                color: #999;
                font-size: 13px;
              }

              div {
                line-height: 26px;
                padding-bottom: 15px;
                font-size: 16px;
                color: #4a4a4a;
              }
            }
          }
        }

        #watchNeed {
          width: 100%;
          height: 500px;
          padding: 60px 30px 0;
          //padding-bottom: 14px;
          //margin-bottom: 23px;
          //font-size: 20px;
          //color: #000;
          //border-bottom: 1px solid #e2e2e2;
          .proDetial {
            padding-bottom: 14px;
            margin-bottom: 23px;
            font-size: 20px;
            color: #000;
            border-bottom: 1px solid #e2e2e2;
          }

          ul {
            margin: 0;
            padding: 0;

            li {
              list-style: none;

              span {
                width: 100%;
                height: 20px;
                line-height: 20px;
                display: block;
                color: #999;
                font-size: 13px;
              }

              div {
                line-height: 26px;
                padding-bottom: 15px;
                font-size: 16px;
                color: #4a4a4a;
              }
            }
          }
        }
      }
    }

    .box-right {
      box-sizing: border-box;
      width: 320px;
      border-left: none;
      padding: 40px 18px 0;

      .service {
        padding: 24px 15px;
        background: #fafafa;
        border: 1px solid #ebebeb;

        .sit {
          display: block;
          margin-bottom: 24px;
          height: 35px;
          line-height: 35px;
          font-size: 12px;
          text-align: center;
          color: #fff;
          cursor: pointer;
          background-color: var(--brand-primary-strong);
          border-radius: 36px;
        }

        .service-note {
          margin-bottom: 18px;

          .service-name {
            font-size: 14px;
            margin-bottom: 10px;

            .icon {
              display: inline-block;
              width: 12px;
              height: 12px;
              background-repeat: no-repeat;
              -webkit-background-size: 12px 12px;
              background-size: 12px 12px
            }

          }

          .service-desc {
            //margin-top: 6px;
            font-size: 12px;
            color: #999;
            margin-bottom: 6px;
          }
        }
      }
      .box-like{
        margin-top: 24px;
        margin-bottom: 24px;
        font-size: 20px;
        color: #000;
        line-height: 28px;
      }
      .search__box{
        list-style: none;
        margin: 0;
        padding: 0;
        .search__item{
          width: 100%;
          height: 160px;
          margin-bottom: 30px;
          display: flex;
          .link{
            width: 120px;
            height: 100%;
            display: inline-block;
            img{
              width: 120px;
              height: 100%;
            }
          }
          .search_item_info{
            width: 157px;
            height: 160px;
            .link__title{
              display: -webkit-box;
              -webkit-box-orient: vertical;
              -webkit-line-clamp: 2;
              line-clamp: 2;
              overflow: hidden;
              font-size: 14px;
              color: #4a4a4a;
              padding-left: 17px;
            }
            .search__item__info__venue{
              margin-top: 12px;
              color: #9b9b9b;
              padding-left: 20px;
              font-size: 12px;

            }
            .search__item__info__price{
              font-size: 16px;
              color: var(--brand-primary-strong);
              margin-top: 39px;
              padding-left: 20px;
              font-weight: bold;
            }
          }
        }
      }
    }
  }


}

.active {
  border-color: var(--brand-primary-strong);
  color: var(--brand-primary-strong);
  background: #fff;
}

.activeCity {
  color: var(--brand-primary-strong) !important;
  border: 1px solid var(--brand-primary-strong) !important;
}

.ticket {
  color: var(--brand-primary-strong) !important;
  border: 1px solid var(--brand-primary-strong) !important;
}

.menuActive {
  //position: relative;
  font-size: 20px;
  color: #000;
  border-bottom: 2px solid var(--brand-primary-strong);
}

.icon-no {
  display: inline-block;
  width: 12px;
  height: 12px;
  background-repeat: no-repeat;
  background-size: 12px 12px;
  background: url('/src/assets/section/no.png')
}

.icon-yes {
  display: inline-block;
  width: 12px;
  height: 12px;
  background-repeat: no-repeat;
  background-size: 12px 12px;
  background: url('/src/assets/section/yes.png')
}

/* 潮声详情页体验层 */
.app-container {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter))) !important;
  padding: 28px 0 48px;

  .wrapper {
    display: grid !important;
    grid-template-columns: minmax(0, 1fr) 292px;
    gap: 22px;
    align-items: start;

    .box-left {
      min-width: 0;

      .box-detail {
        min-height: auto !important;
        padding: 24px !important;
        border: 1px solid var(--brand-line);
        border-radius: var(--radius-lg);
        background: var(--brand-card);
        box-shadow: var(--brand-shadow-soft);

        > .count {
          display: grid;
          grid-template-columns: 260px minmax(0, 1fr);
          gap: 28px;
          padding-left: 0 !important;
          color: var(--brand-text);
          font-size: 16px;

          .box-img img {
            position: static !important;
            width: 260px !important;
            height: 346px !important;
            display: block;
            border-radius: 10px;
            object-fit: cover;
            box-shadow: 0 16px 34px rgba(40, 32, 18, .12);
          }
        }

        .order {
          padding-right: 0 !important;

          .title {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            color: var(--brand-dark);
            font-size: 22px;
            line-height: 1.42;
            font-weight: 600;

            .tips {
              flex: 0 0 auto;
              width: auto !important;
              height: 22px !important;
              line-height: 22px !important;
              padding: 0 8px;
              top: 4px !important;
              border-radius: 999px !important;
              color: var(--brand-dark);
              background: var(--brand-primary) !important;
              font-size: 11px !important;
              font-weight: 500;
            }
          }

          .address {
            margin-top: 14px !important;
            color: var(--brand-muted) !important;
            font-size: 14px !important;
            line-height: 1.65;
          }

          .notice {
            margin-top: 18px !important;
            padding: 14px 16px !important;
            border: 1px solid var(--brand-line);
            border-radius: 10px !important;
            background: #f8f3e7 !important;
          }

          .citys {
            margin-top: 16px !important;
            display: flex;
            align-items: flex-start;
            gap: 12px;

            > span {
              flex: 0 0 42px;
              color: var(--brand-muted) !important;
              font-size: 13px !important;
              font-weight: 500;
              line-height: 32px;
            }

            .city-list {
              width: auto !important;
              height: auto !important;
              display: flex !important;
              flex: 1;
              flex-wrap: wrap;
              gap: 8px;
              overflow: visible !important;

              .city-item {
                width: auto !important;
                min-width: 76px;
                height: auto !important;
                min-height: 32px;
                margin: 0 !important;
                padding: 6px 11px;
                border-radius: 999px !important;
                border-color: var(--brand-line) !important;
                background: #fff;
                font-size: 13px;
                transition: border-color .18s ease, background .18s ease;

                &:hover {
                  border-color: var(--brand-primary-strong) !important;
                }
              }
            }

            .city-more {
              display: none;
            }

            &.movie-venue .city-list .city-item {
              border-radius: 10px !important;
              min-width: 180px;
              background: #fff;
            }
          }

          .order-box {
            margin-top: 14px;

            .notice-time {
              margin: 0 0 6px !important;
              color: var(--brand-subtle) !important;
              font-size: 12px;
            }

            .order-time {
              display: flex !important;
              align-items: flex-start;
              gap: 12px;

              .order-name {
                flex: 0 0 42px;
                height: auto !important;
                color: var(--brand-muted) !important;
                font-size: 13px !important;
                font-weight: 500;
                line-height: 32px;
              }

              .select {
                margin-left: 0 !important;

                .select-list {
                  display: inline-flex !important;
                  flex-wrap: wrap;
                  float: none !important;

                  .select-list-item {
                    float: none !important;
                    min-height: 32px;
                    padding: 6px 12px !important;
                    margin: 0 8px 7px 0 !important;
                    border-radius: 999px !important;
                    border-color: var(--brand-line) !important;
                    background: #fff !important;
                    color: var(--brand-text) !important;
                    font-size: 13px;
                  }
                }
              }

              .order-count {
                color: var(--brand-primary-strong) !important;
                font-size: 21px !important;
                font-weight: 600 !important;
              }
            }
          }

          .order-price {
            align-items: flex-start;
            gap: 12px;
            margin-top: 10px;

            .num {
              flex: 0 0 42px !important;
              height: auto !important;
              color: var(--brand-muted) !important;
              font-size: 13px !important;
              font-weight: 500;
              line-height: 32px;
            }

            .count {
              width: auto !important;
              display: flex !important;
              flex-direction: column;
              gap: 5px;

              .count-info,
              .num-limit {
                float: none !important;
              }

              .num-limit {
                margin: 0 !important;
                color: var(--brand-subtle) !important;
                font-size: 12px;
                line-height: 18px;
              }
            }
          }

          .buy {
            display: flex !important;
            margin-top: 20px !important;

            .buy-link-now {
              width: auto !important;
              min-width: 146px;
              height: 40px !important;
              line-height: 40px !important;
              padding: 0 24px;
              margin: 0 !important;
              border-radius: 999px !important;
              color: var(--brand-dark) !important;
              background: var(--brand-primary) !important;
              font-size: 14px !important;
              font-weight: 500;
              transition: box-shadow .18s ease;

              &:hover:not(.disabled) {
                box-shadow: 0 12px 24px rgba(201, 137, 18, .22);
              }
            }
          }
        }
      }

      .box-item {
        height: auto !important;
        margin-top: 18px;
        border: 1px solid var(--brand-line);
        border-radius: var(--radius-lg);
        background: var(--brand-card);
        box-shadow: var(--brand-shadow-soft);
        overflow: hidden;

        .box-menu {
          height: 54px !important;
          line-height: 54px !important;
          display: flex;
          align-items: center;
          padding-left: 24px !important;
          border-top: 0 !important;
          border-bottom: 1px solid var(--brand-line) !important;
          background: #fff;

          .menu-children {
            color: var(--brand-muted) !important;
            font-weight: 500;
            line-height: 54px;
          }
        }

        #projectDetial,
        #ticketNeed,
        #watchNeed {
          height: auto !important;
          padding: 34px 28px !important;
          scroll-margin-top: 96px;

          .proDetial {
            color: var(--brand-dark) !important;
            border-bottom-color: var(--brand-line) !important;
            font-weight: 500;
          }
        }

        #projectDetial img {
          height: auto !important;
          border-radius: 10px;
        }

        .detail-empty {
          min-height: 128px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: var(--brand-muted);
          font-size: 14px;
          border: 1px dashed var(--brand-border);
          border-radius: 10px;
          background: #fffaf0;
        }
      }
    }

    .box-right {
      width: auto !important;
      position: static;
      top: auto;
      align-self: auto;
      padding: 0 !important;
      max-height: none;
      overflow: visible;
      overscroll-behavior: auto;

      .service {
        padding: 20px !important;
        border: 1px solid var(--brand-line) !important;
        border-radius: var(--radius-lg);
        background: var(--brand-card) !important;
        box-shadow: var(--brand-shadow-soft);
      }

      .box-like {
        margin: 18px 0 10px !important;
        color: var(--brand-dark) !important;
        font-size: 17px !important;
        font-weight: 500;
      }

      .search__box .search__item {
        height: auto !important;
        min-height: 122px;
        margin-bottom: 14px !important;
        gap: 12px;

        .link {
          width: 92px !important;
          height: 122px !important;
          border-radius: 8px;
          overflow: hidden;

          img {
            width: 100% !important;
            height: 100% !important;
            object-fit: cover;
          }
        }

        .search_item_info {
          width: auto !important;
          height: auto !important;
          display: flex;
          flex-direction: column;
          flex: 1;
          min-width: 0;

          .link__title,
          .search__item__info__venue,
          .search__item__info__price {
            padding-left: 0 !important;
          }

          .link__title {
            font-weight: 600;
            line-height: 1.45;
          }

          .search__item__info__venue {
            margin-top: 6px !important;
            line-height: 1.45;
          }

          .search__item__info__price {
            margin-top: auto !important;
            padding-top: 8px;
            font-size: 14px !important;
            line-height: 1.35;
            font-weight: 600 !important;
          }
        }
      }
    }
  }
}

.activeCity,
.ticket {
  position: relative;
  color: var(--brand-dark) !important;
  border-color: var(--brand-primary-strong) !important;
  background: #fff !important;
  font-weight: 500;
  box-shadow: none !important;
}

.select-list-item.ticket {
  padding-right: 24px !important;
}

.menuActive {
  color: var(--brand-dark) !important;
  border-bottom-color: var(--brand-primary-strong) !important;
}

.app-container .wrapper .box-left {
  .box-detail .order {
    .citys .city-list .city-item,
    .order-box .order-time .select .select-list .select-list-item {
      min-height: 34px !important;
      border-radius: 6px !important;
      box-shadow: none !important;
      font-weight: 400;
    }

    .citys.movie-venue .city-list .city-item {
      border-radius: 6px !important;
    }
  }

  .box-item {
    border-radius: 8px !important;

    #projectDetial {
      .detail-figure {
        margin: 0;
        padding: 0;
        overflow: hidden;
        border-radius: 6px;
        background: #fff;
      }

      .detail-content {
        display: grid;
        gap: 12px;
      }

      .detail-figure.is-broken {
        display: none;
      }

      .detail-figure img {
        width: 100% !important;
        max-width: 100%;
        height: auto !important;
        display: block;
        object-fit: contain;
        border-radius: 0 !important;
      }

      .detail-paragraph {
        margin: 0;
        color: var(--brand-text);
        font-size: 15px;
        line-height: 1.9;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .detail-html {
        color: var(--brand-text);
        font-size: 15px;
        line-height: 1.9;
        word-break: break-word;
      }

      .detail-html :deep(p) {
        margin: 0 0 14px;
      }

      .detail-html :deep(img) {
        width: auto !important;
        max-width: 100% !important;
        height: auto !important;
        display: block;
        margin: 14px auto;
        border-radius: 4px !important;
        object-fit: contain;
      }

      .detail-html :deep(figure) {
        margin: 16px 0;
      }

      .detail-html :deep(video) {
        width: 100%;
        max-width: 100%;
        height: auto;
        display: block;
        margin: 14px 0;
        border-radius: 4px;
      }

      .detail-rich-text {
        color: var(--brand-text);
        font-size: 15px;
        line-height: 1.9;

        p {
          margin: 0 0 12px;
        }
      }
    }
  }
}

.app-container .wrapper .box-right {
  .service {
    border-radius: 8px !important;
    box-shadow: 0 10px 26px rgba(40, 32, 18, .06) !important;
  }

  .box-like {
    margin: 18px 0 0 !important;
    padding: 16px 16px 4px;
    border-radius: 8px 8px 0 0;
    background: var(--brand-card);
    box-shadow: 0 10px 26px rgba(40, 32, 18, .06);
  }

  .search__box {
    margin: 0 !important;
    padding: 2px 16px 14px !important;
    border-radius: 0 0 8px 8px;
    background: var(--brand-card);
    box-shadow: 0 12px 26px rgba(40, 32, 18, .06);

    .search__item {
      padding: 12px 0 !important;
      margin: 0 !important;
      border-top: 1px solid #f0eadf;
      background: transparent !important;

      .link {
        width: 78px !important;
        height: 104px !important;
        border-radius: 6px !important;
      }

      .search_item_info .link__title {
        font-size: 13px !important;
        font-weight: 500 !important;
      }
    }
  }
}

.activeCity,
.ticket {
  border-radius: 5px !important;
  font-weight: 500 !important;
  border-color: var(--brand-primary-strong) !important;
  background: #fff !important;
  box-shadow: none !important;
}

.select-list-item.ticket {
  padding-right: 24px !important;
}

.app-container .wrapper .box-left .box-detail .order {
  .citys .city-list .city-item.activeCity,
  .order-box .order-time .select .select-list .select-list-item.activeCity,
  .order-box .order-time .select .select-list .select-list-item.ticket {
    border: 1px solid var(--brand-primary-strong) !important;
    background: #fff !important;
    color: var(--brand-text) !important;
    box-shadow: none !important;
  }

  .citys .city-list .city-item.activeCity {
    padding: 6px 11px !important;
  }

  .order-box .order-time .select .select-list .select-list-item.activeCity,
  .order-box .order-time .select .select-list .select-list-item.ticket {
    padding: 6px 12px !important;
  }
}

@media (max-width: 980px) {
  .app-container .wrapper {
    grid-template-columns: 1fr;

    .box-right {
      position: static;
      max-height: none;
      overflow: visible;
    }
  }
}

@media (max-width: 680px) {
  .app-container .wrapper .box-left .box-detail > .count {
    display: block;

    .box-img img {
      width: 100% !important;
      height: auto !important;
      margin-bottom: 18px;
    }
  }
}
</style>

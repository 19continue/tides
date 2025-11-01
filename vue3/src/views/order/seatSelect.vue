<template>
  <div class="seat-select-container">
    <Header></Header>
    <div class="main-content">
      <!-- 顶部时间和票价区域 -->
      <div class="top-bar">
        <div class="screening-summary">
          <div class="venue-name">{{ seatData.place || detailList.place }}</div>
          <div class="date-time">{{ seatData.showTime }} {{ seatData.showWeekTime }}</div>
        </div>
        <div class="price-tags">
          <div 
            v-for="price in seatData.priceList" 
            :key="price" 
            class="price-tag"
            :class="{ active: selectedPrice === price }"
            @click="filterByPrice(price)"
          >
            <span class="color-dot" :style="{ backgroundColor: getPriceColor(price) }"></span>
            <span class="price-text" :class="{ 'active-price': selectedPrice === price }">{{ selectedPrice === price ? price + '元' : '¥' + price }}</span>
          </div>
          <div 
            class="price-tag"
            :class="{ active: selectedPrice === '' }"
            @click="filterByPrice('')"
          >
            <span class="price-text">全部</span>
          </div>
        </div>
      </div>

      <!-- 座位图区域 -->
      <div class="seat-area">
        <div class="venue-wrapper">
          <!-- 银幕/舞台提示 -->
          <div class="stage-box">
            <span>{{ isMovie ? '银幕' : '舞台' }}</span>
          </div>
          <!-- 场馆轮廓 -->
          <div class="venue-outline">
            <div class="seat-map-container">
            <div 
              v-for="(row, rowIndex) in allRows" 
              :key="row.rowCode" 
              class="seat-row"
              :style="getRowStyle(rowIndex)"
            >
              <span class="row-label">{{ row.rowCode }}排</span>
              <div class="seats">
                <!-- 左区座位 -->
                <div class="seat-section left-section">
                  <div 
                    v-for="seat in getLeftSeats(row.seats)" 
                    :key="seat.id"
                    class="seat"
                    :class="getSeatClass(seat)"
                    @click="toggleSeat(seat)"
                    :title="`${seat.rowCode}排${seat.colCode}座 ¥${seat.price}`"
                  >
                    <span 
                      class="seat-dot"
                      :style="{ backgroundColor: getSeatColor(seat), opacity: getSeatOpacity(seat) }"
                    ></span>
                  </div>
                </div>
                <!-- 左过道 -->
                <div class="aisle"></div>
                <!-- 中区座位 -->
                <div class="seat-section center-section">
                  <div 
                    v-for="seat in getCenterSeats(row.seats)" 
                    :key="seat.id"
                    class="seat"
                    :class="getSeatClass(seat)"
                    @click="toggleSeat(seat)"
                    :title="`${seat.rowCode}排${seat.colCode}座 ¥${seat.price}`"
                  >
                    <span 
                      class="seat-dot"
                      :style="{ backgroundColor: getSeatColor(seat), opacity: getSeatOpacity(seat) }"
                    ></span>
                  </div>
                </div>
                <!-- 右过道 -->
                <div class="aisle"></div>
                <!-- 右区座位 -->
                <div class="seat-section right-section">
                  <div 
                    v-for="seat in getRightSeats(row.seats)" 
                    :key="seat.id"
                    class="seat"
                    :class="getSeatClass(seat)"
                    @click="toggleSeat(seat)"
                    :title="`${seat.rowCode}排${seat.colCode}座 ¥${seat.price}`"
                  >
                    <span 
                      class="seat-dot"
                      :style="{ backgroundColor: getSeatColor(seat), opacity: getSeatOpacity(seat) }"
                    ></span>
                  </div>
                </div>
              </div>
              <span class="row-label">{{ row.rowCode }}排</span>
            </div>
          </div>
          </div>
        </div>
      </div>

      <!-- 底部栏 -->
      <div class="bottom-bar">
        <div class="left-section">
          <div class="price-display">
            <span class="currency">¥</span>
            <span class="amount">{{ totalPrice }}</span>
          </div>
          <div class="selected-seats" v-if="selectedSeats.length > 0">
            <span 
              v-for="seat in selectedSeats" 
              :key="seat.id" 
              class="seat-tag"
              @click="removeSeat(seat)"
            >
              {{ seat.rowCode }}排{{ seat.colCode }}座
              <i class="remove-icon">×</i>
            </span>
          </div>
        </div>
        <button 
          class="buy-btn"
          :class="{ disabled: selectedSeats.length === 0 }"
          :disabled="selectedSeats.length === 0"
          @click="submitOrder"
        >
          {{ isMovie ? '确认选座' : '立即购买' }}
        </button>
      </div>
    </div>
    <Footer></Footer>
  </div>
</template>

<script setup name="seatSelect">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieSeatList, getSeatList } from '@/api/seatDetail'
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 从路由state获取节目详情
const detailList = ref({})
const seatData = ref({
  programId: '',
  place: '',
  showTime: '',
  showWeekTime: '',
  priceList: [],
  seatVoMap: {}
})

// 选中的座位
const selectedSeats = ref([])
// 价格筛选
const selectedPrice = ref('')
const MOVIE_CATEGORY_NAME = '电影'
const isMovie = computed(() => detailList.value?.parentProgramCategoryName === MOVIE_CATEGORY_NAME)
const maxSeats = computed(() => Number(detailList.value?.perOrderLimitPurchaseCount || 6))

// 价格颜色映射 - 潮声风格颜色
const priceColors = {
  0: '#2f8f83',
  1: '#f6c945',
  2: '#d05f3f',
  3: '#6d8fb3',
  4: '#9a7bb5',
  5: '#4d7f7a',
  6: '#c98912',
  7: '#8f6b4a',
  8: '#6c9a65',
  9: '#b8655d'
}

const normalizePrice = (value) => {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return String(value)
  }
  return amount.toFixed(2).replace(/\.?0+$/, '')
}

const priceIndexMap = computed(() => {
  const map = new Map()
  seatData.value.priceList.forEach((price, index) => {
    map.set(normalizePrice(price), index)
  })
  return map
})

// 根据价格获取颜色
const getPriceColor = (price) => {
  const index = priceIndexMap.value.get(normalizePrice(price)) ?? 0
  return priceColors[index % 10] || '#ccc'
}

const getSeatPriceKey = (seat) => normalizePrice(seat?.price)

const isFlagEnabled = (value) => Number(value) === 1

const isFlagDisabled = (value) => Number(value) === 0

const isSeatAvailable = (seat) => {
  const status = Number(seat?.sellStatus ?? seat?.redisSellStatus ?? seat?.dbSellStatus)
  if (status !== 1) {
    return false
  }
  if (isFlagDisabled(seat?.sellableFlag) || isFlagEnabled(seat?.repairFlag) || isFlagEnabled(seat?.aisleFlag)) {
    return false
  }
  return true
}

// 筛选后的座位图
const filteredSeatMap = computed(() => {
  if (selectedPrice.value === '') {
    return seatData.value.seatVoMap
  }
  const filtered = {}
  if (seatData.value.seatVoMap[selectedPrice.value]) {
    filtered[selectedPrice.value] = seatData.value.seatVoMap[selectedPrice.value]
  }
  return filtered
})

// 获取所有行的座位数据
const allRows = computed(() => {
  const rowMap = {}
  // 始终显示所有票档的座位
  Object.values(seatData.value.seatVoMap).forEach(seats => {
    seats.forEach(seat => {
      if (!rowMap[seat.rowCode]) {
        rowMap[seat.rowCode] = {
          rowCode: seat.rowCode,
          seats: []
        }
      }
      rowMap[seat.rowCode].seats.push(seat)
    })
  })
  
  // 按列排序
  Object.values(rowMap).forEach(row => {
    row.seats.sort((a, b) => Number(a.colCode) - Number(b.colCode))
  })
  
  return Object.values(rowMap)
    .sort((a, b) => Number(a.rowCode) - Number(b.rowCode))
    .map((row, rowIndex) => ({
      ...row,
      rowIndex
    }))
})

// 计算总价
const totalPrice = computed(() => {
  const sum = selectedSeats.value.reduce((amount, seat) => amount + Number(seat.price || 0), 0)
  return normalizePrice(sum)
})

// 获取座位颜色
const getSeatColor = (seat) => {
  const isSelected = selectedSeats.value.some(s => s.id === seat.id)
  
  // 选中状态 - 粉红色
  if (isSelected) {
    return '#12110f'
  }
  
  // 已售 - 浅灰色
  if (!isSeatAvailable(seat)) {
    return '#d8d3c8'
  }
  
  // 未售 - 根据价格显示颜色
  return getPriceColor(seat.price)
}

// 获取座位透明度
const getSeatOpacity = (seat) => {
  // 已选中的座位始终不透明
  const isSelected = selectedSeats.value.some(s => s.id === seat.id)
  if (isSelected) {
    return 1
  }
  
  // 已售的座位始终不透明
  if (!isSeatAvailable(seat)) {
    return 1
  }
  
  // 没有选择票档，所有未售座位都半透明
  if (selectedPrice.value === '') {
    return 0.4
  }
  
  // 选中票档的座位100%不透明，其他票档半透明
  return getSeatPriceKey(seat) === selectedPrice.value ? 1 : 0.3
}

// 按行分组
const groupByRow = (seats) => {
  const rowMap = {}
  seats.forEach(seat => {
    if (!rowMap[seat.rowCode]) {
      rowMap[seat.rowCode] = {
        rowCode: seat.rowCode,
        seats: []
      }
    }
    rowMap[seat.rowCode].seats.push(seat)
  })
  // 按列排序
  Object.values(rowMap).forEach(row => {
    row.seats.sort((a, b) => Number(a.colCode) - Number(b.colCode))
  })
  // 按行号排序返回
  return Object.values(rowMap).sort((a, b) => Number(a.rowCode) - Number(b.rowCode))
}

// 获取座位样式类
const getSeatClass = (seat) => {
  const isSelected = selectedSeats.value.some(s => s.id === seat.id)
  const isAvailable = isSeatAvailable(seat)
  return {
    'seat-available': isAvailable,
    'seat-sold': !isAvailable,
    'seat-selected': isSelected
  }
}

// 获取行样式 - 梯形效果已通过座位数量实现
const getRowStyle = (rowIndex) => {
  return {}
}

// 将座位分成三个区域：左、中、右
const getLeftSeats = (seats) => {
  const total = seats.length
  if (total < 6) return []  // 座位太少就不分区
  const leftCount = Math.max(Math.floor(total * 0.18), 2)  // 左区约18%
  return seats.slice(0, leftCount)
}

const getCenterSeats = (seats) => {
  const total = seats.length
  if (total < 6) return seats  // 座位太少就全部放中间
  const leftCount = Math.max(Math.floor(total * 0.18), 2)
  const rightCount = Math.max(Math.floor(total * 0.18), 2)
  return seats.slice(leftCount, total - rightCount)
}

const getRightSeats = (seats) => {
  const total = seats.length
  if (total < 6) return []  // 座位太少就不分区
  const rightCount = Math.max(Math.floor(total * 0.18), 2)  // 右区约18%
  return seats.slice(total - rightCount)
}

// 切换座位选中状态
const toggleSeat = (seat) => {
  // 已售出的座位不可选
  if (!isSeatAvailable(seat)) {
    ElMessage.warning('该座位已售出')
    return
  }
  
  const index = selectedSeats.value.findIndex(s => s.id === seat.id)
  if (index > -1) {
    // 取消选中
    selectedSeats.value.splice(index, 1)
  } else {
    // 检查是否超过最大数量
    if (selectedSeats.value.length >= maxSeats.value) {
      ElMessage.warning(`每笔订单最多选择${maxSeats.value}个座位`)
      return
    }
    // 添加选中
    selectedSeats.value.push(seat)
  }
}

// 移除选中的座位
const removeSeat = (seat) => {
  const index = selectedSeats.value.findIndex(s => s.id === seat.id)
  if (index > -1) {
    selectedSeats.value.splice(index, 1)
  }
}

// 按价格筛选
const filterByPrice = (price) => {
  selectedPrice.value = normalizePrice(price)
}

// 返回上一页
const goBack = () => {
  router.back()
}

const normalizeSeatData = (data = {}) => {
  const seatVoMap = {}
  Object.values(data.seatVoMap || {}).forEach(seats => {
    ;(Array.isArray(seats) ? seats : []).forEach(seat => {
      const normalizedSeat = {
        ...seat,
        price: normalizePrice(seat?.price),
        sellStatus: Number(seat?.sellStatus ?? seat?.redisSellStatus ?? seat?.dbSellStatus ?? 0)
      }
      const key = getSeatPriceKey(normalizedSeat)
      if (!key) {
        return
      }
      if (!seatVoMap[key]) {
        seatVoMap[key] = []
      }
      seatVoMap[key].push(normalizedSeat)
    })
  })

  const priceList = Array.from(new Set([
    ...(data.priceList || []).map(normalizePrice).filter(Boolean),
    ...Object.keys(seatVoMap)
  ])).sort((a, b) => Number(a) - Number(b))

  return {
    ...data,
    priceList,
    seatVoMap
  }
}

// 提交订单
const submitOrder = () => {
  if (selectedSeats.value.length === 0) {
    ElMessage.warning('请先选择座位')
    return
  }
  
  // 获取选中座位的ID列表
  const seatIdList = selectedSeats.value.map(seat => seat.id)
  // 获取票档ID（取第一个选中座位的票档ID）
  const ticketCategoryId = selectedSeats.value[0].ticketCategoryId
  
  // 跳转到订单页面，传递选座信息
  router.replace({
    path: '/order/index',
    state: {
      'detailList': JSON.stringify(detailList.value),
      'allPrice': totalPrice.value,
      'countPrice': selectedSeats.value[0].price,
      'num': selectedSeats.value.length,
      'ticketCategoryId': ticketCategoryId,
      'seatIdList': JSON.stringify(seatIdList),
      'isChooseSeat': true,
      'selectedSeats': JSON.stringify(selectedSeats.value),
      'screeningId': detailList.value.screeningId
    }
  })
}

// 获取座位数据
const fetchSeatData = async () => {
  try {
    const programId = detailList.value.id
    const screeningId = detailList.value.screeningId
    if (isMovie.value && !screeningId) {
      ElMessage.error('缺少电影场次信息')
      return
    }
    const response = isMovie.value
        ? await getMovieSeatList({ screeningId })
        : await getSeatList({ programId })
    if (String(response.code) === '0' && response.data) {
      const data = normalizeSeatData(response.data)
      seatData.value = isMovie.value
          ? {
            ...data,
            place: [data.cinemaName, data.hallName].filter(Boolean).join(' ') || detailList.value.place
          }
          : data
    } else {
      ElMessage.error('获取座位信息失败')
    }
  } catch (error) {
    console.error('获取座位信息失败:', error)
    ElMessage.error('获取座位信息失败')
  }
}

onMounted(() => {
  // 从history.state获取节目详情
  if (history.state && history.state.detailList) {
    detailList.value = JSON.parse(history.state.detailList)
    detailList.value.screeningId = history.state.screeningId || detailList.value.screeningId
    fetchSeatData()
  } else {
    ElMessage.error('缺少节目信息')
    router.back()
  }
})
</script>

<style scoped lang="scss">
.seat-select-container {
  min-height: 100vh;
  background: #f5f5f5;
  
  .main-content {
    width: 100%;
    max-width: 1400px;
    margin: 0 auto;
    padding: 20px 20px 100px;
    
    // 顶部时间和票价区域
    .top-bar {
      background: #fff;
      padding: 15px 20px;
      margin-bottom: 15px;
      border: 1px solid #ece8d2;
      border-radius: 8px;
      
      .screening-summary {
        display: flex;
        align-items: baseline;
        gap: 14px;
        margin-bottom: 15px;
        flex-wrap: wrap;
        
        .venue-name {
          font-size: 18px;
          color: var(--brand-dark);
          font-weight: 600;
        }
        
        .date-time {
          font-size: 14px;
          color: var(--brand-muted);
          font-weight: 500;
        }
      }
      
      .price-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        
        .price-tag {
          display: inline-flex;
          align-items: center;
          padding: 6px 12px;
          border: 1px solid #e5e5e5;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s;
          background: #fff;
          
          &:hover {
            border-color: var(--brand-primary-strong);
          }
          
          &.active {
            border-color: var(--brand-primary-strong);
            background: #fff9d8;
          }
          
          .color-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            margin-right: 6px;
          }
          
          .price-text {
            font-size: 14px;
            color: #666;
            
            &.active-price {
              color: var(--brand-primary-strong);
              font-weight: 500;
            }
          }
        }
      }
    }
    
    // 座位图区域
    .seat-area {
      background: #eef2f5;
      border-radius: 8px;
      padding: 30px 20px;
      min-height: 500px;
      
      .venue-wrapper {
        max-width: 1100px;
        margin: 0 auto;
        
        // 银幕/舞台提示框
        .stage-box {
          width: 240px;
          margin: 0 auto 25px;
          padding: 14px 0;
          background: linear-gradient(180deg, #fffdf0 0%, #f7fbff 100%);
          border: 2px solid #d7dfdf;
          border-radius: 4px;
          text-align: center;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          
          span {
            font-size: 18px;
            color: var(--brand-dark);
            font-weight: 500;
            letter-spacing: 0;
          }
        }
        
        // 场馆轮廓
        .venue-outline {
          background: #fff;
          border-radius: 12px;
          padding: 35px 30px 45px;
          position: relative;
          border: 3px solid #d0d5dc;
          
          .seat-map-container {
            position: relative;
            z-index: 1;
            
            .seat-row {
              display: flex;
              align-items: center;
              justify-content: center;
              margin-bottom: 8px;
              transition: padding 0.3s ease;
              
              .row-label {
                width: 45px;
                font-size: 12px;
                color: #999;
                text-align: center;
                flex-shrink: 0;
              }
              
              .seats {
                display: flex;
                justify-content: center;
                align-items: center;
                flex: 1;
                
                // 座位区域
                .seat-section {
                  display: flex;
                  gap: 3px;
                  
                  &.left-section {
                    justify-content: flex-end;
                  }
                  
                  &.center-section {
                    justify-content: center;
                  }
                  
                  &.right-section {
                    justify-content: flex-start;
                  }
                }
                
                // 过道间隔
                .aisle {
                  width: 25px;
                  height: 16px;
                  flex-shrink: 0;
                }
                
                .seat {
                  width: 16px;
                  height: 16px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  cursor: pointer;
                  transition: transform 0.15s;
                  
                  &:hover:not(.seat-sold) {
                    transform: scale(1.3);
                  }
                  
                  &.seat-sold {
                    cursor: not-allowed;
                  }
                  
                  &.seat-selected {
                    .seat-dot {
                      transform: scale(1.2);
                    }
                  }
                  
                  .seat-dot {
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    transition: all 0.15s;
                  }
                }
              }
            }
            
            // 上下区域之间的横向过道
            .horizontal-aisle {
              height: 20px;
              margin: 5px 0;
            }
          }
        }
      }
    }
    
    // 底部栏
    .bottom-bar {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      background: #fff;
      box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
      padding: 12px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      z-index: 100;
      
      .left-section {
        display: flex;
        align-items: center;
        gap: 20px;
        
        .price-display {
          .currency {
            font-size: 16px;
            color: #333;
          }
          
          .amount {
            font-size: 28px;
            font-weight: bold;
            color: #333;
          }
        }
        
        .selected-seats {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          max-width: 600px;
          
          .seat-tag {
            display: inline-flex;
            align-items: center;
            padding: 4px 10px;
            background: #FFF0F3;
            color: #FF375D;
            border-radius: 4px;
            font-size: 12px;
            cursor: pointer;
            transition: all 0.2s;
            
            &:hover {
              background: #FFE0E6;
            }
            
            .remove-icon {
              margin-left: 4px;
              font-style: normal;
              font-size: 14px;
            }
          }
        }
      }
      
      .buy-btn {
        padding: 14px 50px;
        font-size: 18px;
        color: var(--brand-dark);
        background: linear-gradient(135deg, var(--brand-primary) 0%, var(--brand-primary-strong) 100%);
        border: none;
        border-radius: 25px;
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover:not(.disabled) {
          transform: translateY(-2px);
          box-shadow: 0 4px 15px rgba(244, 183, 0, 0.35);
        }
        
        &.disabled {
          background: #ccc;
          cursor: not-allowed;
        }
      }
    }
  }
}

/* 潮声选座体验层 */
.seat-select-container {
  background: var(--brand-page) !important;

  .main-content {
    width: min(1360px, calc(100% - var(--layout-gutter))) !important;
    max-width: none !important;
    padding: 24px 0 112px !important;

    .top-bar {
      padding: 18px 20px !important;
      margin-bottom: 18px !important;
      border: 1px solid var(--brand-line) !important;
      border-radius: var(--radius-lg) !important;
      background: var(--brand-card) !important;
      box-shadow: var(--brand-shadow-soft);

      .screening-summary {
        margin-bottom: 14px !important;

        .venue-name {
          color: var(--brand-dark) !important;
          font-size: 20px !important;
          font-weight: 900 !important;
        }

        .date-time {
          color: var(--brand-muted) !important;
        }
      }

      .price-tags .price-tag {
        height: 34px;
        padding: 0 12px !important;
        border-color: var(--brand-line) !important;
        border-radius: 999px !important;
        background: #fff !important;

        &:hover,
        &.active {
          border-color: var(--brand-primary-strong) !important;
          background: var(--brand-surface) !important;
        }
      }
    }

    .seat-area {
      min-height: 560px !important;
      padding: 28px !important;
      border: 1px solid var(--brand-line);
      border-radius: var(--radius-lg) !important;
      background:
          linear-gradient(180deg, rgba(255, 253, 248, .88), rgba(242, 236, 223, .88)) !important;
      box-shadow: var(--brand-shadow-soft);

      .venue-wrapper {
        max-width: 1160px !important;

        .stage-box {
          width: min(360px, 70%) !important;
          margin-bottom: 28px !important;
          padding: 13px 0 !important;
          border: 1px solid rgba(18, 17, 15, .16) !important;
          border-radius: 999px !important;
          background: #12110f !important;
          box-shadow: none !important;

          span {
            color: #fff !important;
            font-size: 15px !important;
            font-weight: 900 !important;
            letter-spacing: 0 !important;
          }
        }

        .venue-outline {
          padding: 34px 28px 42px !important;
          border: 1px solid var(--brand-line) !important;
          border-radius: 18px !important;
          background: var(--brand-card) !important;

          .seat-map-container .seat-row {
            margin-bottom: 9px !important;

            .row-label {
              color: var(--brand-subtle) !important;
            }

            .seats {
              .aisle {
                width: 28px !important;
              }

              .seat {
                width: 18px !important;
                height: 18px !important;

                &:hover:not(.seat-sold) {
                  transform: scale(1.24) !important;
                }

                .seat-dot {
                  width: 13px !important;
                  height: 13px !important;
                  border-radius: 4px !important;
                  box-shadow: 0 0 0 1px rgba(18, 17, 15, .12) inset;
                }
              }
            }
          }
        }
      }
    }

    .bottom-bar {
      background: rgba(255, 253, 248, .94) !important;
      border-top: 1px solid var(--brand-line);
      box-shadow: 0 -14px 36px rgba(40, 32, 18, .1) !important;
      backdrop-filter: blur(16px);

      .left-section {
        min-width: 0;

        .price-display {
          min-width: 128px;

          .currency,
          .amount {
            color: var(--brand-dark) !important;
          }

          .amount {
            font-weight: 900 !important;
          }
        }

        .selected-seats .seat-tag {
          background: #f2ecdf !important;
          color: var(--brand-dark) !important;
          border-radius: 999px !important;
        }
      }

      .buy-btn {
        min-width: 148px;
        color: var(--brand-dark) !important;
        background: var(--brand-primary) !important;
        border-radius: 999px !important;
        font-weight: 900;

        &:hover:not(.disabled) {
          box-shadow: 0 12px 24px rgba(201, 137, 18, .24) !important;
        }

        &.disabled {
          color: rgba(18, 17, 15, .52) !important;
          background: #d8d3c8 !important;
        }
      }
    }
  }
}

@media (max-width: 760px) {
  .seat-select-container .main-content {
    width: calc(100% - 24px) !important;

    .seat-area {
      overflow-x: auto;
      padding: 18px !important;

      .venue-wrapper {
        min-width: 720px;
      }
    }

    .bottom-bar {
      align-items: stretch !important;
      flex-direction: column;
      gap: 10px;

      .left-section {
        align-items: flex-start !important;
        flex-direction: column;
        gap: 8px !important;
      }
    }
  }
}
</style>

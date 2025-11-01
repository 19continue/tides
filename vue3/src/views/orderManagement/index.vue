<template>
  <Header></Header>
  <div class="section">
    <MenuSideBar class="sidebarMenu" activeIndex="5"></MenuSideBar>
    <main class="right-section">
      <div class="page-head">
        <div>
          <span>交易中心</span>
          <h1>订单管理</h1>
        </div>
        <button type="button" class="refresh-btn" @click="getOrderList">刷新</button>
      </div>

      <section class="order-panel" v-loading="loading">
        <div class="order-tabs">
          <button
              v-for="item in statusFilters"
              :key="item.value"
              type="button"
              :class="{active: activeStatus === item.value}"
              @click="changeStatus(item.value)"
          >{{ item.label }}<span>{{ item.count }}</span></button>
        </div>

        <div class="order-head">
          <span>项目信息</span>
          <span>数量</span>
          <span>金额</span>
          <span>状态</span>
          <span>操作</span>
        </div>

        <div class="empty-order" v-if="!loading && filteredOrders.length === 0">
          <strong>{{ orderList.length ? '当前筛选下暂无订单' : '暂无订单' }}</strong>
          <p>{{ orderList.length ? '切换筛选条件查看其他状态订单。' : '完成购票后，订单会显示在这里。' }}</p>
        </div>

        <article class="order-card" v-for="order in pagedOrders" :key="order.orderNumber">
          <div class="order-meta">
            <span>订单号：{{ order.orderNumber }}</span>
            <span>{{ formatDate(order.createOrderTime) }}</span>
          </div>
          <div class="order-row">
            <router-link class="program-info" :to="{name:'orderDetail',params:{orderNumber: order.orderNumber}}">
              <img :src="order.programItemPicture" alt="" @error="setImageFallback">
              <div>
                <strong>{{ order.programTitle || '未命名项目' }}</strong>
                <p>{{ getOrderType(order) }}场次：{{ formatDate(order.programShowTime) }}</p>
                <p>{{ getOrderType(order) }}场馆：{{ order.programPlace || '待确认' }}</p>
              </div>
            </router-link>
            <div class="order-count">{{ order.ticketCount || '-' }}</div>
            <div class="order-price">￥{{ formatMoney(order.orderPrice) }}</div>
            <div class="order-status" :class="getOrderStatusClass(order.orderStatus)">
              {{ getOrderStatus(order.orderStatus) }}
            </div>
            <div class="order-actions">
              <button type="button" class="primary" v-if="Number(order.orderStatus) === 1" @click="payOrder(order.orderNumber)">支付订单</button>
              <button type="button" v-if="Number(order.orderStatus) === 1" @click="cancelOrder(order.orderNumber)">取消订单</button>
              <router-link :to="{name:'orderDetail',params:{orderNumber: order.orderNumber}}">订单详情</router-link>
            </div>
          </div>
        </article>
        <div class="order-pagination" v-if="!loading && filteredOrders.length > pageState.pageSize">
          <el-pagination
              v-model:current-page="pageState.pageNumber"
              v-model:page-size="pageState.pageSize"
              :page-sizes="[5, 10, 20]"
              :pager-count="5"
              :total="filteredOrders.length"
              layout="total, sizes, prev, pager, next"
              @size-change="handlePageSizeChange"
          />
        </div>
      </section>
    </main>
  </div>
  <Footer></Footer>
</template>

<script setup name="OrderManagement">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import MenuSideBar from '../../components/menuSidebar/index'
import Header from '../../components/header/index'
import Footer from '../../components/footer/index'
import {useRouter} from 'vue-router'
import {cancelOrderApi, getOrderListApi, getOrderSimpleListApi} from '@/api/order.js'
import {ElMessage} from "element-plus";
import useUserStore from "../../store/modules/user";
import {getUserIdKey} from '@/utils/auth'
import {setImageFallback} from '@/utils/index'

const router = useRouter()
const useUser = useUserStore()
const orderList = ref([])
const loading = ref(false)
const activeStatus = ref('')
const orderListParams = reactive({
  userId: undefined
})
const pageState = reactive({
  pageNumber: 1,
  pageSize: 5
})

const filteredOrders = computed(() => {
  if (activeStatus.value === '') {
    return orderList.value
  }
  if (activeStatus.value === 'closed') {
    return orderList.value.filter(order => [2, 4].includes(Number(order.orderStatus)))
  }
  return orderList.value.filter(order => Number(order.orderStatus) === Number(activeStatus.value))
})

const pagedOrders = computed(() => {
  const start = (pageState.pageNumber - 1) * pageState.pageSize
  return filteredOrders.value.slice(start, start + pageState.pageSize)
})

const statusFilters = computed(() => {
  const countByStatus = status => orderList.value.filter(order => Number(order.orderStatus) === status).length
  return [
    {label: '全部', value: '', count: orderList.value.length},
    {label: '待支付', value: 1, count: countByStatus(1)},
    {label: '已支付', value: 3, count: countByStatus(3)},
    {label: '已关闭', value: 'closed', count: countByStatus(2) + countByStatus(4)}
  ]
})

function normalizeOrderRows(data) {
  if (Array.isArray(data)) {
    return data
  }
  return data?.records || data?.list || []
}

function getCurrentUserId() {
  return getUserIdKey() || useUser.userId
}

function changeStatus(value) {
  activeStatus.value = value
  pageState.pageNumber = 1
}

function handlePageSizeChange() {
  pageState.pageNumber = 1
}

async function getOrderList() {
  const userId = getCurrentUserId()
  if (!userId) {
    orderList.value = []
    return
  }
  orderListParams.userId = userId
  loading.value = true
  try {
    const response = await getOrderListApi(orderListParams)
    let rows = normalizeOrderRows(response.data)
    if (!rows.length) {
      const simpleResponse = await getOrderSimpleListApi(orderListParams)
      rows = normalizeOrderRows(simpleResponse.data)
    }
    orderList.value = rows
  } catch (error) {
    orderList.value = []
    ElMessage.error('订单列表加载失败')
  } finally {
    loading.value = false
  }
}

function getOrderStatus(orderStatus) {
  const status = Number(orderStatus)
  if (status === 1) return '待支付'
  if (status === 2) return '已取消'
  if (status === 3) return '已支付'
  if (status === 4) return '已关闭'
  return '未知状态'
}

function getOrderStatusClass(orderStatus) {
  const status = Number(orderStatus)
  if (status === 1) return 'waiting'
  if (status === 3) return 'paid'
  return 'closed'
}

function getOrderType(order) {
  return order?.screeningId ? '电影' : '演出'
}

function formatDate(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) {
    return '待确认'
  }
  const date = new Date(value)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day} ${hour}:${minute}`
}

function formatMoney(value) {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

function cancelOrder(orderNumber) {
  cancelOrderApi({orderNumber}).then(response => {
    if (response.code == '0') {
      ElMessage.success('取消成功')
      getOrderList()
    } else {
      ElMessage.error(response.message)
    }
  })
}

function payOrder(orderNumber) {
  router.replace({path: '/order/payMethod', state: {'orderNumber': orderNumber}})
}

onMounted(() => {
  getOrderList()
})

watch(filteredOrders, orders => {
  const maxPage = Math.max(1, Math.ceil(orders.length / pageState.pageSize))
  if (pageState.pageNumber > maxPage) {
    pageState.pageNumber = maxPage
  }
})
</script>

<style scoped lang="scss">
.section {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  min-height: 560px;
  display: flex;
  align-items: flex-start;
  gap: 28px;
  margin: 28px auto 72px;
}

.sidebarMenu {
  flex: 0 0 210px;
}

.right-section {
  flex: 1;
  min-width: 0;
}

.page-head {
  min-height: 44px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 0 2px;

  span {
    color: var(--brand-muted);
    font-size: 13px;
  }

  h1 {
    margin: 4px 0 0;
    color: var(--brand-dark);
    font-size: 24px;
    line-height: 32px;
    font-weight: 600;
  }
}

.refresh-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: #fff;
  color: var(--brand-muted);
  cursor: pointer;
}

.order-panel {
  margin-top: 14px;
  padding: 24px;
  border-radius: 10px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.order-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;

  button {
    height: 34px;
    padding: 0 12px;
    border: 0;
    border-radius: 999px;
    background: #f2ecdf;
    color: var(--brand-muted);
    cursor: pointer;

    span {
      margin-left: 6px;
      color: var(--brand-subtle);
    }

    &.active {
      background: var(--brand-primary);
      color: var(--brand-dark);
    }
  }
}

.order-head,
.order-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 76px 110px 96px 118px;
  gap: 16px;
  align-items: center;
}

.order-head {
  height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  background: #f8f3e7;
  color: var(--brand-muted);
  font-size: 13px;
}

.order-card {
  margin-top: 14px;
  border: 1px solid var(--brand-line);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.order-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.order-meta {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid var(--brand-line);
  color: var(--brand-muted);
  font-size: 12px;
}

.order-row {
  padding: 16px;
}

.program-info {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 14px;
  min-width: 0;

  img {
    width: 64px;
    height: 86px;
    border-radius: 6px;
    object-fit: cover;
    background: #f8f3e7;
  }

  strong {
    display: block;
    color: var(--brand-dark);
    font-size: 15px;
    line-height: 21px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  p {
    margin: 7px 0 0;
    color: var(--brand-muted);
    font-size: 12px;
    line-height: 18px;
  }
}

.order-count,
.order-price,
.order-status {
  color: var(--brand-text);
  font-size: 14px;
}

.order-price {
  color: var(--brand-dark);
  font-weight: 600;
}

.order-status {
  width: fit-content;
  min-width: 68px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 12px;

  &.waiting {
    color: #8b5b0b;
    background: #fbf2df;
  }

  &.paid {
    color: #2f8f83;
    background: #edf8ef;
  }

  &.closed {
    color: var(--brand-muted);
    background: #f2eee3;
  }
}

.order-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;

  button,
  a {
    min-width: 82px;
    height: 30px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 12px;
    border: 1px solid var(--brand-line);
    border-radius: 999px;
    background: #fff;
    color: var(--brand-muted);
    font-size: 13px;
    cursor: pointer;
  }

  .primary {
    border-color: var(--brand-primary);
    background: var(--brand-primary);
    color: var(--brand-dark);
  }
}

.empty-order {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: var(--brand-muted);
  text-align: center;

  strong {
    color: var(--brand-dark);
    font-size: 18px;
    font-weight: 500;
  }

  p {
    margin: 8px 0 0;
    font-size: 13px;
  }
}

@media (max-width: 900px) {
  .section {
    display: block;
    width: calc(100% - 24px);
  }

  .right-section {
    margin-top: 18px;
  }

  .order-head {
    display: none;
  }

  .order-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .order-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>

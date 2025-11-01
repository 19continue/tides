<template>
  <div class="order-detail">
    <Header></Header>
    <main class="detail-page" v-loading="loading">
      <template v-if="order.orderNumber">
        <section class="detail-hero">
          <div class="hero-main">
            <button type="button" class="back-btn" @click="router.back()">返回</button>
            <p>订单号 {{ order.orderNumber }}</p>
            <h1>{{ getOrderStatus(order.orderStatus) }}</h1>
            <span>{{ getOrderType(order) }}订单 · {{ order.programTitle }}</span>
          </div>
          <div class="pay-summary">
            <span>{{ Number(order.orderStatus) === 3 ? '实付金额' : '订单金额' }}</span>
            <strong>￥{{ formatMoney(order.orderPrice) }}</strong>
            <button v-if="Number(order.orderStatus) === 1" type="button" @click="payOrder">去支付</button>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title">票品明细</div>
          <div class="program-card">
            <img :src="order.programItemPicture" alt="" @error="setImageFallback">
            <div class="program-main">
              <h2>{{ order.programTitle }}</h2>
              <p>{{ getOrderType(order) }}场次：{{ formatDate(order.programShowTime) }}</p>
              <p>{{ getOrderType(order) }}场馆：{{ order.programPlace || '待确认' }}</p>
            </div>
          </div>
          <div class="ticket-table">
            <div class="ticket-head">
              <span>座位/票档</span>
              <span>单价</span>
              <span>数量</span>
              <span>小计</span>
            </div>
            <div class="ticket-row" v-for="(item, index) in ticketRows" :key="`${item.seatInfo}-${index}`">
              <span>{{ item.seatInfo || '暂无座位信息' }}</span>
              <span>￥{{ formatMoney(item.price) }}</span>
              <span>{{ item.quantity || 0 }}</span>
              <span>￥{{ formatMoney(item.relPrice) }}</span>
            </div>
          </div>
        </section>

        <div class="info-grid">
          <section class="detail-section">
            <div class="section-title">配送信息</div>
            <dl>
              <div><dt>配送方式</dt><dd>{{ order.distributionMode || '电子票' }}</dd></div>
              <div><dt>取票方式</dt><dd>{{ order.takeTicketMode || '线上取票' }}</dd></div>
              <div><dt>联系人</dt><dd>{{ userInfo.name || '未填写' }}</dd></div>
              <div><dt>手机号</dt><dd>{{ userInfo.mobile || '未填写' }}</dd></div>
            </dl>
          </section>
          <section class="detail-section">
            <div class="section-title">订单信息</div>
            <dl>
              <div><dt>订单编号</dt><dd>{{ order.orderNumber }}</dd></div>
              <div><dt>创建时间</dt><dd>{{ formatDate(order.createOrderTime) }}</dd></div>
              <div><dt>订单状态</dt><dd><span class="status-pill" :class="getStatusClass(order.orderStatus)">{{ getOrderStatus(order.orderStatus) }}</span></dd></div>
            </dl>
          </section>
          <section class="detail-section">
            <div class="section-title">金额明细</div>
            <dl>
              <div><dt>商品总价</dt><dd>￥{{ formatMoney(order.orderPrice) }}</dd></div>
              <div><dt>优惠</dt><dd>￥0.00</dd></div>
              <div><dt>应付金额</dt><dd class="amount">￥{{ formatMoney(order.orderPrice) }}</dd></div>
            </dl>
          </section>
        </div>

        <section class="detail-section buyer-section">
          <div class="section-title">购票人</div>
          <div class="buyer-list" v-if="buyerList.length">
            <article class="buyer-card" v-for="ticketUserInfo in buyerList" :key="ticketUserInfo.id || ticketUserInfo.idNumber">
              <div class="buyer-avatar">{{ getNameInitial(ticketUserInfo.relName) }}</div>
              <div>
                <h3>{{ ticketUserInfo.relName || '未填写姓名' }}</h3>
                <p>{{ getIdTypeName(ticketUserInfo.idType) || '证件' }} · {{ maskIdNumber(ticketUserInfo.idNumber) }}</p>
              </div>
            </article>
          </div>
          <div v-else class="empty-lite">暂无购票人信息</div>
        </section>
      </template>

      <section class="empty-page" v-else-if="!loading">
        <h1>订单不存在</h1>
        <p>请返回订单管理重新选择订单。</p>
        <button type="button" @click="router.replace('/orderManagement/index')">返回订单管理</button>
      </section>
    </main>
    <Footer></Footer>
  </div>
</template>

<script setup name="OrderDetail">
import {computed, onMounted, ref} from 'vue'
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {useRoute, useRouter} from 'vue-router'
import {getIdTypeName} from '@/api/common.js'
import {ElMessage} from "element-plus";
import {getOrderDetailApi} from '@/api/order.js'
import {setImageFallback} from '@/utils/index'

const route = useRoute()
const router = useRouter()
const orderData = ref(null)
const loading = ref(false)

const order = computed(() => orderData.value || {})
const ticketRows = computed(() => order.value.orderTicketInfoVoList || [])
const userInfo = computed(() => order.value.userAndTicketUserInfoVo?.userInfoVo || {})
const buyerList = computed(() => order.value.userAndTicketUserInfoVo?.ticketUserInfoVoList || [])

onMounted(() => {
  getOrderDetail()
})

async function getOrderDetail() {
  const orderNumber = route.params.orderNumber
  if (!orderNumber) {
    return
  }
  loading.value = true
  try {
    const response = await getOrderDetailApi({orderNumber})
    if (String(response.code) !== '0' || !response.data) {
      ElMessage.error(response.message || '订单详情加载失败')
      return
    }
    orderData.value = response.data
  } catch (error) {
    ElMessage.error('订单详情加载失败')
  } finally {
    loading.value = false
  }
}

function getOrderStatus(orderStatus) {
  const status = Number(orderStatus)
  if (status === 1) return '待支付'
  if (status === 2) return '已取消'
  if (status === 3) return '已支付'
  if (status === 4) return '交易关闭'
  return '未知状态'
}

function getStatusClass(orderStatus) {
  const status = Number(orderStatus)
  if (status === 1) return 'waiting'
  if (status === 3) return 'paid'
  return 'closed'
}

function getOrderType(row) {
  return row?.screeningId ? '电影' : '演出'
}

function formatMoney(value) {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
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

function maskIdNumber(value) {
  const text = String(value || '')
  if (text.length <= 8) {
    return text || '未填写证件号'
  }
  return `${text.slice(0, 4)} ${'*'.repeat(Math.max(4, text.length - 8))} ${text.slice(-4)}`
}

function getNameInitial(name) {
  return String(name || '购').trim().slice(0, 1)
}

function payOrder() {
  router.replace({path: '/order/payMethod', state: {orderNumber: order.value.orderNumber}})
}
</script>

<style scoped lang="scss">
.order-detail {
  min-height: 100vh;
  background: var(--brand-page);
}

.detail-page {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  min-height: 560px;
  margin: 28px auto 72px;
}

.detail-hero {
  min-height: 142px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 8px;
  background: #15120d;
  color: #fff;
  box-shadow: 0 16px 34px rgba(40, 32, 18, .12);
}

.hero-main {
  min-width: 0;

  p,
  span {
    display: block;
    color: rgba(255, 255, 255, .72);
    font-size: 13px;
  }

  h1 {
    margin: 14px 0 8px;
    font-size: 28px;
    line-height: 36px;
    font-weight: 500;
  }
}

.back-btn {
  height: 28px;
  margin-bottom: 10px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, .18);
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, .78);
  cursor: pointer;
}

.pay-summary {
  min-width: 180px;
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  justify-content: center;

  span {
    color: rgba(255, 255, 255, .68);
    font-size: 13px;
  }

  strong {
    margin-top: 8px;
    color: var(--brand-primary);
    font-size: 30px;
    line-height: 38px;
    font-weight: 600;
  }

  button {
    height: 34px;
    margin-top: 12px;
    padding: 0 16px;
    border: 0;
    border-radius: 6px;
    background: var(--brand-primary);
    color: var(--brand-dark);
    cursor: pointer;
  }
}

.detail-section {
  margin-top: 18px;
  padding: 24px;
  border-radius: 8px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.section-title {
  margin-bottom: 16px;
  color: var(--brand-dark);
  font-size: 17px;
  line-height: 24px;
  font-weight: 500;
}

.program-card {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 18px;

  img {
    width: 82px;
    height: 110px;
    border-radius: 6px;
    object-fit: cover;
    background: #f8f3e7;
  }
}

.program-main {
  min-width: 0;

  h2 {
    margin: 0 0 12px;
    color: var(--brand-dark);
    font-size: 18px;
    line-height: 26px;
    font-weight: 500;
  }

  p {
    margin: 6px 0 0;
    color: var(--brand-muted);
    font-size: 13px;
    line-height: 20px;
  }
}

.ticket-table {
  overflow: hidden;
  border-radius: 6px;
  background: #fbf7ed;
}

.ticket-head,
.ticket-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 120px 90px 120px;
  gap: 16px;
  align-items: center;
  padding: 12px 16px;
}

.ticket-head {
  color: var(--brand-muted);
  font-size: 13px;
}

.ticket-row {
  border-top: 1px solid #eee4d4;
  background: #fff;
  color: var(--brand-text);
  font-size: 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

dl {
  margin: 0;

  div {
    display: grid;
    grid-template-columns: 76px minmax(0, 1fr);
    gap: 12px;
    padding: 8px 0;
    border-top: 1px solid #f1eadf;

    &:first-child {
      border-top: 0;
    }
  }

  dt {
    color: var(--brand-muted);
    font-size: 13px;
  }

  dd {
    margin: 0;
    color: var(--brand-text);
    font-size: 13px;
    line-height: 20px;
    word-break: break-all;
  }

  .amount {
    color: var(--brand-primary-strong);
    font-size: 18px;
    font-weight: 600;
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 6px;
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

.buyer-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.buyer-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 6px;
  background: #fbf7ed;

  h3 {
    margin: 0;
    color: var(--brand-dark);
    font-size: 15px;
    font-weight: 500;
  }

  p {
    margin: 5px 0 0;
    color: var(--brand-muted);
    font-size: 13px;
    line-height: 20px;
  }
}

.buyer-avatar {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--brand-primary);
  color: var(--brand-dark);
  font-weight: 600;
}

.empty-lite,
.empty-page {
  color: var(--brand-muted);
  text-align: center;
}

.empty-page {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 8px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);

  h1 {
    margin: 0;
    color: var(--brand-dark);
    font-size: 22px;
    font-weight: 500;
  }

  p {
    margin: 10px 0 22px;
  }

  button {
    height: 36px;
    padding: 0 16px;
    border: 0;
    border-radius: 6px;
    background: var(--brand-primary);
    color: var(--brand-dark);
    cursor: pointer;
  }
}

@media (max-width: 900px) {
  .detail-hero,
  .info-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .pay-summary {
    align-items: flex-start;
  }

  .ticket-head {
    display: none;
  }

  .ticket-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>

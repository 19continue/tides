<template>
  <div class="app-container">
    <Header></Header>
    <div class="confirm-order">
      <div class="basic-info1">
        <div class="top">
          <span class="title">{{detailList.title}}</span>
          <span class="local">{{ detailList.areaName }}|{{ detailList.place }}</span>
          <div class="line"></div>
          <div class="time"><span>{{ formatDateWithWeekday(detailList.showTime, detailList.showWeekTime) }}</span></div>
          <div class="money"><span>￥<span v-if="allPrice == ''">{{countPrice}}</span><span v-else>{{allPrice}}</span>{{ orderCopy.ticketLabel }}</span><span >×<span  v-if="allPrice == ''">1</span><span  v-else>{{num}}</span>{{ orderCopy.unitLabel }}</span></div>
          <!-- 选座信息展示 -->
          <div class="seat-info" v-if="isChooseSeat && selectedSeatsData.length > 0">
            <span class="seat-label">{{ orderCopy.seatLabel }}：</span>
            <span class="seat-list">
              <span v-for="seat in selectedSeatsData" :key="seat.id" class="seat-item">
                {{ seat.rowCode }}排{{ seat.colCode }}座(￥{{ seat.price }})
              </span>
            </span>
          </div>
          <div class="order-info">
            <span>{{ orderCopy.queueTip }}</span>
          </div>

      </div>
        <div class="bottom">
          <div class="service-box">
            <span class="service">服务</span>
            <div class="service-name" v-if="detailList.permitRefund!=''">
              <i class="icon-warn" v-if="detailList.permitRefund=='0'"></i><span v-if="detailList.permitRefund=='0'">不支持退</span>
              <i class="icon-yes-blue" v-if="detailList.permitRefund=='1'"></i><span v-if="detailList.permitRefund=='1'">条件退</span>
              <i class="icon-yes-blue" v-if="detailList.permitRefund=='2'"></i><span v-if="detailList.permitRefund=='2'">全部退</span>
            </div>
          <div class="service-name" v-if="detailList.relNameTicketEntrance!=''">
            <i class="icon-warn" v-if="detailList.relNameTicketEntrance=='0'"></i><span
              v-if="detailList.relNameTicketEntrance=='0'">{{ orderCopy.noRealNameText }}</span>
            <i class="icon-yes-blue" v-if="detailList.relNameTicketEntrance=='1'"></i><span
              v-if="detailList.relNameTicketEntrance=='1'">{{ orderCopy.realNameText }}</span>
          </div>
          <div class="service-name"   v-if="detailList.permitChooseSeat!=''">
            <i class="icon-warn" v-if="detailList.permitChooseSeat=='0'"></i><span
              v-if="detailList.permitChooseSeat=='0'">不支持选座</span>
            <i class="icon-yes-blue" v-if="detailList.permitChooseSeat=='1'"></i><span
              v-if="detailList.permitChooseSeat=='1'">支持选座</span>
          </div>
          <div class="service-name" v-if="detailList.electronicDeliveryTicket!=''">
            <i class="icon-warn" v-if="detailList.electronicDeliveryTicket=='0'"></i><span
              v-if="detailList.electronicDeliveryTicket=='0'">无票</span>
            <i class="icon-yes-blue" v-if="detailList.electronicDeliveryTicket=='1'"></i><span
              v-if="detailList.electronicDeliveryTicket=='1'">{{ orderCopy.ticketTypeLabel }}</span>
            <i class="icon-yes-blue" v-if="detailList.electronicDeliveryTicket=='2'"></i><span
              v-if="detailList.electronicDeliveryTicket=='2'">快递票</span>
          </div>
          <div class="service-name"  v-if="detailList.electronicInvoice!=''">
            <i class="icon-warn" v-if="detailList.electronicInvoice=='0'"></i><span
              v-if="detailList.electronicInvoice=='0'">纸质发票</span>
            <i class="icon-yes-blue" v-if="detailList.electronicInvoice=='1'"></i><span
              v-if="detailList.electronicInvoice=='1'">电子发票</span>
          </div>
          </div>
          <div class="line"></div>
        </div>
        <div class="isRealName">
          <div class="buyer-head">
            <div class="left"><span class="title">{{ orderCopy.realNameTitle }}</span><span class="notice">{{ orderCopy.realNameNotice }}</span></div>
            <button class="add-buyer" type="button" @click="buyTicketInfo">新增</button>
          </div>
          <div class="ticketInfo" v-if="ticketInfoArr.length">
            <label
                class="ticket"
                v-for="item in ticketInfoArr"
                :key="item.id"
                :class="{ selected: ticketUserIdArr.includes(item.id) }"
            >
              <div class="info" v-if="isSHowInfo">
                <div class="buyer-title-row">
                  <span class="title">{{ item.relName }}</span>
                  <span class="buyer-role">{{ orderCopy.userRole }}</span>
                </div>
                <div class="card">
                  <div class="card-line">
                    <span class="field-label">证件类型</span>
                    <span class="cardType">{{ getIdTypeText(item.idType) }}</span>
                  </div>
                  <div class="card-line">
                    <span class="field-label">证件号码</span>
                    <span class="cardId">{{ maskIdNumber(item.idNumber) }}</span>
                  </div>
                </div>
              </div>
              <div class="buyer-action">
                <span class="selected-mark" v-if="ticketUserIdArr.includes(item.id)">已选择</span>
                <el-checkbox class="checkSelect" :model-value="ticketUserIdArr.includes(item.id)" :value="item.id" size="large" @change="getSelectTicketUser(item.id, $event)"></el-checkbox>
              </div>
            </label>
          </div>
          <div class="buyer-empty" v-else>
            <span>暂无常用{{ orderCopy.userRole }}</span>
            <button type="button" @click="buyTicketInfo">新增{{ orderCopy.userRole }}</button>
          </div>
        </div>
        <div class="line"></div>
        <div class="sendMethod">
          <div  class="sendMethodTitle">{{ orderCopy.deliveryTitle }}</div>
          <div class="ticketType"  v-if="detailList.electronicDeliveryTicket=='1'">{{ orderCopy.ticketTypeLabel }} <el-button   class="ticketbtn"  v-if="detailList.electronicDeliveryTicket=='1'">{{ orderCopy.ticketTag }}</el-button></div>
          <div class="ticketInfo"  v-if="detailList.electronicDeliveryTicket=='1'">{{ orderCopy.ticketInfo }}</div>
<!--          <div class="ticketType"  v-if="detailList.electronicDeliveryTicket=='2'">快递</div>-->
<!--          <div class="ticketInfo"  v-if="detailList.electronicDeliveryTicket=='2'"></div>-->
<!--          <div class="ticketType"  v-if="detailList.electronicDeliveryTicket=='2'">运费</div>-->
<!--          <div class="ten"  v-if="detailList.electronicDeliveryTicket=='2'">￥10.00</div>-->
        </div>
        <div class="sendline"></div>
        <div class="tel">
          <div class="title">联系方式</div>
          <div class="telNum">{{telNum}}</div>
        </div>
        <div class="sendline"></div>
        <div class="payMethod">
          <div class="title">支付方式</div>
          <div class="payMoney"><img :src="pay" alt=""><span>支付宝</span> <el-radio class="radioPay" value="1" size="large"></el-radio></div>
        </div>
        <div class="info">
          <div class="descript">{{ orderCopy.disclaimer }}</div>
        <div class="price">
          <span class="num" v-if="allPrice == ''">￥{{ countPrice }}</span>
          <span class="num" v-else>￥{{ allPrice }}</span>
          <span class="detail">明细</span>
<!--          <el-button type="primary" class="dialogShow"-->
<!--                     @click="dialogShow">点击显示弹框</el-button>-->
<!--          <el-button type="primary" class="dialogShow"-->
<!--                     v-loading.fullscreen.lock="loading"-->
<!--                     element-loading-text="请稍后..."-->
<!--                     :element-loading-spinner="svg"-->
<!--                     element-loading-svg-view-box="-10, -10, 50, 50"-->
<!--                     element-loading-background="rgba(122, 122, 122, 0.8)"-->
<!--                     @click="dialogLoading">点击loading</el-button>-->
          <el-button type="primary" class="submit" @click="submitOrder">提交订单</el-button>
        </div>
        </div>
      </div>
    </div>
    <el-dialog
        v-model="dialogVisible"
        width="420px"
        class="queue-dialog"

    >
      <div class="content">{{ orderCopy.queueDialogText }}</div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" class="btn1">返回</el-button>
          <el-button   class="submit btn2"    @click="dialogVisible = false">
            继续尝试
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="orderIndex">
import {computed, ref, nextTick, onActivated, onMounted,onBeforeUnmount } from 'vue'
import pay from "@/assets/section/pay.png"
import {getCurrentDateTime,formatDateWithWeekday} from '@/utils/index'
import {useRoute, useRouter} from 'vue-router'
import { getUserIdKey} from "@/utils/auth";
import { getPersonInfoId} from '@/api/personInfo'
import {getTicketUser} from "@/api/buyTicketUser";
import {getOrderCacheApi, orderCreateV1Api, orderCreateV2Api, orderCreateV3Api, orderCreateV4Api} from '@/api/order.js'
import {getMovieSeatList, getSeatList} from '@/api/seatDetail'
import {clearOrderState, getOrderState, saveOrderState} from '@/utils/orderState'
import {ElMessage} from "element-plus";
//获取用户信息
import useUserStore from "../../store/modules/user";
import Header from '@/components/header/index'

const useUser = useUserStore()
const router = useRouter();
const detailList = ref({})
const allPrice = ref('')
const countPrice = ref('')
const num = ref('')
const telNum = ref('')
const ticketInfoArr = ref([])
const dialogVisible = ref(false)
const isSHowInfo = ref(true)//此处设置是为了解决弹出框显示后此处界面也会显示到上层的问题。注意：关闭弹框的时候这里要设置为true
const ticketUserIdArr = ref([])
//票档id
const ticketCategoryId = ref('')
const screeningId = ref('')
const orderNumberCache = ref('')
const loading = ref(false)
const orderCreateClientRequestId = ref('')
// 选座相关数据
const seatIdList = ref([])
const isChooseSeat = ref(false)
const selectedSeatsData = ref([])
const svg = `
        <path class="path" d="
          M 30 15
          L 28 17
          M 25.61 25.61
          A 15 15, 0, 0, 1, 15 30
          A 15 15, 0, 1, 1, 27.99 7.5
          L 15 15
        " style="stroke-width: 4px; fill: rgba(0, 0, 0, 0)"/>`
const pollingTimer = ref(null);
const timeoutTimer = ref(null);
// 10s的时间（毫秒）
const tenSecond = 10000;
const MOVIE_CATEGORY_NAME = '电影'
const isMovie = computed(() => detailList.value?.parentProgramCategoryName === MOVIE_CATEGORY_NAME)
const orderCopy = computed(() => {
  if (isMovie.value) {
    return {
      ticketLabel: '影票',
      unitLabel: '张',
      seatLabel: '已选影厅座位',
      queueTip: '按付款顺序出票，热门影片可能稍有延迟',
      realNameTitle: '实名观影人',
      realNameNotice: '需按影票数量选择对应观影人，入场时携带对应证件',
      noRealNameText: '不实名购票和入场',
      realNameText: '实名购票和入场',
      deliveryTitle: '取票方式',
      ticketTypeLabel: '电子影票',
      ticketTag: '扫码入场',
      ticketInfo: '支付成功后，前往票夹查看取票码或入场凭证',
      disclaimer: '电影票承载指定场次的观影服务，具有时效性和座位库存限制，一旦订购成功，不支持随意退换。',
      queueDialogText: '当前出票人数较多，请稍候再试~',
      userRole: '观影人'
    }
  }
  return {
    ticketLabel: '票档',
    unitLabel: '张',
    seatLabel: '已选座位',
    queueTip: '按付款顺序配票，优先连座配票',
    realNameTitle: '实名观演人',
    realNameNotice: '需按票张数量选择对应观演人，入场时携带对应证件',
    noRealNameText: '不实名购票和入场',
    realNameText: '实名购票和入场',
    deliveryTitle: '配送方式',
    ticketTypeLabel: '电子票',
    ticketTag: '直接入场',
    ticketInfo: '支付成功后，无需取票，前往票夹查看入场凭证',
    disclaimer: '由于票品为价票券，非普通商品，其背后承载的文化服务具有时效性、稀缺性等特征，一旦订购成功，不支持退换。',
    queueDialogText: '当前排队人数太多，请稍候再试~',
    userRole: '购票人'
  }
})

function getOrderCreateClientRequestId() {
  if (!orderCreateClientRequestId.value) {
    orderCreateClientRequestId.value = `${getCurrentUserId() || 'u'}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  }
  return orderCreateClientRequestId.value
}

function getCurrentUserId() {
  return getUserIdKey() || useUser.userId
}

//跳转后的接收值
onMounted(()=>{
  const state = getOrderState(history.state) || {}
  if (!state.detailList) {
    ElMessage({
      message: '订单信息已失效，请重新选择场次',
      type: 'error',
    })
    router.replace({path: '/'})
    return
  }
  detailList.value  = JSON.parse(state.detailList)
  allPrice.value  = state.allPrice
  countPrice.value  = state.countPrice
  num.value  = state.num
  ticketCategoryId.value = state.ticketCategoryId
  screeningId.value = state.screeningId || detailList.value.screeningId || ''
  // 接收选座数据
  if (state.isChooseSeat) {
    isChooseSeat.value = true
    seatIdList.value = JSON.parse(state.seatIdList || '[]')
    selectedSeatsData.value = JSON.parse(state.selectedSeats || '[]')
  }
})

getPersonInfoIdList()
getTicketUserList()

async function getPersonInfoIdList() {
  const id = getCurrentUserId()
  if (!id) {
    return
  }
  getPersonInfoId({id: id}).then(response => {
    let {mobile } = response.data
    telNum.value = mobile
  })
}
async function getTicketUserList() {
  const id = getCurrentUserId()
  if (!id) {
    return
  }
  getTicketUser({userId:id}).then(response=>{
    ticketInfoArr.value =response.data
  })
}


function buildOrderState() {
  return saveOrderState({
    detailList: JSON.stringify(detailList.value),
    allPrice: allPrice.value,
    countPrice: countPrice.value,
    num: num.value,
    ticketCategoryId: ticketCategoryId.value,
    screeningId: screeningId.value,
    seatIdList: JSON.stringify(seatIdList.value),
    isChooseSeat: isChooseSeat.value,
    selectedSeats: JSON.stringify(selectedSeatsData.value)
  })
}

function buyTicketInfo(){
  const state = buildOrderState()
  router.replace({path:'/order/buyTicketUser', state})
}

function getSelectTicketUser(ticketUserId,isChecked){
  if (isChecked) {
    if (!ticketUserIdArr.value.includes(ticketUserId)) {
      ticketUserIdArr.value.push(ticketUserId);
    }
  } else {
    ticketUserIdArr.value = ticketUserIdArr.value.filter((item) => item !== ticketUserId);
  }
}

function getIdTypeText(idType) {
  const idTypeMap = {
    1: '身份证',
    2: '港澳台居民居住证',
    3: '港澳居民来往内地通行证',
    4: '台湾居民来往内地通行证',
    5: '护照',
    6: '外国人永久居住证'
  }
  return idTypeMap[Number(idType)] || '证件'
}

function maskIdNumber(value) {
  const text = String(value || '')
  if (text.length <= 8) {
    return text
  }
  return `${text.slice(0, 4)} ${'*'.repeat(Math.max(4, text.length - 8))} ${text.slice(-4)}`
}

function getNameInitial(name) {
  return String(name || '购').trim().slice(0, 1)
}


function getOrderCache(orderNumber){
  const orderNumberParams = {orderNumber}
  getOrderCacheApi(orderNumberParams).then(response => {
    if (response.code == '0' && response.data != null){
      orderNumberCache.value = response.data;
    }
  })
}

//订单查询轮训
const startPolling = (orderNumber,startTime) => {
  pollingTimer.value = setInterval(() => {
    const currentTime = Date.now();
    if (currentTime - startTime >= tenSecond) {
      stopPolling();
      //1. 大于10秒，此订单被舍弃，显示排队弹框
      //2. loading弹出框关闭
      loadingClose();
      //3. 排队弹框显示
      dialogShow();
      return;
    }
    getOrderCache(orderNumber);
    if (orderNumberCache.value !== null && orderNumberCache.value !== '') {
      stopPolling();
      //执行到这里说明订单创建成功
      //loading弹框关闭
      loadingClose();
      clearOrderState()
      router.replace({path:'/order/payMethod',state:{'orderNumber':orderNumberCache.value}})
    }
  }, 200); // 每200毫秒调用一次
};
//停止轮训
const stopPolling = () => {
  clearInterval(pollingTimer.value);
  pollingTimer.value = null;
  clearTimeout(timeoutTimer.value);
  timeoutTimer.value = null;
};

/**
 * 提交订单
 * */
async function submitOrder(){

  if (ticketUserIdArr.value.length != num.value) {
    ElMessage({
      message:`选择的${orderCopy.value.userRole}和${orderCopy.value.unitLabel}数不一致`,
      type: 'error',
    })
    return;
  }

  // 根据是否手动选座构建不同的请求参数
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.error('登录信息已失效，请重新登录')
    return
  }
  if (!(await refreshSelectedSeatsBeforeSubmit())) {
    return
  }
  let orderCreateParams = {
    'programId': detailList.value.id,
    'userId': userId,
    'ticketUserIdList': ticketUserIdArr.value,
    'clientRequestId': getOrderCreateClientRequestId()
  }
  if (screeningId.value) {
    orderCreateParams.screeningId = screeningId.value
  }

  if (isChooseSeat.value && selectedSeatsData.value.length > 0) {
    // 手动选座：传 seatDtoList
    orderCreateParams.seatDtoList = selectedSeatsData.value.map(seat => ({
      id: seat.id,
      ticketCategoryId: seat.ticketCategoryId,
      rowCode: parseInt(seat.rowCode),
      colCode: parseInt(seat.colCode),
      price: seat.price
    }))
  } else {
    // 自动选座：传 ticketCategoryId 和 ticketCount
    orderCreateParams.ticketCategoryId = ticketCategoryId.value
    orderCreateParams.ticketCount = num.value
  }

  const createOrderVersion = import.meta.env.VITE_CREATE_ORDER_VERSION
  if (createOrderVersion == 1) {
    //v1版本的创建订单

    //loading弹出框显示
    loadingShow();
    
    orderCreateV1Api(orderCreateParams).then(response => {
      //loading弹出框关闭
      loadingClose();
      if (response.code == '0') {
        const orderNumber = response.data;
        clearOrderState()
        router.replace({path:'/order/payMethod',state:{'orderNumber':orderNumber}})
      }else{
        // ElMessage({
        //   message:response.message,
        //   type: 'error',
        // })
        //排队弹框显示
        dialogShow();
      }
    })
  }else if (createOrderVersion == 2) {
    //v2版本的创建订单

    //loading弹出框显示
    loadingShow();
    
    orderCreateV2Api(orderCreateParams).then(response => {
      //loading弹出框关闭
      loadingClose();
      if (response.code == '0') {
        const orderNumber = response.data;
        clearOrderState()
        router.replace({path:'/order/payMethod',state:{'orderNumber':orderNumber}})
      }else{
        // ElMessage({
        //   message:response.message,
        //   type: 'error',
        // })
        //排队弹框显示
        dialogShow();
      }
    })
  }else if (createOrderVersion == 3) {
    //v3版本的创建订单

    //loading弹出框显示
    loadingShow();
    
    orderCreateV3Api(orderCreateParams).then(response => {
      //loading弹出框关闭
      loadingClose();
      if (response.code == '0') {
        const orderNumber = response.data;
        clearOrderState()
        router.replace({path:'/order/payMethod',state:{'orderNumber':orderNumber}})
      }else{
        // ElMessage({
        //   message:response.message,
        //   type: 'error',
        // })
        //排队弹框显示
        dialogShow();
      }
    })
  }else if (createOrderVersion == 4) {
    //v4版本的创建订单

    //loading弹出框显示
    loadingShow();
    
    orderCreateV4Api(orderCreateParams).then(response => {
      if (response.code == '0' && response.data != null) {
        //开始定时轮训查询
        startPolling(response.data,Date.now());
        // 设置一个10s后停止轮询的定时器
        timeoutTimer.value = setTimeout(() => {
          if (pollingTimer.value) {
            stopPolling();
            loadingClose();
            dialogShow();
          }
        }, tenSecond + 500);
      }else{
        loadingClose();
        dialogShow();
      }
    }).catch(() => {
      loadingClose();
      ElMessage.error('订单提交失败，请稍后重试')
    })
  }
}

async function refreshSelectedSeatsBeforeSubmit() {
  if (!isChooseSeat.value) {
    return true
  }
  if (!selectedSeatsData.value.length) {
    ElMessage.error('座位信息已失效，请重新选座')
    return false
  }
  try {
    const response = isMovie.value
        ? await getMovieSeatList({screeningId: screeningId.value})
        : await getSeatList({programId: detailList.value.id})
    if (String(response.code) !== '0' || !response.data) {
      ElMessage.error('座位信息校验失败，请重新选座')
      return false
    }
    const currentSeats = Object.values(response.data.seatVoMap || {})
        .flatMap(item => Array.isArray(item) ? item : [])
    const currentSeatMap = new Map(currentSeats.map(seat => [String(seat.id), seat]))
    const nextSeats = []
    for (const selectedSeat of selectedSeatsData.value) {
      const currentSeat = currentSeatMap.get(String(selectedSeat.id))
      if (!isAvailableSeat(currentSeat)) {
        ElMessage.error('座位状态已变化，请重新选座')
        return false
      }
      nextSeats.push(currentSeat)
    }
    selectedSeatsData.value = nextSeats
    seatIdList.value = nextSeats.map(seat => seat.id)
    return true
  } catch (error) {
    ElMessage.error('座位信息校验失败，请重新选座')
    return false
  }
}

function isAvailableSeat(seat) {
  if (!seat) {
    return false
  }
  const status = Number(seat.sellStatus ?? seat.redisSellStatus ?? seat.dbSellStatus)
  return status === 1
      && Number(seat.sellableFlag ?? 1) !== 0
      && Number(seat.repairFlag ?? 0) !== 1
      && Number(seat.aisleFlag ?? 0) !== 1
}
//弹出排队框
function dialogShow(){
  dialogVisible.value = true
  isSHowInfo.value=false
}

function dialogLoading(){
  loading.value = true
  isSHowInfo.value=false
  setTimeout(() => {
    loading.value = false
    isSHowInfo.value=true
  }, 2000)
}

function loadingShow(){
  loading.value = true
  isSHowInfo.value=false
}

function loadingClose(){
  loading.value = false;
  isSHowInfo.value=true;
}

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style scoped lang="scss">
.app-container {
  width: 100%;
  height: 100%;
  background: #ffffff;

  .confirm-order {
    position: relative;
    box-sizing: border-box;
    display: flex;
    -webkit-box-orient: vertical;
    flex-direction: column;
    align-content: flex-start;
    flex-shrink: 0;

    .basic-info1 {
      position: relative;
      //display: flex;
      overflow: hidden;
      width: 100%;
      height: auto;

      .top {
        position: absolute;
        display: flex;
        overflow: hidden;
        -webkit-box-orient: vertical;
        flex-direction: column;
        width: 100%;
        padding-top: 31px;
        height: 318px;
        background: var(--brand-primary-strong);

        .title {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          margin-right: 43px;
          font-size: 37px;
          margin-left: 43px;
          width: 100%;
          max-width: 1800px;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          color: rgb(255, 255, 255);
          font-weight: bold;
          height: auto;
        }

        .local {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          margin-right: 43px;
          font-size: 24px;
          margin-left: 43px;
          width: fit-content;
          overflow: hidden;
          color: rgb(255, 255, 255);
          margin-top: 12px;
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          max-width: 1800px;
        }

        .line {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          margin-right: 43px;
          background-color: var(--brand-primary-strong);
          place-self: center flex-end;
          margin-left: 43px;
          width: 100%;
          max-width: 1800px;
          margin-top: 24px;
          height: 2px;
        }

        .time {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          margin-right: 43px;
          font-size: 33px;
          margin-left: 43px;
          width: fit-content;
          overflow: hidden;
          color: rgb(255, 255, 255);
          margin-top: 24px;
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          max-width: 1800px;
          flex-shrink: 0;
          flex-grow: 0;
          height: fit-content;
          span {
            white-space: pre-wrap;
            line-height: 40px;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }

        .money {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          overflow: hidden;
          width: 100%;

          -webkit-box-orient: horizontal;
          flex-direction: row;
          margin-left: 43px;
          flex-shrink: 0;
          flex-grow: 0;
          height: fit-content;

          span{
            position: relative;
            display: flex;
            flex-shrink: 0;
            flex-grow: 0;
            font-size: 29px;
            width: fit-content;
            color: rgb(255, 255, 255);
            height: auto;
            -webkit-box-pack: start;
            justify-content: flex-start;
            -webkit-box-align: center;
            align-items: center;
            overflow: hidden;
            max-width: none;
          }
        }

        .order-info {
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          margin-right: 43px;
          font-size: 24px;
          visibility: visible;
          margin-left: 43px;
          width: 100%;
          max-width: 1800px;
          color: rgb(255, 255, 255);
          margin-top: 6px;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          flex-shrink: 0;
          flex-grow: 0;
          height: fit-content;
        }

        .seat-info {
          position: relative;
          display: flex;
          flex-wrap: wrap;
          margin-left: 43px;
          margin-top: 10px;
          font-size: 20px;
          color: rgb(255, 255, 255);
          
          .seat-label {
            margin-right: 10px;
          }
          
          .seat-list {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            
            .seat-item {
              background: rgba(255, 255, 255, 0.2);
              padding: 4px 10px;
              border-radius: 4px;
              font-size: 18px;
            }
          }
        }

      }
      .bottom{
        width: 100%;
        height: auto;
        margin-top: 330px;

        .service-box{
          width: 100%;
          height: 33px;
          line-height: 33px;
          display: flex;
          flex-direction: row;
          margin-top: 30px;
          color: #000 !important;
          font-size: 24px;
          .service{
            width: 50px;
            height: 33px;
            line-height: 33px;
            margin-left: 50px;
          }
          .service-name{
            margin-left: 18px;
            width: fit-content;
            height: 33px;
            line-height: 33px;
            display: inline-block;
            .icon-warn{
              display: inline-block;
              width: 12px;
              height: 12px;
              background-repeat: no-repeat;
              background-size: 12px 12px;
              background: url('/src/assets/section/warn.png');
              margin-right: 10px;
            }
            .icon-yes-blue{
              display: inline-block;
              width: 12px;
              height: 12px;
              background-repeat: no-repeat;
              background-size: 12px 12px;
              background: url('/src/assets/section/yes-blue.png');
              margin-right: 10px;
            }
            span{
              width: fit-content;
              height: 33px;
              line-height: 33px;
            }
          }

        }
        .line{
          margin: 20px 0px 20px 50px;
          width: 97%;
          height: 2px;
          background-color: #cccccc;
          opacity: 0.7;
        }
      }
      .isRealName{
        margin-bottom: 20px;
        .left{
          position: relative;
          display: flex;
          flex: 1 1 0%;
          overflow: hidden;
          -webkit-box-orient: vertical;
          flex-direction: column;
          place-self: center flex-start;
          margin-left: 43px;
          width: fit-content;
          -webkit-box-flex: 1;
          height: auto;
          float: left;
          .title{
            position: relative;
            display: flex;
            flex-shrink: 0;
            flex-grow: 0;
            font-size: 24px;
            place-self: flex-start center;
            width: fit-content;
            height: auto;
            -webkit-box-pack: start;
            justify-content: flex-start;
            -webkit-box-align: center;
            align-items: center;
            overflow: hidden;
            max-width: none;
          }
          .notice{
            position: relative;
            display: flex;
            flex: 1 1 0%;
            font-size: 24px;
            place-self: flex-start center;
            width: fit-content;
            -webkit-box-flex: 1;
            color: var(--brand-primary-strong);
            margin-top: 6px;
            height: auto;
            -webkit-box-pack: start;
            justify-content: flex-start;
            -webkit-box-align: center;
            align-items: center;
            overflow: hidden;
            max-width: none;
          }
        }
        .right{
          float: left;
          margin-left: 43px;
          .btn{
            position: relative;
            display: flex;
            flex-shrink: 1;
            flex-grow: 0;
            overflow: hidden;
            margin-right: 43px;
            background-color: var(--brand-primary-strong);
            place-self: center flex-end;
            box-shadow: var(--brand-primary-strong) 0px 0px 0px 1px inset;
            width: 110px;
            height: 55px;
            border-radius: 28px;
            border: none;
            font-size: 24px;
          }
        }
        .ticketInfo{
          width: 100%;
          padding-left: 143.36px;
          padding-right: 143.36px;
          height: auto;
          min-height: 10.67vmin;
          //display: flex;
          -webkit-box-orient: horizontal;
          flex-direction: row;
          -webkit-box-pack: justify;
          justify-content: space-between;
          -webkit-box-align: center;
          align-items: center;
          .ticket{
            width: 100%;
            height: 136px;
            display: flex;
            flex-direction: row;
            align-items: center;
            .info{
              position: relative;
              display: flex;
              flex: 1 1 0%;
              overflow: hidden;
              -webkit-box-orient: vertical;
              flex-direction: column;
              place-self: center flex-start;
              margin-left: 43px;
              width: fit-content;
              -webkit-box-flex: 1;
              height: auto;
              float: left;
              .title{
                position: relative;
                display: flex;
                flex-shrink: 0;
                flex-grow: 0;
                place-self: flex-start center;
                width: fit-content;
                height: auto;
                -webkit-box-pack: start;
                justify-content: flex-start;
                -webkit-box-align: center;
                align-items: center;
                font-size: 4.27vmin;
                color: rgb(0, 0, 0);
                max-width: 60vmin;
                margin-right: 1.2vmin;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }
              .card{
                font-size: 3.2vmin;
                color: rgb(156, 156, 165);
                width: auto;
                overflow: hidden;
                white-space: nowrap;
                margin-right: 2.4vmin;
                //position: relative;
                //display: flex;
                //flex-direction: row;
                //flex: 1 1 0%;
                //place-self: flex-start center;
                //width: fit-content;
                //-webkit-box-flex: 1;
                //margin-top: 6px;
                //height: auto;
                //-webkit-box-pack: start;
                //justify-content: flex-start;
                //-webkit-box-align: center;
                //align-items: center;
                //overflow: hidden;
                //max-width: none;
                //font-size: 3.2vmin;
                //color: rgb(156, 156, 165);
                .cardType{
                  width: 100px;
                  font-size: 24px;
                  color:rgb(156, 156, 165);
                  display: inline-block;
                }
                .cardId{

                }
              }

            }

            .chx{}
          }

        }

      }
      .line {
        margin: 114px 0px 20px 50px;
        width: 97%;
        height: 1px;
        background-color: #cccccc;
        opacity: 0.7;
      }
      .sendMethod{
        margin-left: 43px;
        .sendMethodTitle{
          position: relative;
          display: flex;
          flex: 1 1 0%;
          margin-right: 10px;
          font-size: 24px;
          place-self: center flex-start;
          width: fit-content;
          -webkit-box-flex: 1;
          color: rgb(0, 0, 0);
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          max-width: none;
        }
        .ticketType{
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          font-size: 33px;
          place-self: center flex-start;
          width: fit-content;
          color: rgb(0, 0, 0);
          height: 45px;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          max-width: none;
          margin-top: 20px;
          margin-bottom: 10px;
          .ticketbtn {
            position: relative;
            display: flex;
            flex-shrink: 0;
            flex-grow: 0;
            font-size: 20px;
            place-self: center;
            width: fit-content;
            -webkit-box-pack: center;
            justify-content: center;
            -webkit-box-align: center;
            align-items: center;
            color: rgb(255, 146, 0);
            height: auto;
            overflow: hidden;
            max-width: none;
            border: 1px solid rgb(255, 146, 0);
            border-radius: 20px;
          }
        }
        .ticketInfo{
          position: relative;
          display: flex;
          flex-shrink: 0;
          flex-grow: 0;
          font-size: 24px;
          width: 100%;
          overflow: hidden;
          color: rgb(156, 156, 165);
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          max-width: none;
        }
      }
      .sendline{
        margin: 20px 0px 20px 50px;
        width: 97%;
        height: 1px;
        background-color: #cccccc;
        opacity: 0.7;
      }
      .tel{
        margin-left: 43px;
        .title{
          position: relative;
          display: flex;
          flex: 1 1 0%;
          font-size: 24px;
          place-self: center flex-start;
          width: fit-content;
          -webkit-box-flex: 1;
          color: rgb(0, 0, 0);
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          max-width: none;
         margin: 20px 0;
        }
        .telNum{
          width: 100%;
          height: 100%;
          outline: none;
          border: none;
          padding: 0px;
          margin: 0px;
          user-select: auto;
          font-size: 33px;
          color: rgb(0, 0, 0);
          text-align: left;
        }
      }
      .payMethod{
        margin-left: 43px;
        .title{
          position: relative;
          display: flex;
          flex: 1 1 0%;
          font-size: 24px;
          place-self: center flex-start;
          width: fit-content;
          -webkit-box-flex: 1;
          color: rgb(0, 0, 0);
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          overflow: hidden;
          max-width: none;
          margin: 20px 0;
        }
        .payMoney{
          display: flex;
          height: 300px;
          img{
            width: 80px;
            height: 80px;
          }
          span{
            padding: 30px 15px;
            font-size: 3.4vmin;
            color: #000000;
            letter-spacing: 0;
            line-height: 15px;
            margin-right: 1500px;
          }
          .radioPay{

          }
        }
      }
      .info{
        width: 100%;
        height: 150px;
        position: fixed;
        bottom: 0px;
        background: #ffffff;
        z-index: 100000;
        .descript{
          position: relative;
          display: flex;
          font-size: 22px;
          visibility: visible;
          width: fit-content;
          overflow: hidden;
          color: rgb(156, 156, 165);
          margin-top: 4px;
          height: auto;
          -webkit-box-pack: start;
          justify-content: flex-start;
          -webkit-box-align: center;
          align-items: center;
          max-width: none;
          margin-left: 43px;
        }
        .price{
          display: flex;
          flex-direction: row;
          margin: 20px 0px 20px 43px;
          .num{
            position: relative;
            display: flex;
            flex-shrink: 0;
            flex-grow: 0;
            margin-right: 6px;
            font-size: 41px;
            place-self: center flex-start;
            width: fit-content;
            color: var(--brand-primary-strong);
            height: auto;
            -webkit-box-pack: start;
            justify-content: flex-start;
            -webkit-box-align: center;
            align-items: center;
            overflow: hidden;
            max-width: none;
          }
          .detail{
            position: relative;
            display: flex;
            flex-shrink: 0;
            flex-grow: 0;
            font-size: 24px;
            place-self: center flex-start;
            width: fit-content;
            overflow: hidden;
            color: rgb(0, 0, 0);
            height: auto;
            -webkit-box-pack: start;
            justify-content: flex-start;
            -webkit-box-align: center;
            align-items: center;
            max-width: none;
          }
          .submit{
            position: absolute;
            right: 30px;
            display: flex;
            font-size: 33px;
            width: 266px;
            -webkit-box-pack: center;
            justify-content: center;
            -webkit-box-align: center;
            align-items: center;
            color: rgb(255, 255, 255);
            height: 90px;
            overflow: hidden;
            max-width: none;
            border-radius: 20px;
            background: var(--brand-primary-strong);
            border: none;

          }
        }
      }
    }
  }
  .content{
    width: 100%;
    height:30px;
    line-height: 30px;
    text-align: center;
    font-size: 24px;
    margin-top: 100px;
  }
  .btn1{
    width: 300px;
    height: 50px;
    background: var(--brand-primary-strong);
    color: #FFFFFF;
    display: block;
    margin: 0 auto;
    border-radius: 50px;
    font-size: 20px;
  }
  .btn2{
    width: 300px;
   border: none;
    display: block;
    margin: 20px auto;
    background: transparent;
    font-size: 20px;
  }
}
:deep(.queue-dialog) {
  border-radius: var(--radius-lg);
  background: var(--brand-card);
}

:deep(.queue-dialog .el-dialog__body) {
  padding: 30px 28px 0;
}

:deep(.queue-dialog .el-dialog__footer) {
  padding: 22px 28px 28px;
}
:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--brand-primary-strong);
  background: var(--brand-primary-strong);
}
:deep(.el-checkbox.el-checkbox--large .el-checkbox__inner) {
  width: 4.3vmin;
  height: 4.3vmin;
  color: #dddddd;
}
:deep(.el-checkbox__input.is-checked .el-checkbox__inner ){
  background-color: var(--brand-primary-strong);
  border-color: var(--brand-primary-strong);
  font-size:4.3vmin ;
}
:deep(.el-checkbox__inner::after){
  box-sizing: content-box;
  content: "";
  border: 1px solid var(--el-checkbox-checked-icon-color);
  border-left: 0;
  border-top: 0;
  height: 30px;
  left: 9px;
  position: absolute;
  top: -3px;
  transform: rotate(45deg) scaleY(0);
  width: 19px;
  transition: transform .15s ease-in 50ms;
  transform-origin: center;
}

/* 潮声订单确认体验层 */
.app-container {
  min-height: 100vh !important;
  height: auto !important;
  padding-bottom: 128px;
  background: var(--brand-page) !important;

  .confirm-order {
    width: min(1040px, calc(100% - var(--layout-gutter)));
    margin: 26px auto 0;

    .basic-info1 {
      overflow: visible !important;
      border: 1px solid var(--brand-line);
      border-radius: var(--radius-lg);
      background: var(--brand-card);
      box-shadow: var(--brand-shadow-soft);

      .top {
        position: relative !important;
        height: auto !important;
        padding: 28px 32px !important;
        border-radius: var(--radius-lg) var(--radius-lg) 0 0;
        background: #12110f !important;

        .title,
        .local,
        .time,
        .money,
        .order-info,
        .seat-info {
          margin-left: 0 !important;
          margin-right: 0 !important;
          color: #fff !important;
          max-width: 100% !important;
        }

        .title {
          font-size: 28px !important;
          line-height: 1.3;
          font-weight: 900 !important;
        }

        .local,
        .order-info {
          font-size: 14px !important;
          color: rgba(255, 255, 255, .7) !important;
        }

        .time {
          margin-top: 16px !important;
          font-size: 18px !important;
        }

        .money {
          margin-top: 12px;

          span {
            color: var(--brand-primary) !important;
            font-size: 22px !important;
            font-weight: 900;
          }
        }

        .line {
          display: none !important;
        }

        .seat-info {
          font-size: 14px !important;

          .seat-list .seat-item {
            border-radius: 999px !important;
            background: rgba(255, 255, 255, .12) !important;
          }
        }
      }

      .bottom {
        margin-top: 0 !important;
        padding: 20px 32px 0;

        .service-box {
          height: auto !important;
          line-height: normal !important;
          margin-top: 0 !important;
          flex-wrap: wrap;
          gap: 10px;
          color: var(--brand-text) !important;
          font-size: 14px !important;

          .service {
            width: auto !important;
            margin-left: 0 !important;
            font-weight: 900;
          }

          .service-name {
            height: 30px !important;
            line-height: 30px !important;
            margin-left: 0 !important;
            padding: 0 10px;
            border-radius: 999px;
            background: #f2ecdf;
          }
        }

        .line {
          margin: 18px 0 0 !important;
          width: 100% !important;
          height: 1px !important;
          background: var(--brand-line) !important;
        }
      }

      .isRealName,
      .sendMethod,
      .tel,
      .payMethod {
        margin: 0 !important;
        padding: 24px 32px;
      }

      .isRealName {
        display: grid !important;
        grid-template-columns: minmax(0, 1fr) auto;
        align-items: start;
        gap: 12px 18px;

        .left {
          float: none !important;
          margin-left: 0 !important;

          .title {
            color: var(--brand-dark) !important;
            font-size: 20px !important;
            font-weight: 900;
          }

          .notice {
            display: block !important;
            max-width: 640px;
            color: var(--brand-muted) !important;
            font-size: 14px !important;
            line-height: 1.7;
          }
        }

        .right {
          float: none !important;
          margin: 0 !important;

          .btn {
            width: auto !important;
            height: 38px !important;
            padding: 0 18px;
            border-radius: 999px !important;
            background: var(--brand-primary) !important;
            box-shadow: none !important;
            color: var(--brand-dark);
            font-size: 14px !important;
            font-weight: 900;
          }
        }

        .ticketInfo {
          grid-column: 1 / -1;
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 12px;
          padding: 12px 0 0 !important;

          .ticket {
            position: relative;
            height: auto !important;
            min-height: 76px;
            padding: 14px 16px;
            margin-top: 0;
            border: 1px solid var(--brand-line);
            border-radius: 10px;
            background: #fff;
            transition: background .18s ease, border-color .18s ease, box-shadow .18s ease;

            &:hover {
              border-color: rgba(201, 137, 18, .35);
              box-shadow: 0 10px 24px rgba(40, 32, 18, .06);
            }

            &.selected {
              border-color: var(--brand-primary-strong);
              background: #fff7dc;
              box-shadow: inset 0 0 0 1px rgba(201, 137, 18, .28), 0 10px 24px rgba(40, 32, 18, .08);
            }

            .info {
              flex: 1;
              min-width: 0;
              margin-left: 0 !important;

              .title {
                font-size: 17px !important;
              }

              .card {
                display: flex;
                flex-wrap: wrap;
                gap: 4px 8px;
                line-height: 1.6;
              }

              .card,
              .card .cardType,
              .card .cardId {
                font-size: 13px !important;
                word-break: break-all;
              }
            }

            .chx {
              margin-left: 16px;
            }
          }
        }
      }

      > .line,
      .sendline {
        margin: 0 32px !important;
        width: auto !important;
        height: 1px !important;
        background: var(--brand-line) !important;
      }

      .sendMethod {
        .sendMethodTitle,
        .ticketInfo {
          color: var(--brand-muted) !important;
          font-size: 14px !important;
        }

        .ticketType {
          height: auto !important;
          margin: 10px 0 !important;
          color: var(--brand-dark) !important;
          font-size: 20px !important;
          font-weight: 900;
        }
      }

      .tel,
      .payMethod {
        .title {
          margin: 0 0 10px !important;
          color: var(--brand-muted) !important;
          font-size: 14px !important;
        }

        .telNum {
          color: var(--brand-dark) !important;
          font-size: 18px !important;
          font-weight: 800;
        }

        .payMoney {
          height: auto !important;
          align-items: center;
          gap: 12px;

          img {
            width: 38px !important;
            height: 38px !important;
          }

          span {
            margin-right: 0 !important;
            padding: 0 !important;
            color: var(--brand-dark) !important;
            font-size: 16px !important;
          }
        }
      }

      .info {
        position: sticky;
        bottom: 0;
        z-index: 20;
        height: auto !important;
        min-height: 92px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 18px;
        padding: 16px 32px;
        border-top: 1px solid var(--brand-line);
        background: rgba(255, 253, 248, .95) !important;
        box-shadow: 0 -14px 36px rgba(40, 32, 18, .1);
        backdrop-filter: blur(16px);
        border-radius: 0 0 var(--radius-lg) var(--radius-lg);

        .descript {
          margin: 0 !important;
          max-width: 560px !important;
          color: var(--brand-muted) !important;
          font-size: 13px !important;
          line-height: 1.7;
        }

        .price {
          display: flex !important;
          margin: 0 !important;
          align-items: center;
          justify-content: flex-end;
          gap: 10px;
          flex: 0 0 auto;

          .num {
            color: var(--brand-primary-strong) !important;
            font-size: 28px !important;
            font-weight: 900;
          }

          .detail {
            font-size: 14px !important;
          }

          .submit {
            position: static !important;
            width: auto !important;
            min-width: 148px;
            height: 44px !important;
            border-radius: 999px !important;
            background: var(--brand-primary) !important;
            color: var(--brand-dark) !important;
            font-size: 15px !important;
            font-weight: 900;
          }
        }
      }
    }
  }
}

.app-container .confirm-order .basic-info1 .isRealName {
  display: block !important;

  .buyer-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 18px;
    margin-bottom: 14px;
  }

  .left {
    float: none !important;
    margin-left: 0 !important;
  }

  .add-buyer,
  .buyer-empty button {
    height: 36px;
    flex: 0 0 auto;
    padding: 0 16px;
    border: 0;
    border-radius: 999px;
    background: var(--brand-primary);
    color: var(--brand-dark);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
  }

  .ticketInfo {
    display: grid !important;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 12px;
    padding: 0 !important;
  }

  .ticket {
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;

    .checkSelect {
      margin-left: 16px;
    }
  }

  .buyer-empty {
    min-height: 96px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    gap: 12px;
    border: 1px dashed var(--brand-border);
    border-radius: 10px;
    color: var(--brand-muted);
    background: #fffaf0;
  }
}

:deep(.el-dialog) {
  border-radius: var(--radius-lg) !important;
  background: var(--brand-card) !important;
}

:deep(.el-checkbox.el-checkbox--large .el-checkbox__inner) {
  width: 20px !important;
  height: 20px !important;
}

:deep(.el-checkbox__inner::after) {
  height: 9px !important;
  left: 6px !important;
  top: 2px !important;
  width: 5px !important;
}

.app-container .confirm-order .basic-info1 .isRealName {
  .buyer-head .left {
    .title {
      font-weight: 500 !important;
    }

    .notice {
      color: var(--brand-muted) !important;
    }
  }

  .ticketInfo {
    grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)) !important;
    gap: 10px !important;
    align-items: stretch;
  }

  .ticket {
    min-height: 112px !important;
    display: grid !important;
    grid-template-columns: minmax(0, 1fr) 88px;
    gap: 16px;
    align-items: stretch;
    padding: 15px 16px !important;
    border-radius: 4px !important;
    border-color: #e7dfd2 !important;
    background: #fff !important;
    box-shadow: none !important;

    &:hover {
      border-color: #e7dfd2 !important;
      box-shadow: none !important;
    }

    &.selected {
      border-color: var(--brand-primary-strong) !important;
      background: #fffdf7 !important;
      box-shadow: inset 3px 0 0 var(--brand-primary-strong) !important;
    }
  }

  .ticket .info {
    min-width: 0;
    display: grid;
    align-content: center;
    gap: 8px;
    margin-left: 0 !important;
  }

  .buyer-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .buyer-role {
    height: 20px;
    display: inline-flex;
    align-items: center;
    padding: 0 6px;
    border-radius: 3px;
    border: 1px solid #eadfcd;
    background: #fffaf0;
    color: var(--brand-muted);
    font-size: 12px;
    white-space: nowrap;
  }

  .card {
    display: grid !important;
    gap: 4px;
    color: var(--brand-muted);
    line-height: 1.55;
  }

  .card-line {
    display: grid;
    grid-template-columns: 68px minmax(0, 1fr);
    column-gap: 12px;
    align-items: baseline;
    min-width: 0;
  }

  .field-label {
    color: #9b8f7f;
    font-size: 13px;
    white-space: nowrap;
  }

  .card .cardType,
  .card .cardId {
    width: auto !important;
    min-width: 0;
    display: block !important;
    color: var(--brand-text) !important;
    font-size: 13px !important;
    word-break: break-all;
  }

  .buyer-action {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: center;
    gap: 8px;
    min-width: 72px;
  }

  .selected-mark {
    height: 20px;
    display: inline-flex;
    align-items: center;
    padding: 0;
    background: transparent;
    color: var(--brand-primary-strong);
    font-size: 12px;
    white-space: nowrap;
  }

  .ticket .checkSelect {
    margin-left: 0 !important;
  }

  .buyer-empty {
    border-radius: 6px !important;
  }
}

@media (max-width: 760px) {
  .app-container {
    .confirm-order {
      width: calc(100% - 24px);
    }

    .confirm-order .basic-info1 .info {
      align-items: stretch;
      flex-direction: column;
      padding: 14px 18px;

      .price {
        justify-content: space-between;
      }
    }
  }
}

</style>

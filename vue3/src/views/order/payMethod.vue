<template>
  <div class="app-container">
    <Header></Header>
    <div class="pay-header">
      <div class="back" @click="router.back()"><el-icon><ArrowLeftBold /></el-icon></div>
      <div class="content"><img :src="pay" alt=""><span>支付宝付款</span></div>
    </div>
    <div class="pay-section">
      <el-button
          type="primary"
          class="payContinue"
          :loading="paying || detailLoading"
          :disabled="!canPay"
          @click="continuePay"
      >继续浏览器付款</el-button>
    </div>
  </div>
</template>

<script setup name="PayMethod">
import pay from "@/assets/section/pay.png"
import {computed, ref,onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {getOrderDetailApi,orderPayApi} from "@/api/order.js";
import Header from '@/components/header/index'
import {ElMessage} from 'element-plus'
//订单编号
const orderNumber = ref('')
//订单详情数据
const orderDetailData = ref(null);
const detailLoading = ref(false)
const paying = ref(false)
const router = useRouter();
const paySubject = computed(() => orderDetailData.value?.programTitle || '电影票订单')
const canPay = computed(() => {
  return Boolean(orderNumber.value && orderDetailData.value?.orderPrice)
})


async function continuePay() {
  if (paying.value) {
    return
  }
  try {
    paying.value = true
    //支付前，要调取订单详情
    if (!orderDetailData.value) {
      await getOrderDetail()
    }
    if (!canPay.value) {
      ElMessage.error('订单支付信息不完整，请返回订单列表后重新支付')
      return
    }
    const orderPayParams = {
      'platform':3,
      'orderNumber':orderNumber.value,
      'subject':paySubject.value,
      'price':orderDetailData.value.orderPrice,
      'channel':'alipay',
      'payBillType':1
    }
    const response = await orderPayApi(orderPayParams)
    if (String(response.code) !== '0' || !response.data) {
      ElMessage.error(response.message || '支付宝支付创建失败')
      return
    }
    //将支付宝返回的表单字符串写在浏览器中，表单会自动触发submit提交
    document.open()
    document.write(response.data)
    document.close()
  } catch (error) {
    ElMessage.error('支付宝支付创建失败，请稍后重试')
  } finally {
    paying.value = false
  }
}

//跳转后的接收值
onMounted(async () => {
  await getOrderDetail()
})
//订单详情方法
async function getOrderDetail() {
  orderNumber.value = history.state?.orderNumber || localStorage.getItem('orderNumber') || '';
  if (!orderNumber.value) {
    ElMessage.error('订单号已失效，请返回订单列表后重新支付')
    router.replace({path: '/orderManagement/index'})
    return
  }
  const orderDetailParams = {'orderNumber': orderNumber.value}
  //传值-订单号
  localStorage.setItem('orderNumber',orderNumber.value)
  detailLoading.value = true
  try {
    const response = await getOrderDetailApi(orderDetailParams)
    if (String(response.code) !== '0' || !response.data) {
      ElMessage.error(response.message || '订单详情获取失败')
      return
    }
    orderDetailData.value = response.data;
  } catch (error) {
    ElMessage.error('订单详情获取失败')
  } finally {
    detailLoading.value = false
  }
}

</script>

<style scoped lang="scss">
.app-container {
  min-height: 100vh;
  background: var(--brand-page);
  padding-bottom: 48px;

  .pay-header {
    width: min(720px, calc(100% - var(--layout-gutter)));
    margin: 42px auto 0;
    display: flex;
    align-items: center;
    min-height: 92px;
    padding: 0 24px;
    border: 1px solid var(--brand-line);
    border-radius: var(--radius-lg);
    background: var(--brand-card);
    box-shadow: var(--brand-shadow-soft);

    .back {
      width: 40px;
      height: 40px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      cursor: pointer;
      transition: background .18s ease;

      &:hover {
        background: #f2ecdf;
      }

      .el-icon{
        font-size: 22px;
      }
    }

    .content {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;

      img {
        width: 44px;
        height: 44px;
      }

      span {
        color: var(--brand-dark);
        font-size: 22px;
        font-weight: 900;
      }
    }
  }
  .pay-section{
    width: min(720px, calc(100% - var(--layout-gutter)));
    margin: 18px auto 0;
    padding: 28px;
    border: 1px solid var(--brand-line);
    border-radius: var(--radius-lg);
    background: var(--brand-card);
    box-shadow: var(--brand-shadow-soft);

    .payContinue{
      width: 100%;
      height: 48px;
      margin: 0;
      border: 0;
      border-radius: 999px;
      background: var(--brand-primary);
      color: var(--brand-dark);
      font-size: 16px;
      font-weight: 900;
    }
  }
}

</style>

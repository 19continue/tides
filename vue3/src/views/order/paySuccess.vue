<template>
<div class="app-container" v-show="orderNumber !== '' && orderNumber !== null">
  <Header></Header>
  <div class="main">
    <el-icon :size="54" class="iconCircle"><CircleCheck color="#2f8f83" /></el-icon>
    <span class="paySuccess">支付成功</span>
    <p class="pay-desc">订单已生成，可在订单列表查看入场凭证与支付状态。</p>
   <div class="btn">
     <el-button  class="continueQuery" @click="continueQuery"    >继续逛逛</el-button>
     <el-button   class="orderQuery" @click="orderQuery"    >订单列表</el-button>
   </div>
  </div>
  <Footer></Footer>
</div>
</template>

<script setup name="PaySuccess">
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {payCheckApi} from '@/api/order.js'
const router = useRouter();
const orderNumber = ref('');

//继续逛逛
const  continueQuery=()=>{
  router.replace({path:'/index'})
}
//查看订单列表
const orderQuery=()=>{
  router.push({path:'/orderManagement/index'})
}

onMounted(()=>{
  orderNumber.value =  localStorage.getItem('orderNumber' )
  //将这次获取到订单号移除
  localStorage.removeItem('orderNumber')
  if (orderNumber.value != '' && orderNumber.value != null){
    const tradeCheckParams = {
      'orderNumber':orderNumber.value,
      'payChannelType':'1'
    }
    payCheckApi(tradeCheckParams).catch(() => {
      // 支付结果页只做状态同步，订单列表仍会展示最终状态。
    })
  }else {
    router.replace({path:'/'})
  }
})
</script>

<style scoped lang="scss">
.app-container{
  width: 100%;
  margin: 0 auto;
  overflow: auto;
  background: var(--brand-page);

  .main{
    min-height: 420px;
    width: min(720px, calc(100% - var(--layout-gutter)));
    margin: 32px auto 40px;
    padding: 58px 34px;
    text-align: center;
    position: relative;
    border: 1px solid var(--brand-line);
    border-radius: var(--radius-lg);
    background: var(--brand-card);
    box-shadow: var(--brand-shadow-soft);

    .iconCircle{
      display: inline-flex;
      }
    .paySuccess{
      display: block;
      margin-top: 16px;
      color: var(--brand-dark);
      font-size: 30px;
      font-weight: 900;
    }

    .pay-desc {
      margin: 12px 0 0;
      color: var(--brand-muted);
      font-size: 14px;
    }

    .btn{
      text-align: center;
      margin-top: 28px;
      display: flex;
      justify-content: center;
      gap: 12px;

      .continueQuery,
      .orderQuery{
        width: 112px;
        height: 40px;
        border-radius: 999px;
        font-weight: 800;
        &:hover{
          color: var(--brand-primary-strong);
          border-color:  var(--brand-primary-strong);
          background: #ffffff;
        }
      }

      .orderQuery {
        color: var(--brand-dark);
        background: var(--brand-primary);
        border-color: var(--brand-primary);
      }
    }
  }
}
</style>

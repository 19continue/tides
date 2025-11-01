<template>
  <Header></Header>
  <main class="buyer-page">
    <section class="buyer-panel">
      <div class="page-head">
        <div>
          <span>购票流程</span>
          <h1>新增{{ userRole }}</h1>
        </div>
        <button type="button" class="back-link" @click="backToOrder">返回订单</button>
      </div>

      <el-form ref="formTicket" :model="form" :rules="rules" class="buyer-form" label-width="96px">
        <el-form-item label="姓名" prop="relName">
          <el-input
              v-model="form.relName"
              type="text"
              :placeholder="`请填写${userRole}姓名`"
          ></el-input>
        </el-form-item>
        <el-form-item label="证件类型" prop="idType">
          <el-select v-model="form.idType">
            <el-option
                v-for="item in idType"
                :key="item.value"
                :value="item.value"
                :label="item.name"
            >{{ item.name }}</el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码" prop="idNumber">
          <el-input
              v-model="form.idNumber"
              type="text"
              placeholder="请填写证件号码"
          ></el-input>
        </el-form-item>

        <div class="tips">
          <el-icon><Warning /></el-icon>
          <span>提交后会保存为常用{{ userRole }}，用于本次订单实名校验。</span>
        </div>

        <div class="actions">
          <el-button class="cancel" @click="backToOrder">取消</el-button>
          <el-button class="submit" type="primary" @click="submit">保存并返回订单</el-button>
        </div>
      </el-form>
    </section>
  </main>
  <Footer></Footer>
</template>

<script setup name="BuyTicket">
import {computed, getCurrentInstance, onMounted, ref} from 'vue'
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {Warning} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {saveTicketUser} from "@/api/buyTicketUser";
import {getOrderState, saveOrderState} from '@/utils/orderState'
import {getUserIdKey} from "@/utils/auth";
import {useRouter} from 'vue-router'

const router = useRouter()
const {proxy} = getCurrentInstance()
const form = ref({
  relName: '',
  idType: '1',
  idNumber: ''
})
const orderState = ref({})

const userRole = computed(() => {
  try {
    const detail = JSON.parse(orderState.value.detailList || '{}')
    return detail?.parentProgramCategoryName === '电影' ? '观影人' : '观演人'
  } catch (error) {
    return '购票人'
  }
})

const rules = ref({
  relName: [{required: true, message: '请填写姓名', trigger: 'blur'}],
  idType: [{required: true, message: '请选择证件类型', trigger: 'change'}],
  idNumber: [{required: true, message: '请填写证件号码', trigger: 'blur'}]
})

const idType = ref([{
  name: '身份证',
  value: '1'
}, {
  name: '港澳台居民居住证',
  value: '2'
}, {
  name: '港澳居民来往内地通行证',
  value: '3'
}, {
  name: '台湾居民来往内地通行证',
  value: '4'
}, {
  name: '护照',
  value: '5'
}, {
  name: '外国人永久居住证',
  value: '6'
}])

onMounted(() => {
  const state = getOrderState(history.state) || {}
  orderState.value = {
    detailList: state.detailList,
    allPrice: state.allPrice,
    countPrice: state.countPrice,
    num: state.num,
    ticketCategoryId: state.ticketCategoryId,
    screeningId: state.screeningId,
    seatIdList: state.seatIdList,
    isChooseSeat: state.isChooseSeat,
    selectedSeats: state.selectedSeats
  }
})

function backToOrder() {
  if (!orderState.value.detailList) {
    router.back()
    return
  }
  saveOrderState(orderState.value)
  router.replace({path: '/order/index', state: orderState.value})
}

const submit = () => {
  proxy.$refs.formTicket.validate(valid => {
    if (!valid) {
      return
    }
    const userId = getUserIdKey()
    if (!userId) {
      ElMessage.error('登录信息已失效，请重新登录')
      return
    }
    form.value.userId = userId
    saveTicketUser(form.value).then(response => {
      if (response.code == 0) {
        ElMessage.success('保存成功')
        backToOrder()
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    })
  })
}
</script>

<style scoped lang="scss">
.buyer-page {
  width: min(var(--layout-width), calc(100% - var(--layout-gutter)));
  min-height: 520px;
  margin: 28px auto 72px;
}

.buyer-panel {
  width: min(760px, 100%);
  padding: 28px 32px 34px;
  border-radius: 10px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 28px;

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

.back-link {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  background: #fff;
  color: var(--brand-muted);
  cursor: pointer;
}

.buyer-form {
  width: min(560px, 100%);
}

.tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0 24px 96px;
  color: var(--brand-muted);
  font-size: 13px;
  line-height: 22px;
}

.actions {
  display: flex;
  gap: 10px;
  margin-left: 96px;

  .cancel,
  .submit {
    height: 36px;
    min-width: 96px;
    border-radius: 999px;
  }

  .submit {
    border: 0;
    background: var(--brand-primary);
    color: var(--brand-dark);
    font-weight: 600;
  }
}

.buyer-form :deep(.el-input__wrapper),
.buyer-form :deep(.el-select .el-input__wrapper) {
  flex-grow: 0;
  width: 360px;
  box-shadow: 0 0 0 1px var(--brand-line) inset;
}

.buyer-form :deep(.el-form-item__label) {
  color: var(--brand-muted);
  font-weight: 400;
}

@media (max-width: 760px) {
  .buyer-page {
    width: calc(100% - 24px);
  }

  .buyer-panel {
    padding: 22px 18px 26px;
  }

  .page-head {
    flex-direction: column;
  }

  .tips,
  .actions {
    margin-left: 0;
  }

  .buyer-form :deep(.el-input__wrapper),
  .buyer-form :deep(.el-select .el-input__wrapper) {
    width: 100% !important;
  }
}
</style>

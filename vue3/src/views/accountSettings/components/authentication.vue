<template>
  <Header></Header>
  <div class="section">
    <MenuSideBar class="sidebarMenu" activeIndex="2"></MenuSideBar>
    <div class="right-section">
      <div class="page-head">
        <div>
          <span>账号设置</span>
          <h1>实名认证</h1>
        </div>
        <router-link class="back-link" to="/accountSettings/index">返回账号设置</router-link>
      </div>
      <div class="panel">
        <p class="tips">实名信息用于购票人校验和入场核验，请填写与证件一致的信息。</p>
        <el-form ref="authRef" :model="authForm" :rules="authRules" class="account-form" label-width="112px">
          <el-form-item label="真实姓名" prop="relName">
            <el-input
                v-model="authForm.relName"
                class="input-with-select"
                type="text"
            ></el-input>
          </el-form-item>
          <el-form-item label="身份证号" prop="idNumber">
            <el-input
                v-model="authForm.idNumber"
                class="input-with-select"
                type="password"
                show-password
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button
                type="primary"
                class="btn"
                @click.prevent="savePsd"
            >保存</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
  <Footer></Footer>
</template>

<script setup>

import Header from '../../../components/header/index'
import Footer from '../../../components/footer/index'
import MenuSideBar from '../../../components/menuSidebar/index'
import {ElMessage} from "element-plus"
import {getUserIdKey} from "../../../utils/auth"
import {ref, reactive} from 'vue'
import useUserStore from '@/store/modules/user'
import {getAuthentication} from "../../../api/accountSettings";


const authRef = ref(null)
const userStore = useUserStore()
const authForm = ref({
  idNumber: '',
  relName: '',
  id: getUserIdKey()
})


const authRules = reactive({
      idNumber: [{
        required: true,
        pattern: /(^\d{15}$)|(^\d{17}[\dXx]$)/,
        message: '请输入正确的身份证号码',
        trigger: 'blur'
      }],
      relName: [{required: true, message: '请输入真实姓名', trigger: 'blur'}],
    }
)


function savePsd() {
  authRef.value.validate(valid => {
    if (!valid) {
      return
    }
    getAuthentication(authForm.value).then(response => {
      if (response.code == '0') {
        ElMessage({
          message: '保存成功',
          type: 'success',
        })

        userStore.logOut().then(() => {
          location.href = '../../login';
        })

      } else {
        ElMessage({
          message: response.message,
          type: 'error',
        })
      }
      })
  })
}
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

.back-link {
  color: var(--brand-primary-strong);
  font-size: 13px;
}

.panel {
  margin-top: 14px;
  padding: 28px 32px 34px;
  border-radius: 10px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.tips {
  margin: 0 0 24px;
  color: var(--brand-muted);
  font-size: 13px;
  line-height: 22px;
}

.account-form {
  width: min(560px, 100%);
}

.btn {
  height: 36px;
  min-width: 96px;
  border: 0;
  border-radius: 999px;
  background: var(--brand-primary);
  color: var(--brand-dark);
  font-weight: 600;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  color: var(--brand-muted);
  font-weight: 400;
}

.account-form :deep(.el-input__wrapper) {
  flex-grow: 0;
  width: 360px;
  box-shadow: 0 0 0 1px var(--brand-line) inset;
}

@media (max-width: 760px) {
  .section {
    display: block;
    width: calc(100% - 24px);
  }

  .right-section {
    margin-top: 18px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .account-form :deep(.el-input__wrapper) {
    width: 100%;
  }
}
</style>

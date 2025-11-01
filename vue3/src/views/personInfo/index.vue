<template>
  <!--个人信息-->
  <Header></Header>
  <div class="red-line"></div>
  <div class="section">
    <MenuSideBar class="sidebarMenu" activeIndex="3"></MenuSideBar>
    <div class="right-section">
      <div class="breadcrumb"><span>个人信息</span></div>
      <div class="right-tab">
        <ul class="title">
          <li class="left">基础资料</li>
<!--          <li class="right">资料完整度：<span>30%</span></li>-->
        </ul>
        <div class="box">
          <div class="info-list">
            <div class="tips-info">完善更多个人信息，有助于我们为您提供更加个性化的服务，本程序将尊重并保护您的隐私。</div>
            <el-form ref="perInfoRef" :model="perInfoForm" :rules="perInfoRules" class="perInfo-form">
              <el-form-item label-width="100px" label="昵称:" prop="name">
                <el-input v-model="perInfoForm.name"/>
              </el-form-item>
              <el-form-item label-width="100px" label="真实姓名:" prop="relName">
                <el-input v-model="perInfoForm.relName"/>
              </el-form-item>
              <el-form-item label-width="100px" label="性别:" prop="gender">
                <el-radio-group v-model="perInfoForm.gender">
                  <el-radio label="1" size="large">男</el-radio>
                  <el-radio label="2" size="large">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label-width="100px" label="身份证号:" prop="idNumber">
                <el-input v-model="perInfoForm.idNumber"/>
              </el-form-item>
              <el-button
                  size="small"
                  type="primary"
                  class="btn"
                  @click.prevent="gePersonList"
              >保存
              </el-button>

            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
  <Footer class="foot"></Footer>
</template>

<script setup>
import MenuSideBar from '../../components/menuSidebar/index'
import Header from '../../components/header/index'
import Footer from '../../components/footer/index'
import {ref, reactive, getCurrentInstance,nextTick,onMounted } from 'vue'
import {getPersonInfo, getPersonInfoId} from '@/api/personInfo'
import useUserStore from "../../store/modules/user";
import {ElMessage} from 'element-plus'
import {getName,getUserIdKey} from "@/utils/auth";

const {proxy} = getCurrentInstance();
const useUser = useUserStore()

const perInfoForm = reactive({
  name: '',
  relName: '',
  gender: '1',
  idNumber: '',
  id: useUser.userId.value
})
const perInfoRules = ref({
  name: [
    {required: true, trigger: "blur", message: "请输入昵称"},
  ],
  gender: [
    {required: true, trigger: "blur",},
  ],
})


function gePersonList() {
  proxy.$refs.perInfoRef.validate(valid => {
    if (valid) {
      getPersonInfo(perInfoForm).then(response => {
        if (response.code == 0) {
          ElMessage({
            message: '保存成功',
            type: 'success',
          })
        }else{
            ElMessage({
              message: response.message,
              type: 'error',
            })
        }

      })

    }
  })
}

//回显
onMounted(()=>{
  nextTick(()=>{
    getPersonInfoIdList()
  })
})


async function getPersonInfoIdList() {
  const id = getUserIdKey()
  getPersonInfoId({id: id}).then(response => {
    let {gender, id, idNumber, name, relAuthenticationStatus, relName} = response.data
    perInfoForm.name = name
    perInfoForm.relName = relName
    perInfoForm.gender = gender
    perInfoForm.idNumber = idNumber
    perInfoForm.id = id
  })
}
</script>

<style scoped lang="scss">
.red-line {
  display: none;
}

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

.breadcrumb {
  min-height: 44px;
  display: flex;
  align-items: center;
  padding: 0 2px;
  color: var(--brand-dark);
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
}

.right-tab {
  margin-top: 14px;
  overflow: hidden;
  border-radius: 10px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

.title {
  height: auto;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0 24px;
  border-bottom: 1px solid var(--brand-line);
  list-style: none;

  li.left {
    height: 52px;
    display: flex;
    align-items: center;
    color: var(--brand-dark);
    font-size: 15px;
    font-weight: 500;
  }
}

.box {
  border: 0;
}

.info-list {
  padding: 28px 32px 34px;
  color: var(--brand-text);
}

.tips-info {
  margin-bottom: 24px;
  padding: 12px 16px;
  border: 0;
  border-radius: 8px;
  background: #fbf4df;
  color: var(--brand-muted);
  font-size: 13px;
  line-height: 22px;
}

.perInfo-form {
  width: min(560px, 100%);
}

.btn {
  height: 36px;
  min-width: 96px;
  margin-left: 100px;
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

.perInfo-form :deep(.el-input__wrapper) {
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

  .btn {
    margin-left: 0;
  }

  .perInfo-form :deep(.el-input__wrapper) {
    width: 100%;
  }
}
</style>

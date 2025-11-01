<template>
  <!--个人信息-->
  <div class="container">
    <Header></Header>
    <div class="red-line"></div>
    <div class="section">
      <MenuSideBar class="sidebarMenu" activeIndex="4"></MenuSideBar>
      <div class="right-section">
        <div class="breadcrumb"><span>常用购票人管理</span></div>
        <div class="right-tab">
          <el-button class="addUser" @click="addTicketUser">新增购票人</el-button>
          <el-table :data="ticketUserListData" v-if="isShow" style="width: 100%" border>
            <el-table-column  type="index"  label="序号" width="100px"   align="center"/>
            <el-table-column prop="relName" label="姓名"    align="center"/>
            <el-table-column prop="index" label="证件类型"  align="center">
              <template #default="scope">
                {{ getIdTypeName(scope.row.idType)}}
              </template>
            </el-table-column>
            <el-table-column prop="idNumber" label="证件号"   align="center" />
            <el-table-column prop="index" label="操作"   align="center">
              <template #default="scope">
                <el-button link type="primary" icon="Delete" @click="delTicketUser(scope.row.id)">删除</el-button>
              </template>

            </el-table-column>

          </el-table>
          <div class="addTicketUserInfo"  v-if="!isShow">
            <div class="title">新增购票人信息</div>
            <div class="line"></div>
            <el-form ref="ticketRef" :model="formTicket" :rules="formTicketRules" class="ticketForm" label-width="100px" >
              <el-col :span="24">
                <el-form-item label="姓名:"  prop="relName" >
                  <el-input
                      v-model="formTicket.relName"
                      type="text"
                      placeholder="请填填写姓名"
                  > </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="证件类型:" prop="idType"  >
                  <el-select  v-model="formTicket.idType"  >
                    <el-option v-for="item in idType"
                               :value="item.value"
                               :label="item.name" >{{item.name}}</el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="证件号码:" prop="idNumber" >
                  <el-input
                      v-model="formTicket.idNumber"
                      type="text"
                      placeholder="请填写证件号码"
                  ></el-input>
                </el-form-item>
              </el-col>
             <el-form-item>
               <el-button
                   class="save"
                   @click.prevent="saveTicket"
               >保存</el-button>
               <el-button
                   class="btn"
                   @click.prevent="closeTicket"
               >取消</el-button>
             </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
    <Footer class="foot"></Footer>
  </div>

</template>

<script setup name="TicketUser">
import MenuSideBar from '../../components/menuSidebar/index'
import Header from '../../components/header/index'
import Footer from '../../components/footer/index'
import {ref, computed, onMounted, reactive, getCurrentInstance} from 'vue'
import useUserStore from "../../store/modules/user";
import {delTicketUserApi, selectTicketUserListApi} from '@/api/accountCenter.js'
import {getIdTypeName} from '@/api/common.js'
import {ElMessage} from 'element-plus'
import { getUserIdKey} from "@/utils/auth";
import {saveTicketUser} from "@/api/buyTicketUser";


const {proxy} = getCurrentInstance();
const useUser = useUserStore()
//购票人列表入参
const ticketUserListParams = reactive({
  userId:undefined
})
const formTicket = ref({})
formTicket.value.idType = ref('1')

const formTicketRules = ref({
  relName:  [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber:[{ required: true, message: "请输入证件号码", trigger: "blur" }],
})
//购票人列表数据
const ticketUserListData = ref([])
const isShow = ref(true)

const idType = ref([{
  name:'身份证',
  value:'1'
},{
  name:'港澳台居民居住证',
  value:'2'
},{
  name:'港澳台居民来往内地通行证',
  value:'3'
},{
  name:'台湾居民来往内地通行证',
  value:'4'
},{
  name:'护照',
  value:'5'
},{
  name:'歪果仁永久居住证',
  value:'6'
}])
// 获取路由参数
ticketUserListParams.userId = useUser.userId
selectTicketUserList()
//购票人列表方法
function selectTicketUserList() {
  selectTicketUserListApi(ticketUserListParams).then(response => {
    ticketUserListData.value = response.data;
  })
}

function delTicketUser(ticketUserId){
  const delTicketUserParam = {'id' : ticketUserId}
  delTicketUserApi(delTicketUserParam).then(response => {
    selectTicketUserList()
  })
}
//新增购票人
function addTicketUser(){
  isShow.value =false
  reset()
}



//保存
function saveTicket(){
  proxy.$refs.ticketRef.validate(valid => {
    if (valid) {
      formTicket.value.userId=getUserIdKey()
      saveTicketUser(formTicket.value).then(response=>{
        if(response.code==0){
          isShow.value = true
          selectTicketUserList()
          reset()
        }
      })


    }
  });
}
//取消
function closeTicket(){
  isShow.value =true
  reset()
}

function reset(){
  formTicket.value= {}
  formTicket.value.idType = ref('1')
}
</script>

<style scoped lang="scss">
.container {
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
    padding: 24px;
    border-radius: 10px;
    background: var(--brand-card);
    box-shadow: var(--brand-shadow-soft);
  }

  .addUser {
    height: 36px;
    float: right;
    margin: 0 0 16px;
    border: 0;
    border-radius: 999px;
    background: var(--brand-primary);
    color: var(--brand-dark);
    font-weight: 600;
  }

  .addTicketUserInfo {
    clear: both;
    border: 0;

    .title {
      width: auto;
      line-height: 28px;
      border-bottom: 0;
      padding-left: 0;
      font-size: 16px;
      font-weight: 500;
      color: var(--brand-dark);
    }

    .line {
      width: 100%;
      height: 1px;
      background: var(--brand-line);
      margin: 14px 0 28px;
    }

    .ticketForm {
      width: min(560px, 100%);

      .save {
        border: 0;
        border-radius: 999px;
        background: var(--brand-primary);
        color: var(--brand-dark);
        font-weight: 600;
      }

      .btn {
        border-radius: 999px;
      }
    }
  }

}

:deep(.el-table) {
  clear: both;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background: #f8f3e7;
  color: var(--brand-muted);
  font-weight: 500;
}

.addTicketUserInfo :deep(.el-input__wrapper) {
  flex-grow: 0;
  width: 360px;
  box-shadow: 0 0 0 1px var(--brand-line) inset;
}

:deep(.el-form-item__label) {
  color: var(--brand-muted);
  font-weight: 400;
}

.addTicketUserInfo :deep(.el-select .el-input__wrapper) {
  flex-grow: 0;
  width: 360px !important;
}

@media (max-width: 760px) {
  .container {
    .section {
      display: block;
      width: calc(100% - 24px);
    }

    .right-section {
      margin-top: 18px;
    }
  }

  .addTicketUserInfo :deep(.el-input__wrapper),
  .addTicketUserInfo :deep(.el-select .el-input__wrapper) {
    width: 100% !important;
  }
}
</style>

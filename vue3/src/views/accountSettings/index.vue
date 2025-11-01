<template>
  <!--个人信息-->
  <Header></Header>
  <div class="red-line"></div>
  <div class="section">
    <MenuSideBar class="sidebarMenu" activeIndex="2"></MenuSideBar>
    <div class="right-section">
      <div class="breadcrumb"><span>账号设置</span></div>
      <div class="right-tab">
        <ul class="title">
          <li class="left">账号设置</li>
        </ul>
        <div class="box">
          <div class="account-info" v-for="item in accountLists" :key="item.nameInfo">
            <ul>
              <li :class="item.nameInfoStyle">{{ item.nameInfo }}</li>
              <li class="detail">{{ item.detailInfo }}</li>
              <li class="explain">
                <router-link v-if="experienceAccountFlag != 1" :to="item.path"
                             :class="(item.explainInfo =='立即验证'||item.explainInfo =='立即绑定')? 'pathBtn':'btnColor'">
                  {{ item.explainInfo }}
                </router-link>
                <div class="btnColor" v-if="experienceAccountFlag == 1">
                    体验不支持
                </div>  
              </li>
            </ul>
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
import useUserStore from "../../store/modules/user";
import {getName, getUserIdKey} from "../../utils/auth";
import {getPersonInfoId} from '@/api/personInfo'
import {ref, reactive} from 'vue'

//体验账号标识
const experienceAccountFlag = ref(import.meta.env.VITE_EXPERIENCE_ACCOUNT_FLAG);
let telNum = ref('')

const accountList = reactive([
  {
    nameInfo: '登录密码',
    detailInfo: '',
    explainInfo: '修改',
    path: './editPassword',
    nameInfoStyle: 'name-info-yes'
  },
  {
    nameInfo: '邮箱验证',
    detailInfo: '验证邮箱可帮助您快速找回密码，并可接收订单、演出通知、促销活动等提醒',
    explainInfo: '立即绑定',
    path: './email',
    nameInfoStyle: 'name-info-no'
  },
  {
    nameInfo: '手机验证',
    detailInfo: `您验证的手机：${telNum.value}`,
    explainInfo: '更换',
    path: './mobile',
    nameInfoStyle: 'name-info-yes'
  },
  {
    nameInfo: '实名认证',
    detailInfo: '认证您的实名信息，提高安全等级',
    explainInfo: '立即验证',
    path: './authentication',
    nameInfoStyle: 'name-info-no'

  }
])

let accountLists = ref(accountList)

getIsVaild()

//通过id获取是否进行验证，为验证的话控制图标，按钮的显示
function getIsVaild() {
  const id = getUserIdKey()
  getPersonInfoId({id: id}).then(response => {
    let {relAuthenticationStatus, emailStatus,mobile} = response.data
    telNum.value = mobile
    //此处判断是否验证，来控制显示那种图标
    accountLists.value = accountList.map(item => {
      if (item.nameInfo == '邮箱验证') {
        emailStatus == "0" ? item.nameInfoStyle = 'name-info-no' : item.nameInfoStyle = 'name-info-yes'
      } else if (item.nameInfo == '实名认证') {
        relAuthenticationStatus == "0" ? item.nameInfoStyle = 'name-info-no' : item.nameInfoStyle = 'name-info-yes'
      } else if (item.nameInfo == '手机验证') {
        item.detailInfo = mobile ? `您验证的手机：${mobile}` : '绑定手机可用于登录、订单通知和安全校验'
      }

      return item
    })
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
  padding: 8px 24px 24px;
}

.account-info {
  border: 0;
  border-bottom: 1px solid var(--brand-line);
  padding: 20px 0;
  margin-top: 0;

  &:last-child {
    border-bottom: 0;
  }

  ul {
    min-height: 46px;
    display: grid;
    grid-template-columns: 170px minmax(0, 1fr) 104px;
    align-items: center;
    gap: 22px;
    margin: 0;
    padding: 0;
    list-style: none;
  }
}

.name-info-yes,
.name-info-no {
  position: relative;
  min-height: 36px;
  display: flex;
  align-items: center;
  padding-left: 44px;
  color: var(--brand-dark);
  font-size: 16px;
  font-weight: 500;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 2px;
    width: 32px;
    height: 32px;
    border-radius: 50%;
  }
}

.name-info-yes::before {
  background: #edf8ef;
  box-shadow: 0 0 0 1px #cfe8d3 inset;
}

.name-info-yes::after {
  content: '';
  position: absolute;
  left: 10px;
  top: 11px;
  width: 12px;
  height: 7px;
  border-left: 2px solid #2f8f83;
  border-bottom: 2px solid #2f8f83;
  transform: rotate(-45deg);
}

.name-info-no::before {
  background: #fff4df;
  box-shadow: 0 0 0 1px #efd7a4 inset;
}

.name-info-no::after {
  content: '!';
  position: absolute;
  left: 12px;
  top: 6px;
  color: var(--brand-primary-strong);
  font-weight: 700;
}

.detail {
  color: var(--brand-muted);
  font-size: 13px;
  line-height: 21px;
}

.explain {
  text-align: right;

  .pathBtn,
  .btnColor {
    min-width: 78px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 999px;
    color: var(--brand-dark);
    background: var(--brand-primary);
    font-size: 13px;
    font-weight: 600;
  }

  .btnColor {
    color: var(--brand-muted);
    background: #f2eee3;
  }
}

@media (max-width: 760px) {
  .section {
    display: block;
    width: calc(100% - 24px);
  }

  .right-section {
    margin-top: 18px;
  }

  .account-info ul {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .explain {
    text-align: left;
  }
}
</style>

<template>
  <div class="app-container">
    <Header></Header>
    <div class="main">
      <div class="auth-shell">
        <section class="auth-brand">
          <img :src="logo" alt="潮声" class="brand-logo">
          <p class="eyebrow">潮声账号</p>
          <h1>找演出，去现场。</h1>
          <p class="brand-summary">登录后继续管理订单、购票人和观演偏好，入场信息会自动同步到你的账号。</p>
          <div class="brand-meta">
            <span>精选演出</span>
            <span>实名购票</span>
            <span>订单同步</span>
          </div>
        </section>
        <section class="auth-card">
          <div class="card-head">
            <h2>登录潮声</h2>
            <p>使用手机号或邮箱继续</p>
          </div>
          <el-tabs
              v-model="activeName"
              class="demo-tabs"
              @tab-click="handleClick"
          >
            <el-tab-pane label="密码登录" name="first">
              <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
                <div class="error-tips" v-if="isTips">
                  <WarningFilled class="error-icon"/>
                  {{ tipsContent }}
                </div>
                <el-input v-model="userName" placeholder="请输入手机号或邮箱" prop="userName">
                  <template #prepend>
                    <el-icon :size="30" color="#ffffff">
                      <User/>
                    </el-icon>
                  </template>
                </el-input>
                <el-input type="password" show-password v-model="loginForm.password" placeholder="请输入密码"
                          prop="password">
                  <template #prepend>
                    <el-icon :size="30" color="#ffffff">
                      <Lock/>
                    </el-icon>
                  </template>
                </el-input>
                <el-button
                    :loading="loading"
                    size="large"
                    type="primary"
                    class="btn"
                    @click.prevent="handleLogin"
                >
                  <span v-if="!loading">登 录</span>
                  <span v-else>登 录 中...</span>
                </el-button>
                <div class="form-actions">
                  <router-link v-if="register" class="register-link" to="/register">用户注册</router-link>
                </div>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="短信登录" name="second">
              <div class="empty-pane">暂未开放</div>
            </el-tab-pane>
            <el-tab-pane label="扫码登录" name="third">
              <div class="empty-pane">暂未开放</div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </div>
    <Footer></Footer>
  </div>
</template>

<script setup>
import logo from '@/assets/login/logo.png'
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import {isPhoneNumber, isEmailAddress} from '@/utils/index'
import {ref, getCurrentInstance} from 'vue'
import useUserStore from '@/store/modules/user'
import {useRouter} from 'vue-router'

const userStore = useUserStore()
const router = useRouter();
const loading = ref(false);
const activeName = ref('first')
// 注册开关
const register = ref(true);
const isTips = ref(false)
const tipsContent = ref('')
const {proxy} = getCurrentInstance();

const userName = ref('');
const loginForm = ref({
  email: '',
  mobile: '',
  password: '',
  code: '0001'//pc网站
})

const loginRules = ref({});


const handleClick = () => {}
const handleLogin = () => {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      if (userName.value == '') {
        isTips.value = true
        tipsContent.value = '请输入邮箱或者手机号'
      } else if (loginForm.value.password == '') {
        tipsContent.value = '请输入密码'
        isTips.value = true
      }else{
        if (!identifyType(userName.value)) {
          isTips.value = true
          tipsContent.value = '请输入正确的手机号或邮箱'
          return
        }
        isTips.value = false
        loading.value = true
        // 调用action的登录方法
        userStore.login(loginForm.value).then(() => {
          router.push({path: "/"});
        }).catch(() => {
          loading.value = false;
        });
      }

    }
  });
}


function identifyType(value) {
  if (isPhoneNumber(value)) {
    loginForm.value.mobile = value
    loginForm.value.email = ''
    return true;
  } else if (isEmailAddress(value)) {
    loginForm.value.email = value
    loginForm.value.mobile = ''
    return true;
  }
  return false
}

</script>

<style scoped lang="scss">
.app-container {
  width: 100%;
  min-height: 100%;
  background: var(--brand-page);

  .main {
    width: 100%;
    min-height: calc(100vh - 74px);
    padding: 46px 0 56px;
    background: linear-gradient(180deg, rgba(255, 253, 248, .78), rgba(245, 242, 234, .96));
  }

  .auth-shell {
    width: min(1120px, calc(100% - var(--layout-gutter)));
    min-height: 500px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 390px;
    gap: 44px;
    align-items: center;
  }

  .auth-brand {
    min-height: 460px;
    padding: 58px 64px;
    color: #fff;
    overflow: hidden;
    position: relative;
    border-radius: var(--radius-lg);
    background:
        linear-gradient(90deg, rgba(18, 17, 15, .86), rgba(18, 17, 15, .46)),
        url('@/assets/section/login-hero.png') center/cover;
    box-shadow: var(--brand-shadow);
  }

  .brand-logo {
    width: 68px;
    height: 68px;
    border-radius: 16px;
    object-fit: cover;
    box-shadow: 0 16px 36px rgba(0, 0, 0, .22);
  }

  .eyebrow {
    margin: 42px 0 12px;
    color: var(--brand-primary);
    font-size: 15px;
    font-weight: 700;
  }

  h1 {
    margin: 0;
    font-size: 44px;
    line-height: 1.16;
    color: #fff;
  }

  .brand-summary {
    width: 430px;
    margin: 22px 0 0;
    color: rgba(255, 255, 255, .72);
    font-size: 16px;
    line-height: 1.9;
  }

  .brand-meta {
    margin-top: 54px;
    display: flex;
    gap: 12px;

    span {
      height: 34px;
      line-height: 34px;
      padding: 0 16px;
      border-radius: 999px;
      color: rgba(255, 255, 255, .82);
      background: rgba(255, 255, 255, .08);
      border: 1px solid rgba(255, 255, 255, .12);
      font-size: 13px;
    }
  }

  .auth-card {
    padding: 34px 34px 28px;
    border-radius: var(--radius-lg);
    background: var(--brand-card);
    border: 1px solid var(--brand-line);
    box-shadow: var(--brand-shadow);
  }

  .card-head {
    margin-bottom: 24px;

    h2 {
      margin: 0;
      color: var(--brand-dark);
      font-size: 26px;
      line-height: 1.3;
    }

    p {
      margin: 8px 0 0;
      color: var(--brand-muted);
      font-size: 14px;
    }
  }

}

.form-actions {
  margin-top: 16px;
  text-align: right;
}

.register-link {
  color: var(--brand-primary-strong);
  font-size: 14px;
  font-weight: 700;
}

.empty-pane {
  height: 154px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-muted);
  font-size: 14px;
  background: #fffaf0;
  border: 1px dashed var(--brand-border);
  border-radius: 8px;
}

:deep(.demo-tabs > .el-tabs__content) {
  padding: 20px 0 0;
  color: var(--brand-muted);
  font-size: 14px;
}

:deep(.el-tabs__header) {
  margin: 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  height: 34px;
  padding: 0 18px;
  color: #5f6368;
  font-size: 15px;
}

:deep(.el-tabs__item.is-active) {
  color: var(--brand-primary-strong);
  font-weight: 700;
}

:deep(.el-tabs__active-bar) {
  background-color: var(--brand-primary-strong);
}

:deep(#pane-first) {
  width: 100%;
}

:deep(.el-input-group__prepend) {
  width: 44px;
  padding: 0;
  text-align: center;
  background-color: var(--brand-dark);
  border: none;
  box-shadow: none;
  border-radius: 8px 0 0 8px;
}

:deep(.el-input__wrapper) {
  height: 44px;
  box-shadow: 0 0 0 1px #e7e2ce inset;
  border-radius: 0 8px 8px 0;

  &:hover,
  &.is-focus {
    box-shadow: 0 0 0 1px var(--brand-primary-strong) inset;
  }
}

.el-input-group--prepend {
  border: none;
  height: 44px;
  outline: none;
  font-size: 14px;
  margin-bottom: 18px;
}

.btn {
  background-color: var(--brand-primary);
  background-image: linear-gradient(90deg, var(--brand-primary), var(--brand-primary-strong));
  border-color: var(--brand-primary);
  border-radius: 8px;
  font-size: 17px;
  font-weight: 800;
  height: 44px;
  line-height: 44px;
  outline: none;
  color: var(--brand-dark);
  width: 100%;
  cursor: pointer;
  margin-top: 2px;
}
.error-tips{
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #ff934c;
  background: #fefcee;
  margin-bottom: 16px;
  font-size: 14px;
  padding: 5px 8px;
  overflow: hidden;
  position: relative;
  z-index: 1001;
  text-align: left;
}

.error-icon {
  width: 1em;
  height: 1em;
  margin-left: 2px;
  flex: 0 0 auto;
  color: #ff934c;
}

@media (max-width: 1180px) {
  .app-container {
    .auth-shell {
      width: calc(100% - 48px);
      grid-template-columns: minmax(0, 1fr) 380px;
      gap: 28px;
    }
  }
}

@media (max-width: 860px) {
  .app-container {
    .main {
      padding: 24px 0 36px;
    }

    .auth-shell {
      width: calc(100% - 32px);
      display: block;
    }

    .auth-brand {
      min-height: auto;
      padding: 32px;
      margin-bottom: 18px;
    }

    h1 {
      font-size: 32px;
    }

    .brand-summary {
      width: auto;
    }

    .brand-meta {
      margin-top: 28px;
      flex-wrap: wrap;
    }
  }
}
</style>

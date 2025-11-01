<template>
  <div class="app-container">
    <Header></Header>
    <div class="main">
      <div class="auth-shell">
        <section class="auth-brand">
          <img :src="logo" alt="潮声" class="brand-logo">
          <p class="eyebrow">加入潮声</p>
          <h1>把下一场现场留给你。</h1>
          <p class="brand-summary">创建账号后可维护购票人、接收订单状态，并在下次购票时更快完成实名信息。</p>
          <div class="brand-meta">
            <span>手机号注册</span>
            <span>安全验证</span>
            <span>快速购票</span>
          </div>
        </section>
        <section class="auth-card">
          <div class="card-head">
            <h2>注册潮声</h2>
            <p>请使用常用手机号创建账号</p>
          </div>
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top" class="register-form">
              <el-form-item label="手机号码" prop="mobile">
                <el-input v-model="registerForm.mobile" class="input-with-select" maxlength="11">
                  <template #prepend>
                    <el-select v-model="select" placeholder="区号" class="area-select">
                      <el-option label="中国大陆 +86" value="1"/>
                    </el-select>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="输入密码" prop="password">
                <el-input
                    v-model="registerForm.password"
                    class="input-with-select"
                    type="password"
                >
                </el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                    v-model="registerForm.confirmPassword"
                    class="input-with-select"
                    type="password"
                >
                </el-input>
              </el-form-item>
              <el-form-item :style="chkStyle" class="agreement-item">
                <el-checkbox v-model="checkBox" @change="boxChange"/>
                <span class="chx">{{ agreeOpt }}</span>
              </el-form-item>
              <el-button
                  size="large"
                  type="primary"
                  class="btn"
                  @click.prevent="handleAgreeLogin"
              >
                <span>同意并注册</span>
              </el-button>
              <div class="form-actions">
                <router-link class="login-link" to="/login">已有账号，去登录</router-link>
              </div>
          </el-form>
        </section>
      </div>
    </div>
    <Verify
        mode="pop"
        :captchaType="captchaType"
        :imgSize="{width:'400px',height:'200px'}"
        ref="verify"
        @update:value="handleValueFromChild"

    ></Verify>
    <Footer></Footer>
  </div>
</template>

<script setup>
import Header from '@/components/header/index'
import Footer from '@/components/footer/index'
import Verify from '@/components/verifition/Verify'
import logo from '@/assets/login/logo.png'
import {ref, reactive} from 'vue'
import {isCaptcha, register} from '@/api/login'
import {getCurrentInstance} from 'vue'
import {ElMessage} from 'element-plus'
import {useRouter} from 'vue-router'
import $bus from '../utils/bus'

const {proxy} = getCurrentInstance();
const router = useRouter();

const code = ref('86')
const select = ref('1')
const agreeOpt = ref('我已阅读接受《潮声会员服务协议》《隐私权政策》《订票服务条款》并同意自动注册成为会员')
const checkBox = ref(false)
const chkStyle = ref({})
const registerForm = ref({
  password: '',
  confirmPassword: '',
  captchaId: '',
  mobile: ''
})
//手机号校验
const validatePhone = (rule, value, callback) => {
  const reg = /^1[3-9]\d{9}$/;
  if (!value) {
    return callback(new Error('手机号码不能为空'));
  } else if (!reg.test(value)) {
    return callback(new Error('请输入正确的手机号码'));
  } else {
    callback();
  }
};
//密码校验
const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};
const registerRules = reactive({
  mobile: [{required: true, trigger: "blur", validator: validatePhone}],
  password: [{
    required: true,
    pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$)([^\u4e00-\u9fa5\s]){6,20}$/,
    message: '6-20位英文字母、数字或者符号（除空格），且字母、数字和标点符号至少包含两种',
    trigger: ['blur', 'focus']
  }],
  confirmPassword: [
    {required: true, trigger: "blur", message: "请再次输入您的密码"},
    {required: true, validator: equalToPassword, trigger: "blur"}
  ],
});


/**检查是否需要验证码：如果true需要验证码，如果是false就不需要验证码
 * 获取验证码id，注册用
 */
function handleAgreeLogin() {
  isCaptcha().then(response => {
    let {verifyCaptcha, captchaId} = response.data
    if (verifyCaptcha == false) {
      //如果是false直接登录，如果是true验证码
      registerForm.value.captchaId = captchaId
      //此处勾选协议接口为传参，前端进行校验
      if (checkBox.value == false) {
        chkStyle.value = {color: 'red'}
      } else {
        chkStyle.value = {color: '#666666'}

        registerInfo()
      }
    } else {
      if (checkBox.value == false) {
        chkStyle.value = {color: 'red'}
      } else {
        chkStyle.value = {color: '#666666'}
        proxy.$refs.registerRef.validate(valid => {
          if (valid) {
            onShow('blockPuzzle')
          }
        })
      }

    }
  })
}

function boxChange(val) {
  if (val == true) {
    chkStyle.value = {color: '#666666'}
  }
}

function registerInfo() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      //去掉86区号
      //registerForm.value.mobile = code.value + registerForm.value.mobile
      register(registerForm.value).then(response => {
        if ( response.code == '0') {
          ElMessage({
            message: '注册成功',
            type: 'success',
          })
          router.push({path: "./login"});
          reset()

        }
      }).catch(() => {

      });
    }

  })

}


function reset() {
  registerForm.value = {
    password: '',
    confirmPassword: '',
    captchaId: '',
    mobile: ''
  }
}

//认证
const verify = ref(null)
const captchaType = ref('')
//滑块为例
//
const onShow = (type) => {
  captchaType.value = type
  verify.value.show()
}
let captchaVerify= ref('')


$bus.on('res', (data) => {
  captchaVerify.value = data.repData.captchaVerification
})


//此处是关闭验证码后，提示注册成功跳转到登录页面
function handleValueFromChild(value) {
  if (value == '关闭') {
    registerForm.value.captchaVerification =captchaVerify.value
    isCaptcha().then(res => {
      let {captchaId} = res.data
      registerForm.value.captchaId = captchaId
      //去掉86区号
      //registerForm.value.mobile = code.value + registerForm.value.mobile
      register(registerForm.value).then(response => {
        if (response.code == '0'&&response.data === true) {
          ElMessage({
            message: '注册成功',
            type: 'success',
          })
          router.push({path: "./login"});
          reset()
        }else{
          ElMessage({
            message:response.message,
            type: 'error',
          })
        }
      }).catch(() => {

      });
    })
  }
}
</script>

<style scoped lang="scss">
.app-container {
  width: 100%;
  min-height: 100%;
  background: var(--brand-page);

  .main {
    min-height: calc(100vh - 74px);
    padding: 46px 0 56px;
    background: linear-gradient(180deg, rgba(255, 253, 248, .78), rgba(245, 242, 234, .96));
  }

  .auth-shell {
    width: min(1120px, calc(100% - var(--layout-gutter)));
    min-height: 500px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 430px;
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

  .register-form {
    color: #666666;
  }
}

:deep(.el-form-item__label) {
  color: var(--brand-text);
  font-weight: 700;
  line-height: 22px;
  padding-bottom: 8px;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  box-shadow: 0 0 0 1px #e7e2ce inset;
  border-radius: 8px;

  &:hover,
  &.is-focus {
    box-shadow: 0 0 0 1px var(--brand-primary-strong) inset;
  }
}

:deep(.el-input-group__prepend) {
  padding: 0 10px;
  background: #fffaf0;
  border: none;
  box-shadow: 0 0 0 1px #e7e2ce inset;
  border-radius: 8px 0 0 8px;
}

:deep(.el-input-group--prepend .el-input__wrapper) {
  border-radius: 0 8px 8px 0;
}

:deep(.area-select) {
  width: 145px;
}

.agreement-item {
  margin: 4px 0 20px;

  :deep(.el-form-item__content) {
    align-items: flex-start;
    line-height: 1.6;
  }
}

.chx {
  width: calc(100% - 28px);
  display: inline-block;
  color: var(--brand-muted);
  font-size: 13px;
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
}

.form-actions {
  margin-top: 16px;
  text-align: right;
}

.login-link {
  color: var(--brand-primary-strong);
  font-size: 14px;
  font-weight: 700;
}

.el-form-item--default {
  margin-bottom: 18px;
}

@media (max-width: 1180px) {
  .app-container {
    .auth-shell {
      width: calc(100% - 48px);
      grid-template-columns: minmax(0, 1fr) 410px;
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

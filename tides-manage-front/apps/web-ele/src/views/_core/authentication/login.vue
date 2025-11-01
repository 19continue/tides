
<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import { computed, ref } from 'vue';
import { AuthenticationLogin, z } from '@vben/common-ui';
import { $t } from '@vben/locales';
import { useAuthStore } from '#/store';
import type { Recordable } from '@vben/types';
import { ElButton, ElDialog, ElNotification } from 'element-plus';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const isJavaup = window.location.hostname.includes('javaup.chat');
const formSchema = computed((): VbenFormSchema[] => [
  {
    component: 'VbenInput',
    componentProps: {
      placeholder: $t('authentication.usernameTip'),
    },
    fieldName: 'username',
    // 根据域名切换默认值
    defaultValue: isJavaup ? '' : 'admin',
    label: $t('authentication.username'),
    rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
  },
  {
    component: 'VbenInputPassword',
    componentProps: {
      placeholder: $t('authentication.password'),
    },
    fieldName: 'password',
    // 根据域名切换默认值
    defaultValue: isJavaup ? '' : 'admin',
    label: $t('authentication.password'),
    rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
  },
]);

const dialogVisible = ref(false);

const loginFn = (values: Recordable<any>) => {
  authStore
    .authLogin(values)
    .then((success) => {
      if (!success?.userInfo) {
        ElNotification({
          message: '登录失败，请重试',
          type: 'error',
          title: '错误',
        });
      }
    })
    .catch((error) => {
      console.error('登录失败:', error);
      ElNotification({
        message: '登录失败，请稍后重试',
        type: 'error',
        title: '错误',
      });
    });
};

const handleClose = () => {
  dialogVisible.value = false;
};

const getImgSrc = () => {
  return new URL('../../../asstes/imgs/wechatOfficialAccount.jpg', import.meta.url).href;
};

const openDialog = () => {
  dialogVisible.value = true;
};
// 暴露到模板用于条件渲染
const exposed = { isJavaup };
</script>

<template>
  <div>
    <AuthenticationLogin
      :form-schema="formSchema"
      :loading="authStore.loginLoading"
      :showForgetPassword="false"
      :showRegister="false"
      :showRememberMe="false"
      @submit="loginFn"
    />

    <div class="help-tip" v-if="exposed.isJavaup">
      <el-button type="primary" link class="help-link" @click="openDialog">如何获取账号与密码</el-button>
    </div>

    <el-dialog
      v-if="exposed.isJavaup"
      v-model="dialogVisible"
      title=""
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      append-to-body
    >
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 4px;">
          <span style="font-size:16px;font-weight:600;">关注公众号后继续登录</span>
          <el-button type="text" @click="handleClose">关闭</el-button>
        </div>
      </template>

      <div style="text-align:center;">
        <p style="color:#555;margin-bottom:10px;">请先扫码关注，回复“<span class="code">chaosheng</span>”获取账号与密码</p>
        <img :src="getImgSrc()" alt="公众号二维码" style="width:240px;height:240px;border:1px solid #eee;border-radius:4px;display:block;margin:0 auto;" />
      </div>

      <template #footer>
        <span style="display:flex;justify-content:flex-end;gap:12px;">
          <el-button type="primary" @click="handleClose">我已关注，知道了</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 验证提示文本样式复用 */
.code {
  color: #d00d55; /* 深红色，突出显示验证码 */
  font-size: 16px; /* 字体大小 */
  font-weight: bold; /* 字体加粗 */
  margin: 0 2px; /* 左右各2px的外边距 */
}

.help-tip {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.help-link {
  font-size: 21px;
  font-weight: 600;
  padding: 4px 8px;
}
</style>
